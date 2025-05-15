package no.nav.familie.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;

@ApplicationScoped
public class ValiderUregistrertTjeneste {
    private final GrunnlagDtoTjeneste grunnlagDtoTjeneste;

    @Inject
    public ValiderUregistrertTjeneste(GrunnlagDtoTjeneste grunnlagDtoTjeneste) {
        this.grunnlagDtoTjeneste = grunnlagDtoTjeneste;
    }

    public void validerOmUregistrertKanOpprettes(FpsakKlient.InfoOmSakInntektsmeldingResponse infoOmsak,
                                                    LocalDate førsteUttaksdato,
                                                    Ytelsetype ytelsetype,
                                                    PersonInfo personInfo,
                                                    String organisasjonsnummer) {
        if (!infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING)) {
            if (infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.SØKT_FOR_TIDLIG)) {
                kastForTidligException(personInfo, ytelsetype);
            } else {
                var tekst = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s",
                    ytelsetype,
                    personInfo.aktørId());
                throw new FunksjonellException("INGEN_SAK_FUNNET", tekst, null, null);
            }
        } else {
            var søktForTidligGrense = LocalDate.now().plusMonths(1);
            if (førsteUttaksdatoErEtterGrense(infoOmsak.førsteUttaksdato(), søktForTidligGrense)) {
                kastForTidligException(personInfo, ytelsetype);
            }

            var finnesOrgnummerIAaReg = grunnlagDtoTjeneste.finnesOrgnummerIAaregPåPerson(personInfo.fødselsnummer(),
                organisasjonsnummer,
                førsteUttaksdato);
            if (finnesOrgnummerIAaReg) {
                var tekst = "Det finnes rapportering i aa-registeret på organisasjonsnummeret. Nav vil be om inntektsmelding når vi trenger det";
                throw new FunksjonellException("ORGNR_FINNES_I_AAREG", tekst, null, null);
            }
        }
    }

    private void kastForTidligException(PersonInfo personInfo, Ytelsetype ytelsetype) {
        var ytelseTekst = ytelsetype.equals(Ytelsetype.FORELDREPENGER) ? "foreldrepenger" : "svangerskapspenger";
        var tekst = String.format("Du kan ikke sende inn inntektsmelding før fire uker før personen med aktør id %s starter %s",
            personInfo.aktørId(),
            ytelseTekst);
        throw new FunksjonellException("SENDT_FOR_TIDLIG", tekst, null, null);
    }

    private boolean førsteUttaksdatoErEtterGrense(LocalDate førsteUttaksdato, LocalDate søktForTidligGrense) {
        return førsteUttaksdato != Tid.TIDENES_ENDE && førsteUttaksdato.isAfter(søktForTidligGrense);
    }
}
