package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "ferdigstill.forespørsler.etter.feil", maxFailedRuns = 1)
public class OppdatereTilFerdigHvisMottattImTask implements ProsessTaskHandler {
    static final String DRY_RUN = "dryRun";
    static final String FOM = "fom";
    static final String TOM = "tom";
    private static final Logger LOG = LoggerFactory.getLogger(OppdatereTilFerdigHvisMottattImTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    OppdatereTilFerdigHvisMottattImTask() {
        // for CDI proxy
    }

    @Inject
    public OppdatereTilFerdigHvisMottattImTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste,
                                               InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                               ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);
        var fom = Long.valueOf(prosessTaskData.getPropertyValue(FOM));
        var tom = Long.valueOf(prosessTaskData.getPropertyValue(TOM));

        var forespørsler = hentForespørsler(fom, tom);

        LOG.info("FEILAKTIGE_FORESPØRSLER: Fant {} forespørsler som skal sjekkes om inntektsmelding er mottatt og skal ferdigstilles",
            forespørsler.size());

        forespørsler.forEach(forespørsel -> {
            var innteksmelding = inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørsel.getUuid());
            if (innteksmelding != null) {
                LOG.info("FEILAKTIGE_FORESPØRSLER: Fant innsendt inntektsmelding med id {} for forespørselId {}",
                    innteksmelding.getId(),
                    forespørsel.getId());

                if (dryRun.equals(Boolean.FALSE)) {
                    forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørsel.getUuid(),
                        new AktørId(forespørsel.getAktørId().getAktørId()),
                        new Arbeidsgiver(forespørsel.getOrganisasjonsnummer()),
                        forespørsel.getFørsteUttaksdato(),
                        LukkeÅrsak.ORDINÆR_INNSENDING,
                        Optional.of(innteksmelding.getInntektsmeldingUuid()));
                    LOG.info("FEILAKTIGE_FORESPØRSLER: Oppdatert forespørselId {} til FERDIGSTILT", forespørsel.getId());
                }
            }
        });
        forespørsler.stream().map(ForespørselEntitet::getId).max(Long::compareTo).ifPresent(maxId -> lagNesteTask(maxId + 1, tom, dryRun));
    }

    private void lagNesteTask(long nyFom, Long tom, boolean dryRun) {
        var prosesstaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosesstaskData.setProperty(FOM, String.valueOf(nyFom));
        prosesstaskData.setProperty(TOM, String.valueOf(tom));
        prosesstaskData.setProperty(DRY_RUN, String.valueOf(dryRun));

        prosessTaskTjeneste.lagre(prosesstaskData);
    }


    private List<ForespørselEntitet> hentForespørsler(Long fom, Long tom) {
        var query = entityManager.createQuery("from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
            + " and status = :status order by id", ForespørselEntitet.class);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        query.setParameter("dato", LocalDate.of(2026, 2, 1).atStartOfDay());
        query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING);

        query.setMaxResults(50);
        return query.getResultList();
    }
}

