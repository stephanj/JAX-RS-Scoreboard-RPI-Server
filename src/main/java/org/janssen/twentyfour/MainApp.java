package org.janssen.twentyfour;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The 24s standalone application for the Raspberry PI black box  :)
 *
 * @author Stephan Janssen
 */
public class MainApp {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    private final int THREE_HUNDRED_MILLIS = 300;

    // The REST interfaces
    private HttpGet clockReq;
    private HttpGet isRunningReq;
    private HttpGet isStoppedReq;

    private HttpPut startTimerReq;
    private HttpPut stopTimerReq;
    private HttpPut reset14Req;
    private HttpPut reset24Req;
    private HttpPut inc1Req;
    private HttpPut dec1Req;
    private HttpPut startTimeoutReq;
    private HttpPut show24Req;

    // The status leds
    private GpioPinDigitalOutput wifiLed = null;
    private GpioPinDigitalOutput appRunningLed = null;

    // Debouncing state
    private long lastContact = System.currentTimeMillis();

    /**
     * Setup the URLs and GPIO listeners and then run for ever.
     *
     * @throws InterruptedException app interrupted
     */
    public MainApp() throws InterruptedException {
        logger("<--MainApp--> GPIO 24s app ... started.");

        // create GPIO controller
        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalInput jump1 = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_10, PinMode.DIGITAL_INPUT);
        final GpioPinDigitalInput jump2 = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_11, PinMode.DIGITAL_INPUT);

        int jumpValue1 = jump1.getState().getValue();
        int jumpValue2 = jump2.getState().getValue();
        logger("JUMP1:"+jumpValue1);
        logger("JUMP2:"+jumpValue2);

        String BASE_URL;
        if (jumpValue1 == 1 & jumpValue2 == 1) {
            // jump1 = 1 + jump2 = 1  => Plein A
            BASE_URL = "http://192.168.1.100:8080";
        } else if (jumpValue1 == 0 & jumpValue2 == 1) {
            // jump1 = 0 + jump2 = 1  => Plein B
            BASE_URL = "http://192.168.1.101:8080";
        } else {
            // jump1 = 0 + jump2 = 0  => Plein C
            BASE_URL = "http://192.168.1.102:8080";
        }

        logger(BASE_URL);

        createURLs(BASE_URL);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger("<--MainApp--> Shutdown");

            wifiLed.low();
            appRunningLed.low();
        }));

        // provision gpio pin #02 and #03 as an output for Running App and WIFI led
        appRunningLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
        wifiLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);

        appRunningLed.high();

        // Check if WIFI works by calling the 24s clock REST method, try 5 times over 10 seconds
        checkWifi();

        // provision the GPIO pins as an input pin with its internal pull down resistor enabled
        final GpioPinDigitalInput start60Btn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_14, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput reset14Btn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput reset24Btn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput pls1SecondBtn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput mns1SecondBtn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput timerBtn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_13, PinPullResistance.PULL_DOWN);

        final GpioPinDigitalInput show24Btn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, "24s on off", PinPullResistance.PULL_DOWN);

        // Check if timer button at startup is on or off and act accordingly
        logger("Timer button is " + ((timerBtn.getState().isHigh()) ? "on" : "off"));
        timerButton(timerBtn.getState());

        // create and register gpio pin listeners
        // The 24s timer button
        timerBtn.addListener((GpioPinListenerDigital) event -> timerButton(timerBtn.getState()));

        // The start 60s button
        start60Btn.addListener((GpioPinListenerDigital) event -> handleEvent(startTimeoutReq));

        // The 14s reset button
        reset14Btn.addListener((GpioPinListenerDigital) event -> handleEvent(reset14Req));

        // The 24s reset button
        reset24Btn.addListener((GpioPinListenerDigital) event -> handleEvent(reset24Req));

        // The +1s button
        pls1SecondBtn.addListener((GpioPinListenerDigital) event -> handleEvent(inc1Req));

        // The -1s button
        mns1SecondBtn.addListener((GpioPinListenerDigital) event -> handleEvent(dec1Req));

        // 24s LED button
        show24Btn.addListener((GpioPinListenerDigital) event -> handleEvent(show24Req));

        logger(" ... App will continue running until the program is terminated.");
        logger(" ... PRESS <CTRL-C> TO STOP THE PROGRAM.");

        // keep program running until user aborts (CTRL-C)
        while (true) {
            Thread.sleep(500);
        }
    }

    /**
     *
     * @param BASE_URL  the base URL
     */
    private void createURLs(final String BASE_URL) {
        String REST_24_URL = BASE_URL + "/api/twentyfour/";
        String REST_TIMEOUT_URL = BASE_URL + "/api/timeout/";

        clockReq = new HttpGet(REST_24_URL + "date");                                  // date

        String SECRET_TOKEN = "?token=CAFEBABE";
        startTimerReq = new HttpPut(REST_24_URL + "start" + SECRET_TOKEN);             // Start
        stopTimerReq = new HttpPut(REST_24_URL + "stop" + SECRET_TOKEN);               // Stop
        reset14Req = new HttpPut(REST_24_URL + "fourteen" + SECRET_TOKEN);             // 14s
        reset24Req = new HttpPut(REST_24_URL + "reset" + SECRET_TOKEN);                // 24s
        inc1Req = new HttpPut(REST_24_URL + "inc" + SECRET_TOKEN);                     // +1s
        dec1Req = new HttpPut(REST_24_URL + "dec" + SECRET_TOKEN);                     // -1s
        show24Req = new HttpPut(REST_24_URL + "show" + SECRET_TOKEN);                  // Show 24s LEDs

        startTimeoutReq = new HttpPut(REST_TIMEOUT_URL + "start" + SECRET_TOKEN);      // 60s

        isRunningReq = new HttpGet(REST_24_URL + "running");                           // Is 24s running?
        isStoppedReq = new HttpGet(REST_24_URL + "stopped");                           // Is 24s stopped?

    }

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    private HttpClient getHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        int FIVE_HUNDRED_MILLIS = 500;
        HttpConnectionParams.setConnectionTimeout(params, FIVE_HUNDRED_MILLIS);
        HttpConnectionParams.setSoTimeout(params, FIVE_HUNDRED_MILLIS);
        // ConnManagerParams.setTimeout(params, 3000);
        return httpClient;
    }

    /**
     * Check if we can access the scoreboard over the network.
     */
    private boolean isWifiActive(int retry) {
        boolean wifiState = false;

        try {
            logger(clockReq.toString());

            final HttpClient httpClient = getHttpClient();

            final HttpResponse response = httpClient.execute(clockReq);
            logger("Response: "+EntityUtils.toString(response.getEntity()));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                logger("<--MainApp--> WIFI OK - "+retry+ " retries");
                wifiLed.high();
                wifiState = true;
            } else {
                logger("<--MainApp--> WIFI NOT OK, returned " + response.getStatusLine().getStatusCode() + " - " + retry + " retries");
            }
        } catch (IOException e) {
            logger("<--MainApp--> WIFI EXCEPTION - "+retry+" retries");
            logger(e.getMessage());
        }

        return wifiState;
    }

    /**
     * Handle the start/stop button for the 24s.
     *
     * @param isHigh    is the pin state high?
     * @param putHigh   the http put High state request
     * @param putLow    the http put Low state request
     */
    private void handleEvent(final boolean isHigh, final HttpPut putHigh, final HttpPut putLow) {

        if (hasDebounceOccured()) return;

        wifiLed.high();

        HttpPut retryPut = null;

        HttpResponse response = null;

        try {

            if (isHigh) {
                retryPut = putHigh;
                response = getHttpClient().execute(putHigh);
            } else if (putLow != null) {
                retryPut = putLow;
                response = getHttpClient().execute(putLow);
            }

            logger(retryPut + " " + ((response != null) ?
                    response.getStatusLine().getStatusCode() + " " +
                    EntityUtils.toString(response.getEntity()) : "NULL"));

            if (response != null &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_OK &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_BAD_REQUEST) {

                logger("RETRY");

                response = getHttpClient().execute(retryPut);
                logger(retryPut + " " + ((response != null) ?
                        response.getStatusLine().getStatusCode() + " " +
                        EntityUtils.toString(response.getEntity()) : "NULL"));

                wifiLed.blink(500, 3000);
            }
        } catch (IOException e) {
            try {
                logger("EXCEPTION RETRY");
                response = getHttpClient().execute(retryPut);
                logger(retryPut + " " + ((response != null) ?
                        response.getStatusLine().getStatusCode() + " " +
                        EntityUtils.toString(response.getEntity()) : "NULL"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            wifiLed.blink(500, 3000);
            e.printStackTrace();
        }
    }

    /**
     * Handle the 14, 24, +1, -1 sec buttons.
     *
     * @param putReq   the http put request
     */
    private void handleEvent(final HttpPut putReq) {

        if (hasDebounceOccured()) return;

        wifiLed.high();

        try {
            HttpResponse response = fireRequest(putReq);

            if (response != null &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_OK &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_BAD_REQUEST) {

                logger("RETRY");
                fireRequest(putReq);

                wifiLed.blink(500, 3000);
            }
        } catch (IOException e) {
            try {
                logger("EXCEPTION RETRY");
                fireRequest(putReq);

            } catch (IOException e1) {
                e1.printStackTrace();
            }
            wifiLed.blink(500, 3000);
            e.printStackTrace();
        }
    }

    private HttpResponse fireRequest(final HttpPut putReq) throws IOException {
        HttpResponse response = getHttpClient().execute(putReq);
        logger(putReq+ " " + ((response != null) ?
                response.getStatusLine().getStatusCode() + " " + EntityUtils.toString(response.getEntity()) : "NULL"));
        return response;
    }

    /**
     * Debounce of 300ms check.
     *
     * @return true
     */
    private boolean hasDebounceOccured() {
        final long now = System.currentTimeMillis();
        final long debounce = now - THREE_HUNDRED_MILLIS;

        if (lastContact > debounce) {
            logger(String.format(">> HAS DEBOUNCED @ %d ms", lastContact - debounce));
            return true;
        } else {
            logger(String.format("OK @ %d ms", debounce - lastContact));
            lastContact = now;
            return false;
        }
    }

    private void logger(final String msg) {
        System.out.println(String.format("%s - %s", simpleDateFormat.format(new Date()), msg));
    }

    /**
     * Timer button logic which will only stopped when the timer is actually started or stopped!
     *
     * @param state the timer button state
     */
    private void timerButton(final PinState state) {

        boolean isConfirmed = false;
        boolean isHigh = state.isHigh();

        do {
            handleEvent(isHigh, startTimerReq, stopTimerReq);

            final HttpResponse response;
            try {
                if (isHigh) {

                    response = getHttpClient().execute(isRunningReq);

                    logger("Check running @ " + isRunningReq.toString() + " - " +
                            + response.getStatusLine().getStatusCode() + " "
                            + EntityUtils.toString(response.getEntity()));

                } else {
                    response = getHttpClient().execute(isStoppedReq);

                    logger("Check stopped @ " + isStoppedReq.toString() + " - "
                            + response.getStatusLine().getStatusCode() + " "
                            + EntityUtils.toString(response.getEntity()));
                }

                if (response.getStatusLine().getStatusCode() ==  HttpStatus.SC_OK) {
                    isConfirmed = true;
                }
            } catch (IOException e) {
                logger("Start/Stop event not processed, retry in 300ms");
                e.printStackTrace();
                try {
                    Thread.sleep(THREE_HUNDRED_MILLIS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

        // This logic will loop until either the 24s timer is started or stopped (as requested by Jan :)
        } while (!isConfirmed);
    }

    private void checkWifi() throws InterruptedException {
        int retry = 0;
        int TWENTY_RETRIES = 20;
        while (!isWifiActive(retry) && retry < TWENTY_RETRIES) {        // 20 retries
            retry++;
            if (retry >= TWENTY_RETRIES) {
                System.exit(0);
            }
            Thread.sleep(2000);             // Wait 2 seconds
        }
    }

    /**
     * The main application entry.
     * @param args the arguments
     */
    public static void main(String[] args) throws InterruptedException {
        new MainApp();
    }
}
