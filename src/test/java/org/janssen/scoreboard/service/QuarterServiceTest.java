package org.janssen.scoreboard.service;

import org.janssen.scoreboard.controller.DeviceController;
import org.janssen.scoreboard.controller.GPIOController;
import org.janssen.scoreboard.controller.GameClockController;
import org.janssen.scoreboard.dao.GameDAO;
import org.janssen.scoreboard.dao.TeamDAO;
import org.janssen.scoreboard.dao.TokenDAO;
import org.janssen.scoreboard.model.Game;
import org.janssen.scoreboard.model.Team;
import org.janssen.scoreboard.model.Token;
import org.janssen.scoreboard.model.type.GPIOType;
import org.janssen.scoreboard.model.type.GameType;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Stephan Janssen
 */
public class QuarterServiceTest {

    private QuarterService quarterService;

    private GameClockController mockGameClockController;
    private TokenDAO mockTokenDAO;
    private GameDAO mockGameDAO;
    private TeamDAO mockTeamDAO;
    private DeviceController mockDeviceController;
    private GPIOController mockGPIOController;

    @Before
    public void init() {
        mockGameClockController = createMock(GameClockController.class);
        mockTokenDAO = createMock(TokenDAO.class);
        mockGameDAO = createMock(GameDAO.class);
        mockTeamDAO = createMock(TeamDAO.class);
        mockDeviceController = createMock(DeviceController.class);
        mockGPIOController = createMock(GPIOController.class);

        quarterService = new QuarterService();
        quarterService.gameClockController = mockGameClockController;
        quarterService.tokenDAO = mockTokenDAO;
        quarterService.gameDAO = mockGameDAO;
        quarterService.teamDAO = mockTeamDAO;
        quarterService.device = mockDeviceController;
        quarterService.gpioController = mockGPIOController;
    }

    @Test
    public void incrementQuarterForBasketGame() {

        Team teamA = new Team();
        teamA.setKey("A");
        teamA.setName("Home");
        teamA.setTimeOut(1);
        teamA.setFouls(2);

        Team teamB = new Team();
        teamB.setKey("B");
        teamB.setName("Visitors");
        teamB.setTimeOut(2);
        teamB.setFouls(3);

        Game game = new Game();
        game.setCreatedOn(new Date());
        game.setGameType(GameType.BASKET);
        game.setTeamA(teamA);
        game.setTeamB(teamB);

        expect(mockGameClockController.isRunning()).andReturn(false).times(2);
        expect(mockGameClockController.getSeconds()).andReturn(0).times(2);
        expect(mockTokenDAO.find(eq("test"))).andReturn(new Token()).times(2);
        expect(mockGameDAO.find(eq(100L))).andReturn(game).times(2);
        mockGameDAO.setGameClock(eq(game));
        expectLastCall().times(2);
        mockGameClockController.setSeconds(eq(600));
        expectLastCall().times(2);
        mockGPIOController.showTwentyFourSeconds(eq(true));
        expectLastCall().times(2);
        expect(mockTeamDAO.update(isA(Team.class))).andReturn(teamA).times(3);
        expect(mockTeamDAO.update(isA(Team.class))).andReturn(teamB).times(3);
        expect(mockGameDAO.update(eq(game))).andReturn(game).times(2);
        mockDeviceController.setFoulsHome(eq(0));
        expectLastCall().times(2);
        mockDeviceController.setFoulsVisitors(eq(0));
        expectLastCall().times(2);
        mockDeviceController.setClockOnly(eq(600));
        expectLastCall().times(2);
        mockGPIOController.setLed(GPIOType.TIME_OUT_H1, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_V1, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_H2, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_V2, false);

        replay(mockGameClockController, mockTokenDAO, mockGameDAO, mockTeamDAO, mockDeviceController, mockGPIOController);

        // ----------------------------------------------- 1st Quarter
        assertTrue(2 == teamA.getFouls());
        assertTrue(3 == teamB.getFouls());

        quarterService.incrementQuarter(100L, "test");  // 2nd Quarter

        assertTrue(2L == game.getQuarter());

        // Fouls are reset per quarter
        assertTrue(0 == teamA.getFouls());
        assertTrue(0 == teamB.getFouls());

        // Timeouts shouldn't be reset yet
        assertTrue(1 == teamA.getTimeOut());
        assertTrue(2 == teamB.getTimeOut());

        quarterService.incrementQuarter(100L, "test");  // 3rd Quarter

        assertTrue(3L == game.getQuarter());

        // Timeouts should be reset now
        assertTrue(0 == teamA.getTimeOut());
        assertTrue(0 == teamB.getTimeOut());

        verify(mockGameClockController, mockTokenDAO, mockGameDAO, mockTeamDAO, mockDeviceController, mockGPIOController);
    }

