package ru.shemplo.cave.app.network;


public class MessageService {
    
    public static String [] parseMessage (String message) {
        return message.split (";;;");
    }
    
    public static String packMessage (String ... values) {
        return String.join (";;;", values);
    }
    
    public static String packMessage (String first, String ... values) {
        return String.format ("%s;;;%s", first, String.join (";;;", values));
    }
    
}
