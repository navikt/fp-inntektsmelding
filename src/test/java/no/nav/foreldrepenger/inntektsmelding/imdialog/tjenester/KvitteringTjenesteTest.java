package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.DokumentGeneratorTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class KvitteringTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final LocalDate START_DATO = LocalDate.now();
    private static final AktørId SØKER_AKTØR_ID = new AktørId("1111111111111");

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private DokumentGeneratorTjeneste dokumentGeneratorTjeneste;

    private KvitteringTjeneste kvitteringTjeneste;

    @BeforeAll
    static void beforeAll() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest(INNMELDER_UID, "kompakt", IdentType.EksternBruker,
            new OpenIDToken(OpenIDProvider.TOKENX, new TokenString("token")), UUID.randomUUID(), Set.of()));
    }

    @AfterAll
    static void afterAll() {
        KontekstHolder.fjernKontekst();
    }

    @BeforeEach
    void setUp() {
        kvitteringTjeneste = new KvitteringTjeneste(forespørselBehandlingTjeneste, inntektsmeldingTjeneste, dokumentGeneratorTjeneste);
    }

    @Test
    void skal_teste_at_pdf_genereres() {
        // Arrange
        var imId = 1L;
        var im = InntektsmeldingDto.builder()
            .medInntekt(BigDecimal.ZERO)
            .medStartdato(START_DATO)
            .medAktørId(new AktørId(SØKER_AKTØR_ID.getAktørId()))
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .build();
        when(inntektsmeldingTjeneste.hentInntektsmelding(imId)).thenReturn(im);
        var forespørsel = new ForespørselEntitet(ARBEIDSGIVER_IDENT, LocalDate.now(),
            new AktørIdEntitet(SØKER_AKTØR_ID.getAktørId()), Ytelsetype.FORELDREPENGER, "123", START_DATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(forespørselBehandlingTjeneste.finnForespørsler(SØKER_AKTØR_ID, Ytelsetype.FORELDREPENGER, ARBEIDSGIVER_IDENT)).thenReturn(List.of(ForespørselDtoMapper.mapFraEntitet(forespørsel)));

        // Act
        kvitteringTjeneste.hentPDF(imId);

        // Assert
        verify(dokumentGeneratorTjeneste, times(1)).mapDataOgGenererPdf(im, ForespørselType.BESTILT_AV_FAGSYSTEM);
    }

}
