package ru.shemplo.cave.app.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import ru.shemplo.cave.app.server.room.state.ServerState;

public class CaveServer implements Closeable {
    
    public static final String SERVER_SALT = String.valueOf (new Random ().nextInt ());
    public static final int SERVER_ID_STEP = 1 + new Random ().nextInt (3);
    
    public static void main (String ... args) throws IOException, InterruptedException {
        System.setProperty ("javax.net.ssl.keyStore", "server.jks");
        for (final var state : ServerState.values ()) {
            state.name ();
        }
        
        @SuppressWarnings ("resource")
        final var server = new CaveServer ();
        server.open (12763);
        
        System.out.println ("Server started"); // SYSOUT
    }
    
    private ConnectionsPool pool = new ConnectionsPool (this);
    private SSLServerSocket socket;
    private Thread acceptor;
    
    public void open (int port) throws IOException {
        ServerState.initialize ();
        pool.open ();
        
        final var factory = SSLServerSocketFactory.getDefault ();
        socket = (SSLServerSocket) factory.createServerSocket (port, 10);
        //socket.setNeedClientAuth (true);
        socket.setEnabledCipherSuites (new String [] {
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"
        });
        socket.setEnabledProtocols (new String [] {
            "TLSv1.2"
        });
        socket.setSoTimeout (5000);
        
        acceptor = new Thread (() -> {
            while (true) {
                if (Thread.currentThread ().isInterrupted ()) {
                    return;
                }
                
                try {                        
                    final var client = (SSLSocket) socket.accept ();
                    System.out.println ("New connection accepted"); // SYSOUT
                    pool.applyConnection (client);
                } catch (SocketTimeoutException ste) {
                    // this is okey, it's need just to start new cycle loop to check interruption
                } catch (SocketException se) {
                    // any case of this exceptions is a statement for Socket, so stop accepting
                    System.out.println (se.getMessage ()); // SYSOUT
                    return;
                } catch (IOException ioe) {
                    
                }
            }
        }, "Income-Connections-Acceptor-Thread");
        acceptor.start ();
    }

    @Override
    public void close () throws IOException {
        socket.close (); // stop accepting new connections
        pool.close (); // closing existing connections
    }
    
}
