package org.janssen.scoreboard.controller;

import static org.janssen.scoreboard.service.util.Constants.*;
import org.janssen.scoreboard.model.type.GPIOType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The 60s clock controller.
 *
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class TimeoutClockController {

    private static final String CLOCK_NAME = "timeout";

    private final TwentyFourClockController twentyFourClockController;

    private final DeviceController device;

    private final GPIOController gpioController;

    private final TimerService timerService;

    private int twentyFourSecondsValue;

    private int timeoutValue;

    private boolean isRunning = false;

    public TimeoutClockController(TwentyFourClockController twentyFourClockController,
                                  DeviceController device,
                                  GPIOController gpioController,
                                  TimerService timerService) {
        this.twentyFourClockController = twentyFourClockController;
        this.device = device;
        this.gpioController = gpioController;
        this.timerService = timerService;
    }

    public synchronized void start() {

        if (!isRunning) {
            timeoutValue = SIXTY_SECONDS;

            if (twentyFourClockController.isRunning()) {
                twentyFourClockController.stop();
            }

            twentyFourSecondsValue = twentyFourClockController.getTwentyFourSeconds();

            TimerConfig config = new TimerConfig();
            config.setPersistent(false);
            config.setInfo(CLOCK_NAME);

            timerService.createIntervalTimer(ONE_SECOND_IN_MILLI, ONE_SECOND_IN_MILLI, config);

            isRunning = true;
        }
    }

    @Timeout
    private void ejbTimeout() {

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

        if (isRunning) {
            for (Object obj : timerService.getTimers()) {
                javax.ejb.Timer timer = (javax.ejb.Timer) obj;
                String timerInfo = (String) timer.getInfo();
                if (timerInfo.equals(CLOCK_NAME)) {
                    timer.cancel();
                    isRunning =  false;
                }
            }

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
