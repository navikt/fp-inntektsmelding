package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class ForespørselTjeneste {

    private ForespørselRepository forespørselRepository;

    @Inject
    public ForespørselTjeneste(ForespørselRepository forespørselRepository) {
        this.forespørselRepository = forespørselRepository;
    }

    public ForespørselTjeneste() {
    }

    public UUID opprettForespørsel(LocalDate skjæringstidspunkt,
                                   Ytelsetype ytelseType,
                                   AktørIdEntitet brukerAktørId,
                                   OrganisasjonsnummerDto orgnr,
                                   SaksnummerDto fagsakSaksnummer,
                                   LocalDate førsteUttaksdato) {
        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.getAktørId(), orgnr.orgnr(),
            fagsakSaksnummer.saksnr(), førsteUttaksdato, ForespørselType.BESTILT_AV_FAGSYSTEM);
    }

    public UUID opprettForespørselArbeidsgiverinitiert(Ytelsetype ytelseType,
                                                       AktørIdEntitet brukerAktørId,
                                                       OrganisasjonsnummerDto orgnr,
                                                       LocalDate førsteUttaksdato,
                                                       ForespørselType forespørselType,
                                                       LocalDate skjæringstidspunkt) {
        return forespørselRepository.lagreForespørsel(skjæringstidspunkt, ytelseType, brukerAktørId.getAktørId(), orgnr.orgnr(),
            null, førsteUttaksdato, forespørselType);
    }

    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }

    public ForespørselEntitet setFørsteUttaksdato(ForespørselEntitet forespørselEnitet, LocalDate førsteUttaksdato) {
        return forespørselRepository.oppdaterFørsteUttaksdato(forespørselEnitet, førsteUttaksdato);
    }

    public void setArbeidsgiverNotifikasjonSakId(UUID forespørselUUID, String arbeidsgiverNotifikasjonSakId) {
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUUID, arbeidsgiverNotifikasjonSakId);
    }

    public void ferdigstillForespørsel(String arbeidsgiverNotifikasjonSakId) {
        forespørselRepository.ferdigstillForespørsel(arbeidsgiverNotifikasjonSakId);
    }

    public void settForespørselTilUtgått(String arbeidsgiverNotifikasjonSakId) {
        forespørselRepository.settForespørselTilUtgått(arbeidsgiverNotifikasjonSakId);
    }

    public Optional<ForespørselEntitet> finnGjeldendeForespørsel(LocalDate skjæringstidspunkt,
                                                                 Ytelsetype ytelseType,
                                                                 AktørIdEntitet brukerAktørId,
                                                                 OrganisasjonsnummerDto orgnr,
                                                                 SaksnummerDto fagsakSaksnummer, LocalDate førsteUttaksdato) {
        return forespørselRepository.finnGjeldendeForespørsel(brukerAktørId, ytelseType, orgnr, skjæringstidspunkt, fagsakSaksnummer, førsteUttaksdato);
    }

    public List<ForespørselEntitet> finnÅpneForespørslerForFagsak(SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.finnÅpenForespørsel(fagsakSaksnummer);
    }

    public Optional<ForespørselEntitet> finnÅpenForespørslelForFagsak(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnr) {
        return forespørselRepository.finnÅpenForespørsel(fagsakSaksnummer, orgnr);
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUuid) {
        return forespørselRepository.hentForespørsel(forespørselUuid);
    }

    public List<ForespørselEntitet> finnForespørslerForAktørid(AktørIdEntitet aktørId, Ytelsetype ytelsetype) {
        return forespørselRepository.finnForespørslerForAktørId(aktørId, ytelsetype);
    }

    public List<ForespørselEntitet> finnForespørslerForFagsak(SaksnummerDto fagsakSaksnummer) {
        return forespørselRepository.hentForespørsler(fagsakSaksnummer);
    }

    public List<ForespørselEntitet> finnForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselRepository.finnForespørsler(aktørId, ytelsetype, orgnr);
    }

    public void setDialogportenUuid(UUID forespørselUuid, UUID dialogportenUuid) {
        forespørselRepository.oppdaterDialogportenUuid(forespørselUuid, dialogportenUuid);

    }
}
