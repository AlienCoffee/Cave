package ru.shemplo.cave.app.network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLSocket;

import lombok.Getter;
import lombok.Setter;
import ru.shemplo.cave.utils.Utils;

public class ClientConnection implements Closeable {
    
    private final SSLSocket socket;
    
    private final String idh;
    private final int id;
    
    private final OutputStream os;
    private final Writer w;
    
    private final InputStream is;
    private final BufferedReader br;
    
    @Setter
    private Runnable onHandshakeFinished;
    
    @Getter
    private boolean isAlive = true;
    
    public ClientConnection (int id, SSLSocket socket) throws IOException {
        this.id = id; this.socket = socket; 
        
        idh = Utils.digest (String.valueOf (id), RunServer.SERVER_SALT);
        
        os = socket.getOutputStream ();
        w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
        
        is = socket.getInputStream ();
        final var r = new InputStreamReader (is, StandardCharsets.UTF_8);
        br = new BufferedReader (r);
        
        socket.addHandshakeCompletedListener (this::authorizeConnection);
    }
    
    public void startHandshake () throws IOException {
        socket.startHandshake ();
    }
    
    private void authorizeConnection (HandshakeCompletedEvent hce) {
        sendMessage (String.format ("identifier;%s", idh));
        Optional.ofNullable (onHandshakeFinished).ifPresent (Runnable::run);
    }
    
    public void sendMessage (String message) {
        try {
            w.write (message); w.write ("\n");
            w.flush ();
        } catch (Exception ioe) {
            System.out.println ("Connection #" + id + " has died"); // SYSOUT
            isAlive = false;
        }
    }
    
    public Optional <String> readMessage () {
        try {            
            if (!br.ready ()) { return Optional.empty (); }
            final var line = br.readLine ();
            
            final var idEnd = line.indexOf (';');
            if (idEnd == -1) {
                return Optional.empty ();
            }
            
            // Check for authorization
            final var identifier = line.substring (0, idEnd);
            if (idh.equals (identifier)) {
                return Optional.of (line.substring (idEnd + 1));
            }
            
            return Optional.empty ();
        } catch (IOException ioe) {
            System.out.println ("Connection #" + id + " has died"); // SYSOUT
            isAlive = false;
            
            return Optional.empty ();
        }
    }

    @Override
    public void close () throws IOException {
        socket.close ();
    }
    
}
