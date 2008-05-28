package org.mobicents.csapi.jr.slee.cc.gccs;

import org.mobicents.csapi.jr.slee.ParlayConnection;

/**
 *    This event indicates to the application that all event notifications have been temporarily interrupted (for example, due to faults detected).
Note that more permanent failures are reported via the Framework (integrity management).
 * 
 * 
 */
public class CallNotificationInterruptedEvent extends org.mobicents.csapi.jr.slee.ResourceEvent{

    /**
     * Constructor for CallNotificationInterruptedEvent
     * @param tpServiceIdentifier the service this event is related to
     */
    public CallNotificationInterruptedEvent(org.mobicents.csapi.jr.slee.TpServiceIdentifier tpServiceIdentifier){
        super(tpServiceIdentifier);
    }


    /**
     * Indicates whether some other object is 'equal to' this one.
     */
    public boolean equals(Object o) {
        if (o == null) { 
            return false;
        }
        if(!(o instanceof CallNotificationInterruptedEvent)) {
            return false;
        } 
        CallNotificationInterruptedEvent callNotificationInterruptedEvent = (CallNotificationInterruptedEvent) o;
        if(!(this.getService() == callNotificationInterruptedEvent.getService())) {
            return false;
        }
        if (this.hashCode() != o.hashCode()) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hashcode value for the object.
     */
    public int hashCode() {
        return 1;
    }

    // VARIABLES
    // .......................................................


} // CallNotificationInterruptedEvent

