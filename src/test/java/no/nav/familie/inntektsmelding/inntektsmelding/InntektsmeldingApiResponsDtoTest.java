package no.nav.familie.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.nav.familie.inntektsmelding.inntektsmelding.kontrakt.ArbeidsgiverInformasjonDto;
import no.nav.familie.inntektsmelding.inntektsmelding.kontrakt.Endringsårsak;
import no.nav.familie.inntektsmelding.inntektsmelding.kontrakt.InntektsmeldingApiResponsDto;
import no.nav.familie.inntektsmelding.inntektsmelding.kontrakt.Naturalytelsetype;
import no.nav.familie.inntektsmelding.inntektsmelding.kontrakt.Ytelse;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InntektsmeldingApiResponsDtoTest {

    private static final JsonMapper JSON_MAPPER = DefaultJsonMapper.getJsonMapper();

    private InntektsmeldingApiResponsDto lagTestDto(LocalDate dato, LocalDateTime tidspunkt, UUID uuid, UUID forespørselUuid,
                                                    InntektsmeldingApiResponsDto.Innsendingsårsak innsendingsårsak,
                                                    InntektsmeldingApiResponsDto.Innsendingstype innsendingstype,
                                                    List<InntektsmeldingApiResponsDto.SøktRefusjon> refusjoner,
                                                    List<InntektsmeldingApiResponsDto.BortfaltNaturalytelse> naturalytelser,
                                                    List<InntektsmeldingApiResponsDto.Endringsårsaker> endringsårsaker) {
        return new InntektsmeldingApiResponsDto(
            uuid,
            forespørselUuid,
            "12345678901",
            Ytelse.FORELDREPENGER,
            new ArbeidsgiverInformasjonDto("999999999"),
            new InntektsmeldingApiResponsDto.Kontaktperson("12345678", "Ola Nordmann"),
            dato,
            new BigDecimal("50000"),
            innsendingsårsak,
            innsendingstype,
            tidspunkt,
            new InntektsmeldingApiResponsDto.AvsenderSystem("MinLønn", "1.0"),
            refusjoner,
            naturalytelser,
            endringsårsaker
        );
    }

    @Test
    void skal_serialisere_komplett_dto_til_forventet_json_format() throws JsonProcessingException {
        var dato = LocalDate.of(2026, 3, 10);
        var tidspunkt = LocalDateTime.of(2026, 3, 10, 9, 58, 3, 663000000);
        var uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        var forespørselUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");

        var dto = lagTestDto(dato, tidspunkt, uuid, forespørselUuid,
            InntektsmeldingApiResponsDto.Innsendingsårsak.NY,
            InntektsmeldingApiResponsDto.Innsendingstype.FORESPURT,
            List.of(new InntektsmeldingApiResponsDto.SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new InntektsmeldingApiResponsDto.BortfaltNaturalytelse(dato, dato,
                Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, new BigDecimal("1000"))),
            List.of(
                new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.BONUS, null, null, null),
                new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.FERIE, dato, dato, null),
                new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, dato)
            )
        );

        var json = JSON_MAPPER.writeValueAsString(dto);

        assertThat(json)
            .contains("\"inntektsmeldingUuid\"")
            .contains("\"fnr\"")
            .contains("\"ytelse\"")
            .contains("\"arbeidsgiver\"")
            .contains("\"kontaktperson\"")
            .contains("\"startdato\"")
            .contains("\"inntekt\"")
            .contains("\"innsendingstype\"")
            .contains("\"innsendtTidspunkt\"")
            .contains("\"avsenderSystem\"")
            .contains("\"bortfaltNaturalytelsePerioder\"")
            .contains("\"BONUS\"")
            .contains("\"FERIE\"")
            .contains("\"TARIFFENDRING\"")
            .contains("\"NY\"")
            .contains("\"FORESPURT\"");
    }

    @Test
    void skal_serialisere_og_verifisere_verdier() throws JsonProcessingException {
        var dato = LocalDate.of(2026, 3, 10);
        var tidspunkt = LocalDateTime.of(2026, 3, 10, 9, 58, 3, 663000000);
        var uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        var forespørselUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");

        var dto = lagTestDto(dato, tidspunkt, uuid, forespørselUuid,
            InntektsmeldingApiResponsDto.Innsendingsårsak.NY,
            InntektsmeldingApiResponsDto.Innsendingstype.FORESPURT,
            List.of(new InntektsmeldingApiResponsDto.SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new InntektsmeldingApiResponsDto.BortfaltNaturalytelse(dato, null,
                Naturalytelsetype.ANNET, new BigDecimal("500"))),
            List.of(new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.BONUS, null, null, null))
        );

        var json = JSON_MAPPER.writeValueAsString(dto);

        assertThat(json)
            .contains("\"inntektsmeldingUuid\"")
            .contains("\"BONUS\"")
            .contains("\"ANNET\"")
            .contains("\"NY\"")
            .contains("\"FORESPURT\"");
    }

    @Test
    void skal_serialisere_med_tomme_lister() throws JsonProcessingException {
        var dto = new InntektsmeldingApiResponsDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "12345678901",
            Ytelse.SVANGERSKAPSPENGER,
            new ArbeidsgiverInformasjonDto("999999999"),
            new InntektsmeldingApiResponsDto.Kontaktperson("87654321", "Kari Nordmann"),
            LocalDate.now(),
            new BigDecimal("40000"),
            InntektsmeldingApiResponsDto.Innsendingsårsak.ENDRING,
            InntektsmeldingApiResponsDto.Innsendingstype.ARBEIDSGIVER_INITIERT,
            LocalDateTime.now(),
            new InntektsmeldingApiResponsDto.AvsenderSystem("System", "2.0"),
            List.of(),
            List.of(),
            List.of()
        );

        var json = JSON_MAPPER.writeValueAsString(dto);

        assertThat(json)
            .contains("\"SVANGERSKAPSPENGER\"")
            .contains("\"ENDRING\"")
            .contains("\"ARBEIDSGIVER_INITIERT\"");
    }

    @Test
    void skal_matche_forventet_json_struktur_eksakt() throws JsonProcessingException {
        var dato = LocalDate.of(2026, 3, 10);
        var tidspunkt = LocalDateTime.of(2026, 3, 10, 9, 58, 3, 663000000);
        var uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        var forespørselUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");

        var dto = lagTestDto(dato, tidspunkt, uuid, forespørselUuid,
            InntektsmeldingApiResponsDto.Innsendingsårsak.NY,
            InntektsmeldingApiResponsDto.Innsendingstype.FORESPURT,
            List.of(new InntektsmeldingApiResponsDto.SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new InntektsmeldingApiResponsDto.BortfaltNaturalytelse(dato, dato,
                Naturalytelsetype.BIL, new BigDecimal("2000"))),
            List.of(
                new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.BONUS, null, null, null),
                new InntektsmeldingApiResponsDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, dato, null, dato)
            )
        );

        var jsonTree = JSON_MAPPER.readTree(JSON_MAPPER.writeValueAsString(dto));

        // Verifiser toppnivå-struktur
        assertThat(jsonTree.has("inntektsmeldingUuid")).isTrue();
        assertThat(jsonTree.has("fnr")).isTrue();
        assertThat(jsonTree.has("ytelse")).isTrue();
        assertThat(jsonTree.has("arbeidsgiver")).isTrue();
        assertThat(jsonTree.has("kontaktperson")).isTrue();
        assertThat(jsonTree.has("startdato")).isTrue();
        assertThat(jsonTree.has("inntekt")).isTrue();
        assertThat(jsonTree.has("innsendingstype")).isTrue();
        assertThat(jsonTree.has("innsendtTidspunkt")).isTrue();
        assertThat(jsonTree.has("avsenderSystem")).isTrue();
        assertThat(jsonTree.has("bortfaltNaturalytelsePerioder")).isTrue();

        // Verifiser kontaktperson
        var kontaktpersonNode = jsonTree.get("kontaktperson");
        assertThat(kontaktpersonNode.get("tlf").asText()).isEqualTo("12345678");
        assertThat(kontaktpersonNode.get("navn").asText()).isEqualTo("Ola Nordmann");

        // Verifiser bortfalt naturalytelse
        assertThat(jsonTree.get("bortfaltNaturalytelsePerioder")).hasSize(1);
        assertThat(jsonTree.get("bortfaltNaturalytelsePerioder").get(0).get("naturalytelsetype").asText()).isEqualTo("BIL");

        // Verifiser avsenderSystem
        var avsenderNode = jsonTree.get("avsenderSystem");
        assertThat(avsenderNode.get("systemNavn").asText()).isEqualTo("MinLønn");
        assertThat(avsenderNode.get("systemVersjon").asText()).isEqualTo("1.0");

        // Verifiser enum-verdier
        assertThat(jsonTree.get("ytelse").asText()).isEqualTo("FORELDREPENGER");
        assertThat(jsonTree.get("innsendingstype").asText()).isEqualTo("FORESPURT");
    }
}
