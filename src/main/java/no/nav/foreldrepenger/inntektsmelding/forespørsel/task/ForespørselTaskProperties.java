package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

/**
 * Felles task-property-nøkler for prosesstaskene knyttet til opprettelse av forespørsel
 * ({@link OpprettSakTask}, {@link OpprettOppgaveTask} og {@link OpprettDialogTask}). Samlet på ett sted slik at nøkkelen
 * garantert er den samme overalt, i stedet for å duplisere den som en konstant i hver task.
 */
public final class ForespørselTaskProperties {

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";

    private ForespørselTaskProperties() {
        // Skal ikke instansieres
    }
}
