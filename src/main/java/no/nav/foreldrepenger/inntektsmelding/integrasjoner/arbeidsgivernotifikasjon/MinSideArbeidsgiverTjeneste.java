package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MinSideArbeidsgiverTjeneste {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke);

    String opprettSak(ForespørselDto forespørselDto);

    String oppdaterSakTilleggsinformasjon(String id, String overstyrtTilleggsinformasjon);

    String ferdigstillSak(String id, boolean arbeidsgvierInitiert);

    String opprettOppgave(ForespørselDto forespørselDto);

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

    String sendNyBeskjedMedKvittering(ForespørselDto forespørselDto, UUID inntektsmeldingUuid, String beskjedTekst);

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
