package org.janssen.scoreboard.service.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Produce a broadcast message for the mirrored scoreboard.
 *
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class ProducerService extends AbstractBroadcaster {

    private final Logger log = LoggerFactory.getLogger(ProducerService.class);

    private static final String BASE_URL = "http://192.168.1.100:8080/api/broadcast/consumer";
//    private static final String BASE_URL = "http://10.0.1.82:8080/api/broadcast/consumer";

    // We can take scoreboard A as the FIXED mirrored target server.
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(1))
            .build();

    public void printFoulsA(final Integer foul) {
        postData(FOULS_A, foul);
    }

    public void printFoulsB(final Integer foul) {
        postData(FOULS_B, foul);
    }

    public void setPlayerFoul(final Integer totalPersonaFoul) {
        postData(PERSONAL_FOUL, totalPersonaFoul);
    }

    public void printHomeScore(final Integer score) {
        postData(HOME, score);
    }

    public void printVisitorsScore(final Integer score) {
        postData(VISITORS, score);
    }

    public void printTimeInSeconds(final Integer seconds) {
        postData(TIME, seconds);
    }

    public void printHomeTimeout(final Integer timeout) {
        postData(TIMEOUT_HOME, timeout);
    }

    public void printVisitorsTimeout(final Integer timeout) {
        postData(TIMEOUT_VISITORS, timeout);
    }

    public void newGame() {
        postData(NEW_GAME, 0);
    }

    @Async
    public void postData(final String path, final Integer value) {
        log.debug("Post data to {} with value {}", path, value);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(value.toString()))
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("HTTP POST status {}", response.statusCode());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
