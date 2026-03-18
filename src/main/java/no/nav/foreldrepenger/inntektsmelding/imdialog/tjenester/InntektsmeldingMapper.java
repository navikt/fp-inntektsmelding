package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMapper {

    private InntektsmeldingMapper() {
        // Skjuler default konstruktør
    }

    public static InntektsmeldingDto mapTilDtoArbeidsgiverinitiert(SendInntektsmeldingRequestDto dto) {
        // Frontend sender kun inn liste med refusjon. Vi utleder startsum og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(dto.refusjon(), dto.startdato())
            .orElseThrow(() -> new IllegalStateException("Finner ikke refusjon på arbeidsgiverinitiert inntektsmeldsing, ugyldig tilstand"));
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(dto.refusjon(), dto.startdato()).orElse(Tid.TIDENES_ENDE);
        var builder = opprettDtoBuilderOgSettFellesFelter(dto);
        // Vi ønsker ikke be arbeidsgiver om inntekt i disse tilfellene da de sjelden har hatt utbetaling som nyansatt, og dette uansett ikke vil brukes i saksbehandlingen.
        // Setter derfor samme beløp som refusjon
        return builder
            .medInntekt(refusjonPrMnd)
            .medMånedRefusjon(refusjonPrMnd)
            .medOpphørsdatoRefusjon(opphørsdato)
            .medSøkteRefusjonsperioder(mapRefusjonsendringerTilDto(dto.startdato(), opphørsdato, dto.refusjon()))
            .medEndringAvInntektÅrsaker(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .build();
    }

    public static InntektsmeldingDto mapTilDto(SendInntektsmeldingRequestDto dto) {
        // Frontend sender kun inn liste med refusjon. Vi utleder startsum og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(dto.refusjon(), dto.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(dto.refusjon(), dto.startdato()).orElse(Tid.TIDENES_ENDE);
        var builder = opprettDtoBuilderOgSettFellesFelter(dto);
        return builder
            .medInntekt(dto.inntekt())
            .medMånedRefusjon(refusjonPrMnd)
            .medOpphørsdatoRefusjon(opphørsdato)
            .medSøkteRefusjonsperioder(mapRefusjonsendringerTilDto(dto.startdato(), opphørsdato, dto.refusjon()))
            .medBortfaltNaturalytelsePerioder(mapBortfalteNaturalYtelserTilDto(dto.bortfaltNaturalytelsePerioder()))
            .medEndringAvInntektÅrsaker(mapEndringsårsakerTilDto(dto.endringAvInntektÅrsaker()))
            .build();
    }

    private static InntektsmeldingDto.Builder opprettDtoBuilderOgSettFellesFelter(SendInntektsmeldingRequestDto dto) {
        return InntektsmeldingDto.builder()
            .medAktørId(new AktørId(dto.aktorId().id()))
            .medArbeidsgiver(new Arbeidsgiver(dto.arbeidsgiverIdent().orgnr()))
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medStartdato(dto.startdato())
            .medYtelse(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson(dto.kontaktperson().telefonnummer(), dto.kontaktperson().navn()));
    }

    private static List<InntektsmeldingDto.Endringsårsaker> mapEndringsårsakerTilDto(List<SendInntektsmeldingRequestDto.EndringsårsakerRequestDto> endringsårsaker) {
        return endringsårsaker.stream().map(InntektsmeldingMapper::mapEndringsårsakTilDto).toList();
    }

    private static InntektsmeldingDto.Endringsårsaker mapEndringsårsakTilDto(SendInntektsmeldingRequestDto.EndringsårsakerRequestDto e) {
        return new InntektsmeldingDto.Endringsårsaker(
            KodeverkMapper.mapEndringsårsak(e.årsak()),
            e.fom(),
            e.tom(),
            e.bleKjentFom()
        );
    }

    private static List<InntektsmeldingDto.BortfaltNaturalytelse> mapBortfalteNaturalYtelserTilDto(List<SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new InntektsmeldingDto.BortfaltNaturalytelse(
                d.fom(),
                Tid.tomEllerMax(d.tom()),
                KodeverkMapper.mapNaturalytelseTilDomene(d.naturalytelsetype()),
                d.beløp()))
            .toList();
    }

    private static List<InntektsmeldingDto.SøktRefusjon> mapRefusjonsendringerTilDto(LocalDate startdato, LocalDate opphørsdato, List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDto) {
        if (opphørsdato == null) {
            return List.of();
        }
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendringRequestDto.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new InntektsmeldingDto.SøktRefusjon(dto.fom(), dto.beløp()))
            .toList();
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<SendInntektsmeldingRequestDto.Refusjon> refusjon, LocalDate startdato) {
        if (refusjon.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjon.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    public static InntektsmeldingResponseDto mapFraDomene(InntektsmeldingDto dto, ForespørselDto forespørsel) {
        var refusjoner = mapRefusjonerTilDto(dto);

        var bortfalteNaturalytelser = dto.getBortfaltNaturalytelsePerioder().stream().map(i ->
            new SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto(
                i.fom(),
                Objects.equals(i.tom(), Tid.TIDENES_ENDE) ? null : i.tom(),
                NaturalytelsetypeDto.valueOf(i.naturalytelsetype().name()),
                i.beløp()
            )
        ).toList();
        var endringsårsaker = dto.getEndringAvInntektÅrsaker().stream().map(e ->
            new SendInntektsmeldingRequestDto.EndringsårsakerRequestDto(KodeverkMapper.mapEndringsårsak(e.årsak()),
                e.fom(),
                e.tom(),
                e.bleKjentFom()))
            .toList();

        var forespørselType = ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT.equals(forespørsel.forespørselType()) ? ArbeidsgiverinitiertÅrsakDto.NYANSATT : null;

        return new InntektsmeldingResponseDto(
            dto.getId(),
            forespørsel.uuid(),
            new AktørIdDto(dto.getAktørId().getAktørId()),
            KodeverkMapper.mapYtelsetype(dto.getYtelse()),
            new OrganisasjonsnummerDto(dto.getArbeidsgiver().orgnr()),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto(dto.getKontaktperson().navn(), dto.getKontaktperson().telefonnummer()),
            dto.getStartdato(),
            dto.getMånedInntekt(),
            dto.getInnsendtTidspunkt(),
            refusjoner,
            bortfalteNaturalytelser,
            endringsårsaker,
            forespørselType
            );
    }

    private static List<SendInntektsmeldingRequestDto.Refusjon> mapRefusjonerTilDto(InntektsmeldingDto dto) {
        List<SendInntektsmeldingRequestDto.Refusjon> refusjoner = new ArrayList<>();
        if (dto.getMånedRefusjon() != null) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(dto.getStartdato(), dto.getMånedRefusjon()));
        }
        // Frontend forventer at opphørsdato mappes til en liste der fom = første dag uten refusjon, må derfor legge på en dag.
        if (dto.getOpphørsdatoRefusjon() != null && !dto.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE)) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(dto.getOpphørsdatoRefusjon().plusDays(1), BigDecimal.ZERO));
        }
        dto.getSøkteRefusjonsperioder().stream().map(i -> new SendInntektsmeldingRequestDto.Refusjon(i.fom(), i.beløp())).forEach(refusjoner::add);
        return refusjoner.stream().sorted(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom)).toList();
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                       LocalDate startdato) {
        var sisteEndring = finnSisteEndring(refusjonsendringRequestDtos, startdato);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(sr -> sr.fom().minusDays(1));
    }

    private static Optional<SendInntektsmeldingRequestDto.Refusjon> finnSisteEndring(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                                           LocalDate startdato) {
        return refusjonsendringRequestDtos.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom));
    }

}
