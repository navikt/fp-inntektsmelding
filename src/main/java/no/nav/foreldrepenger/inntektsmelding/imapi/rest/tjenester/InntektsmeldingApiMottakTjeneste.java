package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollResultat;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste;
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
    private InntektKontrollTjeneste inntektKontrollTjeneste;

    InntektsmeldingApiMottakTjeneste() {
        //CDI
    }

    @Inject
    public InntektsmeldingApiMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                            FellesMottakTjeneste fellesMottakTjeneste,
                                            InntektKontrollTjeneste inntektKontrollTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.inntektKontrollTjeneste = inntektKontrollTjeneste;
    }

    public SendInntektsmeldingResponse mottaInntektsmelding(InntektsmeldingDto inntektsmelding, UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørselOptional(forespørselUuid).orElse(null);
        if (forespørsel == null) {
            LOG.info("Finner ikke forespørsel for uuid {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.TOM_FORESPOERSEL, "Finner ikke forespørsel for uuid " + forespørselUuid,
                    forespørselUuid.toString()));
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            LOG.info("Forespørsel har status utgått. Inntektsmelding kan ikke mottas. forespørselUuid: {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.UGYLDIG_FORESPOERSEL,
                    "Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel",
                    forespørselUuid.toString()));
        }

        var sisteInntektsmelding = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørsel.uuid());
        if (sisteInntektsmelding != null && inntektsmeldingerErLike(inntektsmelding, sisteInntektsmelding)) {
            LOG.info(
                "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding. inntektsmeldingId: {}",
                inntektsmelding.getId());
            return new SendInntektsmeldingResponse(false, null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.DUPLIKAT,
                    "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding med id: "
                        + sisteInntektsmelding.getInntektsmeldingUuid(),
                    sisteInntektsmelding.getInntektsmeldingUuid().toString()));
        }

        fellesMottakTjeneste.settForrigeInntektsmeldingUtdatertHvisVenterVurdering(forespørsel);

        var sendInntektsmeldingResponse = sjekkMånedInntektMotRapportertInntekt(forespørsel, inntektsmelding);
        // Både avvisning (ulik inntekt) og nedetid i a-inntekt (venter vurdering) er allerede ferdigbehandlet av
        // sjekkMånedInntektMotRapportertInntekt og gir feilinformasjon tilbake - da skal vi ikke fortsette videre.
        if (sendInntektsmeldingResponse.feilinformasjon() != null) {
            return sendInntektsmeldingResponse;
        }

        var lagretIm = fellesMottakTjeneste.lagreImOgOpprettJournalførTask(inntektsmelding, forespørsel);
        fellesMottakTjeneste.ferdigstillOgOppdaterEksterneSystemer(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);
        return new SendInntektsmeldingResponse(true, lagretIm.getInntektsmeldingUuid(), InntektsmeldingKontraktMapper.mapTilKontrakt(lagretIm.getStatus()), null);
    }

    private SendInntektsmeldingResponse sjekkMånedInntektMotRapportertInntekt(ForespørselDto forespørsel, InntektsmeldingDto inntektsmelding) {
        return switch (inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørsel, inntektsmelding)) {
            case InntektKontrollResultat.Godkjent _ -> new SendInntektsmeldingResponse(true, null, null, null);
            case InntektKontrollResultat.Nedetid(var melding) -> {
                var inntektsmeldingMedStatus = InntektsmeldingDto.builder(inntektsmelding).medStatus(InntektsmeldingStatus.VENTER_VURDERING).build();
                var lagretInntektsmelding = fellesMottakTjeneste.lagreImOgOpprettTaskForEtterkontroll(inntektsmeldingMedStatus, forespørsel);
                MetrikkerTjeneste.loggInnsendtInntektsmeldingUnderNedetid();
                yield new SendInntektsmeldingResponse(true,
                    lagretInntektsmelding.getInntektsmeldingUuid(),
                    InntektsmeldingStatusDto.VENTER_VURDERING,
                    new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.NEDETID_AINNTEKT, melding, forespørsel.uuid().toString()));
            }
            case InntektKontrollResultat.UlikInntekt(var feilmelding, _) -> new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.ULIK_INNTEKT, feilmelding, forespørsel.uuid().toString()));
        };
    }

    private boolean inntektsmeldingerErLike(InntektsmeldingDto nyInntektsmelding, InntektsmeldingDto tidligereInntektsmelding) {
        return Objects.equals(nyInntektsmelding.getStartdato(), tidligereInntektsmelding.getStartdato())
            && Objects.equals(nyInntektsmelding.getKontaktperson(), tidligereInntektsmelding.getKontaktperson())
            && erBeløpLike(nyInntektsmelding.getMånedInntekt(),tidligereInntektsmelding.getMånedInntekt())
            && erBeløpLike(nyInntektsmelding.getMånedRefusjon(),tidligereInntektsmelding.getMånedRefusjon())
            && refusjonsendringerErLike(nyInntektsmelding.getSøkteRefusjonsperioder(), tidligereInntektsmelding.getSøkteRefusjonsperioder())
            && naturalytelserErLike(nyInntektsmelding.getBortfaltNaturalytelsePerioder(), tidligereInntektsmelding.getBortfaltNaturalytelsePerioder())
            && Objects.equals(nyInntektsmelding.getYtelse(), tidligereInntektsmelding.getYtelse())
            && Objects.equals(nyInntektsmelding.getOpphørsdatoRefusjon(), tidligereInntektsmelding.getOpphørsdatoRefusjon())
            && endringsårsakerErLike(nyInntektsmelding.getEndringAvInntektÅrsaker(), tidligereInntektsmelding.getEndringAvInntektÅrsaker());
    }

    private boolean erBeløpLike(BigDecimal beløp1, BigDecimal beløp2) {
        if (beløp1 == null || beløp2 == null) {
            return beløp1 == null && beløp2 == null;
        }
        return beløp1.compareTo(beløp2) == 0;
    }

    private boolean refusjonsendringerErLike(List<InntektsmeldingDto.SøktRefusjon> nyListe,
                                             List<InntektsmeldingDto.SøktRefusjon> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean naturalytelserErLike(List<InntektsmeldingDto.BortfaltNaturalytelse> nyListe,
                                         List<InntektsmeldingDto.BortfaltNaturalytelse> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean endringsårsakerErLike(List<InntektsmeldingDto.Endringsårsak> nyListe,
                                          List<InntektsmeldingDto.Endringsårsak> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }
}
