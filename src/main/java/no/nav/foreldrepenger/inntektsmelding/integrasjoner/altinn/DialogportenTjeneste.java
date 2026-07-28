package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class DialogportenTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DialogportenTjeneste.class);
    private static final Environment ENV = Environment.current();

    private DialogportenKlient dialogportenKlient;
    private PersonTjeneste personTjeneste;

    public DialogportenTjeneste() {
        // CDI
    }

    @Inject
    public DialogportenTjeneste(DialogportenKlient dialogportenKlient, PersonTjeneste personTjeneste) {
        this.dialogportenKlient = dialogportenKlient;
        this.personTjeneste = personTjeneste;
    }

    public UUID opprettDialog(ForespørselDto forespørsel) {
        var saksTittel = lagSaksTittel(forespørsel);

        var dialogPortenUuid = dialogportenKlient.opprettDialog(forespørsel.uuid(),
            forespørsel.arbeidsgiver(), saksTittel, forespørsel.førsteUttaksdato(), forespørsel.ytelseType());

        var vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        return UUID.fromString(vasketDialogUuid);
    }

    public void ferdigstillDialog(ForespørselDto forespørsel, LukkeÅrsak årsak, Optional<UUID> inntektsmeldingUuid) {
        if (forespørsel.dialogportenUuid() == null) {
            return;
        }

        var saksTittel = lagSaksTittel(forespørsel);

        dialogportenKlient.ferdigstillDialog(forespørsel.dialogportenUuid(),
            forespørsel.arbeidsgiver(),
            saksTittel,
            forespørsel.ytelseType(),
            forespørsel.førsteUttaksdato(),
            inntektsmeldingUuid,
            årsak);
    }

    public void settDialogTilUtgått(ForespørselDto forespørsel) {
        if (forespørsel.dialogportenUuid() == null) {
            return;
        }

        var saksTittel = lagSaksTittel(forespørsel);
        dialogportenKlient.settDialogTilUtgått(forespørsel.dialogportenUuid(), saksTittel);
    }

    public void oppdaterDialogMedEndretInntektsmelding(ForespørselDto forespørsel, Optional<UUID> inntektsmeldingUuid) {
        if (forespørsel.dialogportenUuid() == null) {
            return;
        }

        dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(forespørsel.dialogportenUuid(),
            forespørsel.arbeidsgiver(),
            inntektsmeldingUuid);
    }

    public void utførMedFeiltoleranse(Runnable handling) {
        try {
            handling.run();
        } catch (Exception e) {
            if (ENV.isProd()) {
                throw new IllegalStateException("Feil ved kall til dialogporten: " + e.getMessage(), e);
            } else {
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }
    }

    private String lagSaksTittel(ForespørselDto forespørsel) {
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }
}
