package no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.ekstern.SendInntektsmeldingEksternRequest;
import no.nav.familie.inntektsmelding.imdialog.tjenester.FellesMottakTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;

@ApplicationScoped
public class EksternInntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(EksternInntektsmeldingMottakTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;
    private PersonTjeneste personTjeneste;

    EksternInntektsmeldingMottakTjeneste() {
        //CDI
    }

    @Inject
    public EksternInntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                                InntektsmeldingTjeneste inntektsmeldingTjeneste, FellesMottakTjeneste fellesMottakTjeneste,
                                                PersonTjeneste personTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.personTjeneste = personTjeneste;
    }

    public EksternInntektsmeldingResponseDto mottaEksternInntektsmelding(SendInntektsmeldingEksternRequest eksternRequest) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(eksternRequest.foresporselUuid()).orElse(null);
        if (forespørselEntitet == null) {
            LOG.error("Finner ikke forespørsel for uuid {}", eksternRequest.foresporselUuid());
            return new EksternInntektsmeldingResponseDto(false, Optional.empty(), "Finner ikke forespørsel for uuid " + eksternRequest.foresporselUuid());
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            return new EksternInntektsmeldingResponseDto(false, Optional.empty(),"Kan ikke sende inn inntektsmelding når Forespørsel har status forkastet.");
        }

        var aktørId = Optional.ofNullable(eksternRequest.fødselsnummer())
            .flatMap(ident -> personTjeneste.finnAktørIdForIdent(new PersonIdent(ident)));
        if (aktørId.isEmpty()) {
            SECURE_LOG.error("Finner ikke aktørId for fødselsnummer {}", eksternRequest.fødselsnummer());
            return new EksternInntektsmeldingResponseDto(false, Optional.empty(),
                "Finner ikke informasjon for fødselsnummer. Sjekk at fødselsnummer er korrekt");
        }

        var nyInntektsmelding = InntektsmeldingEksternMapper.mapTilEntitet(eksternRequest, aktørId.get());
        var sisteInntektsmeldingForForespørsel = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselEntitet.getUuid()).stream()
            .max(Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt));

        if (sisteInntektsmeldingForForespørsel.isPresent() &&
            inntektsmeldingerErLike(nyInntektsmelding, sisteInntektsmeldingForForespørsel.get())) {
            return new EksternInntektsmeldingResponseDto(false, Optional.empty(),
                "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding.");
        }

        //todo skal vi ha en trycath slik at ting ikke feiler ut?
        var lagretIm = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(nyInntektsmelding, forespørselEntitet);
        fellesMottakTjeneste.behandlerForespørsel(forespørselEntitet, lagretIm.getUuid());

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        //Todo hente beregnet inntekt fra inntektskomponenten og dersom den er ulik og ikke har endringsårsak skal den settes til status forkastet - må sende inn på nytt
        //todo avklare skal vi sjekke uansett om endringsårsak er oppgitt eller ikke?
        //Todo Avklaring: Hva skal vi gjøre om inntektskomponenten er nede og vi ikke får sjekket dette? La de sende inn, men sette til status forkastet med forklaring?

        return new EksternInntektsmeldingResponseDto(true, lagretIm.getUuid(),"Inntektsmelding mottatt, og brukes i saksbehandling.");

    }

    private boolean inntektsmeldingerErLike(InntektsmeldingEntitet nyInntektsmelding, InntektsmeldingEntitet tidligereInntektsmelding) {
        return Objects.equals(nyInntektsmelding.getStartDato(), tidligereInntektsmelding.getStartDato())
            && Objects.equals(nyInntektsmelding.getKontaktperson().getNavn(), tidligereInntektsmelding.getKontaktperson().getNavn())
            && Objects.equals(nyInntektsmelding.getKontaktperson().getTelefonnummer(), tidligereInntektsmelding.getKontaktperson().getTelefonnummer())
            && erMånedInntektLike(nyInntektsmelding.getMånedInntekt(),tidligereInntektsmelding.getMånedInntekt())
            && refusjonsendringerErLike(nyInntektsmelding.getRefusjonsendringer(), tidligereInntektsmelding.getRefusjonsendringer())
            && naturalytelserErLike(nyInntektsmelding.getBorfalteNaturalYtelser(), tidligereInntektsmelding.getBorfalteNaturalYtelser())
            && Objects.equals(nyInntektsmelding.getYtelsetype(), tidligereInntektsmelding.getYtelsetype())
            && Objects.equals(nyInntektsmelding.getOpphørsdatoRefusjon(), tidligereInntektsmelding.getOpphørsdatoRefusjon())
            && endringsårsakerErLike(nyInntektsmelding.getEndringsårsaker(), tidligereInntektsmelding.getEndringsårsaker());
    }

    private boolean erMånedInntektLike(BigDecimal månedInntekt, BigDecimal månedInntekt1) {
        if (månedInntekt == null && månedInntekt1 == null) {
            return true;
        }
        if (månedInntekt == null || månedInntekt1 == null) {
            return false;
        }
        return månedInntekt.compareTo(månedInntekt1) == 0;
    }


    private boolean refusjonsendringerErLike(List<RefusjonsendringEntitet> nyListe,
                                             List<RefusjonsendringEntitet> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean naturalytelserErLike(List<BortaltNaturalytelseEntitet> nyListe,
                                         List<BortaltNaturalytelseEntitet> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean endringsårsakerErLike(List<no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet> nyListe,
                                          List<no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }
}
