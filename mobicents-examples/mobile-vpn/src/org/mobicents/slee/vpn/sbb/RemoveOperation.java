/*
/**
 * Mobile Virtual Private Network service.
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

package org.mobicents.slee.vpn.sbb;

/**
 *
 * @author Oleg Kulikov
 */
public class RemoveOperation extends Operation {
    
    /** Creates a new instance of RemoveOperation */
    public RemoveOperation() {
    }

    public RemoveOperation(Object[] args) {
        super(args);
    }

    public String doExecute(String[] args) {
        String subj = args[0];
        int pos = Integer.parseInt(args[1]);
        int len = Integer.parseInt(args[2]);
        
        String left = subj.substring(0, pos);
        String right = subj.substring(pos);
        
        return left + right.substring(len);
    }
    
}
