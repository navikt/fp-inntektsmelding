package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;

import no.nav.foreldrepenger.inntektsmelding.forvaltning.RekjørFeiledeTasksBatchTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.server.exceptions.InntektsmeldingException;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UregistrertValiderer {

    private static final Logger LOG = LoggerFactory.getLogger(UregistrertValiderer.class);

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
                LOG.info("Kan ikke sende inn inntektsmelding på {} for aktørid {}", ytelsetype, personInfo.aktørId());
                throw new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.INGEN_SAK_FUNNET);
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
        LOG.info("Kan ikke sende inn inntektsmelding før fire uker før aktørid {} starter {}", personInfo.aktørId(), ytelseTekst);
        throw new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.SENDT_FOR_TIDLIG);
    }

    private static boolean førsteUttaksdatoErEtterGrense(LocalDate førsteUttaksdato, LocalDate søktForTidligGrense) {
        return førsteUttaksdato != Tid.TIDENES_ENDE && førsteUttaksdato.isAfter(søktForTidligGrense);
    }
}
