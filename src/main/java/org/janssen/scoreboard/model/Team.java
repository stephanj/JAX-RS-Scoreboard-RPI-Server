package org.janssen.scoreboard.model;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Stephan Janssen
 */
@Entity
@XmlRootElement(name = "team")
public class Team extends Model {

    private String name;

    private String key;

    private Integer score = 0;

    private Integer fouls = 0;

    private Integer timeOut = 0;

    private Boolean mirrored = false;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(final Integer score) {
        this.score = score;
    }

    public Integer getFouls() {
        return fouls;
    }

    public void setFouls(final Integer fouls) {
        this.fouls = fouls;
    }

    public Integer getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(final Integer timeOut) {
        this.timeOut = timeOut;
    }

    public Boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(final Boolean mirrored) {
        this.mirrored = mirrored;
    }
}
