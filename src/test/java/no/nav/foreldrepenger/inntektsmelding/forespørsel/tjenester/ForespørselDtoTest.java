package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

class ForespørselDtoTest {

    private static final UUID UUID_1 = UUID.randomUUID();
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra("974760673");
    private static final AktørId AKTØR_ID = AktørId.fra("1234567891234");
    private static final LocalDate STP = LocalDate.of(2025, 6, 1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2025, 6, 15);
    private static final Saksnummer SAKSNUMMER = Saksnummer.fra("SAK_123");
    private static final LocalDateTime OPPRETTET = LocalDateTime.of(2025, 5, 1, 10, 0);

    @Test
    void skal_opprette_med_builder_alle_felter() {
        var dialogUuid = UUID.randomUUID();

        var dto = ForespørselDto.builder()
            .uuid(UUID_1)
            .arbeidsgiver(ARBEIDSGIVER)
            .aktørId(AKTØR_ID)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .skjæringstidspunkt(STP)
            .førsteUttaksdato(FØRSTE_UTTAKSDATO)
            .fagsystemSaksnummer(SAKSNUMMER)
            .opprettetTidspunkt(OPPRETTET)
            .arbeidsgiverNotifikasjonSakId("sak-1")
            .oppgaveId("oppgave-1")
            .dialogportenUuid(dialogUuid)
            .build();

        assertThat(dto.uuid()).isEqualTo(UUID_1);
        assertThat(dto.arbeidsgiver()).isEqualTo(ARBEIDSGIVER);
        assertThat(dto.aktørId()).isEqualTo(AKTØR_ID);
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.BESTILT_AV_FAGSYSTEM);
        assertThat(dto.skjæringstidspunkt()).isEqualTo(STP);
        assertThat(dto.førsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
        assertThat(dto.fagsystemSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(dto.opprettetTidspunkt()).isEqualTo(OPPRETTET);
        assertThat(dto.arbeidsgiverNotifikasjonSakId()).isEqualTo("sak-1");
        assertThat(dto.oppgaveId()).isEqualTo("oppgave-1");
        assertThat(dto.dialogportenUuid()).isEqualTo(dialogUuid);
    }

    @Test
    void skal_opprette_med_builder_kun_påkrevde_felter() {
        var dto = ForespørselDto.builder()
            .uuid(UUID_1)
            .arbeidsgiver(ARBEIDSGIVER)
            .aktørId(AKTØR_ID)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .build();

        assertThat(dto.uuid()).isEqualTo(UUID_1);
        assertThat(dto.arbeidsgiver()).isEqualTo(ARBEIDSGIVER);
        assertThat(dto.aktørId()).isEqualTo(AKTØR_ID);
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(dto.forespørselType()).isNull();
        assertThat(dto.skjæringstidspunkt()).isNull();
        assertThat(dto.førsteUttaksdato()).isNull();
        assertThat(dto.fagsystemSaksnummer()).isNull();
        assertThat(dto.opprettetTidspunkt()).isNull();
        assertThat(dto.arbeidsgiverNotifikasjonSakId()).isNull();
        assertThat(dto.oppgaveId()).isNull();
        assertThat(dto.dialogportenUuid()).isNull();
    }

    @Test
    void skal_opprette_med_konstruktor() {
        var dto = new ForespørselDto(UUID_1, ARBEIDSGIVER, AKTØR_ID, Ytelsetype.SVANGERSKAPSPENGER,
            ForespørselStatus.FERDIG, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT,
            STP, FØRSTE_UTTAKSDATO, SAKSNUMMER, OPPRETTET, "sak-2", "oppgave-2", null);

        assertThat(dto.uuid()).isEqualTo(UUID_1);
        assertThat(dto.ytelseType()).isEqualTo(Ytelsetype.SVANGERSKAPSPENGER);
        assertThat(dto.status()).isEqualTo(ForespørselStatus.FERDIG);
        assertThat(dto.forespørselType()).isEqualTo(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        assertThat(dto.dialogportenUuid()).isNull();
    }

    @Test
    void skal_ha_record_equals_for_like_objekter() {
        var dto1 = ForespørselDto.builder()
            .uuid(UUID_1)
            .arbeidsgiver(ARBEIDSGIVER)
            .aktørId(AKTØR_ID)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .skjæringstidspunkt(STP)
            .førsteUttaksdato(FØRSTE_UTTAKSDATO)
            .fagsystemSaksnummer(SAKSNUMMER)
            .build();

        var dto2 = ForespørselDto.builder()
            .uuid(UUID_1)
            .arbeidsgiver(ARBEIDSGIVER)
            .aktørId(AKTØR_ID)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .skjæringstidspunkt(STP)
            .førsteUttaksdato(FØRSTE_UTTAKSDATO)
            .fagsystemSaksnummer(SAKSNUMMER)
            .build();

        assertThat(dto1).isEqualTo(dto2)
            .hasSameHashCodeAs(dto2);
    }

    @Test
    void skal_ikke_være_lik_ved_ulik_status() {
        var dto1 = ForespørselDto.builder()
            .uuid(UUID_1)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .build();

        var dto2 = ForespørselDto.builder()
            .uuid(UUID_1)
            .status(ForespørselStatus.FERDIG)
            .build();

        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void skal_ikke_være_lik_ved_ulik_uuid() {
        var dto1 = ForespørselDto.builder().uuid(UUID_1).build();
        var dto2 = ForespørselDto.builder().uuid(UUID.randomUUID()).build();

        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void skal_støtte_alle_statuser() {
        for (var status : ForespørselStatus.values()) {
            var dto = ForespørselDto.builder().status(status).build();
            assertThat(dto.status()).isEqualTo(status);
        }
    }

    @Test
    void skal_støtte_alle_ytelsetyper() {
        for (var ytelse : Ytelsetype.values()) {
            var dto = ForespørselDto.builder().ytelseType(ytelse).build();
            assertThat(dto.ytelseType()).isEqualTo(ytelse);
        }
    }

    @Test
    void skal_støtte_alle_forespørseltyper() {
        for (var type : ForespørselType.values()) {
            var dto = ForespørselDto.builder().forespørselType(type).build();
            assertThat(dto.forespørselType()).isEqualTo(type);
        }
    }

    @Test
    void skal_ha_meningsfull_toString() {
        var dto = ForespørselDto.builder()
            .uuid(UUID_1)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .build();

        var toString = dto.toString();
        assertThat(toString).contains("ForespørselDto")
            .contains(UUID_1.toString())
            .contains("UNDER_BEHANDLING");
    }
}

