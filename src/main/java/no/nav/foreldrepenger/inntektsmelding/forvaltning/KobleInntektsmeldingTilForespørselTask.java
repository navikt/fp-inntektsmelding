package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "koble.inntektsmelding.forespørsel", maxFailedRuns = 1)
public class KobleInntektsmeldingTilForespørselTask implements ProsessTaskHandler {
    static final String DRY_RUN = "dryRun";
    static final String FOM = "fom";
    static final String TOM = "tom";
    private static final Logger LOG = LoggerFactory.getLogger(KobleInntektsmeldingTilForespørselTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    KobleInntektsmeldingTilForespørselTask() {
        // for CDI proxy
    }

    @Inject
    public KobleInntektsmeldingTilForespørselTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste,
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

        var inntektsmeldinger = hentInntektsmeldinger(fom, tom);

        LOG.info("KOBLE_INNTEKTSMELDINGER: Fant {} inntektsmeldinger som skal kobles til en forespørsel",
            inntektsmeldinger.size());

        inntektsmeldinger.forEach(im -> {
            var matchendeForespørsler = forespørselBehandlingTjeneste.finnForespørsler(new AktørId(im.getAktørId().getAktørId()),
                im.getYtelsetype(),
                im.getArbeidsgiverIdent());
            if (matchendeForespørsler.isEmpty()) {
                kastIngenForespørselFeil(im);
            } else if (matchendeForespørsler.size() == 1) {
                oppdaterInntektsmeldingMedForespørsel(im, matchendeForespørsler.getFirst());
            } else {
                LOG.info("KOBLE_INNTEKTSMELDINGER: Fant {} mulige forespørsler som matcher for inntektsmelding {}",
                    matchendeForespørsler.size(),
                    im.getId());
                finnBesteMatchUtfraOpprettetTidOgStartdato(im, matchendeForespørsler);
            }
        });
        inntektsmeldinger.stream().map(InntektsmeldingEntitet::getId).max(Long::compareTo).ifPresent(maxId -> lagNesteTask(maxId + 1, tom, dryRun));
    }

    private void kastIngenForespørselFeil(InntektsmeldingEntitet im) {
        throw new IllegalStateException("Finner ingen forespørsel som matcher med inntektsmelding " + im.getId());
    }

    private void finnBesteMatchUtfraOpprettetTidOgStartdato(InntektsmeldingEntitet im, List<ForespørselDto> matchendeForespørsler) {
        var forespørslerOpprettetFørImMedMatchendeStartdato = matchendeForespørsler.stream().filter(f -> f.opprettetTidspunkt().isBefore(im.getOpprettetTidspunkt())
            && im.getStartDato()
            .equals(f.førsteUttaksdato())).toList();
        if (forespørslerOpprettetFørImMedMatchendeStartdato.isEmpty()) {
            kastIngenForespørselFeil(im);
        } else if (forespørslerOpprettetFørImMedMatchendeStartdato.size() == 1) {
            oppdaterInntektsmeldingMedForespørsel(im, forespørslerOpprettetFørImMedMatchendeStartdato.getFirst());
        } else {
            // Her mistenker vi at kun arbeidsgiverinitiert IM gjennstår, da disse ikke nødvendigvis vil matche på startdato hvis den er blitt endret, men vi logger i første omgang for å være sikker
            var forespørselUuider = matchendeForespørsler.stream().map(ForespørselDto::uuid).toList();
            LOG.info("KOBLE_INNTEKTSMELDINGER_FEIL: Klarte ikke identifisere hvilken forespørsel inntektsmelding {} skal kobles til. Fant følgende forespørsler: {}", im.getId(), forespørselUuider);
        }
    }

    private void oppdaterInntektsmeldingMedForespørsel(InntektsmeldingEntitet im, ForespørselDto forespørsel) {
        inntektsmeldingTjeneste.oppdaterInntektsmelding(im, forespørsel);
    }

    private void lagNesteTask(long nyFom, Long tom, boolean dryRun) {
        var prosesstaskData = ProsessTaskData.forProsessTask(KobleInntektsmeldingTilForespørselTask.class);
        prosesstaskData.setProperty(FOM, String.valueOf(nyFom));
        prosesstaskData.setProperty(TOM, String.valueOf(tom));
        prosesstaskData.setProperty(DRY_RUN, String.valueOf(dryRun));

        prosessTaskTjeneste.lagre(prosesstaskData);
    }


    private List<InntektsmeldingEntitet> hentInntektsmeldinger(Long fom, Long tom) {
        var query = entityManager.createQuery("from InntektsmeldingEntitet where id >= :fom and id <= :tom"
            + " and kildesystem = :kilde and forespørsel is null order by id", InntektsmeldingEntitet.class);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        query.setParameter("kilde", Kildesystem.ARBEIDSGIVERPORTAL);

        query.setMaxResults(50);
        return query.getResultList();
    }
}

