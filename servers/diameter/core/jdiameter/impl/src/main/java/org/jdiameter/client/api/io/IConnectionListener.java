/*
 * Copyright (c) 2006 jDiameter.
 * https://jdiameter.dev.java.net/
 *
 * License: GPL v3
 *
 * e-mail: erick.svenson@yahoo.com
 *
 */
package org.jdiameter.client.api.io;

import org.jdiameter.client.api.IMessage;

import java.util.List;

/**
 * <P>
 * An object that registers to be notified of events generated by a
 * <code>IConnection</code> object.
 * <P>
 * The <code>ConnectionListener</code> interface is implemented by a
 * PCB component.
 */

public interface IConnectionListener {

    /**
     * Notifies that connection is created
     * @param connKey identifier of created connection
     */  
    void connectionOpened(String connKey);

    /**
     * Notifies that connection is closed
     * @param connKey identifier of closed connection
     * @param notSended array of not sended messages
     */
    void connectionClosed(String connKey, List notSended);

    /**
     * Notifies that connection is received incoming message
     * @param connKey identifier of connection
     * @param message received incoming message
     */
    void messageReceived(String connKey, IMessage message);

    /**
     * Notifies that connection is generated excpetion
     * @param connKey identifier of connection
     * @param message  the message from that failed
     * @param cause generated exceptions
     */
    void internalError(String connKey, IMessage message, TransportException cause);
}
