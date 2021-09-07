package org.janssen.scoreboard.controller;

import static org.janssen.scoreboard.service.util.Constants.*;
import org.janssen.scoreboard.model.type.GPIOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The 60s clock controller.
 *
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class TimeoutClockController {

    private final Logger log = LoggerFactory.getLogger(TimeoutClockController.class);

    private final TwentyFourClockController twentyFourClockController;

    private final DeviceController device;

    private final GPIOController gpioController;

    private final TimerTask timeoutClockTask;

    public TimeoutClockController(DeviceController device,
                                  GPIOController gpioController,
                                  TwentyFourClockController twentyFourClockController) {
        this.device = device;
        this.gpioController = gpioController;
        this.twentyFourClockController = twentyFourClockController;

        timeoutClockTask = new TimerTask() {
            @Override
            public void run() {
                clockTask();
            }
        };
    }

    private int twentyFourSecondsValue;

    private int timeoutValue;

    private Timer timer;

    private boolean isRunning = false;

    public synchronized void start() {

        log.debug(">>>> Start timeout clock");

        if (!isRunning) {
            timeoutValue = SIXTY_SECONDS;

            if (twentyFourClockController.isRunning()) {
                twentyFourClockController.stop();
            }

            twentyFourSecondsValue = twentyFourClockController.getTwentyFourSeconds();

            timer = new Timer("gameClock");
            timer.scheduleAtFixedRate(timeoutClockTask, 0, 1000);

            isRunning = true;
        }
    }

    private void clockTask() {

        timeoutValue--;

        device.setTwentyFour(timeoutValue);

        if (timeoutValue == 10) {
            gpioController.setBuzz(GPIOType.ATTENTION, ONE_SECOND_IN_MILLI);
        }

        if (timeoutValue <= ZERO_SECONDS) {

            stop();

            // Reset 24s to original value
            device.setTwentyFour(twentyFourSecondsValue);

            gpioController.setBuzz(GPIOType.ATTENTION, ONE_SECOND_IN_MILLI);
        }
    }

    public synchronized void stop() {

        log.debug(">>>> Stop timeout clock");

        if (isRunning) {
            timer.cancel();
            device.setTwentyFour(twentyFourSecondsValue);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isNotRunning() {
        return !isRunning;
    }
}
