/*
 * SocketAcceptor
 * 
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Accept socket connections from a ServerSocket, and notify a listener using a
 * separate thread.
 * 
 * @author jmfee
 * 
 */
public class SocketAcceptor implements Runnable {

    /** Track active sockets. */
    private LinkedList<Future<?>> activeSockets = new LinkedList<Future<?>>();

    /** Socket used to accept connections. */
    private ServerSocket listener;

    /** Handler used to process accepted connections. */
    private SocketListenerInterface callback;

    /** Sockets are processed by this executor. */
    private ExecutorService socketExecutor;

    /** Whether to keep accepting connections. */
    private boolean listening = true;

    /**
     * Create a new SocketAcceptor object that uses a single thread executor.
     * 
     * @param listener
     *            the server socket to accept connections from.
     * @param callback
     *            the object that processes accepted connections.
     */
    public SocketAcceptor(final ServerSocket listener,
            SocketListenerInterface callback) {
        this(listener, callback, Executors.newSingleThreadExecutor());
    }

    /**
     * 
     * @param listener
     *            the server socket to accept connections from.
     * @param callback
     *            the object that processes accepted connections.
     * @param executor
     *            the executor used to invoke callback.
     */
    public SocketAcceptor(final ServerSocket listener, 
    		SocketListenerInterface callback, ExecutorService executor) {
        this.listener = listener;
        this.callback = callback;
        this.socketExecutor = executor;
    }

    /**
     * Start accepting connections in a background thread.
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * Stop accepting connections.
     */
    public void stop() {
        // tell accept thread to stop accepting
        listening = false;
        try {
            // close the server socket, will cause exception in blocking accept
            // call
            listener.close();
        } catch (Exception e) {
            // ignore
        }

        // process any queued sockets
        socketExecutor.shutdown();
    }

    /**
     * Accept connections until the shutdown method is called.
     */
    public void run() {
        Socket socket = null;
        while (listening) {
            try {
                socket = listener.accept();
            } catch (Exception e) {
                socket = null;
                if (!listening) {
                    //exception was thrown because socket was closed
                    break;
                }
                System.err.println("Error accepting connection");
                e.printStackTrace();
            }

            // check if this is a valid socket
            if (socket != null) {
                final Socket threadSocket = socket;
                final SocketListenerInterface threadCallback = callback;

                // schedule processing
                Future<?> socketThread = socketExecutor.submit(new Runnable() {
                    public void run() {
                        try {
                            threadCallback.onSocket(threadSocket);
                        } catch (Exception e) {
                            System.err.println("SocketListener callback threw exception:");
                            e.printStackTrace();
                        }
                    }
                });
                //track this to see if a socket is blocking...
                activeSockets.add(socketThread);
            } else {
                System.err.println("Socket is null while accepting connection.");
            }

            // see how many sockets are still in the queue
            // but first remove any completed sockets
            Iterator<Future<?>> iter = activeSockets.iterator();
            while (iter.hasNext()) {
                Future<?> next = iter.next();
                try {
                    if ( (next.get(0, TimeUnit.MILLISECONDS)) == null ) {
                        iter.remove();
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
            System.err.println("There are " + activeSockets.size() + " active/queued socket connections");
        }
    }

}
