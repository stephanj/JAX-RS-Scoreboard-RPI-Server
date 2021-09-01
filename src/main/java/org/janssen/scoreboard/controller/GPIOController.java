package org.janssen.scoreboard.controller;

import com.pi4j.io.gpio.*;
import org.janssen.scoreboard.model.type.GPIOType;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;

import static org.janssen.scoreboard.service.util.Constants.TWO_SECONDS_IN_MILLI;

/**
 * @link http://pi4j.com/usage.html
 * @author Stephan Janssen
 */
@Singleton
public class GPIOController {

    private GpioPinDigitalOutput timeoutVisitors1;
    private GpioPinDigitalOutput timeoutVisitors2;

    private GpioPinDigitalOutput timeoutHome1;
    private GpioPinDigitalOutput timeoutHome2;

    private GpioPinDigitalOutput endQuarter;
    private GpioPinDigitalOutput endTwentyFourSeconds;
    private GpioPinDigitalOutput attentionRefs;

    private GpioPinDigitalOutput twentyFourSeconds;

    @PostConstruct
    public void init() {
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
    }

    @Asynchronous
    public void setLed(final GPIOType ledType, final boolean isOn) {
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

    @Asynchronous
    public void setBuzz(final GPIOType buzzType, int duration) {
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

    /**
     * "Play" the buzzer.
     *
     * @param buzzType the type of buzzer to play
     */
    @Asynchronous
    public void setBuzz(final GPIOType buzzType) {
        setBuzz(buzzType, TWO_SECONDS_IN_MILLI);
    }

    /**
     * Make 24 seconds counter (un)visible.
     *
     * @param isVisible true is on
     */
    @Asynchronous
    public void showTwentyFourSeconds(final boolean isVisible) {

        // SET 24 seconds LEDS
        if (isVisible) {
            twentyFourSeconds.low();   // On
        } else {
            twentyFourSeconds.high();    // Off
        }

    }

    @Asynchronous
    public void switchTwentyFourSeconds() {

        if (twentyFourSeconds.isHigh()) {
            twentyFourSeconds.low();   // On
        } else {
            twentyFourSeconds.high();    // Off
        }

    }

    private void activateLed(final GpioPinDigitalOutput pin, final boolean isOn) {

        if (isOn & pin.isLow()) {
            pin.high();
        } else if (pin.isHigh()) {
            pin.low();
        }

    }
}
