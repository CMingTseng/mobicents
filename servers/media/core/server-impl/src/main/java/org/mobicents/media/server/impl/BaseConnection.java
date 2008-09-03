/*
 * Mobicents Media Gateway
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */
package org.mobicents.media.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.util.id.UID;
import org.mobicents.media.server.impl.common.ConnectionMode;
import org.mobicents.media.server.impl.common.ConnectionState;
import org.mobicents.media.server.impl.dsp.Processor;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author Oleg Kulikov
 */
public abstract class BaseConnection implements Connection {

    protected String id;
    protected BaseEndpoint endpoint;
    protected String endpointName;
    
    protected ConnectionMode mode;
    protected ConnectionState state = ConnectionState.NULL;
    private int lifeTime = 30;
    
    protected Multiplexer mux;
    protected Demultiplexer demux;
    
    protected Processor inputDsp;
    protected Processor outputDsp;
    
    private TimerTask closeTask;
    private boolean timerStarted = false;
    
    private ReentrantLock stateLock = new ReentrantLock();
    private ArrayList<ConnectionListener> listeners = new ArrayList();
    
    protected transient Logger logger = Logger.getLogger(BaseConnection.class);

    private class CloseConnectionTask extends TimerTask {
        public void run() {
            logger.info("Connection timer expired, Disconnecting");
            timerStarted = false;
            endpoint.deleteConnection(id);
        }
    }

    public BaseConnection(Endpoint endpoint, ConnectionMode mode) throws ResourceUnavailableException {
        this.id = genID();
        this.mode = mode;

        this.endpoint = (BaseEndpoint) endpoint;
        this.endpointName = endpoint.getLocalName();

        mux = new Multiplexer();
        demux = new Demultiplexer();

        inputDsp = new Processor();
        outputDsp = new Processor();
        
        try {
            mux.getOutput().connect(outputDsp.getInput());
            demux.getInput().connect(inputDsp.getOutput());
            demux.start();
        } catch (IOException e) {
            throw new ResourceUnavailableException(e.getMessage());
        }
        
        setLifeTime(lifeTime);
        setState(ConnectionState.NULL);
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getId();
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the current state of this connection.
     * 
     * @return current state;
     */
    public ConnectionState getState() {
        return this.state;
    }

    /**
     * Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getLifeTime().
     */
    public int getLifeTime() {
        return lifeTime;
    }
    
    /**
     * Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#setLifeTime(int).
     */
    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
        if (timerStarted) {
            closeTask.cancel();
            BaseEndpoint.connectionTimer.purge();
            timerStarted = false;
        }
        this.closeTask = new CloseConnectionTask();
        BaseEndpoint.connectionTimer.schedule(closeTask, 60 * lifeTime * 1000);
        timerStarted = true;
    }
    
    /**
     * Generates unique identifier for this connection.
     * 
     * @return hex view of the unique integer.
     */
    private String genID() {
        return (new UID()).toString();
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getMode();
     */
    public ConnectionMode getMode() {
        return mode;
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#setMode(int);
     */
    public void setMode(ConnectionMode mode) {
        this.mode = mode;
    // @todo rebuilt send/recv streams.
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getEndpoint(int);
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Multiplexer getMux() {
        return mux;
    }

    public Demultiplexer getDemux() {
        return demux;
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#addListener(ConnectionListener)
     */
    public void addListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * (Non-Javadoc).
     * 
     * @see org.mobicents.media.server.spi.Connection#removeListener(ConnectionListener)
     */
    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    protected void setState(ConnectionState newState) {
        ConnectionState oldState = this.state;
        this.state = newState;
        
        for (ConnectionListener cl : listeners) {
            cl.onStateChange(this, oldState);
        }
        
        for (ConnectionListener cl : endpoint.connectionListeners) {
            cl.onStateChange(this, oldState);
        }
        
    }

    /**
     * Used for test cases only.
     * 
     * @return true if life time started.
     */
    protected boolean getLifeTimeTimerState() {
        return timerStarted;
    }
    
    /**
     * Releases all resources requested by this connection.
     * 
     * @throws InterruptedException
     */
    public void close() {
        if (timerStarted) {
            closeTask.cancel();
            timerStarted = false;
            BaseEndpoint.connectionTimer.purge();
        }

        mux.getOutput().disconnect(outputDsp.getInput());
        demux.getInput().disconnect(inputDsp.getOutput());

        this.setState(ConnectionState.CLOSED);
    }

    public void lockState() throws InterruptedException {
        this.stateLock.lock();
    }

    public void releaseState() {
        this.stateLock.unlock();
    }
}
