package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.InntektsmeldingFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingKontraktMapper;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiRestTest {

    private static final String ORGNR = "974760673";
    private static final String FNR = "11111111111";
    private static final String AKTØR_ID = "1234567890123";

    private InntektsmeldingApiRest inntektsmeldingApiRest;

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private InntektsmeldingApiMottakTjeneste inntektsmeldingMottakTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private Tilgang tilgang;
    @Mock
    private InntektsmeldingKontraktMapper inntektsmeldingKontraktMapper;

    @BeforeEach
    void setUp() {
        inntektsmeldingApiRest = new InntektsmeldingApiRest(
            inntektsmeldingTjeneste,
            inntektsmeldingMottakTjeneste,
            personTjeneste,
            tilgang,
            inntektsmeldingKontraktMapper
        );
    }

    @Test
    void skal_hente_inntektsmelding_med_uuid() {
        var uuid = UUID.randomUUID();
        var dto = lagInntektsmeldingDto();
        var response = lagHentResponse(uuid);

        when(inntektsmeldingTjeneste.hentInntektsmelding(uuid)).thenReturn(dto);
        when(inntektsmeldingKontraktMapper.mapTilKontrakt(dto)).thenReturn(response);

        var resultat = inntektsmeldingApiRest.hentInntektsmelding(uuid);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(resultat.getEntity()).isEqualTo(response);
        verify(tilgang).sjekkErSystembruker();
    }


    @Test
    void skal_hente_inntektsmeldinger_med_forespørselUuid() {
        var forespørselUuid = UUID.randomUUID();
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), null, null, forespørselUuid, null, null);

        var dto = lagInntektsmeldingDto();
        var response = lagHentResponse(UUID.randomUUID());

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(List.of(dto));
        when(inntektsmeldingKontraktMapper.mapTilKontrakt(dto)).thenReturn(response);

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        @SuppressWarnings("unchecked")
        var liste = (List<HentInntektsmeldingResponse>) resultat.getEntity();
        assertThat(liste).hasSize(1).containsExactly(response);
        verify(inntektsmeldingTjeneste).hentInntektsmeldinger(forespørselUuid);
        verifyNoInteractions(personTjeneste);
    }

    @Test
    void skal_hente_inntektsmeldinger_med_kun_orgnr() {
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), null, null, null, null, null);

        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(eq(ORGNR), isNull(), isNull(), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        @SuppressWarnings("unchecked")
        var liste = (List<HentInntektsmeldingResponse>) resultat.getEntity();
        assertThat(liste).isEmpty();
        verify(tilgang).sjekkErSystembruker();
    }

    @Test
    void skal_hente_inntektsmeldinger_med_orgnr_og_fnr() {
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), new FødselsnummerDto(FNR), null, null, null, null);

        var aktørId = new AktørId(AKTØR_ID);
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.of(aktørId));

        var dto = lagInntektsmeldingDto();
        var response = lagHentResponse(UUID.randomUUID());

        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(
            eq(ORGNR), eq(new AktørIdEntitet(AKTØR_ID)), isNull(), isNull(), isNull()))
            .thenReturn(List.of(dto));
        when(inntektsmeldingKontraktMapper.mapTilKontrakt(dto)).thenReturn(response);

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        @SuppressWarnings("unchecked")
        var liste = (List<HentInntektsmeldingResponse>) resultat.getEntity();
        assertThat(liste).hasSize(1);
    }

    @Test
    void skal_hente_inntektsmeldinger_med_orgnr_og_ytelsetype() {
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), null, YtelseTypeDto.FORELDREPENGER, null, null, null);

        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(
            eq(ORGNR), isNull(), eq(Ytelsetype.FORELDREPENGER), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(inntektsmeldingTjeneste).hentInntektsmeldingerFraFilter(
            eq(ORGNR), isNull(), eq(Ytelsetype.FORELDREPENGER), isNull(), isNull());
    }

    @Test
    void skal_mappe_svangerskapspenger_ytelsetype_korrekt() {
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), null, YtelseTypeDto.SVANGERSKAPSPENGER, null, null, null);

        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(
            eq(ORGNR), isNull(), eq(Ytelsetype.SVANGERSKAPSPENGER), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(inntektsmeldingTjeneste).hentInntektsmeldingerFraFilter(
            eq(ORGNR), isNull(), eq(Ytelsetype.SVANGERSKAPSPENGER), isNull(), isNull());
    }

    @Test
    void skal_hente_inntektsmeldinger_med_datofilter() {
        var fom = LocalDate.of(2025, 1, 1);
        var tom = LocalDate.of(2025, 12, 31);
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), null, null, null, fom, tom);

        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(eq(ORGNR), isNull(), isNull(), eq(fom), eq(tom)))
            .thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(inntektsmeldingTjeneste).hentInntektsmeldingerFraFilter(ORGNR, null, null, fom, tom);
    }

    @Test
    void skal_hente_med_alle_filterparametre() {
        var fom = LocalDate.of(2025, 3, 1);
        var tom = LocalDate.of(2025, 6, 30);
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), new FødselsnummerDto(FNR),
            YtelseTypeDto.FORELDREPENGER, null, fom, tom);

        var aktørId = new AktørId(AKTØR_ID);
        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.of(aktørId));

        var dto = lagInntektsmeldingDto();
        var response = lagHentResponse(UUID.randomUUID());
        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(
            ORGNR, new AktørIdEntitet(AKTØR_ID), Ytelsetype.FORELDREPENGER, fom, tom))
            .thenReturn(List.of(dto));
        when(inntektsmeldingKontraktMapper.mapTilKontrakt(dto)).thenReturn(response);

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        @SuppressWarnings("unchecked")
        var liste = (List<HentInntektsmeldingResponse>) resultat.getEntity();
        assertThat(liste).hasSize(1).containsExactly(response);
    }

    @Test
    void skal_sende_null_aktørId_når_fnr_ikke_finnes_i_pdl() {
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), new FødselsnummerDto(FNR), null, null, null, null);

        when(personTjeneste.finnAktørIdForIdent(any(PersonIdent.class))).thenReturn(Optional.empty());
        when(inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(eq(ORGNR), isNull(), isNull(), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(inntektsmeldingTjeneste).hentInntektsmeldingerFraFilter(eq(ORGNR), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void skal_prioritere_forespørselUuid_over_andre_filtre() {
        var forespørselUuid = UUID.randomUUID();
        var filter = new InntektsmeldingFilterRequest(
            new OrganisasjonsnummerDto(ORGNR), new FødselsnummerDto(FNR),
            YtelseTypeDto.FORELDREPENGER, forespørselUuid,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid)).thenReturn(Collections.emptyList());

        var resultat = inntektsmeldingApiRest.hentInntektsmeldinger(filter);

        assertThat(resultat.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(inntektsmeldingTjeneste).hentInntektsmeldinger(forespørselUuid);
        verifyNoInteractions(personTjeneste);
    }

    // ---- Hjelpemetoder ----

    private InntektsmeldingDto lagInntektsmeldingDto() {
        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(UUID.randomUUID())
            .medArbeidsgiver(new Arbeidsgiver(ORGNR))
            .medStartdato(LocalDate.now())
            .medInntekt(BigDecimal.valueOf(50000))
            .medInnsendtTidspunkt(LocalDateTime.now())
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .build();
    }

    private HentInntektsmeldingResponse lagHentResponse(UUID uuid) {
        return new HentInntektsmeldingResponse(
            uuid, UUID.randomUUID(),
            new FødselsnummerDto(FNR),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(ORGNR),
            null, LocalDate.now(),
            BigDecimal.valueOf(50000),
            LocalDateTime.now(),
            null, null, null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }
}
