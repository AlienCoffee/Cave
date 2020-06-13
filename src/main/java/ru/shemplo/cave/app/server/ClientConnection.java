package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

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
import java.util.function.BiConsumer;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLSocket;

import lombok.Getter;
import lombok.Setter;
import ru.shemplo.cave.utils.Utils;

public class ClientConnection implements Closeable {
    
    private final SSLSocket socket;
    
    @Setter private String idh;
    @Getter private final int id;
    
    private final OutputStream os;
    private final Writer w;
    
    private final InputStream is;
    private final BufferedReader br;
    
    @Setter
    private Runnable onHandshakeFinished;
    
    @Setter
    private BiConsumer <String [], ClientConnection> onReadMessage;
    
    @Setter @Getter
    private boolean isAlive = true;
    private long lastAliveTest;
    
    private final Thread readThread;
    
    @Setter @Getter
    private String login;
    
    public ClientConnection (Integer id, SSLSocket socket) throws IOException {
        this.id = id == null ? -1 : id.intValue (); this.socket = socket; 
        
        os = socket.getOutputStream ();
        w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
        
        is = socket.getInputStream ();
        final var r = new InputStreamReader (is, StandardCharsets.UTF_8);
        br = new BufferedReader (r);
        
        readThread = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted () && isAlive) {                
                try {
                    final var parts = Optional.ofNullable (br.readLine ())
                        . map (MessageService::parseMessage).orElse (null);
                    if (parts == null) { throw new IOException (); }
                    lastAliveTest = System.currentTimeMillis ();
                    
                    if (idh == null || (parts.length > 0 && idh.equals (parts [0]))) {
                        Optional.ofNullable (onReadMessage).ifPresent (h -> h.accept (parts, this));
                    }
                } catch (IOException ioe) {
                    System.out.println ("Connection #" + id + " is over"); // SYSOUT
                    isAlive = false;
                }
            }
            
            System.out.println ("Reading thread of #" + id + " is interrupted"); // SYSOUT
            Thread.currentThread ().interrupt ();
        });
        
        readThread.setDaemon (true);
        
        if (id != null) {
            idh = Utils.digest (String.valueOf (id), CaveServer.SERVER_SALT);
            socket.addHandshakeCompletedListener (this::authorizeConnection);
        } else {
            socket.addHandshakeCompletedListener (this::onHandshakeFinished);
        }
    }
    
    public void startHandshake () throws IOException {
        System.out.println ("Starting handshake with #" + id); // SYSOUT
        socket.startHandshake ();
    }
    
    private void authorizeConnection (HandshakeCompletedEvent hce) {
        System.out.println ("Handshake with #" + id + " is finished");
        sendMessage (IDENTIFIER.getValue ());
        onHandshakeFinished (hce);
    }
    
    private void onHandshakeFinished (HandshakeCompletedEvent hce) {
        Optional.ofNullable (onHandshakeFinished).ifPresent (Runnable::run);
        readThread.start ();
    }
    
    private void sendPackedMessage (String message) {
        try {
            //System.out.println ("Out: " + message); // SYSOUT
            w.write (message); w.write ('\n'); 
            w.flush ();
            
            lastAliveTest = System.currentTimeMillis ();
        } catch (Exception ioe) {
            System.out.println ("Connection #" + id + " is over"); // SYSOUT
            isAlive = false;
        }
    }
    
    public void sendMessage (String ... values) {
        sendPackedMessage (MessageService.packMessage (idh, values));
    }
    
    public long getNonTestedTime () {
        return System.currentTimeMillis () - lastAliveTest;
    }
    
    public boolean canBeRemoved (ConnectionsPool pool) {
        if (isAlive ()) { return false; }
        
        if (!isClosed) {
            pool.broadcastMessage (LEAVE_LOBBY.getValue (), getLogin ());
            
            try   { close (); }
            catch (IOException e) {}
        }
        
        return true;
    }

    private boolean isClosed = false;
    
    @Override
    public void close () throws IOException {
        isClosed = true;
        
        System.out.println ("Closing #" + id); // SYSOUT
        readThread.interrupt ();
        socket.close ();
    }
    
}
