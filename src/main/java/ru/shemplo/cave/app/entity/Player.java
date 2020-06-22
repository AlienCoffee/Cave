package ru.shemplo.cave.app.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@AllArgsConstructor
@EqualsAndHashCode (of = {"login", "idhh"})
public class Player {
    
    private String login;
    
    private String idhh;
    
    private boolean ready = false;
    
    public String serialize () {
        return String.join (",", login, idhh, String.valueOf (ready));
    }
    
}
