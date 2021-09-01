package org.janssen.scoreboard.service.broadcast;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.TeamType;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
@Path("/api/broadcast/consumer")
@Produces({MediaType.APPLICATION_JSON})
public class ConsumerService extends AbstractBroadcaster {

    private static final int NO_TIMEOUTS = 0;
    private static final int ONE_TIMEOUT = 1;

    @Inject
    private DeviceController device;

    @EJB
    private org.janssen.scoreboard.controller.GPIOController GPIOController;

    @POST
    @Path(NEW_GAME)
    public void newMirroredBasketGame() {
        final Team team = new Team();
        team.setKey(TeamType.A.toString());
        device.setScore(team);

        team.setKey(TeamType.B.toString());
        device.setScore(team);

        device.setClockOnly(600);
        device.setFoulsHome(0);
        device.setFoulsVisitors(0);

        printTimeoutHome(NO_TIMEOUTS);
        printTimeoutVisitors(NO_TIMEOUTS);
    }

    @POST
    @Path(FOULS_A)
    public void printFoulsHome(final int fouls) {
        device.setFoulsHome(fouls);
    }

    @POST
    @Path(FOULS_B)
    public void printFoulsVisitors(final int fouls) {
        device.setFoulsVisitors(fouls);
    }

    @POST
    @Path(PERSONAL_FOUL)
    public void printPersonalFouls(final int totalPersonalFouls) {
        device.setPlayerFoul(totalPersonalFouls);
    }

    @POST
    @Path(QUARTER)
    public void printQuarter(final int quarter) {
        device.setPlayerFoul(quarter);
    }

    @POST
    @Path(HOME)
    public void printScoreHome(final int score) {
        device.setScoreHome(score);
    }

    @POST
    @Path(VISITORS)
    public void printScoreVisitors(final int score) {
        device.setScoreVisitors(score);
    }

    @POST
    @Path(TIME)
    public void printTime(final int seconds) {
        device.setClockOnly(seconds);
    }

    @POST
    @Path(TIMEOUT_HOME)
    public void printTimeoutHome(final int timeout) {

        if (timeout == NO_TIMEOUTS) {
            GPIOController.setLed(GPIOType.TIME_OUT_H1, false);
            GPIOController.setLed(GPIOType.TIME_OUT_H2, false);
        } else if (timeout == ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_H1, true);
        } else if (timeout > ONE_TIMEOUT) {
            GPIOController.setLed(GPIOType.TIME_OUT_H2, true);
        }
    }

    @POST
    @Path(TIMEOUT_VISITORS)
    public void printTimeoutVisitors(final int timeout) {

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
