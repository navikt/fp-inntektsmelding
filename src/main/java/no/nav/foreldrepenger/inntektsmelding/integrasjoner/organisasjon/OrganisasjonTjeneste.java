package no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.utils.OrganisasjonsnummerValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class OrganisasjonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisasjonTjeneste.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final LRUCache<String, Organisasjon> CACHE = new LRUCache<>(2500, CACHE_ELEMENT_LIVE_TIME_MS);

    private EregKlient eregRestKlient;

    public OrganisasjonTjeneste() {
        // CDI
    }

    @Inject
    public OrganisasjonTjeneste(EregKlient eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    /**
     * Henter informasjon fra Enhetsregisteret hvis applikasjonen ikke kjenner til
     * orgnr eller har data som er eldre enn 24 timer.
     *
     * @param arbeidsgiver orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i
     *                                  enhetsreg
     */

    public Organisasjon finnOrganisasjon(Arbeidsgiver arbeidsgiver) {
        return finnOrganisasjonOptional(arbeidsgiver).orElseThrow(
            () -> new IllegalStateException("Forventet å finne organisasjon med orgnummer " + arbeidsgiver));
    }

    public Optional<Organisasjon> finnOrganisasjonOptional(Arbeidsgiver arbeidsgiver) {
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgiver.orgnr())) {
            LOG.info("Ugyldig orgnummer: {}", arbeidsgiver);
            return Optional.empty();
        }
        return Optional.of(hent(arbeidsgiver));
    }

    private Organisasjon hent(Arbeidsgiver arbeidsgiver) {
        var virksomhet = Optional.ofNullable(CACHE.get(arbeidsgiver.orgnr())).orElseGet(() -> hentOrganisasjonRest(arbeidsgiver));
        CACHE.put(arbeidsgiver.orgnr(), virksomhet);
        return virksomhet;
    }

    private Organisasjon hentOrganisasjonRest(Arbeidsgiver arbeidsgiver) {
        Objects.requireNonNull(arbeidsgiver, "orgNummer");
        var org = eregRestKlient.hentOrganisasjon(arbeidsgiver.orgnr());
        return new Organisasjon(org.getNavn(), org.organisasjonsnummer());
    }
}
