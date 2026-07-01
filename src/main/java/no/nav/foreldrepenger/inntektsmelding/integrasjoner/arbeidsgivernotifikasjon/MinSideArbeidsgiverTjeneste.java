package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface MinSideArbeidsgiverTjeneste {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke);

    // Brukes for migrerte saker: Setter tidspunktet på saken til en tid basert på førsteUttaksdato istedenfor "nå",
    // slik at migrerte saker havner nederst i sakslisten i arbeidsgiverportalen til arbeidsgiver.
    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke,
                      Optional<LocalDate> førsteUttaksdato);

    String oppdaterSakTilleggsinformasjon(String id, String overstyrtTilleggsinformasjon);

    String ferdigstillSak(String id, boolean arbeidsgvierInitiert);

    // Brukes for migrerte saker: Setter tidspunktet på saken til en tid basert på førsteUttaksdato istedenfor "nå",
    // slik at migrerte saker havner nederst i sakslisten i arbeidsgiverportalen til arbeidsgiver.
    String ferdigstillSak(String id, boolean arbeidsgvierInitiert, Optional<LocalDate> førsteUttaksdato);

    String opprettOppgave(String grupperingsid,
                          Merkelapp merkelapp,
                          String eksternId,
                          String virksomhetsnummer,
                          String oppgaveTekst,
                          String varselTekst,
                          String påminnelseTekst,
                          URI lenke);

    String oppgaveUtført(String oppgaveId, OffsetDateTime utførtTidspunkt);

    String oppgaveUtgått(String oppgaveId, OffsetDateTime utgåttTidspunkt);

    String slettSak(String id);

    String sendNyBeskjedMedKvittering(String grupperingsid,
                                           Merkelapp merkelapp,
                                           String eksternId,
                                           String virksomhetsnummer,
                                           String beskjedTekst,
                                           URI kvitteringLenke);

    String sendNyBeskjedMedEksternVarsling(String grupperingsid,
                                           Merkelapp merkelapp,
                                           String eksternId,
                                           String virksomhetsnummer,
                                           String beskjedTekst,
                                           String varselTekst,
                                           URI lenke);

}
