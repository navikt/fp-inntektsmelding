package no.nav.foreldrepenger.inntektsmelding.server;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import no.nav.foreldrepenger.inntektsmelding.server.app.api.ApiConfig;
import no.nav.foreldrepenger.inntektsmelding.server.app.forvaltning.ForvaltningApiConfig;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

public class RestApiTester {

    static final List<Class<?>> UNNTATT = List.of(ProsessTaskRestTjeneste.class);

    static Collection<Method> finnAlleRestMetoder() {
        List<Method> liste = new ArrayList<>();
        for (var klasse : finnAlleRestTjenester()) {
            for (var method : klasse.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }

    static Collection<Class<?>> finnAlleRestTjenester() {
        var resultList = new ArrayList<Class<?>>();
        resultList.addAll(finnAlleRestTjenester(new ApiConfig()));
        resultList.addAll(finnAlleRestTjenester(new ForvaltningApiConfig()));
        return resultList;
    }

    static Collection<Class<?>> finnAlleRestTjenester(Application config) {
        return config.getClasses().stream()
            .filter(c -> c.getAnnotation(Path.class) != null)
            .filter(c -> !UNNTATT.contains(c))
            .toList();
    }
}
