package org.janssen.scoreboard.controller;

import com.pi4j.io.gpio.*;
import org.janssen.scoreboard.model.type.GPIOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static org.janssen.scoreboard.service.util.Constants.ONE_SECOND_IN_MILLI;
import static org.janssen.scoreboard.service.util.Constants.TWO_SECONDS_IN_MILLI;

/**
 * @link http://pi4j.com/usage.html
 * @author Stephan Janssen
 */
@Component
@Scope("singleton")
public class GPIOController {

    private final Logger log = LoggerFactory.getLogger(GPIOController.class);

    private GpioPinDigitalOutput timeoutVisitors1;
    private GpioPinDigitalOutput timeoutVisitors2;

    private GpioPinDigitalOutput timeoutHome1;
    private GpioPinDigitalOutput timeoutHome2;

    private GpioPinDigitalOutput endQuarter;
    private GpioPinDigitalOutput endTwentyFourSeconds;
    private GpioPinDigitalOutput attentionRefs;

    private GpioPinDigitalOutput twentyFourSeconds;

    @Value("${running.on.rpi}")
    private boolean runningOnRPI;

    @PostConstruct
    public void init() {
        if (runningOnRPI) {
            final GpioController gpio = GpioFactory.getInstance();

            // Timeout LEDs
            timeoutHome1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "TimeOut Home 1", PinState.LOW);
            timeoutHome2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "TimeOut Home 2", PinState.LOW);
            timeoutVisitors1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "TimeOut Visitors 1", PinState.LOW);
            timeoutVisitors2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "TimeOut Visitors 2", PinState.LOW);

            // Buzzers
            endQuarter = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "End Quarter", PinState.LOW);
            endTwentyFourSeconds = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "End 24s", PinState.LOW);
            attentionRefs = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Attention", PinState.LOW);

            // 24s LEDs
            twentyFourSeconds = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "24s LEDs", PinState.LOW);

            log.info("Scoreboard startup buzzer.... PING  :)");
            setBuzz(GPIOType.ATTENTION, ONE_SECOND_IN_MILLI);
        } else {
            log.info(">>>> NOT RUNNING ON RPI !!");
        }
    }

    @Async
    public void setLed(final GPIOType ledType, final boolean isOn) {
        log.debug(">>>> Set LED {} to {}", ledType.toString(), isOn);

        if (runningOnRPI) {
            switch (ledType) {
                case TIME_OUT_H1:
                    activateLed(timeoutHome1, isOn);
                    break;

                case TIME_OUT_H2:
                    activateLed(timeoutHome2, isOn);
                    break;

                case TIME_OUT_V1:
                    activateLed(timeoutVisitors1, isOn);
                    break;

                case TIME_OUT_V2:
                    activateLed(timeoutVisitors2, isOn);
                    break;
            }
        }
    }

    @Async
    public void setBuzz(final GPIOType buzzType, int duration) {
        log.debug(">>>> Set BUZZ");

        if (runningOnRPI) {
            switch (buzzType) {

                case END_QUARTER:
                    endQuarter.pulse(duration, true);
                    break;

                case END_TWENTY_FOUR:
                    endTwentyFourSeconds.pulse(duration, true);
                    break;

                case ATTENTION:
                    attentionRefs.pulse(duration, true);
                    break;
            }
        }
    }

    /**
     * "Play" the buzzer.
     *
     * @param buzzType the type of buzzer to play
     */
    @Async
    public void setBuzz(final GPIOType buzzType) {
        setBuzz(buzzType, TWO_SECONDS_IN_MILLI);
    }

    /**
     * Make 24 seconds counter (un)visible.
     *
     * @param isVisible true is on
     */
    @Async
    public void showTwentyFourSeconds(final boolean isVisible) {
        log.debug("Show 24s");

        if (runningOnRPI) {
            // SET 24 seconds LEDS
            if (isVisible) {
                twentyFourSeconds.low();    // ON
            } else {
                twentyFourSeconds.high();    // OFF
            }
        }
    }

    /**
     * Switch the 24 seconds LEDs on/off depending on current state.
     */
    @Async
    public void switchTwentyFourSeconds() {
        log.debug("Switch 24s");

        if (runningOnRPI) {
            if (twentyFourSeconds.isHigh()) {
                twentyFourSeconds.low();        // ON
            } else {
                twentyFourSeconds.high();       // OFF
            }
        }
    }

    private void activateLed(final GpioPinDigitalOutput pin, final boolean isOn) {
        log.debug("Activate LEDs");

        if (isOn & pin.isLow()) {
            pin.high();
        } else if (pin.isHigh()) {
            pin.low();
        }
    }
}
