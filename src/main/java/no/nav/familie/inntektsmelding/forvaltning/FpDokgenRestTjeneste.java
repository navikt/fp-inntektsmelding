package no.nav.familie.inntektsmelding.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper.mapArbeidsgiverinitiertÅrsak;
import static no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper.mapEndringsårsak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.ArbeidsgiverinitiertÅrsak;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@Path(FpDokgenRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@AutentisertMedAzure
/*Denne tjenesten er ment brukt til testformål, og eventuelt for å gjenskape feilsituasjoner i produksjon*/
public class FpDokgenRestTjeneste {
    public static final String BASE_PATH = "/inntektsmelding-pdf";
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private FpDokgenTjeneste fpDokgenTjeneste;
    private Tilgang tilgang;

    private InntektsmeldingRepository inntektsmeldingRepository;

    public FpDokgenRestTjeneste() {
        //CDI
    }

    @Inject
    public FpDokgenRestTjeneste(FpDokgenTjeneste fpDokgenTjeneste, Tilgang tilgang, InntektsmeldingRepository inntektsmeldingRepository) {
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.tilgang = tilgang;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces("application/pdf")
    @Operation(description = "Generer en pdf av en inntektsmelding", tags = "forvaltning")
    @Tilgangskontrollert
    public Response genererPdf(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtKallerHarRollenDrift();

        InntektsmeldingEntitet inntektsmeldingEntitet;
        if (inntektsmeldingRequest.inntektsmeldingId != null) {
            inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingRequest.inntektsmeldingId.intValue());
            LOG.info("Generer en pdf av en inntektsmelding med id: {}", inntektsmeldingRequest.inntektsmeldingId);
        } else {
            //For testformål
            var builder = InntektsmeldingEntitet.builder()
                .medAktørId(AktørIdEntitet.dummy())
                .medKontaktperson(new KontaktpersonEntitet(inntektsmeldingRequest.kontaktpersonNavn, inntektsmeldingRequest.kontaktpersonTlf))
                .medMånedInntekt(inntektsmeldingRequest.maanedInntekt())
                .medYtelsetype(mapYtelseType(inntektsmeldingRequest.ytelsetype()))
                .medOpprettetTidspunkt(LocalDateTime.now())
                .medMånedRefusjon(inntektsmeldingRequest.maanedRefusjon())
                .medRefusjonOpphørsdato(inntektsmeldingRequest.opphoersdatoRefusjon())
                .medStartDato(inntektsmeldingRequest.startdatoPermisjon())
                .medArbeidsgiverIdent(inntektsmeldingRequest.arbeidsgiverIdent());

            if(inntektsmeldingRequest.endringsårsaker() != null) {
                builder.medEndringsårsaker(mapEndringsårsker(inntektsmeldingRequest.endringsårsaker()));
            }

            if (inntektsmeldingRequest.opphoersdatoRefusjon() != null) {
                builder.medRefusjonOpphørsdato(inntektsmeldingRequest.opphoersdatoRefusjon());
            } else {
                builder.medRefusjonOpphørsdato(Tid.TIDENES_ENDE);
            }

            if (inntektsmeldingRequest.refusjon() != null) {
                builder.medRefusjonsendringer(mapRefusjonsendringer(inntektsmeldingRequest.startdatoPermisjon(), inntektsmeldingRequest.opphoersdatoRefusjon(), inntektsmeldingRequest.refusjon() ));
            }
            if (inntektsmeldingRequest.naturalytelser() != null) {
                builder.medBortfaltNaturalytelser(mapBortfalteNaturalytelser(inntektsmeldingRequest.naturalytelser));
            }
            inntektsmeldingEntitet = builder.build();
        }

        var arbeidsgiverinitiertÅrsak = mapArbeidsgiverinitiertÅrsak(inntektsmeldingRequest.arbeidsgiverinitiertÅrsakDto);

        var pdf = fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet, utledForespørselTypeForPdf(arbeidsgiverinitiertÅrsak));

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=dokument.pdf");
        return responseBuilder.build();
    }

    private ForespørselType utledForespørselTypeForPdf(ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak) {
        if (arbeidsgiverinitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            return ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT;
        }
        return ForespørselType.BESTILT_AV_FAGSYSTEM;
    }

    private List<EndringsårsakEntitet> mapEndringsårsker(List<EndringsårsakerDto> endringsårsaker) {
        return endringsårsaker.stream().map(endringsårsak -> new EndringsårsakEntitet.Builder().
                medÅrsak(mapEndringsårsak(endringsårsak.aarsak()))
                .medFom(endringsårsak.fom())
                .medTom(endringsårsak.tom())
                .medBleKjentFra(endringsårsak.bleKjentFom())
                .build())
            .toList();
    }

    private List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato, LocalDate opphørsdato, List<Refusjon> refusjonsendring) {
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendring.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beloep()))
            .toList();
    }


    private List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<BortfaltNaturalytelseDto> naturalYtelser) {
        return naturalYtelser.stream()
            .map(periode -> new BortaltNaturalytelseEntitet.Builder().medPeriode(periode.fom(), periode.tom())
                .medType(periode.type())
                .medMånedBeløp(periode.maanedBortfaltNaturalytelse())
                .build())
            .toList();
    }

    private Ytelsetype mapYtelseType(String ytelsetype) {
        return switch (ytelsetype.toLowerCase()) {
            case "foreldrepenger" -> Ytelsetype.FORELDREPENGER;
            case "svangerskapspenger" -> Ytelsetype.SVANGERSKAPSPENGER;
            default -> throw new IllegalArgumentException("Ugyldig ytelsetype: " + ytelsetype);
        };
    }

    public record InntektsmeldingRequest(Long inntektsmeldingId, String ytelsetype, String arbeidsgiverIdent, String kontaktpersonNavn,
                                         String kontaktpersonTlf, LocalDate startdatoPermisjon, LocalDate opphoersdatoRefusjon,
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedRefusjon,
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedInntekt,
                                         List<Refusjon> refusjon, List<BortfaltNaturalytelseDto> naturalytelser,
                                         List<EndringsårsakerDto> endringsårsaker,
                                         @NotNull @Valid ArbeidsgiverinitiertÅrsakDto arbeidsgiverinitiertÅrsakDto) {
    }

    public record Refusjon(@NotNull LocalDate fom,
                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloep) {
    }

    public record BortfaltNaturalytelseDto(@NotNull LocalDate fom, LocalDate tom, NaturalytelseType type,
                                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedBortfaltNaturalytelse) {
    }

    public record EndringsårsakerDto(@NotNull EndringsårsakDto aarsak, LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
