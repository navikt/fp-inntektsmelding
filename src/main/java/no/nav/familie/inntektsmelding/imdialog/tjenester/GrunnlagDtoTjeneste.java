package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SlåOppArbeidstakerResponseDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidsforholdTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.AnsettelsesperiodeDto;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class GrunnlagDtoTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(GrunnlagDtoTjeneste.class);
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

        var organisasjonsnummer = forespørsel.getOrganisasjonsnummer();
        var personDto = lagPersonDto(forespørsel.getAktørId(), forespørsel.getYtelseType());
        var organisasjonDto = lagOrganisasjonDto(organisasjonsnummer);
        var innmelderDto = lagInnmelderDto(forespørsel.getYtelseType());
        var datoForInntekter = forespørsel.erArbeidsgiverInitiert() ? forespørsel.getFørsteUttaksdato() : forespørsel.getSkjæringstidspunkt().orElseThrow();
        var inntektDtoer = lagInntekterDto(forespørsel.getUuid(),
            forespørsel.getAktørId(),
            datoForInntekter,
            forespørsel.getOrganisasjonsnummer());

        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            inntektDtoer,
            datoForInntekter,
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()),
            forespørsel.getUuid(),
            KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()),
            forespørsel.getFørsteUttaksdato(),
            forespørsel.erArbeidsgiverInitiert() ? finnAnsettelsesperioder(new PersonIdent(personDto.fødselsnummer()), organisasjonsnummer, forespørsel.getFørsteUttaksdato()) : Collections.emptyList());
    }

    public InntektsmeldingDialogDto lagArbeidsgiverinitiertDialogDto(PersonIdent fødselsnummer,
                                                                          Ytelsetype ytelsetype,
                                                                          LocalDate førsteFraværsdag,
                                                                          String organisasjonsnummer) {
        var personInfo = finnPersoninfo(fødselsnummer, ytelsetype);

        var harForespørselPåOrgnrSisteTreMnd = finnForespørslerSisteTreÅr(ytelsetype, førsteFraværsdag, personInfo.aktørId()).stream()
            .filter(f -> f.getOrganisasjonsnummer().equals(organisasjonsnummer))
            .filter(f -> innenforIntervall(førsteFraværsdag, f.getFørsteUttaksdato()))
            .toList();

        if (!harForespørselPåOrgnrSisteTreMnd.isEmpty()) {
            var forespørsel = harForespørselPåOrgnrSisteTreMnd.stream()
                .max(Comparator.comparing(ForespørselEntitet::getFørsteUttaksdato))
                .orElseThrow(() -> new IllegalStateException("Finner ikke siste forespørsel"));
            if (forespørsel.getForespørselType().equals(ForespørselType.BESTILT_AV_FAGSYSTEM)) {
                MetrikkerTjeneste.loggRedirectFraAGITilVanligForespørsel(forespørsel);
            }
            return lagDialogDto(forespørsel.getUuid());
        }

        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        var organisasjonDto = lagOrganisasjonDto(organisasjonsnummer);
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
            finnAnsettelsesperioder(personInfo.fødselsnummer(), organisasjonsnummer, førsteFraværsdag));
    }

    private List<InntektsmeldingDialogDto.AnsettelsePeriodeDto> finnAnsettelsesperioder(PersonIdent fødselsnummer,
                                                                                        String organisasjonsnummer,
                                                                                        LocalDate førsteFraværsdag) {
        return arbeidsforholdTjeneste.hentArbeidsforhold(fødselsnummer, førsteFraværsdag).stream()
            .filter(arbeidsforhold -> arbeidsforhold.organisasjonsnummer().equals(organisasjonsnummer))
            .map(arbeidsforhold -> mapAnsettelsePeriode(arbeidsforhold.ansettelsesperiode()))
            .toList();
    }

    private InntektsmeldingDialogDto.AnsettelsePeriodeDto mapAnsettelsePeriode(AnsettelsesperiodeDto ansettelsesperiode) {
        if (ansettelsesperiode != null) {
            return new InntektsmeldingDialogDto.AnsettelsePeriodeDto(ansettelsesperiode.periode().fom(),
                ansettelsesperiode.periode().tom() != null ? ansettelsesperiode.periode().tom() : Tid.TIDENES_ENDE);
        }
        return null;
    }

    private boolean innnenforIntervallÅr(LocalDate førsteUttaksdato, LocalDate førsteFraværsdag) {
        if (førsteUttaksdato == null) {
            return false;
        }
        return (førsteUttaksdato.isEqual(førsteFraværsdag) || førsteUttaksdato.isBefore(førsteFraværsdag)) && førsteUttaksdato.isAfter(LocalDate.now().minusYears(3));
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

    private InntektsmeldingDialogDto.InntektsopplysningerDto lagInntekterDto(UUID uuid,
                                                                             AktørIdEntitet aktørId,
                                                                             LocalDate skjæringstidspunkt,
                                                                             String organisasjonsnummer) {
        var inntektsopplysninger = inntektTjeneste.hentInntekt(aktørId, skjæringstidspunkt, LocalDate.now(),
            organisasjonsnummer);
        if (uuid == null) {
            LOG.info("Inntektsopplysninger for aktørId {} var {}", aktørId, inntektsopplysninger);
        } else {
            LOG.info("Inntektsopplysninger for forespørsel {} var {}", uuid, inntektsopplysninger);
        }
        var inntekter = inntektsopplysninger.måneder()
            .stream()
            .map(i -> new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();
        return new InntektsmeldingDialogDto.InntektsopplysningerDto(inntektsopplysninger.gjennomsnitt(), inntekter);
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(String organisasjonsnummer) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer);
        return new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(AktørIdEntitet aktørId, Ytelsetype ytelseType) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelseType);
        return new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
    }

    public Optional<SlåOppArbeidstakerResponseDto> finnArbeidsforholdForFnr(PersonInfo personInfo,
                                                                            LocalDate førsteFraværsdag) {
        // TODO Skal vi sjekke noe mtp kode 6/7
        var arbeidsforholdBrukerHarTilgangTil = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(personInfo.fødselsnummer(), førsteFraværsdag);
        if (arbeidsforholdBrukerHarTilgangTil.isEmpty()) {
            return Optional.empty();
        }

        var arbeidsforholdDto = arbeidsforholdBrukerHarTilgangTil.stream()
            .map(a -> new SlåOppArbeidstakerResponseDto.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(a.organisasjonsnummer()).navn(),
                a.organisasjonsnummer()))
            .collect(Collectors.toSet());
        return Optional.of(new SlåOppArbeidstakerResponseDto(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforholdDto,
            personInfo.kjønn()));
    }

    public PersonInfo finnPersoninfo(PersonIdent fødselsnummer, Ytelsetype ytelsetype) {
        return personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype);
    }

    public List<ForespørselEntitet> finnForespørslerSisteTreÅr(Ytelsetype ytelsetype, LocalDate førsteFraværsdag, AktørIdEntitet aktørId) {
        return forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype).stream()
            .filter(eksF -> innnenforIntervallÅr(eksF.getFørsteUttaksdato(), førsteFraværsdag))
            .toList();
    }
}
