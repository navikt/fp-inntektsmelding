package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.TelefonnummerResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class PersonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(PersonTjeneste.class);
    private static final LocalDate DUMMY_FØDSELSDATO = LocalDate.of(1900, 1, 1);
    private PdlKlient pdlKlient;

    PersonTjeneste() {
        // CDI
    }

    @Inject
    public PersonTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public PersonInfo hentPersonInfoFraAktørId(AktørIdEntitet aktørId, Ytelsetype ytelseType) {
        var request = new HentPersonQueryRequest();
        request.setIdent(aktørId.getAktørId());

        var projection = new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        PersonIdent personIdent = hentPersonidentForAktørId(aktørId).orElseThrow(
            () -> new IllegalStateException("Finner ikke personnummer for id " + aktørId));

        LOG.info("Henter personobjekt");
        var person = pdlKlient.hentPerson(utledYtelse(ytelseType), request, projection);

        var falskId = finnEvtFalskId(person, Optional.of(aktørId), personIdent);
        if (falskId != null) {
            return falskId;
        }

        var navn = person.getNavn().getFirst();

        return new PersonInfo(navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn(), personIdent, aktørId, mapFødselsdato(person), null, null);
    }

    public PersonInfo hentPersonFraIdent(PersonIdent personIdent, Ytelsetype ytelseType) {
        var request = new HentPersonQueryRequest();
        request.setIdent(personIdent.getIdent());

        var projection = new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .telefonnummer(new TelefonnummerResponseProjection().landskode().nummer())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var aktørId = finnAktørIdForIdent(personIdent);
        var person = pdlKlient.hentPerson(utledYtelse(ytelseType), request, projection);

        var falskId = finnEvtFalskId(person, aktørId, personIdent);
        if (falskId != null) {
            return falskId;
        }

        var navn = person.getNavn().getFirst();
        var kjønn = person.getKjoenn().isEmpty() ? PersonInfo.Kjønn.UKJENT : mapKjønn(person.getKjoenn().getFirst().getKjoenn());

        return new PersonInfo(navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn(), personIdent, aktørId.orElse(null), mapFødselsdato(person),
            mapTelefonnummer(person), kjønn);
    }

    private PersonInfo finnEvtFalskId(Person person, Optional<AktørIdEntitet> aktørId, PersonIdent personIdent) {
        if (PersonMappers.manglerIdentifikator(person)) {
            var brukId = aktørId.map(AktørIdEntitet::getAktørId).orElseGet(personIdent::getIdent);
            var falskId = FalskIdentitet.finnFalskIdentitet(brukId, pdlKlient).orElse(null);
            if (falskId != null) {
                var brukFødselsdato = Optional.ofNullable(falskId.fødselsdato()).orElse(DUMMY_FØDSELSDATO);
                return new PersonInfo(falskId.fornavn(), falskId.mellomnavn(), falskId.etternavn(), personIdent,
                    aktørId.orElse(null), brukFødselsdato, null, mapKjønn(falskId.kjønn()));
            }
        }
        return null;
    }

    private PersonInfo.Kjønn mapKjønn(KjoennType kjønn) {
        return switch (kjønn) {
            case MANN -> PersonInfo.Kjønn.MANN;
            case KVINNE -> PersonInfo.Kjønn.KVINNE;
            case UKJENT -> PersonInfo.Kjønn.UKJENT;
        };
    }

    private Optional<AktørIdEntitet> finnAktørIdForIdent(PersonIdent personIdent) {
        return pdlKlient.hentAktørIdForPersonIdent(personIdent.getIdent(), true).map(AktørIdEntitet::new);
    }

    public PersonIdent finnPersonIdentForAktørId(AktørIdEntitet aktørIdEntitet) {
        return hentPersonidentForAktørId(aktørIdEntitet).orElseThrow(
            () -> new IllegalStateException("Finner ikke personnummer for id " + aktørIdEntitet));
    }

    public PersonInfo hentInnloggetPerson(Ytelsetype ytelsetype) {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        return hentPersonFraIdent(PersonIdent.fra(pid), ytelsetype);
    }

    private LocalDate mapFødselsdato(Person person) {
        return PersonMappers.mapFødselsdato(person).orElse(null);
    }

    private String mapTelefonnummer(Person person) {
        return person.getTelefonnummer()
            .stream()
            .findFirst()
            .map(telefonnummer -> telefonnummer.getLandskode() + telefonnummer.getNummer())
            .orElse(null);
    }

    private Optional<PersonIdent> hentPersonidentForAktørId(AktørIdEntitet aktørId) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(aktørId.getAktørId());
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID));
        request.setHistorikk(Boolean.FALSE);
        var projection = new IdentlisteResponseProjection().identer(new IdentInformasjonResponseProjection().ident());
        try {
            LOG.info("Henter ident for person");
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).map(PersonIdent::new);
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                LOG.warn("Finner ikke person i PDL, returnerer tomt objekt. Gjelder aktørId {}", aktørId);
                return Optional.empty();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FT-723618", "PDL timeout") : e;
        }
    }

    private static Persondata.Ytelse utledYtelse(Ytelsetype ytelseType) {
        if (Ytelsetype.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return Persondata.Ytelse.SVANGERSKAPSPENGER;
        } else {
            return Persondata.Ytelse.FORELDREPENGER;
        }
    }

}
