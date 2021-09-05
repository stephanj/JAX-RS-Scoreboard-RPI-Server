package org.janssen.scoreboard.controller;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.type.TeamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
@Component
public class DeviceController {

    private final Logger log = LoggerFactory.getLogger(DeviceController.class);

    final String cmd_score = "/home/pi/score";
//    final String cmd_score = "/bin/echo";

    final DefaultExecutor executor = new DefaultExecutor();

    private final GameClockController gameClockController;

    private final TwentyFourClockController twentyFourClockController;

    public DeviceController(GameClockController gameClockController,
                            TwentyFourClockController twentyFourClockController) {
        this.gameClockController = gameClockController;
        this.twentyFourClockController = twentyFourClockController;
    }

    @PostConstruct
    public void init() {
        executor.setExitValue(1);
    }

    @Async
    public void setScore(final Team team) {
        if (team.getKey().equalsIgnoreCase(TeamType.A.toString())) {
            setScoreHome(team.getScore());
        } else {
            setScoreVisitors(team.getScore());
        }
    }

    @Async
    public void setScoreHome(final int score) {
        execute(String.format("%s -h%03d", cmd_score, score));
    }

    @Async
    public void setScoreVisitors(final int score) {
        execute(String.format("%s -v%03d", cmd_score, score));
    }

    @Async
    public void setPlayerFoul(final int foul) {
        execute(String.format("%s -q%d", cmd_score, foul));

        try {
            Thread.sleep(SEVEN_SECONDS_IN_MILLI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        execute(String.format("%s -q%d", cmd_score, 0));
    }

    @Async
    public void setFoul(final Team team) {
        if (team.getKey().equalsIgnoreCase(TeamType.A.toString())) {
            setFoulsHome(team.getFouls());
        } else {
            setFoulsVisitors(team.getFouls());
        }
    }

    @Async
    public void setFoulsHome(final int fouls) {
        execute(String.format("%s -a%d", cmd_score, fouls));
    }

    @Async
    public void turnOff() {
        execute(String.format("%s -z", cmd_score));
    }

    @Async
    public void setFoulsVisitors(final int fouls) {
        execute(String.format("%s -b%d", cmd_score, fouls));
    }

    @Async
    public void setAllClocks(final int seconds, final int twentyFourSeconds) {
        setClockOnly(seconds);
        execute(String.format("%s -t%02d", cmd_score, twentyFourSeconds));
    }

    @Async
    public void setClockOnly(final int seconds) {
        execute(String.format("%s -k%02d%02d", cmd_score, seconds/60, seconds%60));
    }

    @Async
    public void setTwentyFour(final int seconds) {
        execute(String.format("%s -t%02d", cmd_score, seconds));
    }

    @Async
    public void clearBoard() {
        execute(String.format("%s -z", cmd_score));
    }

    @Async
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
            log.error(e.getMessage());
        }
    }
}
