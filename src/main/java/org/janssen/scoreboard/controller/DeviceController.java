package org.janssen.scoreboard.controller;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.TeamType;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;

import static org.janssen.scoreboard.service.util.Constants.SEVEN_SECONDS_IN_MILLI;
import static org.janssen.scoreboard.service.util.Constants.TWENTY_FOUR_SECONDS;

/**
 * score
 * Usage:
 *  -hnnn score 'h'ome team
 *  -vnnn score 'v'isitor team
 *  -m mm (minutes)
 *  -s ss (seconds)
 *  -t tt (24 seconds)
 *  -q n (quarter 1..4)
 *  -a n (foul team A 0..5)
 *  -b n (foul team B 0..5)
 *  -d debug
 *  -z clear all leds
 *  -c 1..16 clear led n
 *  -x test
 *  -f Full scoreboard (included all values)
 *
 * @author Stephan Janssen
 */
@Stateless
public class DeviceController {

     final String cmd_score = "/home/pi/score";
//    final String cmd_score = "/bin/echo";

    final DefaultExecutor executor = new DefaultExecutor();

    @EJB
    private GameClockController gameClockController;

    @EJB
    private TwentyFourClockController twentyFourClockController;

    @PostConstruct
    public void init() {
        executor.setExitValue(1);
    }

    @Asynchronous
    public void setScore(final Team team) {
        if (team.getKey().equalsIgnoreCase(TeamType.A.toString())) {
            setScoreHome(team.getScore());
        } else {
            setScoreVisitors(team.getScore());
        }
    }

    @Asynchronous
    public void setScoreHome(final int score) {
        execute(String.format("%s -h%03d", cmd_score, score));
    }

    @Asynchronous
    public void setScoreVisitors(final int score) {
        execute(String.format("%s -v%03d", cmd_score, score));
    }

    @Asynchronous
    public void setPlayerFoul(final int foul) {
        execute(String.format("%s -q%d", cmd_score, foul));

        try {
            Thread.sleep(SEVEN_SECONDS_IN_MILLI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        execute(String.format("%s -q%d", cmd_score, 0));
    }

    @Asynchronous
    public void setFoul(final Team team) {
        if (team.getKey().equalsIgnoreCase(TeamType.A.toString())) {
            setFoulsHome(team.getFouls());
        } else {
            setFoulsVisitors(team.getFouls());
        }
    }

    @Asynchronous
    public void setFoulsHome(final int fouls) {
        execute(String.format("%s -a%d", cmd_score, fouls));
    }

    @Asynchronous
    public void turnOff() {
        execute(String.format("%s -z", cmd_score));
    }

    @Asynchronous
    public void setFoulsVisitors(final int fouls) {
        execute(String.format("%s -b%d", cmd_score, fouls));
    }

    @Asynchronous
    public void setAllClocks(final int seconds, final int twentyFourSeconds) {
        setClockOnly(seconds);
        execute(String.format("%s -t%02d", cmd_score, twentyFourSeconds));
    }

    @Asynchronous
    public void setClockOnly(final int seconds) {
        execute(String.format("%s -k%02d%02d", cmd_score, seconds/60, seconds%60));
    }

    @Asynchronous
    public void setTwentyFour(final int seconds) {
        execute(String.format("%s -t%02d", cmd_score, seconds));
    }

    @Asynchronous
    public void clearBoard() {
        execute(String.format("%s -z", cmd_score));
    }

    @Asynchronous
    public void setScoreboard(final String value) {
        execute(String.format("%s -f%s", cmd_score, value));
    }

    public void setGame(final Game game) {

        twentyFourClockController.setTwentyFourSeconds(TWENTY_FOUR_SECONDS);
        gameClockController.setSeconds(game.getClock());

        setScore(game.getTeamA());
        setScore(game.getTeamB());
        setFoul(game.getTeamA());
        setFoul(game.getTeamB());
        setPlayerFoul(game.getQuarter());
        setAllClocks(game.getClock(), TWENTY_FOUR_SECONDS);
    }

    private void execute(final String cmd) {
        final CommandLine cmdLine = CommandLine.parse(cmd);
        try {
            executor.execute(cmdLine);

        } catch (IOException e) {
            // e.printStackTrace();
        }
    }
}
