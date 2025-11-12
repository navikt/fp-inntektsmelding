package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@Dependent
public class TilgangTjeneste implements Tilgang {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final AltinnTilgangTjeneste altinnTilgangTjeneste;
    private final PipTjeneste pipTjeneste;

    @Inject
    public TilgangTjeneste(PipTjeneste pipTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.pipTjeneste = pipTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid) {
        sjekkErBorger();

        var orgNrSet = Optional.of(forespørselUuid)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(OrganisasjonsnummerDto organisasjonsnummer) {
        sjekkErBorger();

        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(organisasjonsnummer.orgnr()));
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(long inntektsmeldingId) {
        sjekkErBorger();

        var orgNrSet = Optional.of(inntektsmeldingId)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }


    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(UUID inntektsmeldingUUID) {
        sjekkErBorger();

        var orgNrSet = Optional.of(inntektsmeldingUUID)
            .stream()
            .map(pipTjeneste::hentInntektsmeldingOrganisasjonsnummerFor)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkAtAnsattHarRollenDrift() {
        var kontekst = KontekstHolder.getKontekst();
        if (erNavAnsatt(kontekst) && ansattHarRollen(kontekst, AnsattGruppe.DRIFT)) {
            return;
        }
        ikkeTilgang("Ansatt mangler en rolle.");
    }

    public void sjekkAtAnsattHarRollenSaksbehandler() {
        var kontekst = KontekstHolder.getKontekst();
        if (erNavAnsatt(kontekst) && ansattHarRollen(kontekst, AnsattGruppe.SAKSBEHANDLER)) {
            return;
        }
        ikkeTilgang("Ansatt mangler en rolle.");
    }

    @Override
    public void sjekkErSystembruker() {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq && rq.getIdentType().erSystem()) {
            return;
        }
        ikkeTilgang("Kun systemkall støttes.");
    }

    private boolean erNavAnsatt(Kontekst kontekst) {
        return IdentType.InternBruker.equals(kontekst.getIdentType());
    }

    private boolean ansattHarRollen(Kontekst kontekst, AnsattGruppe rolle) {
        return kontekst instanceof RequestKontekst requestKontekst && requestKontekst.harGruppe(rolle);
    }

    private void sjekkErBorger() {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq && IdentType.EksternBruker.equals(rq.getIdentType())) {
            return;
        }
        ikkeTilgang("Kun borger kall støttes.");
    }

    private void sjekkBorgersAltinnTilgangTilOrganisasjon(Set<String> organisasjoner) {
        if (organisasjoner.isEmpty()) {
            ikkeTilgang("Mangler informasjon om bedrift.");
        } else {
            for (var orgNr : organisasjoner) {
                if (altinnTilgangTjeneste.manglerTilgangTilBedriften(orgNr)) {
                    SECURE_LOG.warn("Bruker mangler tilgang til bedrift {} i Altinn.", orgNr);
                    ikkeTilgang("Bruker mangler tilgang til bedriften i Altinn.");
                }
            }
        }
    }

    private static void ikkeTilgang(String begrunnelse) {
        LOG.info("Fikk ikke tilgang pga: {}", begrunnelse);
        throw new ManglerTilgangException("IM-00403", "Mangler tilgang til tjenesten. %s".formatted(begrunnelse));
    }

}
