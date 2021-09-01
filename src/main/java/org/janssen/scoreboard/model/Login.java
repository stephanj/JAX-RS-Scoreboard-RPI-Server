package org.janssen.scoreboard.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Stephan Janssen
 */
@XmlRootElement
public class Login {

    private String username;

    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
