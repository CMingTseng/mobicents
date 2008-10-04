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

import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.Buffer;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.apache.log4j.Logger;
/**
 *
 * @author Oleg Kulikov
 */
public abstract class AbstractSource implements MediaSource {

    protected MediaSink sink;
    private List<NotificationListener> listeners = new ArrayList();
	protected  Logger logger=Logger.getLogger(this.getClass());


    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.MediaStream#connect(MediaSink).
     */
    public void connect(MediaSink sink) {
        this.sink = sink;
        if (((AbstractSink) sink).mediaStream == null) {
            sink.connect(this);
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.MediaStream#diconnection(MediaSink).
     */
    public void disconnect(MediaSink sink) {
        this.sink = null;
        ((AbstractSink) sink).mediaStream = null;
    }

    public void addListener(NotificationListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(NotificationListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected void sendEvent(NotifyEvent evt) {
        synchronized (listeners) {
            for (NotificationListener listener : listeners) {
                listener.update(evt);
            }
        }
    }

    public void dispose() {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /**
     * Makes delivery on connected sink. 
     * @param buffer
     * @return <ul><li><b>true</b> - if delivery was success and can repeat </li><li><b>false</b> - delivery failed due to some error, it should not be repeated</li></ul>
     */
    protected boolean makeReceive(Buffer buffer)
    {
    	//lets give us a slightest chance
    	if(sink==null)
    	{
    		return false;
    	}else
    	{
    	
    		try{
    			if (!sink.isAcceptable(buffer.getFormat())) {
        			if (logger.isDebugEnabled()) {
        				logger.debug("xxx Discard " + buffer + ", not acceptable");
        			}
        			return true;
        		}

    			sink.receive(buffer);
    		}catch(NullPointerException npe)
    		{
    			logger.info(" Source : delivery failed, possibly out of sync delivery.");
    			return false;
    		}catch(RuntimeException re)
    		{
    			
    			if(logger.isDebugEnabled())
    			{
    				logger.debug(" Source : delivery failed, due to unknown error",re);
    			}else
    			{
    				logger.info(" Source : delivery failed, due to unknown error");
    			}
    			
    			return false;
    		}
    	}
    	return true;
    	
    }
    
}
