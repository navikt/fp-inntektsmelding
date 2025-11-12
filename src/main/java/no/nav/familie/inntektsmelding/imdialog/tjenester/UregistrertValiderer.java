package no.nav.familie.inntektsmelding.imdialog.tjenester;

import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;

public class UregistrertValiderer {

    private UregistrertValiderer() {
        // Skjuler default konstruktør
    }
    public static void validerOmUregistrertKanOpprettes(FpsakKlient.InfoOmSakInntektsmeldingResponse infoOmsak,
                                                    Ytelsetype ytelsetype,
                                                    PersonInfo personInfo) {
        if (!infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING)) {
            if (infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.SØKT_FOR_TIDLIG)) {
                kastForTidligException(personInfo, ytelsetype);
            } else {
                var tekst = "Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s".formatted(
                    ytelsetype,
                    personInfo.aktørId());
                throw new FunksjonellException("INGEN_SAK_FUNNET", tekst, null, null);
            }
        } else {
            var søktForTidligGrense = LocalDate.now().plusMonths(1);
            if (førsteUttaksdatoErEtterGrense(infoOmsak.førsteUttaksdato(), søktForTidligGrense)) {
                kastForTidligException(personInfo, ytelsetype);
            }
        }
    }

    private static void kastForTidligException(PersonInfo personInfo, Ytelsetype ytelsetype) {
        var ytelseTekst = ytelsetype.equals(Ytelsetype.FORELDREPENGER) ? "foreldrepenger" : "svangerskapspenger";
        var tekst = "Du kan ikke sende inn inntektsmelding før fire uker før personen med aktør id %s starter %s".formatted(
            personInfo.aktørId(),
            ytelseTekst);
        throw new FunksjonellException("SENDT_FOR_TIDLIG", tekst, null, null);
    }

    private static boolean førsteUttaksdatoErEtterGrense(LocalDate førsteUttaksdato, LocalDate søktForTidligGrense) {
        return førsteUttaksdato != Tid.TIDENES_ENDE && førsteUttaksdato.isAfter(søktForTidligGrense);
    }
}
