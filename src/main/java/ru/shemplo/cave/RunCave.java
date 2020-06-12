package ru.shemplo.cave;

import javafx.application.Application;
import ru.shemplo.cave.app.CaveApplication;

public class RunCave {
    
    public static void main (String ... args) {
        System.setProperty ("javax.net.ssl.trustStore", "client.jks");
        Application.launch (CaveApplication.class, args);
    }
    
}
