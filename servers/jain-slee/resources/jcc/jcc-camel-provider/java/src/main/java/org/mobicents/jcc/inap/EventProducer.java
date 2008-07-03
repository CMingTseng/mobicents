/*
 * The Java Call Control API for CAMEL 2
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

package org.mobicents.jcc.inap;

import javax.csapi.cc.jcc.JccConnectionListener;
import javax.csapi.cc.jcc.JccConnectionEvent;
import javax.csapi.cc.jcc.EventFilter;

import org.apache.log4j.Logger;

/**
 *
 * @author Oleg Kulikov
 */
public class EventProducer implements Runnable {
    
    private JccConnectionListener listener;
    private JccConnectionEvent event;
    
    private Logger logger = Logger.getLogger(EventProducer.class);
    
    /** Creates a new instance of EventProducer */
    public EventProducer(JccConnectionListener listener, JccConnectionEvent event) {
        this.listener = listener;
        this.event = event;
    }
    
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Thread.yield();
/*        try {
            Thread.currentThread().sleep(10);
        } catch (InterruptedException e) {
        }
 */
        switch (event.getID()) {
            case JccConnectionEvent.CALL_ACTIVE :
                listener.callActive(event);
                break;
            case JccConnectionEvent.CALL_CREATED :
                listener.callCreated(event);
                break;
            case JccConnectionEvent.CALL_EVENT_TRANSMISSION_ENDED :
                listener.callEventTransmissionEnded(event);
                break;
            case JccConnectionEvent.CALL_INVALID :
                listener.callInvalid(event);
                break;
            case JccConnectionEvent.CALL_SUPERVISE_END :
                listener.callSuperviseEnd(event);
                break;
            case JccConnectionEvent.CALL_SUPERVISE_START :
                listener.callSuperviseStart(event);
                break;
            case JccConnectionEvent.CONNECTION_ADDRESS_ANALYZE :
                listener.connectionAddressAnalyze(event);
                break;
            case JccConnectionEvent.CONNECTION_ADDRESS_COLLECT :
                listener.connectionAddressCollect(event);
                break;
            case JccConnectionEvent.CONNECTION_ALERTING :
                listener.connectionAlerting(event);
                break;
            case JccConnectionEvent.CONNECTION_AUTHORIZE_CALL_ATTEMPT :
                listener.connectionAuthorizeCallAttempt(event);
                break;
            case JccConnectionEvent.CONNECTION_CALL_DELIVERY :
                listener.connectionCallDelivery(event);
                break;
            case JccConnectionEvent.CONNECTION_CONNECTED :
                listener.connectionConnected(event);
                break;
            case JccConnectionEvent.CONNECTION_CREATED :
                listener.connectionCreated(event);
                break;
            case JccConnectionEvent.CONNECTION_DISCONNECTED :
                listener.connectionDisconnected(event);
                break;
            case JccConnectionEvent.CONNECTION_FAILED :
                listener.connectionFailed(event);
                break;
            case JccConnectionEvent.CONNECTION_MID_CALL :
                listener.connectionMidCall(event);
                break;
        }
    }
    
}
