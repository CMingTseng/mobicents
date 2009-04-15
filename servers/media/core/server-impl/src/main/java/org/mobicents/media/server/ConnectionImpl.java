/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server;

import org.jboss.util.id.UID;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 */
public abstract class ConnectionImpl implements Connection {

    private String id;
    private int index;
    
    private EndpointImpl endpoint;
    private ConnectionState state = ConnectionState.NULL;
    private ConnectionMode mode;
    
    public ConnectionImpl(EndpointImpl endpoint, ConnectionMode mode) {
        this.id = genID();
        this.endpoint = endpoint;
        this.mode = mode;
        this.index = endpoint.getIndex();
    }
    
    /**
     * Generates unique identifier for this connection.
     * 
     * @return hex view of the unique integer.
     */
    private String genID() {
        return (new UID()).toString();
    }
    
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getIndex() {
        return index;
    }
    
    public ConnectionState getState() {
        return state;
    }

    protected void setState(ConnectionState state) {
        this.state = state;
    }
    
    public int getLifeTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setLifeTime(int lifeTime) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ConnectionMode getMode() {
        return mode;
    }

    public void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }


    public void addListener(ConnectionListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeListener(ConnectionListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void close() {
        
    }
}
