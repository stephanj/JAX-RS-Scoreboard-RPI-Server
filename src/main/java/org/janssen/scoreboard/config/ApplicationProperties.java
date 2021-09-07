package org.janssen.scoreboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to web app.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    private boolean runningOnPI;

    public boolean isRunningOnPI() {
        return runningOnPI;
    }

    public void setRunningOnPI(boolean runningOnPI) {
        log.debug(">>> Running on RPI?  {}", runningOnPI);
        this.runningOnPI = runningOnPI;
    }
}
