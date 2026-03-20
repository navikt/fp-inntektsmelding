package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntektsmelding.kontrakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.FødselsnummerDto;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.AvsenderSystem;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.BortfaltNaturalytelse;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsaker;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.HentInntektsmeldingRespons;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Innsendingstype;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Innsendingsårsak;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Kontaktperson;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Naturalytelsetype;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SøktRefusjon;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class HentInntektsmeldingResponsTest {

    private static final JsonMapper JSON_MAPPER = DefaultJsonMapper.getJsonMapper();

    private HentInntektsmeldingRespons lagTestDto(LocalDate dato, LocalDateTime tidspunkt, UUID uuid, UUID forespørselUuid,
                                                  Innsendingsårsak innsendingsårsak,
                                                  Innsendingstype innsendingstype,
                                                  List<SøktRefusjon> refusjoner,
                                                  List<BortfaltNaturalytelse> naturalytelser,
                                                  List<Endringsårsaker> endringsårsaker) {
        return new HentInntektsmeldingRespons(
            uuid,
            forespørselUuid,
            new FødselsnummerDto("12345678901"),
            YtelseType.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"),
            new Kontaktperson("Ola Nordmann", "12345678"),
            dato,
            new BigDecimal("50000"),
            innsendingsårsak,
            innsendingstype,
            tidspunkt,
            new AvsenderSystem("MinLønn", "1.0"),
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
            Innsendingsårsak.NY,
            Innsendingstype.FORESPURT,
            List.of(new SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new BortfaltNaturalytelse(dato, dato,
                Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, new BigDecimal("1000"))),
            List.of(
                new Endringsårsaker(Endringsårsak.BONUS, null, null, null),
                new Endringsårsaker(Endringsårsak.FERIE, dato, dato, null),
                new Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, dato)
            )
        );

        var json = JSON_MAPPER.writeValueAsString(dto);

        assertThat(json)
            .contains("\"inntektsmeldingUuid\"")
            .contains("\"fnr\"")
            .contains("\"ytelseType\"")
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
            Innsendingsårsak.NY,
            Innsendingstype.FORESPURT,
            List.of(new SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new BortfaltNaturalytelse(dato, null,
                Naturalytelsetype.ANNET, new BigDecimal("500"))),
            List.of(new Endringsårsaker(Endringsårsak.BONUS, null, null, null))
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
        var dto = new HentInntektsmeldingRespons(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new FødselsnummerDto("12345678901"),
            YtelseType.SVANGERSKAPSPENGER,
            new ArbeidsgiverDto("999999999"),
            new Kontaktperson("87654321", "Kari Nordmann"),
            LocalDate.now(),
            new BigDecimal("40000"),
            Innsendingsårsak.ENDRING,
            Innsendingstype.ARBEIDSGIVER_INITIERT,
            LocalDateTime.now(),
            new AvsenderSystem("System", "2.0"),
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
            Innsendingsårsak.NY,
            Innsendingstype.FORESPURT,
            List.of(new SøktRefusjon(dato, new BigDecimal("30000"))),
            List.of(new BortfaltNaturalytelse(dato, dato,
                Naturalytelsetype.BIL, new BigDecimal("2000"))),
            List.of(
                new Endringsårsaker(Endringsårsak.BONUS, null, null, null),
                new Endringsårsaker(Endringsårsak.TARIFFENDRING, dato, null, dato)
            )
        );

        var jsonTree = JSON_MAPPER.readTree(JSON_MAPPER.writeValueAsString(dto));

        // Verifiser toppnivå-struktur
        assertThat(jsonTree.has("inntektsmeldingUuid")).isTrue();
        assertThat(jsonTree.has("fnr")).isTrue();
        assertThat(jsonTree.has("ytelseType")).isTrue();
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
        assertThat(kontaktpersonNode.get("telefonnummer").asText()).isEqualTo("12345678");
        assertThat(kontaktpersonNode.get("navn").asText()).isEqualTo("Ola Nordmann");

        // Verifiser bortfalt naturalytelse
        assertThat(jsonTree.get("bortfaltNaturalytelsePerioder")).hasSize(1);
        assertThat(jsonTree.get("bortfaltNaturalytelsePerioder").get(0).get("naturalytelsetype").asText()).isEqualTo("BIL");

        // Verifiser avsenderSystem
        var avsenderNode = jsonTree.get("avsenderSystem");
        assertThat(avsenderNode.get("systemNavn").asText()).isEqualTo("MinLønn");
        assertThat(avsenderNode.get("systemVersjon").asText()).isEqualTo("1.0");

        // Verifiser enum-verdier
        assertThat(jsonTree.get("ytelseType").asText()).isEqualTo("FORELDREPENGER");
        assertThat(jsonTree.get("innsendingstype").asText()).isEqualTo("FORESPURT");
    }
}
