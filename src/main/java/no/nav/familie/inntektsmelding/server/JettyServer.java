package no.nav.familie.inntektsmelding.server;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Server;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.familie.inntektsmelding.server.app.api.ApiConfig;
import no.nav.familie.inntektsmelding.server.app.forvaltning.ForvaltningApiConfig;
import no.nav.familie.inntektsmelding.server.app.internal.InternalApiConfig;
import no.nav.foreldrepenger.konfig.Environment;

public class JettyServer {
    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private static final Environment ENV = Environment.current();
    protected static final String APPLICATION = "jakarta.ws.rs.Application";

    private static final String CONTEXT_PATH = ENV.getProperty("context.path", "/fpinntektsmelding");

    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    void bootStrap() throws Exception {
        System.setProperty("task.manager.runner.threads", "4");
        konfigurerLogging();
        migrer(setupDataSource());
        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private static void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static void migrer(DataSource dataSource) {
        var flyway = flywayConfig(dataSource);
        flyway.load().migrate();
    }

    private static FluentConfiguration flywayConfig(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:/db/postgres/defaultDS").baselineOnMigrate(true);
    }

    private static DataSource setupDataSource() throws NamingException {
        var dataSource = dataSource();
        new EnvEntry("jdbc/defaultDS", dataSource);
        return dataSource;
    }

    private static DataSource dataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl(ENV.getRequiredProperty("DB_JDBC_URL"));
        config.setUsername(ENV.getRequiredProperty("DB_USERNAME"));
        config.setPassword(ENV.getRequiredProperty("DB_PASSWORD"));
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(2));
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(16);
        config.setInitializationFailTimeout(TimeUnit.SECONDS.toMillis(30));
        config.setConnectionTestQuery("select 1");
        config.setDriverClassName("org.postgresql.Driver");
        config.setAutoCommit(false);

        // optimaliserer inserts for postgres
        var dsProperties = new Properties();
        dsProperties.setProperty("reWriteBatchedInserts", "true");
        dsProperties.setProperty("logServerErrorDetail", "false"); // skrur av batch exceptions som lekker statements i åpen logg
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }

    private void start() throws Exception {
            var server = new Server(getServerPort());
            LOG.info("Starter server");
            var context = new ServletContextHandler(CONTEXT_PATH, ServletContextHandler.NO_SESSIONS);

            // Sikkerhet
            context.setSecurityHandler(simpleConstraints());

            // Servlets
            registerDefaultServlet(context);
            registerServlet(context, 0, InternalApiConfig.API_URI, InternalApiConfig.class);
            registerServlet(context, 1, ApiConfig.API_URI, ApiConfig.class);
            registerServlet(context, 2, ForvaltningApiConfig.API_URI, ForvaltningApiConfig.class);

            // Starter tjenester
            context.addEventListener(new ServiceStarterListener());

            // Enable Weld + CDI
            context.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
            context.addServletContainerInitializer(new CdiServletContainerInitializer());
            context.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

            server.setHandler(context);
            server.setStopAtShutdown(true);
            server.setStopTimeout(10000);
            server.start();

            LOG.info("Server startet på port: {}", getServerPort());
            server.join();
    }

    private static void registerDefaultServlet(ServletContextHandler context) {
        var defaultServlet = new ServletHolder(new DefaultServlet());
        context.addServlet(defaultServlet, "/*");
    }

    private static void registerServlet(ServletContextHandler context, int prioritet, String path, Class<?> appClass) {
        var servlet = new ServletHolder(new ServletContainer());
        servlet.setInitOrder(prioritet);
        servlet.setInitParameter(APPLICATION, appClass.getName());
        context.addServlet(servlet, path + "/*");
    }

    private static ConstraintSecurityHandler simpleConstraints() {
        var handler = new ConstraintSecurityHandler();
        // Slipp gjennom kall fra plattform til JaxRs. Foreløpig kun behov for GET
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, InternalApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ApiConfig.API_URI + "/*"));
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ForvaltningApiConfig.API_URI + "/*"));
        // Alt annet av paths og metoder forbudt - 403
        handler.addConstraintMapping(pathConstraint(Constraint.FORBIDDEN, "/*"));
        return handler;
    }

    private static ConstraintMapping pathConstraint(Constraint constraint, String path) {
        var mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path);
        return mapping;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
