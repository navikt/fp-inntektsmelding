package no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
     * @param orgNummer orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i
     *                                  enhetsreg
     */

    public Organisasjon finnOrganisasjon(String orgNummer) {
        return finnOrganisasjonOptional(orgNummer).orElseThrow(
            () -> new IllegalStateException("Forventet å finne organisasjon med orgnummer " + orgNummer));
    }

    public Optional<Organisasjon> finnOrganisasjonOptional(String orgNummer) {
        if (!OrganisasjonsnummerValidator.erGyldig(orgNummer)) {
            LOG.info("Ugyldig orgnummer: {}", orgNummer);
            return Optional.empty();
        }
        return Optional.of(hent(orgNummer));
    }

    private Organisasjon hent(String orgnr) {
        var virksomhet = Optional.ofNullable(CACHE.get(orgnr)).orElseGet(() -> hentOrganisasjonRest(orgnr));
        CACHE.put(orgnr, virksomhet);
        return virksomhet;
    }

    private Organisasjon hentOrganisasjonRest(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer");
        var org = eregRestKlient.hentOrganisasjon(orgNummer);
        return new Organisasjon(org.getNavn(), org.organisasjonsnummer());
    }
}
