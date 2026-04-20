package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SlåOppArbeidstakerResponseDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.Arbeidsforhold;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.exceptions.InntektsmeldingException;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class GrunnlagDtoTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    GrunnlagDtoTjeneste() {
        // CDI
    }

    @Inject
    public GrunnlagDtoTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                               PersonTjeneste personTjeneste,
                               OrganisasjonTjeneste organisasjonTjeneste,
                               InntektTjeneste inntektTjeneste,
                               ArbeidstakerTjeneste arbeidstakerTjeneste,
                               ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException(
                "Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        var organisasjonsnummer = forespørsel.arbeidsgiver();
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
        var personDto = lagPersonDto(personInfo);
        var organisasjonDto = lagOrganisasjonDto(organisasjonsnummer);
        var innmelderDto = lagInnmelderDto(forespørsel.ytelseType());
        var erArbeidsgiverInitiertNyansatt = ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT.equals(forespørsel.forespørselType());
        var datoForInntekter = erArbeidsgiverInitiertNyansatt ? forespørsel.førsteUttaksdato() : Optional.ofNullable(forespørsel.skjæringstidspunkt()).orElse(forespørsel.førsteUttaksdato());
        var inntektDtoer = lagInntekterDto(personInfo,
            datoForInntekter,
            forespørsel.arbeidsgiver());

        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            inntektDtoer,
            datoForInntekter,
            KodeverkMapper.mapYtelsetype(forespørsel.ytelseType()),
            forespørsel.uuid(),
            KodeverkMapper.mapForespørselStatus(forespørsel.status()),
            forespørsel.førsteUttaksdato(),
            erArbeidsgiverInitiertNyansatt ? finnAnsettelsesperioder(new PersonIdent(personDto.fødselsnummer()),
                organisasjonsnummer,
                forespørsel.førsteUttaksdato()) : Collections.emptyList());
    }

    public InntektsmeldingDialogDto lagArbeidsgiverinitiertNyansattDialogDto(PersonIdent fødselsnummer,
                                                                             Ytelsetype ytelsetype,
                                                                             LocalDate førsteFraværsdag,
                                                                             Arbeidsgiver arbeidsgiver) {
        var personInfo = finnPersoninfo(fødselsnummer, ytelsetype);

        var harForespørselPåOrgnrSisteTreMnd = finnForespørslerSisteTreÅr(ytelsetype, førsteFraværsdag, personInfo.aktørId()).stream()
            .filter(f -> f.arbeidsgiver().orgnr().equals(arbeidsgiver.orgnr()))
            .filter(f -> innenforIntervall(førsteFraværsdag, f.førsteUttaksdato()))
            .toList();

        if (!harForespørselPåOrgnrSisteTreMnd.isEmpty()) {
            var forespørsel = harForespørselPåOrgnrSisteTreMnd.stream()
                .max(Comparator.comparing(ForespørselDto::førsteUttaksdato))
                .orElseThrow(() -> new IllegalStateException("Finner ikke siste forespørsel"));
            if (forespørsel.forespørselType().equals(ForespørselType.BESTILT_AV_FAGSYSTEM)) {
                MetrikkerTjeneste.loggRedirectFraAGITilVanligForespørsel(forespørsel);
            }
            return lagDialogDto(forespørsel.uuid());
        }

        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        var organisasjonDto = lagOrganisasjonDto(arbeidsgiver);
        var innmelderDto = lagInnmelderDto(ytelsetype);

        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            // Vi preutfyller ikke inntekter på arbeidsgiverinitiert inntektsmelding(nyansatt)
            new InntektsmeldingDialogDto.InntektsopplysningerDto(null, Collections.emptyList()),
            førsteFraværsdag,
            KodeverkMapper.mapYtelsetype(ytelsetype),
            null,
            KodeverkMapper.mapForespørselStatus(ForespørselStatus.UNDER_BEHANDLING),
            førsteFraværsdag,
            finnAnsettelsesperioder(personInfo.fødselsnummer(), arbeidsgiver, førsteFraværsdag));
    }

    public InntektsmeldingDialogDto lagArbeidsgiverinitiertUregistrertDialogDto(PersonIdent fødselsnummer, Ytelsetype ytelsetype, LocalDate førsteUttaksdato, Arbeidsgiver arbeidsgiver,
                                                                                LocalDate skjæringstidspunkt) {
        var personInfo = finnPersoninfo(fødselsnummer, ytelsetype);

        var eksisterendeForespørselPåUttaksdato = finnForespørslerSisteTreÅr(ytelsetype, førsteUttaksdato, personInfo.aktørId()).stream()
            .filter(f -> f.arbeidsgiver().equals(arbeidsgiver))
            .filter(f -> førsteUttaksdato.isEqual(f.førsteUttaksdato()) && f.skjæringstidspunkt() != null
                && skjæringstidspunkt.isEqual(f.skjæringstidspunkt()))
            .max(Comparator.comparing(ForespørselDto::opprettetTidspunkt));

        if (eksisterendeForespørselPåUttaksdato.isPresent()) {
            var forespørsel = eksisterendeForespørselPåUttaksdato.get();

            if (forespørsel.forespørselType().equals(ForespørselType.BESTILT_AV_FAGSYSTEM)) {
                MetrikkerTjeneste.loggRedirectFraAGITilVanligForespørsel(forespørsel);
            }
            return lagDialogDto(forespørsel.uuid());
        }
        //Er denne sjekken i det hele tatt er nødvendig?
        var finnesOrgnummerIAaReg = finnesOrgnummerIAaregPåPerson(fødselsnummer, arbeidsgiver.orgnr(), førsteUttaksdato);
        if (finnesOrgnummerIAaReg) {
            throw new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.FINNES_I_AAREG);
        }

        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        var organisasjonDto = lagOrganisasjonDto(arbeidsgiver);
        var innmelderDto = lagInnmelderDto(ytelsetype);

        var inntektDtoer = lagInntekterDto(personInfo,
            skjæringstidspunkt.isEqual(Tid.TIDENES_ENDE) ? førsteUttaksdato : skjæringstidspunkt,
            arbeidsgiver);

        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            inntektDtoer,
            skjæringstidspunkt.isEqual(Tid.TIDENES_ENDE) ? førsteUttaksdato : skjæringstidspunkt,
            KodeverkMapper.mapYtelsetype(ytelsetype),
            null,
            KodeverkMapper.mapForespørselStatus(ForespørselStatus.UNDER_BEHANDLING),
            førsteUttaksdato,
            Collections.emptyList());
    }

    private List<InntektsmeldingDialogDto.AnsettelsePeriodeDto> finnAnsettelsesperioder(PersonIdent fødselsnummer,
                                                                                        Arbeidsgiver arbeidsgiver,
                                                                                        LocalDate førsteFraværsdag) {
        return arbeidsforholdTjeneste.hentArbeidsforhold(fødselsnummer, førsteFraværsdag).stream()
            .filter(arbeidsforhold -> arbeidsforhold.organisasjonsnummer().equals(arbeidsgiver.orgnr()))
            .map(arbeidsforhold -> mapAnsettelsePeriode(arbeidsforhold.ansettelsesperiode()))
            .toList();
    }

    private InntektsmeldingDialogDto.AnsettelsePeriodeDto mapAnsettelsePeriode(Arbeidsforhold.Ansettelsesperiode ansettelsesperiode) {
        if (ansettelsesperiode != null) {
            return new InntektsmeldingDialogDto.AnsettelsePeriodeDto(ansettelsesperiode.fom(), ansettelsesperiode.tom());
        }
        return null;
    }

    private boolean innnenforIntervallÅr(LocalDate førsteUttaksdato, LocalDate førsteFraværsdag) {
        if (førsteUttaksdato == null) {
            return false;
        }
        return (førsteUttaksdato.isEqual(førsteFraværsdag) || førsteUttaksdato.isBefore(førsteFraværsdag)) && førsteUttaksdato.isAfter(LocalDate.now()
            .minusYears(3));
    }

    private boolean innenforIntervall(LocalDate førsteFraværsdag, LocalDate førsteUttaksdato) {
        if (førsteUttaksdato == null) {
            return false;
        }
        return førsteFraværsdag.isAfter(førsteUttaksdato.minusMonths(3)) && førsteFraværsdag.isBefore(førsteUttaksdato.plusMonths(3));
    }

    private InntektsmeldingDialogDto.InnsenderDto lagInnmelderDto(Ytelsetype ytelsetype) {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = finnPersoninfo(PersonIdent.fra(pid), ytelsetype);
        return new InntektsmeldingDialogDto.InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.telefonnummer());
    }

    private InntektsmeldingDialogDto.InntektsopplysningerDto lagInntekterDto(PersonInfo personinfo,
                                                                             LocalDate skjæringstidspunkt,
                                                                             Arbeidsgiver arbeidsgiver) {
        var harJobbetHeleBeregningsperioden = harJobbetHeleBeregningsperioden(personinfo, skjæringstidspunkt, arbeidsgiver);
        var inntektsopplysninger = inntektTjeneste.hentInntekt(personinfo.aktørId(), skjæringstidspunkt, LocalDate.now(),
            arbeidsgiver, harJobbetHeleBeregningsperioden);
        var inntekter = inntektsopplysninger.måneder()
            .stream()
            .map(i -> new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();
        return new InntektsmeldingDialogDto.InntektsopplysningerDto(inntektsopplysninger.gjennomsnitt(), inntekter);
    }

    private boolean harJobbetHeleBeregningsperioden(PersonInfo personinfo, LocalDate skjæringstidspunkt, Arbeidsgiver arbeidsgiver) {
        var førsteDagIBeregningsperiode = skjæringstidspunkt.minusMonths(3).withDayOfMonth(1);
        return arbeidsforholdTjeneste.hentArbeidsforhold(personinfo.fødselsnummer(), skjæringstidspunkt).stream()
            .filter(af -> af.organisasjonsnummer().equals(arbeidsgiver.orgnr()))
            .anyMatch(af -> af.ansettelsesperiode().fom().isBefore(førsteDagIBeregningsperiode));
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(Arbeidsgiver arbeidsgiver) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        return new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(PersonInfo personInfo) {
        return new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
    }

    public Optional<SlåOppArbeidstakerResponseDto> finnArbeidsforholdForFnr(PersonInfo personInfo,
                                                                            LocalDate førsteFraværsdag) {
        // TODO Skal vi sjekke noe mtp kode 6/7
        var arbeidsforholdSøkerHarHosArbeidsgiver = arbeidstakerTjeneste.finnSøkersArbeidsforholdSomArbeidsgiverHarTilgangTil(personInfo.fødselsnummer(),
            førsteFraværsdag);
        if (arbeidsforholdSøkerHarHosArbeidsgiver.isEmpty()) {
            return Optional.empty();
        }

        var arbeidsforholdDto = arbeidsforholdSøkerHarHosArbeidsgiver.stream()
            .map(a -> new SlåOppArbeidstakerResponseDto.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(Arbeidsgiver.fra(a.organisasjonsnummer())).navn(),
                a.organisasjonsnummer()))
            .collect(Collectors.toSet());
        return Optional.of(new SlåOppArbeidstakerResponseDto(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforholdDto,
            personInfo.kjønn()));
    }


    public Optional<SlåOppArbeidstakerResponseDto> hentSøkerinfoOgOrganisasjonerArbeidsgiverHarTilgangTil(PersonInfo personInfo) {
        var organisasjonerArbeidsgiverHarTilgangTil = arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil(personInfo.fødselsnummer());

        var organisasjoner = organisasjonerArbeidsgiverHarTilgangTil.stream()
            .map(orgnrDto -> new SlåOppArbeidstakerResponseDto.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(orgnrDto).navn(),
                orgnrDto.orgnr()))
            .collect(Collectors.toSet());
        return Optional.of(new SlåOppArbeidstakerResponseDto(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            organisasjoner,
            personInfo.kjønn()));
    }

    public PersonInfo finnPersoninfo(PersonIdent fødselsnummer, Ytelsetype ytelsetype) {
        return personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype);
    }

    public List<ForespørselDto> finnForespørslerSisteTreÅr(Ytelsetype ytelsetype, LocalDate førsteFraværsdag, AktørId aktørId) {
        return forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype).stream()
            .filter(eksF -> innnenforIntervallÅr(eksF.førsteUttaksdato(), førsteFraværsdag))
            .toList();
    }

    public boolean finnesOrgnummerIAaregPåPerson(PersonIdent personIdent,
                                                 String organisasjonsnummer,
                                                 LocalDate førsteUttaksdato) {
        return arbeidsforholdTjeneste.hentArbeidsforhold(personIdent, førsteUttaksdato).stream()
            .filter(arbeidsforhold -> arbeidsforhold.organisasjonsnummer().equals(organisasjonsnummer))
            .anyMatch(arbeidsforhold -> inkludererDato(førsteUttaksdato,
                arbeidsforhold.ansettelsesperiode().fom(),
                arbeidsforhold.ansettelsesperiode().tom()));
    }

    private boolean inkludererDato(LocalDate førsteUttaksdato, LocalDate fom, LocalDate tom) {
        var fomLikEllerEtter = førsteUttaksdato.isEqual(fom) || førsteUttaksdato.isAfter(fom);
        var tomLikEllerFør = førsteUttaksdato.isEqual(tom) || førsteUttaksdato.isBefore(tom);
        return fomLikEllerEtter && tomLikEllerFør;
    }
}
