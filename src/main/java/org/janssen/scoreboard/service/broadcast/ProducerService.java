package org.janssen.scoreboard.service.broadcast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Produce a broadcast message for the mirrored scoreboard.
 *
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class ProducerService extends AbstractBroadcaster {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(ProducerService.class);

    private static final String BASE_URL = "http://192.168.1.100:8080/api/broadcast/consumer";
//    private static final String BASE_URL = "http://10.0.1.82:8080/api/broadcast/consumer";

    // We can take scoreboard A as the FIXED mirrored target server.
    HttpClient httpclient;

    @PostConstruct
    public void init() {
        httpclient = new DefaultHttpClient();
    }

    @PreDestroy
    public void cleanup () {
        httpclient.getConnectionManager().shutdown();
    }

    public void printFoulsA(final int foul) {
        postData(FOULS_A, foul);
    }

    public void printFoulsB(final int foul) {
        postData(FOULS_B, foul);
    }

    public void setPlayerFoul(final int totalPersonaFoul) {
        postData(PERSONAL_FOUL, totalPersonaFoul);
    }

    public void printHomeScore(final int score) {
        postData(HOME, score);
    }

    public void printVisitorsScore(final int score) {
        postData(VISITORS, score);
    }

    public void printTimeInSeconds(final int seconds) {
        postData(TIME, seconds);
    }

    public void printHomeTimeout(final int timeout) {
        postData(TIMEOUT_HOME, timeout);
    }

    public void printVisitorsTimeout(final int timeout) {
        postData(TIMEOUT_VISITORS, timeout);
    }

    public void newGame() {
        postData(NEW_GAME, 0);
    }

    @Async
    public void postData(final String path, final int foul) {
        try {
            HttpPost httppost = new HttpPost(BASE_URL + path);
            httppost.setEntity(new StringEntity("" + foul));
            httpclient.execute(httppost);
        } catch (IOException e) {
            // Ignore because it's a fire and forget REST call
            log.error(e.getMessage());
        }
    }
}
