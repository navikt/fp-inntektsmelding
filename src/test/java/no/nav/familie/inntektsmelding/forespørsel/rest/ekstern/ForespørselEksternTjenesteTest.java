package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

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

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørId;


@ExtendWith(MockitoExtension.class)
class ForespørselEksternTjenesteTest {
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    private ForespørselEksternTjeneste forespørselEksternTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselEksternTjeneste = new ForespørselEksternTjeneste(personTjeneste, forespørselBehandlingTjeneste);
    }

    @Test
    void skal_returnere_forespørsel_med_fnr() {
        var aktørId = new AktørId("9999999999999");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var forespørsel = new ForespørselEntitet(orgnr,
            LocalDate.now(),
            aktørId,
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.familie.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))).thenReturn(fnr);
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        var dto = forespørselEksternTjeneste.hentForesørselDto(forespørselUuid);

        assertThat(dto).isPresent();
        assertThat(dto.get().fødselsnummer()).isEqualTo(fnr.getIdent());
        assertThat(dto.get().orgnummer().orgnr()).isEqualTo(orgnr);
    }

    @Test
    void skal_filtrere_forespørsel_på_orgnr() {
        var aktørId = new AktørId("9999999999999");
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var forespørsel = new ForespørselEntitet(orgnr,
            LocalDate.now(),
            aktørId,
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.familie.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))).thenReturn(fnr);
        when(forespørselBehandlingTjeneste.hentForespørsler(new OrganisasjonsnummerDto(orgnr), null, null, null, null, null)).thenReturn(List.of(
            forespørsel));

        var dto = forespørselEksternTjeneste.hentForespørslerDto(new OrganisasjonsnummerDto(orgnr), null, null, null, null, null);

        assertThat(dto).hasSize(1);
        assertThat(dto.getFirst().fødselsnummer()).isEqualTo(fnr.getIdent());
        assertThat(dto.getFirst().orgnummer().orgnr()).isEqualTo(orgnr);
    }

}
