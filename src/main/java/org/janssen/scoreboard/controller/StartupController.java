package org.janssen.scoreboard.controller;

import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.model.type.GPIOType;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import java.util.logging.Logger;

import static org.janssen.scoreboard.service.util.Constants.ONE_SECOND_IN_MILLI;

/**
 * @author Stephan Janssen
 */
@Singleton
@Startup
public class StartupController {

    private static final Logger LOGGER = Logger.getLogger(StartupController.class.getName());

    @EJB
    private GameDAO gameDAO;

    @EJB
    private GPIOController gpioController;

    /**
     * Have the buzzer beep two times to indicate that the scoreboard app is ready for action!
     */
    @PostConstruct
    public void onStartup() {
        LOGGER.info("Scoreboard (28 Okt. 2014)");

        LOGGER.info("Scoreboard startup buzzer.... PING  :)");

        // Used to trigger JPA initialization
        gameDAO.totalGames();

        gpioController.setBuzz(GPIOType.ATTENTION, ONE_SECOND_IN_MILLI);

        try {
            Thread.sleep(ONE_SECOND_IN_MILLI);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        gpioController.setBuzz(GPIOType.ATTENTION, ONE_SECOND_IN_MILLI);
    }
}
