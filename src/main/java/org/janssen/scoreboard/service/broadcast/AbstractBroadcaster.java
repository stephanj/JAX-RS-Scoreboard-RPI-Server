package org.janssen.scoreboard.service.broadcast;

/**
 * @author Stephan Janssen
 */
public abstract class AbstractBroadcaster {

    public final String NEW_GAME = "/game";
    public final String PERSONAL_FOUL = "/fouls";
    public final String FOULS_A = "/foulsA";
    public final String FOULS_B = "/foulsB";
    public final String QUARTER = "/quarter";
    public final String HOME = "/home";
    public final String VISITORS = "/visitors";
    public final String TIME = "/time";
    public final String TIMEOUT_HOME = "/timeout/home";
    public final String TIMEOUT_VISITORS = "/timeout/visitors";

}
