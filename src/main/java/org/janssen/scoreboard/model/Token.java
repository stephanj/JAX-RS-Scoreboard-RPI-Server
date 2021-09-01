package org.janssen.scoreboard.model;

import org.janssen.scoreboard.model.type.GameType;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Authentication token
 *
 * @author Stephan Janssen
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "token.find", query = "select t from Token t where t.value = :tokenValue")
})
@XmlRootElement(name = "token")
public class Token extends DatedModel {

    @XmlTransient
    private String value;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private String fullName;

    public Token() {
    }

    public Token(String token) {
        this.value = token;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(final GameType gameType) {
        this.gameType = gameType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }
}
