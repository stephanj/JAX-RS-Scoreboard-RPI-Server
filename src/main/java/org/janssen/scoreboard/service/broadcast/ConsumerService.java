package org.janssen.scoreboard.service.broadcast;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.model.type.GPIOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The broadcast service receives all the info of to set the scoreboard
 *
 * Fixed length
 * 1) Score Home (3 digits)
 * 2) Score Visitors (3 digits)
 * 3) # timeouts Home (1 digit)
 * 4) # timeouts Visitors (1 digit)
 * 5) # fouls Home (1 digit)
 * 6) # fouls Visitors (1 digit)
 * 7) minutes clock (2 digits)
 * 8) seconds clock (2 digits)
 * 9) quarter (1 digit)
 *
 * For example: 120140103409591
 *
 * @author Stephan Janssen
 */
@RestController
@RequestMapping(value = "/api/broadcast/consumer", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumerService extends AbstractBroadcaster {

    private final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    private static final int NO_TIMEOUTS = 0;
    private static final int ONE_TIMEOUT = 1;

    private final DeviceController device;

    private final GPIOController GPIOController;

    public ConsumerService(DeviceController device, GPIOController GPIOController) {
        this.device = device;
        this.GPIOController = GPIOController;
    }

    @PostMapping(NEW_GAME)
    public void newMirroredBasketGame() {
        log.debug("Start new mirrored basketball game");

        // Reset Home team
        device.setScoreHome(0);
        device.setFoulsHome(0);
        printTimeoutHome(NO_TIMEOUTS);

        // Reset visiting team
        device.setScoreVisitors(0);
        device.setFoulsVisitors(0);
        printTimeoutVisitors(NO_TIMEOUTS);

        device.setClockOnly(600);
    }

    @PostMapping(FOULS_A)
    public void printFoulsHome(@RequestBody final Integer fouls) {
        log.debug("Print fouls home team : {}", fouls);
        device.setFoulsHome(fouls);
    }

    @PostMapping(FOULS_B)
    public void printFoulsVisitors(@RequestBody final Integer fouls) {
        log.debug("Print fouls visitor team : {}", fouls);
        device.setFoulsVisitors(fouls);
    }

    @PostMapping(PERSONAL_FOUL)
    public void printPersonalFouls(@RequestBody final Integer totalPersonalFouls) {
        log.debug("Print personal fouls : {}", totalPersonalFouls);
        device.setPlayerFoul(totalPersonalFouls);
    }

    @PostMapping(QUARTER)
    public void printQuarter(@RequestBody final Integer quarter) {
        log.debug("Print quarter : {}", quarter);
        device.setPlayerFoul(quarter);
    }

    @PostMapping(HOME)
    public void printScoreHome(@RequestBody final Integer score) {
        log.debug("Print home score : {}", score);
        device.setScoreHome(score);
    }

    @PostMapping(VISITORS)
    public void printScoreVisitors(@RequestBody final Integer score) {
        log.debug("Print visitors score : {}", score);
        device.setScoreVisitors(score);
    }

    @PostMapping(TIME)
    public void printTime(@RequestBody final Integer seconds) {
        log.debug("Print time in seconds : {}", seconds);
        device.setClockOnly(seconds);
    }

    @PostMapping(TIMEOUT_HOME)
    public void printTimeoutHome(@RequestBody final Integer timeout) {
        log.debug("Print home timeout : {}", timeout);
        if (timeout == NO_TIMEOUTS) {
            GPIOController.setLed(GPIOType.TIME_OUT_H1, false);
            GPIOController.setLed(GPIOType.TIME_OUT_H2, false);
        } else if (timeout == ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_H1, true);
        } else if (timeout > ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_H2, true);
        }
    }

    @PostMapping(TIMEOUT_VISITORS)
    public void printTimeoutVisitors(@RequestBody final Integer timeout) {
        log.debug("Print visitors timeout : {}", timeout);
        // Set visitors timeout LEDs
        if (timeout == NO_TIMEOUTS) {
            GPIOController.setLed(GPIOType.TIME_OUT_V1, false);
            GPIOController.setLed(GPIOType.TIME_OUT_V2, false);
        } else if (timeout == ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_V1, true);
        } else if (timeout > ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_V2, true);
        }
    }
}
