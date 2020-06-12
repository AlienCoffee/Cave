package ru.shemplo.cave.app.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class GameServer implements Closeable {
    
    public static void main (String ... args) throws IOException, InterruptedException {
        //System.setProperty ("javax.net.ssl.keyStorePassword", "");
        //System.setProperty ("javax.net.ssl.keyStore", "server.jks");
        
        System.out.println ("Server started"); // SYSOUT
        final var server = new GameServer ();
        server.open (12763);
    }
    
    private ConnectionsPool pool = new ConnectionsPool ();
    private SSLServerSocket socket;
    private Thread acceptor;
    
    public void open (int port) throws IOException {
        final var factory = SSLServerSocketFactory.getDefault ();
        socket = (SSLServerSocket) factory.createServerSocket (port, 10);
        //socket.setNeedClientAuth (true);
        socket.setEnabledCipherSuites (new String [] {
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"
        });
        socket.setEnabledProtocols (new String [] {
            "TLSv1.2"
        });
        socket.setSoTimeout (10000);
        
        acceptor = new Thread (() -> {
            while (true) {
                System.out.println ("New loop"); // SYSOUT
                if (Thread.currentThread ().isInterrupted ()) {
                    return;
                }
                
                try {                        
                    final var client = (SSLSocket) socket.accept ();
                    System.out.println ("Connection accepted"); // SYSOUT
                    pool.addConnection (client);
                } catch (SocketTimeoutException ste) {
                    // this is okey, it's need just to start new cycle loop to check interruption
                } catch (SocketException se) {
                    // any case of this exceptions is a statement for Socket, so stop accepting
                    System.out.println (se.getMessage ()); // SYSOUT
                    return;
                } catch (IOException ioe) {
                    
                }
            }
        }, "Income-Connections-Acceptor");
        acceptor.start ();
    }

    @Override
    public void close () throws IOException {
        socket.close (); // stop accepting new connections
        pool.close (); // closing existing connections
    }
    
}
