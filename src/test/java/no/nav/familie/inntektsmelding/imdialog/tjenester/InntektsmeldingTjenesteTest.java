package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private FpDokgenTjeneste fpDokgenTjeneste;

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

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
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository, fpDokgenTjeneste);
    }

    @Test
    void skal_hent_alle_inntektsmeldinger_for_en_forespørsel() {
        // Arrange
        var uid = UUID.randomUUID();
        var orgnr = "123";
        var søkerAktørId = new AktørIdEntitet("1111111111111");
        when(forespørselBehandlingTjeneste.hentForespørsel(uid)).thenReturn(Optional.of(new ForespørselEntitet(orgnr, LocalDate.now(),
            søkerAktørId, Ytelsetype.FORELDREPENGER, "123", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM)));
        var inntektsmelding = InntektsmeldingEntitet.builder().medMånedInntekt(BigDecimal.ZERO).medStartDato(LocalDate.now()).build();
        when(inntektsmeldingRepository.hentInntektsmeldinger(søkerAktørId, orgnr, LocalDate.now(), Ytelsetype.FORELDREPENGER)).thenReturn(
            List.of(inntektsmelding));

        // Act
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(uid);

        // Assert
        assertThat(inntektsmeldinger).hasSize(1).containsAll(List.of(inntektsmelding));
    }
}
