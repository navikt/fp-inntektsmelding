package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

/**
 * Hjelpeklasse for forespørsel-prosesstasks. Tilbyr felles konstanter for task-properties
 * og hjelpemetoder for å hente forespørsel og inntektsmeldingUuid fra {@link ProsessTaskData}.
 */
public class ForespørselTaskTjeneste {

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";

    private ForespørselTaskTjeneste() {
        // statisk hjelpeklasse
    }

    public static ForespørselDto hentForespørsel(ForespørselTjeneste forespørselTjeneste, ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        return forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid));
    }

    public static Optional<UUID> hentInntektsmeldingUuid(ProsessTaskData prosessTaskData) {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID))
            .map(UUID::fromString);
    }

    public static ProsessTaskGruppe opprettTaskGruppe(UUID forespørselUuid) {
        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        return taskGruppe;
    }
}
