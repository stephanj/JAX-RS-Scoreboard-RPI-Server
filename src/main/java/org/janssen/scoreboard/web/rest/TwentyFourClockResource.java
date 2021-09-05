package org.janssen.scoreboard.web.rest;

import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.controller.TimeoutClockController;
import org.janssen.scoreboard.controller.TwentyFourClockController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

import static org.janssen.scoreboard.service.util.Constants.TWENTY_FOUR_SECONDS;

/**
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/twentyfour", produces = MediaType.APPLICATION_JSON_VALUE)
public class TwentyFourClockResource {

    private final Logger log = LoggerFactory.getLogger(TwentyFourClockResource.class);

    private final TwentyFourClockController twentyFourClockController;

    private final TimeoutClockController timeoutClockController;

    private final GameClockController gameClockController;

    private boolean timerOn = false;

    public TwentyFourClockResource(TwentyFourClockController twentyFourClockController,
                                   TimeoutClockController timeoutClockController,
                                   GameClockController gameClockController) {
        this.twentyFourClockController = twentyFourClockController;
        this.timeoutClockController = timeoutClockController;
        this.gameClockController = gameClockController;
    }

    @GetMapping
    public ResponseEntity<?> getClock() {
        log.debug("Get clock");
        return ResponseEntity.ok(twentyFourClockController.getTwentyFourSeconds());
    }

    @GetMapping("/date")
    public ResponseEntity<?> getDate() {
        log.debug("Get date");
        return ResponseEntity.ok(new Date().toString());
    }

    @PutMapping("/start")
    public ResponseEntity<?> startClock() {
        log.debug("Start clock");

        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        timerOn = true;

        if (twentyFourClockController.isNotRunning()) {
            twentyFourClockController.start();
            return ResponseEntity.ok().build();

        } else {
            return ResponseEntity.badRequest().body("Clock already running");
        }
    }

    @PutMapping("/stop")
    public ResponseEntity<?> stopClock() {
        log.debug("Stop clock");

        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        timerOn = false;

        if (twentyFourClockController.isRunning()) {

            twentyFourClockController.stop();
            return ResponseEntity.ok().build();

        } else {
            return ResponseEntity.badRequest().body("Clock not running");
        }
    }

    @GetMapping("/running")
    public ResponseEntity<?> isRunning() {
        log.debug("Is clock running ?");

        if (twentyFourClockController.isRunning()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Clock not running");
        }
    }

    @GetMapping("/stopped")
    public ResponseEntity<?> isStopped() {
        log.debug("Is clock stopped ?");

        if (twentyFourClockController.isNotRunning()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Clock is running");
        }
    }

    @PutMapping("/reset")
    public ResponseEntity<?> resetClock() {
        log.debug("Reset clock");

        // If 60s timeout is running then stop it
        if (timeoutClockController.isRunning()) {
            timeoutClockController.stop();
        }

        twentyFourClockController.reset();

        if (timerOn && twentyFourClockController.isNotRunning()) {

            if (timeoutClockController.isRunning()) {
                timeoutClockController.stop();
            }

            twentyFourClockController.start();
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/fourteen")
    public ResponseEntity<?> setFourteenSeconds() {
        log.debug("Set 14 seconds");

        if (timeoutClockController.isNotRunning()) {
            twentyFourClockController.setFourTeen();
        }

        if (timerOn &&
            twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            twentyFourClockController.start();
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/inc")
    public ResponseEntity<?> incClock() {
        log.debug("Increment clock with 1 second");

        if (twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            int twentyFourSeconds = twentyFourClockController.getTwentyFourSeconds();

            if (twentyFourSeconds < TWENTY_FOUR_SECONDS) {
                twentyFourSeconds++;
                twentyFourClockController.setTwentyFourSeconds(twentyFourSeconds);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("24 secs can go beyond 24 secs  :)");
            }
        } else {
            return ResponseEntity.badRequest().body("Can't change clock will running");
        }
    }

    @PutMapping("/dec")
    public ResponseEntity<?> decClock() {
        log.debug("Decrement clock with 1 second");

        if (twentyFourClockController.isNotRunning() &&
            timeoutClockController.isNotRunning()) {

            int twentyFourSeconds = twentyFourClockController.getTwentyFourSeconds();

            if (twentyFourSeconds > 1) {
                twentyFourSeconds --;
                twentyFourClockController.setTwentyFourSeconds(twentyFourSeconds);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Can't have a negative twenty four seconds");
            }
        } else {
            return ResponseEntity.badRequest().body("Can't change clock will running");
        }
    }

    @PutMapping("/show")
    public ResponseEntity<?> setVisible() {
        log.debug("Make 24s LEDs visible");

        final int seconds = gameClockController.getSeconds();
        if (seconds < 25 && seconds > 0) {

            log.debug("switchTwentyFourSeconds()");
            twentyFourClockController.switchTwentyFourSeconds();
        } else {
            log.debug("setVisible(true)");
            twentyFourClockController.setVisible(true);
        }

        return ResponseEntity.ok().build();
    }
}
