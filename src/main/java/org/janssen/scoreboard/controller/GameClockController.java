package org.janssen.scoreboard.controller;

import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.GameType;
import org.janssen.scoreboard.service.broadcast.ProducerService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.janssen.scoreboard.service.util.Constants.FOOTBALL_DURATION;
import static org.janssen.scoreboard.service.util.Constants.ZERO_SECONDS;

/**
 * @author Stephan Janssen
 */
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Lock(LockType.READ)
@Startup
@Singleton
public class GameClockController {

    // private static final Logger LOGGER = Logger.getLogger(GameClockController.class.getName());

    // We start at 1 this way it will take max. 99ms
    // to update the clock when "start" has been signaled!
    private static final int START_CLOCK = 1;

    // When the milli seconds counter reaches 0 we update the game clock
    private static final int UPDATE_CLOCK = 0;

    // We reset the milli seconds timer counter to 10 when the clock has been updated.
    // This results in 10 x 100ms to pass by to update the game clock again at 1000ms
    private static final int RESET_TIMER = 10;

    private int milliCounter = START_CLOCK;

    private boolean isRunning = false;

    private int currentTimeInSeconds;

    private GameType gameType;

    @Inject
    private GPIOController gpioController;

    @Inject
    private DeviceController device;

    @Inject
    private ProducerService producerService;


    private boolean mirrored = false;

    @Resource
    private ManagedScheduledExecutorService ses;

    private ScheduledFuture<?> future;

    @PostConstruct
    public void init() {

        future = ses.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                if (isRunning) {

                    milliCounter--;

                    if (milliCounter <= UPDATE_CLOCK) {

                        switch(gameType) {

                            // Increment clock for football
                            case FOOTBALL:

                                currentTimeInSeconds++;     // Clock 'counts up'

                                setClock();
                                if (currentTimeInSeconds >= FOOTBALL_DURATION) {
                                    endQuarter();
                                }
                                break;

                            case BASKET:
                            case BASKET_KIDS:

                                currentTimeInSeconds--;     // For Basket the clock 'counts down'

                                setClock();
                                if (currentTimeInSeconds <= ZERO_SECONDS) {
                                    endQuarter();
                                }
                                break;
                        }

                        milliCounter = RESET_TIMER;
                    }
                }
            }
        },  0, 100, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        future.cancel(true);
    }

    public synchronized void start(final int currentTimeInSeconds,
                                   final GameType gameType,
                                   final boolean mirrored) {
        this.gameType = gameType;
        this.mirrored = mirrored;

        if (!isRunning && currentTimeInSeconds > 0) {
            this.currentTimeInSeconds = currentTimeInSeconds;
            isRunning =  true;
            milliCounter = 1;
        }
    }

    public synchronized void stop() {
        if (isRunning) {
            isRunning = false;
        }
    }

    private void endQuarter() {
        stop();

        gpioController.setBuzz(GPIOType.END_QUARTER);
    }

    public void setSeconds(int seconds) {
        currentTimeInSeconds = seconds;

        setClock();
    }

    private void setClock() {
        if (mirrored) {
            producerService.printTimeInSeconds(currentTimeInSeconds);
        }

        device.setClockOnly(currentTimeInSeconds);
    }

    public int getSeconds() {
        return currentTimeInSeconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isNotRunning() {
        return !isRunning;
    }
}
