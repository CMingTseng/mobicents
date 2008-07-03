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

import javax.csapi.cc.jcc.JccCallEvent;
import javax.csapi.cc.jcc.JccCall;

/**
 *
 * @author Oleg Kulikov
 */
public class JccCallEventImpl implements JccCallEvent {
    
    private JccCall call = null;
    private Object source = null;
    private int id;
    private int cause;
    
    /** Creates a new instance of JccCallEventImpl */
    public JccCallEventImpl(Object source, JccCall call, int id, int cause) {
        this.call = call;
        this.source = source;
        this.id = id;
        this.cause = cause;
    }

    public JccCall getCall() {
        return call;
    }

    public int getCause() {
        return cause;
    }

    public int getID() {
        return id;
    }

    public Object getSource() {
        return source;
    }
    
}
