package com.github.curiousoddman.curious_tunes.util;

import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * The H2 web console for development, now that the app has no servlet container (Spring's H2 console needs one).
 * Runs H2's own small web server; open {@code http://localhost:<port>} and connect with the datasource URL from
 * {@code application.yaml}. Since it runs in the app's JVM, it opens the same database while the app is running.
 * <p>
 * On only when {@code ctunes.h2-console.enabled} is true (the dev profile). Remote connections are refused (H2's
 * default without {@code -webAllowOthers}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ctunes.h2-console.enabled", havingValue = "true")
public class H2DevConsole implements InitializingBean, DisposableBean {
    private final int port;
    private Server    server;

    public H2DevConsole(@Value("${ctunes.h2-console.port:8082}") int port) {
        this.port = port;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            server = Server.createWebServer("-webPort", Integer.toString(port), "-webDaemon")
                           .start();
            log.info("H2 console: {}", server.getURL());
        } catch (SQLException e) {
            // A busy port shouldn't stop the player from starting.
            log.warn("H2 console could not start on port {}", port, e);
        }
    }

    @Override
    public void destroy() {
        if (server != null) {
            server.stop();
        }
    }
}
