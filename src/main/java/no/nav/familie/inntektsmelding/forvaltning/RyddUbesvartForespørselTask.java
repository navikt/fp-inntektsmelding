package no.nav.familie.inntektsmelding.forvaltning;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "rydd.ubesvart.forespoersel", maxFailedRuns = 1)
public class RyddUbesvartForespørselTask implements ProsessTaskHandler {
    static final String FORESPØRSEL_UUID = "foresporselUuid";
    static final String DRY_RUN = "dryRun";
    private static final Logger LOG = LoggerFactory.getLogger(RyddUbesvartForespørselTask.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FpsakKlient fpsakKlient;

    RyddUbesvartForespørselTask() {
        // for CDI proxy
    }

    @Inject
    public RyddUbesvartForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                       FpsakKlient fpsakKlient) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.fpsakKlient = fpsakKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);
        var forespørselUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID)).map(String::valueOf).orElseThrow();
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(UUID.fromString(forespørselUuid)).orElseThrow();

        var forespørselSkalBeholdes = fpsakKlient.erInntektsmeldingFortsattPåkrevd(forespørsel);
        var stp = forespørsel.getSkjæringstidspunkt().orElseThrow();

        String loggKode = forespørselSkalBeholdes ? "UBESVART_FORESPØRSEL_MÅ_BEHOLDES" : "UBESVART_FORESPØRSEL_KAN_SLETTES";
        LOG.info("{}: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {}",
            loggKode,
            forespørsel.getUuid(),
            Optional.ofNullable(forespørsel.getOppgaveId()),
            forespørsel.getFagsystemSaksnummer().orElseThrow(),
            forespørsel.getOrganisasjonsnummer(),
            stp);
        if (!dryRun && !forespørselSkalBeholdes) {
            LOG.info("{}: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {} blir slettet",
                loggKode,
                forespørsel.getUuid(),
                Optional.ofNullable(forespørsel.getOppgaveId()),
                forespørsel.getFagsystemSaksnummer().orElseThrow(),
                forespørsel.getOrganisasjonsnummer(),
                stp);
            forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(forespørsel.getFagsystemSaksnummer().orElseThrow()),
                new OrganisasjonsnummerDto(forespørsel.getOrganisasjonsnummer()),
                stp);
        }
    }
}

