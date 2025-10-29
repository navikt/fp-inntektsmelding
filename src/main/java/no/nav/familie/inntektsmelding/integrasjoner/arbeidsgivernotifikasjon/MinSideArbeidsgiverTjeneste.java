package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;

public interface MinSideArbeidsgiverTjeneste {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke);

    String oppdaterSakTilleggsinformasjon(String id, String overstyrtTilleggsinformasjon);

    String ferdigstillSak(String id, boolean arbeidsgvierInitiert);

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

    String opprettNyBeskjedMedEksternVarsling(String grupperingsid,
                                              Merkelapp merkelapp,
                                              String eksternId,
                                              String virksomhetsnummer,
                                              String beskjedTekst,
                                              String varselTekst,
                                              URI lenke);

}
