package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;


@ExtendWith(MockitoExtension.class)
class ForespørselApiTjenesteTest {
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    private ForespørselApiTjeneste forespørselApiTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselApiTjeneste = new ForespørselApiTjeneste(personTjeneste, forespørselBehandlingTjeneste);
    }

    @Test
    void skal_returnere_forespørsel_med_fnr() {
        var aktørId = new AktørIdEntitet("9999999999999");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var forespørsel = new ForespørselEntitet(orgnr,
            LocalDate.now(),
            aktørId,
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))).thenReturn(fnr);
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(forespørsel)));

        var dto = forespørselApiTjeneste.hentForesørselDto(forespørselUuid);

        assertThat(dto).isPresent();
        assertThat(dto.get().fødselsnummer()).isEqualTo(fnr.getIdent());
        assertThat(dto.get().orgnummer().orgnr()).isEqualTo(orgnr);
    }

    @Test
    void skal_filtrere_forespørsel_på_orgnr() {
        var aktørId = new AktørIdEntitet("9999999999999");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var forespørsel = new ForespørselEntitet(orgnr,
            LocalDate.now(),
            aktørId,
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))).thenReturn(fnr);
        when(forespørselBehandlingTjeneste.hentForespørsler(Arbeidsgiver.fra(orgnr), null, null, null, null, null)).thenReturn(List.of(
            ForespørselDtoMapper.mapFraEntitet(forespørsel)));

        var dto = forespørselApiTjeneste.hentForespørslerDto(Arbeidsgiver.fra(orgnr), null, null, null, null, null);

        assertThat(dto).hasSize(1);
        assertThat(dto.getFirst().fødselsnummer()).isEqualTo(fnr.getIdent());
        assertThat(dto.getFirst().orgnummer().orgnr()).isEqualTo(orgnr);
    }

}
