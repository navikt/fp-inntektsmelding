package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ExtendWith(MockitoExtension.class)
class DialogportenTjenesteTest {

    private static final UUID FORESPOERSEL_UUID = UUID.randomUUID();
    private static final UUID DIALOG_UUID = UUID.randomUUID();
    private static final AktørId AKTØR_ID = AktørId.fra("1234567891234");
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra("974760673");
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2025, 1, 1);

    @Mock
    private DialogportenKlient dialogportenKlient;
    @Mock
    private PersonTjeneste personTjeneste;

    private DialogportenTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new DialogportenTjeneste(dialogportenKlient, personTjeneste);
    }

    @Test
    void skal_opprette_dialog_med_sakstittel_fra_person() {
        var forespørsel = forespørsel(null);
        mockPerson();
        when(dialogportenKlient.opprettDialog(eq(FORESPOERSEL_UUID),
            eq(ARBEIDSGIVER),
            eq("Inntektsmelding for Navn Navnesen (01.01.61)"),
            eq(FØRSTE_UTTAKSDATO),
            eq(Ytelsetype.FORELDREPENGER))).thenReturn("\"%s\"".formatted(DIALOG_UUID));

        var resultat = tjeneste.opprettDialog(forespørsel);

        assertThat(resultat).isEqualTo(DIALOG_UUID);
    }

    @Test
    void skal_ferdigstille_dialog_med_sakstittel_fra_person() {
        var forespørsel = forespørsel(DIALOG_UUID);
        var inntektsmeldingUuid = UUID.randomUUID();
        mockPerson();

        tjeneste.ferdigstillDialog(forespørsel, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(inntektsmeldingUuid));

        verify(dialogportenKlient).ferdigstillDialog(DIALOG_UUID,
            ARBEIDSGIVER,
            "Inntektsmelding for Navn Navnesen (01.01.61)",
            Ytelsetype.FORELDREPENGER,
            FØRSTE_UTTAKSDATO,
            Optional.of(inntektsmeldingUuid),
            LukkeÅrsak.ORDINÆR_INNSENDING);
    }

    @Test
    void skal_ikke_kalle_klient_når_dialog_uuid_mangler() {
        tjeneste.settDialogTilUtgått(forespørsel(null));

        verifyNoInteractions(dialogportenKlient);
    }

    private void mockPerson() {
        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID, Ytelsetype.FORELDREPENGER)).thenReturn(new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            AKTØR_ID,
            LocalDate.of(1961, 1, 1),
            null,
            null));
    }

    private ForespørselDto forespørsel(UUID dialogportenUuid) {
        return ForespørselDto.builder()
            .uuid(FORESPOERSEL_UUID)
            .arbeidsgiver(ARBEIDSGIVER)
            .aktørId(AKTØR_ID)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(FØRSTE_UTTAKSDATO)
            .dialogportenUuid(dialogportenUuid)
            .build();
    }
}
