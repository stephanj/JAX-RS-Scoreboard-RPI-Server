package org.janssen.scoreboard.controller;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;

import static org.janssen.scoreboard.service.util.Constants.*;
import org.janssen.scoreboard.model.type.GPIOType;

/**
 * The 60s clock controller.
 *
 * @author Stephan Janssen
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class TimeoutClockController {

    private static final String CLOCK_NAME = "timeout";
    @EJB
    private TwentyFourClockController twentyFourClockController;

    @Inject
    private DeviceController device;

    @Inject
    private GPIOController gpioController;

    private int twentyFourSecondsValue;

    private int timeoutValue;

    @Resource
    private TimerService timerService;

    private boolean isRunning = false;

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
