package ru.shemplo.cave.app.network;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLSocket;

public class ConnectionsPool implements Closeable {
    
    public void addConnection (SSLSocket socket) throws IOException {
        socket.addHandshakeCompletedListener (hce -> {
            System.out.println (hce.getCipherSuite ()); // SYSOUT
            try (
                final var in = socket.getInputStream ();
            ) {
                System.out.println (new String (in.readAllBytes (), StandardCharsets.UTF_8)); // SYSOUT
            } catch (IOException ioe) {
                ioe.printStackTrace ();
            }
        });
        socket.startHandshake ();
    }

    @Override
    public void close () throws IOException {
        
    }
    
}
