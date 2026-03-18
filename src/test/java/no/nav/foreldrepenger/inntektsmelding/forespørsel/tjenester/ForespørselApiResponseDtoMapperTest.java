package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

class ForespørselApiResponseDtoMapperTest {

    private static final String ORGNR = "999999999";
    private static final Arbeidsgiver ARBEIDSGIVER = new Arbeidsgiver(ORGNR);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2026, 3, 1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2026, 4, 1);
    private static final String AKTØR_ID_VERDI = "1234567890123";
    private static final AktørIdEntitet AKTØR_ID_ENTITET = new AktørIdEntitet(AKTØR_ID_VERDI);
    private static final AktørId AKTØR_ID = AktørId.fra(AKTØR_ID_VERDI);
    private static final String FAGSAK_SAKSNUMMER = "SAK123";

    @Test
    void skal_mappe_alle_felter_for_bestilt_av_fagsystem() {
        var entitet = new ForespørselEntitet(
            ORGNR,
            SKJÆRINGSTIDSPUNKT,
            AKTØR_ID_ENTITET,
            Ytelsetype.FORELDREPENGER,
            FAGSAK_SAKSNUMMER,
            FØRSTE_UTTAKSDATO,
            ForespørselType.BESTILT_AV_FAGSYSTEM
        );

        var dto = ForespørselDtoMapper.mapFraEntitet(entitet);
        var expectedSaksnummer = Saksnummer.fra(FAGSAK_SAKSNUMMER);

        assertThat(dto.uuid()).isEqualTo(entitet.getUuid());
        assertThat(dto.arbeidsgiver()).isEqualTo(ARBEIDSGIVER);
        assertThat(dto.aktørId()).isEqualTo(AKTØR_ID);
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.BESTILT_AV_FAGSYSTEM);
        assertThat(dto.skjæringstidspunkt()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(dto.førsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
        assertThat(dto.fagsystemSaksnummer()).isEqualTo(expectedSaksnummer);
        assertThat(dto.opprettetTidspunkt()).isNotNull();
        assertThat(dto.oppgaveId()).isNull();
        assertThat(dto.arbeidsgiverNotifikasjonSakId()).isNull();
        assertThat(dto.dialogportenUuid()).isNull();
    }

    @Test
    void skal_mappe_med_null_skjæringstidspunkt_for_arbeidsgiverinitiert_nyansatt() {
        var entitet = new ForespørselEntitet(
            ORGNR,
            null,
            AKTØR_ID_ENTITET,
            Ytelsetype.FORELDREPENGER,
            null,
            FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT
        );

        var dto = ForespørselDtoMapper.mapFraEntitet(entitet);

        assertThat(dto.uuid()).isNotNull();
        assertThat(dto.arbeidsgiver()).isEqualTo(ARBEIDSGIVER);
        assertThat(dto.aktørId()).isEqualTo(AKTØR_ID);
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        assertThat(dto.skjæringstidspunkt()).isNull();
        assertThat(dto.førsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
        assertThat(dto.fagsystemSaksnummer()).isNull();
    }

    @Test
    void skal_mappe_med_skjæringstidspunkt_for_arbeidsgiverinitiert_uregistrert() {
        var entitet = new ForespørselEntitet(
            ORGNR,
            SKJÆRINGSTIDSPUNKT,
            AKTØR_ID_ENTITET,
            Ytelsetype.SVANGERSKAPSPENGER,
            null,
            FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT
        );

        var dto = ForespørselDtoMapper.mapFraEntitet(entitet);

        assertThat(dto.uuid()).isNotNull();
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.SVANGERSKAPSPENGER);
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
        assertThat(dto.skjæringstidspunkt()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(dto.fagsystemSaksnummer()).isNull();
    }

    @Test
    void skal_mappe_uten_skjæringstidspunkt_for_arbeidsgiverinitiert_uregistrert() {
        var entitet = new ForespørselEntitet(
            ORGNR,
            null,
            AKTØR_ID_ENTITET,
            Ytelsetype.FORELDREPENGER,
            null,
            FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT
        );

        var dto = ForespørselDtoMapper.mapFraEntitet(entitet);

        assertThat(dto.skjæringstidspunkt()).isNull();
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
    }

    @Test
    void skal_bevare_uuid_fra_entitet() {
        var entitet = new ForespørselEntitet(
            ORGNR,
            SKJÆRINGSTIDSPUNKT,
            AKTØR_ID_ENTITET,
            Ytelsetype.FORELDREPENGER,
            FAGSAK_SAKSNUMMER,
            FØRSTE_UTTAKSDATO,
            ForespørselType.BESTILT_AV_FAGSYSTEM
        );

        var dto1 = ForespørselDtoMapper.mapFraEntitet(entitet);
        var dto2 = ForespørselDtoMapper.mapFraEntitet(entitet);

        assertThat(dto1.uuid()).isEqualTo(dto2.uuid());
        assertThat(dto1).isEqualTo(dto2);
    }
}
