/*
 * MsTerminationEventImpl.java
 *
 * The Simple Media API RA
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
package org.mobicents.mscontrol.impl;

import java.util.Collection;
import org.mobicents.mscontrol.MsLink;
import org.mobicents.mscontrol.MsLinkEvent;
import org.mobicents.mscontrol.MsLinkEventCause;
import org.mobicents.mscontrol.MsLinkEventID;
import org.mobicents.mscontrol.MsLinkListener;

/**
 * 
 * @author Oleg Kulikov
 */
public class MsLinkEventImpl implements MsLinkEvent, Runnable {

    /**
     * 
     */
    private static final long serialVersionUID = -3952630702449890912L;
    private MsLinkImpl source;
    private MsLinkEventID eventID;
    private MsLinkEventCause cause;
    private String msg;

    /** Creates a new instance of MsTerminationEventImpl */
    public MsLinkEventImpl(MsLinkImpl source, MsLinkEventID eventID, MsLinkEventCause cause) {
        this.source = source;
        this.eventID = eventID;
        this.cause = cause;
    }

    public MsLinkEventImpl(MsLinkImpl source, MsLinkEventID eventID, MsLinkEventCause cause, String msg) {
        this.source = source;
        this.eventID = eventID;
        this.cause = cause;
        this.msg = msg;
    }

    public MsLink getSource() {
        return source;
    }

    public MsLinkEventID getEventID() {
        return eventID;
    }

    public MsLinkEventCause getCause() {
        return cause;
    }

    public String getMessage() {
        return msg;
    }

    private void update(Collection<MsLinkListener> listeners) {
        for (MsLinkListener listener : listeners) {
            switch (eventID) {
                case LINK_CREATED:
                    listener.linkCreated(this);
                    break;
                case LINK_CONNECTED:
                    listener.linkConnected(this);
                    break;
                case LINK_DISCONNECTED:
                    listener.linkDisconnected(this);
                    break;
                case LINK_FAILED:
                    listener.linkFailed(this);
                    break;
                case MODE_HALF_DUPLEX :
                    listener.modeHalfDuplex(this);
                    break;
                case MODE_FULL_DUPLEX :
                    listener.modeFullDuplex(this);
                    break;
                    
            }
        }
    }

    public void run() {
        update(source.session.provider.linkListeners);
        update(source.linkLocalLinkListeners);
    }
}