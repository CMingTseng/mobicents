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

package org.mobicents.media.server.impl.jmf.player;

import java.io.Serializable;

/**
 *
 * @author Oleg Kulikov
 */
public class PlayerEvent implements Serializable {
    public final static int STARTED = 1;
    public final static int END_OF_MEDIA = 2;
    public final static int STOP_BY_REQUEST = 3;
    public final static int FACILITY_ERROR = 4;
    
    private int eventID;
    private AudioPlayer source;
    private String msg;
    
    public PlayerEvent(AudioPlayer source, int eventID, String msg) {
        this.source = source;
        this.eventID = eventID;
        this.msg = msg;
    }
    
    public AudioPlayer getSource() {
        return source;
    }
    
    public int getEventID() {
        return eventID;
    }
    
    public String getMessage() {
        return msg;
    }
}
