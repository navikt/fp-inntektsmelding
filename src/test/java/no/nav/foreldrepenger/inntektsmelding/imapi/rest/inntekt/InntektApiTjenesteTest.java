package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntekt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesGrunnlagTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class InntektApiTjenesteTest {
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private FellesGrunnlagTjeneste fellesGrunnlagTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;

    private InntektApiTjeneste inntektApiTjeneste;

    @Test
    void skal_returnere_tomt_resultat_når_forespørsel_ikke_finnes() {
        inntektApiTjeneste = new InntektApiTjeneste(forespørselBehandlingTjeneste, personTjeneste, fellesGrunnlagTjeneste, inntektTjeneste);
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørselOptional(forespørselUuid)).thenReturn(Optional.empty());

        var resultat = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_hente_inntekt_og_mappe_til_dto() {
        inntektApiTjeneste = new InntektApiTjeneste(forespørselBehandlingTjeneste, personTjeneste, fellesGrunnlagTjeneste, inntektTjeneste);
        var orgnr = "999999999";
        var skjæringstidspunkt = LocalDate.of(2025, 6, 1);
        var aktørIdEntitet = new AktørIdEntitet("9999999999999");
        var forespørsel = new ForespørselEntitet(orgnr,
            skjæringstidspunkt,
            aktørIdEntitet,
            Ytelsetype.FORELDREPENGER,
            "123",
            skjæringstidspunkt,
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørselDto = ForespørselDtoMapper.mapFraEntitet(forespørsel);
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørselOptional(forespørselUuid)).thenReturn(Optional.of(forespørselDto));

        var aktørId = new AktørId(aktørIdEntitet.getAktørId());
        var personinfo = new PersonInfo("Fornavn", null, "Etternavn", new PersonIdent("11111111111"), aktørId, null, null, null);
        when(personTjeneste.hentPersonInfoFraAktørId(aktørId, Ytelsetype.FORELDREPENGER)).thenReturn(personinfo);

        var arbeidsgiver = Arbeidsgiver.fra(orgnr);
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(personinfo, skjæringstidspunkt, arbeidsgiver)).thenReturn(true);

        var måned1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30000), YearMonth.of(2025, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var måned2 = new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2025, 4), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT);
        var inntektsopplysninger = new Inntektsopplysninger(BigDecimal.valueOf(30000), orgnr, List.of(måned1, måned2));
        when(inntektTjeneste.hentInntekt(ArgumentMatchers.eq(aktørId), ArgumentMatchers.eq(skjæringstidspunkt), ArgumentMatchers.any(),
            ArgumentMatchers.eq(arbeidsgiver), ArgumentMatchers.eq(true))).thenReturn(inntektsopplysninger);

        var resultat = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        assertThat(resultat).isPresent();
        assertThat(resultat.get().gjennomsnitt()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(resultat.get().inntektPerMåned()).containsEntry(YearMonth.of(2025, 3), BigDecimal.valueOf(30000));
        assertThat(resultat.get().inntektPerMåned()).containsEntry(YearMonth.of(2025, 4), null);
    }
}
