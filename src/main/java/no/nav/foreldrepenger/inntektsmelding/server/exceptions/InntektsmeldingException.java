package no.nav.foreldrepenger.inntektsmelding.server.exceptions;

import java.net.HttpURLConnection;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.VLLogLevel;

public class InntektsmeldingException extends FunksjonellException {

    public enum LokalFeilKode {
        INGEN_SAK_FUNNET("Ingen sak funnet"),
        SENDT_FOR_TIDLIG("Sendt inntektsmelding for tidlig"),
        FINNES_I_AAREG("Organisasjonsnummer er rapportert i Aa-reg");
        private final String feiltekst;
        LokalFeilKode(String feiltekst) {
            this.feiltekst = feiltekst;
        }

        public String getFeiltekst() {
            return feiltekst;
        }
    }

    private final LokalFeilKode feilkode;

    public InntektsmeldingException(LokalFeilKode feilkode) {
        super(null, feilkode.getFeiltekst());
        this.feilkode = feilkode;
    }

    @Override
    public int getStatusCode() {
        return HttpURLConnection.HTTP_FORBIDDEN;
    }

    @Override
    public String getFeilkode() {
        return feilkode.name();
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.INFO;
    }
}
