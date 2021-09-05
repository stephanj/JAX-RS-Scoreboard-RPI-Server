package org.janssen.scoreboard.controller;

import org.janssen.scoreboard.model.type.GPIOType;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.janssen.scoreboard.service.util.Constants.*;

/**
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class TwentyFourClockController {

    // We start at 1 this way it will take max. 99ms
    // to update the clock when "start" has been signaled!
    private static final int START_CLOCK = 1;

    // When the ms counter reaches 0 we update the 24s clock
    private static final int UPDATE_CLOCK = 0;

    // We reset the ms timer counter to 10 when the 24s has been updated.
    // This results in 10 x 100ms to pass by to update the 24s clock again at 1000ms
    private static final int RESET_TIMER = 10;

    private int milliCounter = START_CLOCK;

    private final DeviceController device;

    private final GPIOController gpioController;

    private int twentyFourSeconds = TWENTY_FOUR_SECONDS;

    private boolean isRunning = false;

    public TwentyFourClockController(DeviceController device,
                                     GPIOController gpioController) {
        this.device = device;
        this.gpioController = gpioController;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 1000)
    public void twentyFourSecondsClock() {

        if (isRunning) {

            milliCounter--;

            if (milliCounter <= UPDATE_CLOCK) {

                // Having the increment here will never show 24 seconds on the clock
                twentyFourSeconds--;

                device.setTwentyFour(twentyFourSeconds);

                if (twentyFourSeconds <= ZERO_SECONDS) {

                    stop();

                    gpioController.setBuzz(GPIOType.END_TWENTY_FOUR);

                    twentyFourSeconds = TWENTY_FOUR_SECONDS;
                }

                milliCounter = RESET_TIMER;
            }
        }
    }

    public synchronized void start() {

        if (!isRunning && twentyFourSeconds > 0) {

            if (twentyFourSeconds == TWENTY_FOUR_SECONDS) {
                twentyFourSeconds = TWENTY_FOUR_SECONDS + 1;
            }

            milliCounter = 1;
            isRunning = true;
        }
    }

    public synchronized void stop() {
        if (isRunning) {
            isRunning =  false;
        }
    }

    public void reset() {
        setTwentyFourSeconds(TWENTY_FOUR_SECONDS);
    }

    public void setFourTeen() {
        boolean wasRunning = isRunning();

        if (isRunning()) {
            stop();
        }

        this.twentyFourSeconds = FOUR_TEEN_SECONDS + 1;
        device.setTwentyFour(FOUR_TEEN_SECONDS);

        if (wasRunning) {
            start();
        }

    }

    public int getTwentyFourSeconds() {
        return twentyFourSeconds;
    }

    public void setTwentyFourSeconds(final int twentyFourSeconds) {
        boolean wasRunning = isRunning();

        if (isRunning()) {
            stop();
        }

        this.twentyFourSeconds = twentyFourSeconds;
        device.setTwentyFour(twentyFourSeconds);

        if (wasRunning) {
            start();
        }
    }

    /**
     * Make 24 seconds LEDs (un)visible
     *
     * @param flag true makes the 24s LEDS visible
     */
    public void setVisible(boolean flag) {
        gpioController.showTwentyFourSeconds(flag);
    }

    /**
     * Switch the 24 seconds LEDs on / off depending on current state.
     */
    public void switchTwentyFourSeconds() {
        gpioController.switchTwentyFourSeconds();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isNotRunning() {
        return !isRunning;
    }
}
