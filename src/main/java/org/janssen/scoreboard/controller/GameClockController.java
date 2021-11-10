package org.janssen.scoreboard.controller;

import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.GameType;
import org.janssen.scoreboard.service.broadcast.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static org.janssen.scoreboard.service.util.Constants.FOOTBALL_DURATION;
import static org.janssen.scoreboard.service.util.Constants.ZERO_SECONDS;

/**
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class GameClockController {

    private final Logger log = LoggerFactory.getLogger(GameClockController.class);

    // We start at 1 this way it will take max. 99ms
    // to update the clock when "start" has been signaled!
    private static final int START_CLOCK = 1;

    // When the milli-seconds counter reaches 0 we update the game clock
    private static final int UPDATE_CLOCK = 0;

    // We reset the milli-seconds timer counter to 10 when the clock has been updated.
    // This results in 10 x 100ms to pass by to update the game clock again at 1000ms
    private static final int RESET_TIMER = 10;

    private int milliCounter = START_CLOCK;

    private boolean isRunning = false;

    private int currentTimeInSeconds;

    private GameType gameType;

    private final GPIOController gpioController;

    private final DeviceController device;

    private final ProducerService producerService;

    public GameClockController(GPIOController gpioController,
                               DeviceController device,
                               ProducerService producerService) {
        this.gpioController = gpioController;
        this.device = device;
        this.producerService = producerService;
    }

    @PostConstruct
    public void postConstruct() {
        device.setGameClockController(this);
    }

    private boolean mirrored = false;

    @Scheduled(initialDelay = 0, fixedDelay = 100)
    public void init() {

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

    public synchronized void start(final int currentTimeInSeconds,
                                   final GameType gameType,
                                   final boolean mirrored) {

        log.debug(">>>> Start game clock with {} seconds", currentTimeInSeconds);

        this.gameType = gameType;
        this.mirrored = mirrored;

        if (isNotRunning() && currentTimeInSeconds > 0) {
            this.currentTimeInSeconds = currentTimeInSeconds;
            isRunning =  true;
            milliCounter = 1;
        }
    }

    public synchronized void stop() {
        log.debug("Stop game clock");

        if (isRunning()) {
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
        log.debug("set clock {}", currentTimeInSeconds);

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
