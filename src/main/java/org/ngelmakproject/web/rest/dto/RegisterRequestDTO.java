package org.ngelmakproject.web.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * View Model object for storing a user's info.
 */
public class RegisterRequestDTO {

    @NotNull
    @Size(min = 3, max = 50)
    private String username;

    @NotNull
    @Size(min = 8, max = 100)
    private String password;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "RegisterRequestDTO [username=" + username + ", password=" + password + "]";
    }

}