    /**
     * De instellingen voor de U10.
     *
     * Fouten worden ook telkens terug op nul geplaatst na iedere periode van 2 maal 4 minuten.
     * Time outs worden op nul geplaatst bij aanvang van de tweede helft (dus na 4 maal 4 minuten).
     */
    @Test
    public void incrementQuarterForKidsBasketGame() {

        Team teamA = new Team();
        teamA.setKey("A");
        teamA.setName("Home");
        teamA.setTimeOut(1);

        Team teamB = new Team();
        teamB.setKey("B");
        teamB.setName("Visitors");
        teamB.setTimeOut(2);

        Game game = new Game();
        game.setCreatedOn(new Date());
        game.setGameType(GameType.BASKET_KIDS);
        game.setTeamA(teamA);
        game.setTeamB(teamB);

        expect(mockGameClockController.isRunning()).andReturn(false).times(4);
        expect(mockGameClockController.getSeconds()).andReturn(0).times(4);
        expect(mockTokenDAO.find(eq("test"))).andReturn(new Token()).times(4);
        expect(mockGameDAO.find(eq(100L))).andReturn(game).times(4);
        mockGameDAO.setGameClock(eq(game));
        expectLastCall().times(4);
        mockGameClockController.setSeconds(eq(600));
        expectLastCall().times(4);
        mockGPIOController.showTwentyFourSeconds(eq(true));
        expectLastCall().times(4);
        expect(mockTeamDAO.update(isA(Team.class))).andReturn(teamA).times(3);
        expect(mockTeamDAO.update(isA(Team.class))).andReturn(teamB).times(3);
        expect(mockGameDAO.update(eq(game))).andReturn(game).times(4);
        mockDeviceController.setFoulsHome(eq(0));
        expectLastCall().times(4);
        mockDeviceController.setFoulsVisitors(eq(0));
        expectLastCall().times(4);
        mockDeviceController.setClockOnly(eq(600));
        expectLastCall().times(4);
        mockGPIOController.setLed(GPIOType.TIME_OUT_H1, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_V1, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_H2, false);
        mockGPIOController.setLed(GPIOType.TIME_OUT_V2, false);

        replay(mockGameClockController, mockTokenDAO, mockGameDAO, mockTeamDAO, mockDeviceController, mockGPIOController);

                                                            // 1st Quarter
        assertTrue(1 == teamA.getTimeOut());
        assertTrue(2 == teamB.getTimeOut());

        quarterService.incrementQuarter(100L, "test");      // 2nd Quarter
        assertTrue(2L == game.getQuarter());
        assertTrue(1 == teamA.getTimeOut());    // Timeouts shouldn't be reset yet
        assertTrue(2 == teamB.getTimeOut());

        quarterService.incrementQuarter(100L, "test");      // 3rd Quarter
        assertTrue(3L == game.getQuarter());
        assertTrue(1 == teamA.getTimeOut());
        assertTrue(2 == teamB.getTimeOut());

        quarterService.incrementQuarter(100L, "test");      // 4th Quarter
        assertTrue(4L == game.getQuarter());
        assertTrue(1 == teamA.getTimeOut());
        assertTrue(2 == teamB.getTimeOut());

        quarterService.incrementQuarter(100L, "test");      // 5th Quarter
        assertTrue(5L == game.getQuarter());
        assertTrue(0 == teamA.getTimeOut());    // Timeouts should be reset
        assertTrue(0 == teamB.getTimeOut());

        verify(mockGameClockController, mockTokenDAO, mockGameDAO, mockTeamDAO, mockDeviceController, mockGPIOController);
    }
}
