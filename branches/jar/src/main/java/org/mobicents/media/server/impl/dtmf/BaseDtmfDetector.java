/*
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
package org.mobicents.media.server.impl.dtmf;

import java.util.ArrayList;
import java.util.List;
import org.mobicents.media.server.impl.BaseResource;
import org.mobicents.media.server.impl.common.events.EventCause;
import org.mobicents.media.server.impl.common.events.EventID;
import org.mobicents.media.server.spi.MediaSink;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.dtmf.DTMF;
import org.mobicents.media.server.spi.events.NotifyEvent;
/**
 * Implements common fatures for DTMF detector.
 * 
 * @author Oleg Kulikov
 */
public abstract class BaseDtmfDetector extends BaseResource 
        implements MediaSink, DTMF {

    protected DtmfBuffer digitBuffer;
    private List <NotificationListener> listeners = new ArrayList();
    

    public BaseDtmfDetector() {
        digitBuffer = new DtmfBuffer(this);
    }
    
    public void setDtmfMask(String mask) {
        digitBuffer.setMask(mask);
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

    protected void sendEvent(String seq) {
        NotifyEvent evt = new NotifyEvent(this, EventID.DTMF, getCause(seq), seq);
        synchronized (listeners) {
            for (NotificationListener listener : listeners) {
                listener.update(evt);
            }

            listeners.clear();
        }
        this.stop();
    }
    
    private EventCause getCause(String seq) {
        if (seq.equals("0")) {
            return EventCause.DTMF_DIGIT_0;
        } else if (seq.equals("1")) {
            return EventCause.DTMF_DIGIT_1;
        } else if (seq.equals("2")) {
            return EventCause.DTMF_DIGIT_2;
        } else if (seq.equals("3")) {
            return EventCause.DTMF_DIGIT_3;
        } else if (seq.equals("4")) {
            return EventCause.DTMF_DIGIT_4;
        } else if (seq.equals("5")) {
            return EventCause.DTMF_DIGIT_5;
        } else if (seq.equals("6")) {
            return EventCause.DTMF_DIGIT_6;
        } else if (seq.equals("7")) {
            return EventCause.DTMF_DIGIT_7;
        } else if (seq.equals("8")) {
            return EventCause.DTMF_DIGIT_8;
        } else if (seq.equals("9")) {
            return EventCause.DTMF_DIGIT_9;
        } else if (seq.equals("10")) {
            return EventCause.DTMF_DIGIT_STAR;
        } else if (seq.equals("11")) {
            return EventCause.DTMF_DIGIT_NUM;
        } else {
            return EventCause.DTMF_SEQ;
        }
    }
    
}
