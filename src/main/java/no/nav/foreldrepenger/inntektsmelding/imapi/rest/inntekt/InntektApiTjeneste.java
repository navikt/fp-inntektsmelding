package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntekt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntekt.InntektResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesGrunnlagTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;

@ApplicationScoped
public class InntektApiTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private PersonTjeneste personTjeneste;
    private FellesGrunnlagTjeneste fellesGrunnlagTjeneste;
    private InntektTjeneste inntektTjeneste;

    InntektApiTjeneste() {
        // CDI
    }

    @Inject
    public InntektApiTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                              PersonTjeneste personTjeneste,
                              FellesGrunnlagTjeneste fellesGrunnlagTjeneste,
                              InntektTjeneste inntektTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.personTjeneste = personTjeneste;
        this.fellesGrunnlagTjeneste = fellesGrunnlagTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    public Optional<InntektResponse> hentInntektDto(UUID forespørselUuid) {
        return forespørselBehandlingTjeneste.hentForespørselOptional(forespørselUuid).map(forespørsel -> {
            var personinfo = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
            var skjæringstidspunkt = forespørsel.skjæringstidspunkt();
            var harJobbetHeleBeregningsperioden = fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(personinfo, skjæringstidspunkt,
                forespørsel.arbeidsgiver());
            var inntektsopplysninger = inntektTjeneste.hentInntekt(personinfo.aktørId(), skjæringstidspunkt, LocalDate.now(),
                forespørsel.arbeidsgiver(), harJobbetHeleBeregningsperioden);
            return mapTilDto(inntektsopplysninger);
        });
    }

    private InntektResponse mapTilDto(Inntektsopplysninger inntektsopplysninger) {
        // Bruker LinkedHashMap manuelt (ikke Collectors.toMap) siden en måned kan mangle rapportert beløp (null),
        // og Map.merge (som Collectors.toMap benytter internt) ikke tillater null-verdier.
        Map<YearMonth, BigDecimal> inntektPerMåned = new LinkedHashMap<>();
        inntektsopplysninger.måneder().forEach(måned -> inntektPerMåned.put(måned.månedÅr(), måned.beløp()));
        return new InntektResponse(inntektPerMåned, inntektsopplysninger.gjennomsnitt());
    }
}
