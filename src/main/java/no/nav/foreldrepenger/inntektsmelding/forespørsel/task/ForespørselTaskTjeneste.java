package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Tjeneste for forespørsel-prosesstasks. Tilbyr felles konstanter for task-properties
 * og hjelpemetoder for å hente forespørsel og inntektsmeldingUuid fra {@link ProsessTaskData}.
 */
@ApplicationScoped
public class ForespørselTaskTjeneste {

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";

    private ForespørselTjeneste forespørselTjeneste;

    ForespørselTaskTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselTaskTjeneste(ForespørselTjeneste forespørselTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public ForespørselDto hentForespørsel(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        return forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid));
    }

    public Optional<UUID> hentInntektsmeldingUuid(ProsessTaskData prosessTaskData) {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID))
            .map(UUID::fromString);
    }
}
