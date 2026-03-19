package no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.ekstern.SendInntektsmeldingEksternRequest;
import no.nav.familie.inntektsmelding.imdialog.tjenester.FellesMottakTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class EksternInntektsmeldingMottakTjenesteTest {

    private EksternInntektsmeldingMottakTjeneste eksternInntektsmeldingMottakTjeneste;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FellesMottakTjeneste fellesMottakTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;

    @BeforeEach
    void setup() {
        eksternInntektsmeldingMottakTjeneste = new EksternInntektsmeldingMottakTjeneste(forespørselBehandlingTjeneste, inntektsmeldingTjeneste, fellesMottakTjeneste, personTjeneste);
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_ikke_finnes() {
        var foresporselUuid = UUID.randomUUID();
        var request = lagRequest(foresporselUuid, null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.empty());

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(request);

        assertThat(response.success()).isFalse();
        assertThat(response.inntektsmeldingUuid()).isEmpty();
        assertThat(response.melding()).contains("Finner ikke forespørsel for uuid");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_er_utgått() {
        var foresporselUuid = UUID.randomUUID();
        var foresporsel = lagForesporsel(foresporselUuid, null);
        foresporsel.setStatus(ForespørselStatus.UTGÅTT);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(foresporsel));

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(lagRequest(foresporselUuid, null));

        assertThat(response.success()).isFalse();
        assertThat(response.melding()).contains("status forkastet");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_returnere_feilrespons_nar_aktørid_ikke_finnes_for_fødselsnummer() {
        var foresporselUuid = UUID.randomUUID();
        var foresporsel = lagForesporsel(foresporselUuid, null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(foresporsel));
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.empty());

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(lagRequest(foresporselUuid, null));

        assertThat(response.success()).isFalse();
        assertThat(response.inntektsmeldingUuid()).isEmpty();
        assertThat(response.melding()).isEqualTo("Finner ikke informasjon for fødselsnummer. Sjekk at fødselsnummer er korrekt");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_inntektsmelding_er_ny() {
        var foresporselUuid = UUID.randomUUID();
        var request = lagRequest(foresporselUuid, null);
        var foresporsel = lagForesporsel(foresporselUuid,null);
        var lagretIm = lagInntektsmelding(null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(foresporsel));
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.of(new AktørId("1234567891011")));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(foresporselUuid)).thenReturn(List.of());
        when(fellesMottakTjeneste.lagreOgJournalførInntektsmelding(any(), any())).thenReturn(lagretIm);

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(request);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).contains(lagretIm.getUuid().orElseThrow());
        verify(fellesMottakTjeneste).behandlerForespørsel(foresporsel, lagretIm.getUuid());
    }

    @Test
    void skal_avvise_semantisk_like_inntektsmeldinger() {
        var foresporselUuid = UUID.randomUUID();
        var request = lagRequest(foresporselUuid, null);
        var foresporsel = lagForesporsel(foresporselUuid, null);
        var tidligereLiktInnhold = lagInntektsmelding(null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(foresporsel));
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.of(new AktørId("1234567891011")));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(foresporselUuid)).thenReturn(List.of(tidligereLiktInnhold));

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(request);

        assertThat(response.success()).isFalse();
        assertThat(response.melding()).contains("Ingen endring på ny inntektsmelding");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_endring() {
        var foresporselUuid = UUID.randomUUID();
        var nyStardato = LocalDate.now();
        var request = lagRequest(foresporselUuid, nyStardato);
        var foresporsel = lagForesporsel(foresporselUuid,nyStardato);
        var forrigeInnsendteIm = lagInntektsmelding(null);
        var nyInnsendtIm = lagInntektsmelding(nyStardato);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(foresporsel));
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.of(new AktørId("1234567891011")));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(foresporselUuid)).thenReturn(List.of(forrigeInnsendteIm));
        when(fellesMottakTjeneste.lagreOgJournalførInntektsmelding(any(), any())).thenReturn(nyInnsendtIm);

        var response = eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(request);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).contains(nyInnsendtIm.getUuid().orElseThrow());
        verify(fellesMottakTjeneste).behandlerForespørsel(foresporsel, nyInnsendtIm.getUuid());
    }

    private static ForespørselEntitet lagForesporsel(UUID uuid, LocalDate nyStardato) {
        var stardato = nyStardato == null ? LocalDate.of(2026, 1, 10) : nyStardato;
        var foresporsel = new ForespørselEntitet(
            "999999999",
            LocalDate.of(2026, 1, 1),
            new AktørIdEntitet("1234567891011"),
            Ytelsetype.FORELDREPENGER,
            "SAK-1",
            stardato,
            ForespørselType.BESTILT_AV_FAGSYSTEM
        );
        // Keep request and foresporsel UUID aligned for the test scenario.
        try {
            var uuidField = ForespørselEntitet.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(foresporsel, uuid);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Klarte ikke sette test-uuid", e);
        }
        return foresporsel;
    }

    private static SendInntektsmeldingEksternRequest lagRequest(UUID foresporselUuid, LocalDate nyStardato) {
        var startdato = nyStardato == null ? LocalDate.of(2026, 1, 10) : nyStardato;
        return new SendInntektsmeldingEksternRequest(
            foresporselUuid,
            "12345678901",
            new OrganisasjonsnummerDto("999999999"),
            startdato,
            SendInntektsmeldingEksternRequest.YtelseTypeRequest.FORELDREPENGER,
            new SendInntektsmeldingEksternRequest.KontaktpersonRequest("Kontakt Person", "12345678"),
            BigDecimal.valueOf(45000),
            List.of(
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato, BigDecimal.valueOf(10000)),
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato.plusDays(5), BigDecimal.valueOf(9000)),
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato.plusDays(10), BigDecimal.ZERO)
            ),
            List.of(new SendInntektsmeldingEksternRequest.BortfaltNaturalytelseRequest(
                startdato.plusDays(2),
                null,
                NaturalytelsetypeDto.BIL,
                BigDecimal.valueOf(1200)
            )),
            List.of(new SendInntektsmeldingEksternRequest.EndringsårsakerRequest(
                EndringsårsakDto.TARIFFENDRING,
                null,
                null,
                startdato.plusDays(1)
            )),
            new SendInntektsmeldingEksternRequest.AvsenderSystemRequest("test-lps", "1.0.0", LocalDateTime.of(2026, 1, 2, 10, 30))
        );
    }

    private static InntektsmeldingEntitet lagInntektsmelding( LocalDate startdatoRequest) {
        var startdato = startdatoRequest == null ? LocalDate.of(2026, 1, 10) : startdatoRequest;
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("1234567891011"))
            .medArbeidsgiverIdent("999999999")
            .medStartDato(startdato)
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new KontaktpersonEntitet("Kontakt Person", "12345678"))
            .medMånedInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(10000))
            .medRefusjonOpphørsdato(startdato.plusDays(9))
            .medRefusjonsendringer(List.of(new RefusjonsendringEntitet(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelser(List.of(
                BortaltNaturalytelseEntitet.builder()
                    .medPeriode(startdato.plusDays(2), Tid.TIDENES_ENDE)
                    .medType(NaturalytelseType.BIL)
                    .medMånedBeløp(BigDecimal.valueOf(1200))
                    .build()
            ))
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder()
                    .medÅrsak(Endringsårsak.TARIFFENDRING)
                    .medBleKjentFra(startdato.plusDays(1))
                    .build()
            ))
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medLpsSystemNavn("test-lps")
            .medLpsSystemVersjon("1.0.0")
            .medOpprettetTidspunkt(LocalDateTime.now())
            .build();
    }
}
