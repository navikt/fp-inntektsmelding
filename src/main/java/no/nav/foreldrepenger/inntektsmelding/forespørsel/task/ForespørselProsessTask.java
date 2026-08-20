package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Abstrakt basisklasse for prosesstasks som opererer på en forespørsel.
 * Tilbyr felles konstanter for task-properties og hjelpemetoder for å hente
 * forespørsel og inntektsmeldingUuid fra {@link ProsessTaskData}.
 */
public abstract class ForespørselProsessTask implements ProsessTaskHandler {

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";

    protected ForespørselTjeneste forespørselTjeneste;

    protected ForespørselProsessTask() {
        // CDI
    }

    protected ForespørselProsessTask(ForespørselTjeneste forespørselTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
    }

    protected ForespørselDto hentForespørsel(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        return forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid));
    }

    protected Optional<UUID> hentInntektsmeldingUuid(ProsessTaskData prosessTaskData) {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID))
            .map(UUID::fromString);
    }
}
