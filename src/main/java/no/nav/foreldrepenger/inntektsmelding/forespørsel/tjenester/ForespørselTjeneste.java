package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

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
                                   AktørId brukerAktørId,
                                   Arbeidsgiver arbeidsgiver,
                                   Saksnummer fagsakSaksnummer,
                                   LocalDate førsteUttaksdato) {
        return forespørselRepository.lagreForespørsel(new ForespørselEntitet(arbeidsgiver.orgnr(),
            skjæringstidspunkt,
            new AktørIdEntitet(brukerAktørId.getAktørId()),
            ytelseType,
            fagsakSaksnummer.saksnummer(),
            førsteUttaksdato,
            ForespørselType.BESTILT_AV_FAGSYSTEM));
    }

    public UUID opprettForespørselArbeidsgiverinitiert(Ytelsetype ytelseType,
                                                       AktørId brukerAktørId,
                                                       Arbeidsgiver arbeidsgiver,
                                                       LocalDate førsteUttaksdato,
                                                       ForespørselType forespørselType,
                                                       LocalDate skjæringstidspunkt) {
        return forespørselRepository.lagreForespørsel(new ForespørselEntitet(arbeidsgiver.orgnr(),
            skjæringstidspunkt,
            new AktørIdEntitet(brukerAktørId.getAktørId()),
            ytelseType,
            null,
            førsteUttaksdato,
            forespørselType));
    }

    public void setOppgaveId(UUID forespørselUUID, String oppgaveId) {
        forespørselRepository.oppdaterOppgaveId(forespørselUUID, oppgaveId);
    }

    public ForespørselDto setFørsteUttaksdato(UUID forespørselUUID, LocalDate førsteUttaksdato) {
        return ForespørselDtoMapper.mapFraEntitet(forespørselRepository.oppdaterFørsteUttaksdato(forespørselUUID, førsteUttaksdato));
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

    public Optional<ForespørselDto> finnGjeldendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelseType,
                                                             AktørId brukerAktørId,
                                                             Arbeidsgiver arbeidsgiver,
                                                             Saksnummer fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        return forespørselRepository.finnGjeldendeForespørsel(mapTilEntitet(brukerAktørId), ytelseType,
                skjæringstidspunkt, arbeidsgiver.orgnr(), fagsakSaksnummer.saksnummer(), førsteUttaksdato)
            .map(ForespørselDtoMapper::mapFraEntitet);
    }

    public List<ForespørselDto> finnÅpneForespørslerForFagsak(Saksnummer fagsakSaksnummer) {
        return forespørselRepository.finnÅpenForespørsel(fagsakSaksnummer.saksnummer()).stream().map(ForespørselDtoMapper::mapFraEntitet).toList();
    }

    public Optional<ForespørselDto> finnÅpenForespørslelForFagsak(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver) {
        return forespørselRepository.finnÅpenForespørsel(fagsakSaksnummer.saksnummer(), arbeidsgiver.orgnr()).map(ForespørselDtoMapper::mapFraEntitet);
    }

    public Optional<ForespørselDto> hentForespørsel(UUID forespørselUuid) {
        return forespørselRepository.hentForespørsel(forespørselUuid).map(ForespørselDtoMapper::mapFraEntitet);
    }

    public List<ForespørselDto> finnForespørslerForAktørid(AktørId aktørId, Ytelsetype ytelsetype) {
        return forespørselRepository.finnForespørslerForAktørId(mapTilEntitet(aktørId), ytelsetype)
            .stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
    }

    public List<ForespørselDto> finnForespørslerForFagsak(Saksnummer fagsakSaksnummer) {
        return forespørselRepository.hentForespørslerPåSak(fagsakSaksnummer.saksnummer()).stream().map(ForespørselDtoMapper::mapFraEntitet).toList();
    }

    public List<ForespørselDto> finnForespørsler(AktørId aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselRepository.finnForespørsler(mapTilEntitet(aktørId), ytelsetype, orgnr)
            .stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
    }

    public void setDialogportenUuid(UUID forespørselUuid, UUID dialogportenUuid) {
        forespørselRepository.oppdaterDialogportenUuid(forespørselUuid, dialogportenUuid);

    }

    public List<ForespørselDto> hentForespørsler(Arbeidsgiver arbeidsgiver,
                                                 AktørId aktørId,
                                                 ForespørselStatus status,
                                                 Ytelsetype ytelseType,
                                                 LocalDate fom, LocalDate tom) {
        return forespørselRepository.hentForespørslerFraFilter(arbeidsgiver.orgnr(), mapTilEntitet(aktørId), status, ytelseType, fom, tom).stream()
            .map(ForespørselDtoMapper::mapFraEntitet).toList();
    }

    private AktørIdEntitet mapTilEntitet(AktørId aktørId) {
        return Optional.ofNullable(aktørId).map(aktør -> new AktørIdEntitet(aktør.getAktørId())).orElse(null);
    }

}
