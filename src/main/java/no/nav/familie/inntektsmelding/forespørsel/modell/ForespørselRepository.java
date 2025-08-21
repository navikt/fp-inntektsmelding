package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@Dependent
public class ForespørselRepository {

    private EntityManager entityManager;
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRepository.class);

    public ForespørselRepository() {
    }

    @Inject
    public ForespørselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String fagsakSaksnummer,
                                 LocalDate førsteUttaksdato, ForespørselType forespørselType) {
        var forespørselEntitet = new ForespørselEntitet(orgnummer,
            skjæringstidspunkt,
            new AktørIdEntitet(aktørId),
            ytelsetype,
            fagsakSaksnummer,
            førsteUttaksdato,
            forespørselType);
        LOG.info("ForespørselRepository: lagrer forespørsel entitet: {}", forespørselEntitet);
        entityManager.persist(forespørselEntitet);
        entityManager.flush();
        return forespørselEntitet.getUuid();
    }

    public void oppdaterOppgaveId(UUID forespørselUUID, String oppgaveId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setOppgaveId(oppgaveId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public void oppdaterArbeidsgiverNotifikasjonSakId(UUID forespørselUUID, String arbeidsgiverNotifikasjonSakId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setArbeidsgiverNotifikasjonSakId(arbeidsgiverNotifikasjonSakId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID uuid) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where uuid = :foresporselUUID", ForespørselEntitet.class)
            .setParameter("foresporselUUID", uuid);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel for oppgitt uuid " + uuid);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public void ferdigstillForespørsel(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.FERDIG);
            entityManager.persist(f);
        });
        entityManager.flush();
    }

    public void settForespørselTilUtgått(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :SAK_ID", ForespørselEntitet.class)
            .setParameter("SAK_ID", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.UTGÅTT);
            entityManager.persist(f);
        });
        entityManager.flush();
    }


    public List<ForespørselEntitet> hentForespørsler(SaksnummerDto fagsakSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet f where fagsystemSaksnummer = :saksnr", ForespørselEntitet.class)
            .setParameter("saksnr", fagsakSaksnummer.saksnr());
        return query.getResultList();
    }

    public Optional<ForespørselEntitet> finnGjeldendeForespørsel(AktørIdEntitet aktørId,
                                                                 Ytelsetype ytelsetype,
                                                                 OrganisasjonsnummerDto organisasjonsnummer,
                                                                 LocalDate stp,
                                                                 SaksnummerDto fagsakSaksnummer,
                                                                 LocalDate førsteUttaksdato) {
        var arbeidsgiverIdent = organisasjonsnummer.orgnr();
        var query = entityManager.createQuery("FROM ForespørselEntitet where status in(:fpStatuser) "
                    + "and aktørId = :brukerAktørId "
                    + "and fagsystemSaksnummer = :fagsakNr "
                    + "and organisasjonsnummer = :arbeidsgiverIdent "
                    + "and skjæringstidspunkt = :skjæringstidspunkt "
                    + "and førsteUttaksdato = :førsteUttaksdato "
                    + "and ytelseType = :ytelsetype",
                ForespørselEntitet.class)
            .setParameter("fpStatuser", Set.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG))
            .setParameter("brukerAktørId", aktørId)
            .setParameter("fagsakNr", fagsakSaksnummer.saksnr())
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("skjæringstidspunkt", stp)
            .setParameter("førsteUttaksdato", førsteUttaksdato)
            .setParameter("ytelsetype", ytelsetype);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Forventet å finne kun en forespørsel for gitt sak {}, orgnr {}, skjæringstidspunkt {} og første uttaksdato" + fagsakSaksnummer
                    + organisasjonsnummer + stp + førsteUttaksdato);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> finnÅpenForespørsel(SaksnummerDto fagsystemSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status=:status " + "and fagsystemSaksnummer=:saksnummer",
                ForespørselEntitet.class)
            .setParameter("saksnummer", fagsystemSaksnummer.saksnr())
            .setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        return query.getResultList();
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(SaksnummerDto fagsakSaksnummer,
                                                            OrganisasjonsnummerDto organisasjonsnummer) {
        var arbeidsgiverIdent = organisasjonsnummer.orgnr();
        var query = entityManager.createQuery("FROM ForespørselEntitet where status = :fpStatus "
                    + "and fagsystemSaksnummer = :fagsakNr "
                    + "and organisasjonsnummer = :arbeidsgiverIdent ",
                ForespørselEntitet.class)
            .setParameter("fpStatus", ForespørselStatus.UNDER_BEHANDLING)
            .setParameter("fagsakNr", fagsakSaksnummer.saksnr())
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Forventet å finne kun en åpen forespørsel for gitt sak {} og orgnr {}" + fagsakSaksnummer
                    + organisasjonsnummer);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørId, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and status !=:utgått and ytelseType=:ytelseType",
                ForespørselEntitet.class)
            .setParameter("aktørId", aktørId)
            .setParameter("utgått", ForespørselStatus.UTGÅTT)
            .setParameter("ytelseType", ytelsetype);
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and ytelseType=:ytelseType and organisasjonsnummer=:orgnr",
                ForespørselEntitet.class)
            .setParameter("aktørId", aktørId)
            .setParameter("ytelseType", ytelsetype)
            .setParameter("orgnr", orgnr);
        return query.getResultList();
    }

    public ForespørselEntitet oppdaterFørsteUttaksdato(ForespørselEntitet forespørselEnitet, LocalDate førsteUttaksdato) {
        forespørselEnitet.setFørsteUttaksdato(førsteUttaksdato);
        entityManager.persist(forespørselEnitet);
        entityManager.flush();
        return forespørselEnitet;
    }

    public void oppdaterDialogportenUuid(UUID forespørselUuid, UUID dialogportenUuid) {
        var forespørselOpt = hentForespørsel(forespørselUuid);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setDialogportenUuid(dialogportenUuid);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }
}
