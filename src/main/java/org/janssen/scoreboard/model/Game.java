package org.janssen.scoreboard.model;

import org.janssen.scoreboard.model.type.AgeCategory;
import org.janssen.scoreboard.model.type.GameType;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

import static org.janssen.scoreboard.service.util.Constants.*;

/**
 * @author Stephan Janssen
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "game.list", query = "select g from Game g where g.createdOn > :oneDayOld"),

        @NamedQuery(name = "game.count", query = "select count(g) from Game g")
})
@XmlRootElement(name = "game")
public class Game extends DatedModel {

    private String userName;

    @OneToOne
    private Team teamA;

    @OneToOne
    private Team teamB;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    private AgeCategory ageCategory;

    private Integer quarter = 1;

    private Integer clock = TEN_MINUTES_IN_SECONDS;

    private String court;

    private Boolean mirrored = false;

    public Team getTeamA() {
        return teamA;
    }

    public void setTeamA(final Team teamA) {
        this.teamA = teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public void setTeamB(final Team teamB) {
        this.teamB = teamB;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(final Integer quarter) {
        this.quarter = quarter;
    }

    public void incrementQuarter() {
        quarter++;
    }

    public void decrementQuarter() {
        quarter--;
    }

    public Integer getClock() {
        return clock;
    }

    public void setClock(final Integer clock) {
        this.clock = clock;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(final GameType gameType) {
        this.gameType = gameType;
    }

    public AgeCategory getAgeCategory() {
        return ageCategory;
    }

    public void setAgeCategory(final AgeCategory ageCategory) {
        this.ageCategory = ageCategory;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getCourt() {
        return court;
    }

    public void setCourt(final String court) {
        this.court = court;
    }

    public Boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(final Boolean mirrored) {
        this.mirrored = mirrored;
    }

    @Override
    public String toString() {
        return "Game{" +
            "userName='" + userName + '\'' +
            ", teamA=" + teamA +
            ", teamB=" + teamB +
            ", gameType=" + gameType +
            ", ageCategory=" + ageCategory +
            ", quarter=" + quarter +
            ", clock=" + clock +
            ", court='" + court + '\'' +
            ", mirrored=" + mirrored +
            '}';
    }
}
