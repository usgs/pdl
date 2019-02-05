/*
 * SocketListenerInterface
 * 
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.net.Socket;

/**
 * An object that processes sockets.
 */
public interface SocketListenerInterface {

    /**
     * Called with any sockets to be read.
     * 
     * @param socket
     *            socket to be processed.
     */
    public void onSocket(final Socket socket);

}
