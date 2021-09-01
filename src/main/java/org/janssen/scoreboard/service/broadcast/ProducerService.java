package org.janssen.scoreboard.service.broadcast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import java.io.IOException;

/**
 * Produce a broadcast message for the mirrored scoreboard.
 *
 * @author Stephan Janssen
 */
@Singleton
public class ProducerService extends AbstractBroadcaster {

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

    @Asynchronous
    public void printFoulsA(final int foul) {
        postData(FOULS_A, foul);
    }

    @Asynchronous
    public void printFoulsB(final int foul) {
        postData(FOULS_B, foul);
    }

    @Asynchronous
    public void setPlayerFoul(final int totalPersonaFoul) {
        postData(PERSONAL_FOUL, totalPersonaFoul);
    }

    @Asynchronous
    public void printQuarter(final int quarter) {
        postData(QUARTER, quarter);
    }

    @Asynchronous
    public void printHomeScore(final int score) {
        postData(HOME, score);
    }

    @Asynchronous
    public void printVisitorsScore(final int score) {
        postData(VISITORS, score);
    }

    @Asynchronous
    public void printTimeInSeconds(final int seconds) {
        postData(TIME, seconds);
    }

    @Asynchronous
    public void printHomeTimeout(final int timeout) {
        postData(TIMEOUT_HOME, timeout);
    }

    @Asynchronous
    public void printVisitorsTimeout(final int timeout) {
        postData(TIMEOUT_VISITORS, timeout);
    }

    @Asynchronous
    public void newGame() {
        postData(NEW_GAME, 0);
    }

    private void postData(final String path, final int foul) {
        try {
            HttpPost httppost = new HttpPost(BASE_URL + path);
            httppost.setEntity(new StringEntity("" + foul));
            httpclient.execute(httppost);
        } catch (ClientProtocolException e) {

        } catch (IOException e) {

        }
    }
}
