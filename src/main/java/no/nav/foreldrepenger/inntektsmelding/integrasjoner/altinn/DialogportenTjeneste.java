package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;

@ApplicationScoped
public class DialogportenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenTjeneste.class);

    private DialogportenKlient dialogportenKlient;
    private PersonTjeneste personTjeneste;
    protected DialogportenTjeneste() {
        // CDI
    }

    @Inject
    public DialogportenTjeneste(DialogportenKlient dialogportenKlient, PersonTjeneste personTjeneste) {
        this.dialogportenKlient = dialogportenKlient;
        this.personTjeneste = personTjeneste;
    }

    public UUID opprettSakDialogporten(ForespørselDto forespørselDto) {
        var saksTittelDialog = lagSakstittel(forespørselDto);

        var dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselDto.uuid(),
            forespørselDto.arbeidsgiver(), saksTittelDialog, forespørselDto.førsteUttaksdato(), forespørselDto.ytelseType());
        var vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);

        return UUID.fromString(vasketDialogUuid);
    }

    public void settSakTilUtgått(ForespørselDto forespørselDto) {
        var saksTittelDialog = lagSakstittel(forespørselDto);
        Optional.ofNullable(forespørselDto.dialogportenUuid()).ifPresent(d -> dialogportenKlient.settDialogTilUtgått(d, saksTittelDialog));
    }

    private String lagSakstittel(ForespørselDto forespørselDto) {
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørselDto.aktørId(), forespørselDto.ytelseType());
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }

    public void ferdigstillDialog(ForespørselDto forespørselDto, Optional<UUID> inntektsmeldingUuid, LukkeÅrsak lukkeÅrsak) {
        dialogportenKlient.ferdigstillDialog(forespørselDto.dialogportenUuid(),
            forespørselDto.arbeidsgiver(),
            lagSakstittel(forespørselDto),
            forespørselDto.ytelseType(),
            forespørselDto.førsteUttaksdato(),
            inntektsmeldingUuid,
            lukkeÅrsak);

    }
}
