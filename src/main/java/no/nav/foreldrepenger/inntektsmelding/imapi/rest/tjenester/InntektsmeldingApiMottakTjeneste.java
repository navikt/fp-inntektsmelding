package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SendInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

@ApplicationScoped
public class InntektsmeldingApiMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;

    InntektsmeldingApiMottakTjeneste() {
        //CDI
    }

    @Inject
    public InntektsmeldingApiMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                            FellesMottakTjeneste fellesMottakTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
    }

    public SendInntektsmeldingResponse mottaInntektsmelding(InntektsmeldingDto inntektsmelding, UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).orElse(null);
        if (forespørsel == null) {
            LOG.info("Finner ikke forespørsel for uuid {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false, null, "Finner ikke forespørsel for uuid " + forespørselUuid);
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            LOG.info("Forespørsel har status utgått, og inntektsmelding kan ikke mottas. forespørselUuid: {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false, null,"Kan ikke sende inn inntektsmelding når Forespørsel har status forkastet.");
        }

        var sisteInntektsmelding = inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørsel.uuid());
        if (sisteInntektsmelding != null && inntektsmeldingerErLike(inntektsmelding, sisteInntektsmelding)) {
            LOG.info("Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding. inntektsmeldingId: {}", inntektsmelding.getId());
            return new SendInntektsmeldingResponse(false, null,
                "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding.");
        }

        //todo skal vi ha en trycath slik at ting ikke feiler ut?
        var lagretIm = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmelding, forespørsel);
        fellesMottakTjeneste.behandlerForespørsel(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        //Todo hente beregnet inntekt fra inntektskomponenten og dersom den er ulik og ikke har endringsårsak skal den settes til status forkastet - må sende inn på nytt
        //todo avklare skal vi sjekke uansett om endringsårsak er oppgitt eller ikke?
        //Todo Avklaring: Hva skal vi gjøre om inntektskomponenten er nede og vi ikke får sjekket dette? La de sende inn, men sette til status forkastet med forklaring?

        return new SendInntektsmeldingResponse(true, lagretIm.getInntektsmeldingUuid(),"Inntektsmelding mottatt");
    }

    private boolean inntektsmeldingerErLike(InntektsmeldingDto nyInntektsmelding, InntektsmeldingDto tidligereInntektsmelding) {
        return Objects.equals(nyInntektsmelding.getStartdato(), tidligereInntektsmelding.getStartdato())
            && Objects.equals(nyInntektsmelding.getKontaktperson().navn(), tidligereInntektsmelding.getKontaktperson().navn())
            && Objects.equals(nyInntektsmelding.getKontaktperson().telefonnummer(), tidligereInntektsmelding.getKontaktperson().telefonnummer())
            && erMånedInntektLike(nyInntektsmelding.getMånedInntekt(),tidligereInntektsmelding.getMånedInntekt())
            && refusjonsendringerErLike(nyInntektsmelding.getSøkteRefusjonsperioder(), tidligereInntektsmelding.getSøkteRefusjonsperioder())
            && naturalytelserErLike(nyInntektsmelding.getBortfaltNaturalytelsePerioder(), tidligereInntektsmelding.getBortfaltNaturalytelsePerioder())
            && Objects.equals(nyInntektsmelding.getYtelse(), tidligereInntektsmelding.getYtelse())
            && Objects.equals(nyInntektsmelding.getOpphørsdatoRefusjon(), tidligereInntektsmelding.getOpphørsdatoRefusjon())
            && endringsårsakerErLike(nyInntektsmelding.getEndringAvInntektÅrsaker(), tidligereInntektsmelding.getEndringAvInntektÅrsaker());
    }

    private boolean erMånedInntektLike(BigDecimal månedInntekt, BigDecimal månedInntekt1) {
        return månedInntekt.compareTo(månedInntekt1) == 0;
    }


    private boolean refusjonsendringerErLike(List<InntektsmeldingDto.SøktRefusjon> nyListe,
                                             List<InntektsmeldingDto.SøktRefusjon> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean naturalytelserErLike(List<InntektsmeldingDto.BortfaltNaturalytelse> nyListe,
                                         List<InntektsmeldingDto.BortfaltNaturalytelse> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean endringsårsakerErLike(List<InntektsmeldingDto.Endringsårsaker> nyListe,
                                          List<InntektsmeldingDto.Endringsårsaker> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }
}
