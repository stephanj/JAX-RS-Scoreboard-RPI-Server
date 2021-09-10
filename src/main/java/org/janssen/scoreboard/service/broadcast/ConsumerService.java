package org.janssen.scoreboard.service.broadcast;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.model.type.GPIOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping(value = "/api/broadcast/consumer")
public class ConsumerService extends AbstractBroadcaster {

    private final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    private static final Integer NO_TIMEOUTS = 0;
    private static final Integer ONE_TIMEOUT = 1;

    private final DeviceController device;

    private final GPIOController GPIOController;

    public ConsumerService(DeviceController device, GPIOController GPIOController) {
        this.device = device;
        this.GPIOController = GPIOController;
    }

    @PostMapping(value = NEW_GAME)
    public void newMirroredBasketGame() {
        log.debug(">>> Start new mirrored basketball game");

        // Reset Home team
        device.setScoreHome(0);
        device.setFoulsHome(0);
        printTimeoutHome(NO_TIMEOUTS.toString());

        // Reset visiting team
        device.setScoreVisitors(0);
        device.setFoulsVisitors(0);
        printTimeoutVisitors(NO_TIMEOUTS.toString());

        device.setClockOnly(600);
    }

    @PostMapping(FOULS_A)
    public void printFoulsHome(@RequestBody String fouls) {
        log.debug(">>> Print fouls home team : {}", fouls);
        device.setFoulsHome(Integer.parseInt(fouls));
    }

    @PostMapping(FOULS_B)
    public void printFoulsVisitors(@RequestBody String fouls) {
        log.debug(">>> Print fouls visitor team : {}", fouls);
        device.setFoulsVisitors(Integer.parseInt(fouls));
    }

    @PostMapping(PERSONAL_FOUL)
    public void printPersonalFouls(@RequestBody String totalPersonalFouls) {
        log.debug(">>> Print personal fouls : {}", totalPersonalFouls);
        device.setPlayerFoul(Integer.parseInt(totalPersonalFouls));
    }

    @PostMapping(QUARTER)
    public void printQuarter(@RequestBody String quarter) {
        log.debug(">>> Print quarter : {}", quarter);
        device.setPlayerFoul(Integer.parseInt(quarter));
    }

    @PostMapping(value = HOME)
    public void printScoreHome(@RequestBody String score) {
        log.debug(">>> Print home score : {}", score);
        device.setScoreHome(Integer.parseInt(score));
    }

    @PostMapping(VISITORS)
    public void printScoreVisitors(@RequestBody String score) {
        log.debug(">>> Print visitors score : {}", score);
        device.setScoreVisitors(Integer.parseInt(score));
    }

    @PostMapping(TIME)
    public void printTime(@RequestBody String seconds) {
        log.debug(">>> Print time in seconds : {}", seconds);
        device.setClockOnly(Integer.parseInt(seconds));
    }

    @PostMapping(TIMEOUT_HOME)
    public void printTimeoutHome(@RequestBody String strTimeout) {
        log.debug(">>> Print home timeout : {}", strTimeout);
        int timeout = Integer.parseInt(strTimeout);
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
    public void printTimeoutVisitors(@RequestBody String strTimeout) {
        log.debug(">>> Print visitors timeout : {}", strTimeout);
        int timeout = Integer.parseInt(strTimeout);
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
