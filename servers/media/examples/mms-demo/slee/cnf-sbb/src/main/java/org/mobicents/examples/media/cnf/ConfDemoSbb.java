/*
 * CallSbb.java
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
package org.mobicents.examples.media.cnf;

import java.util.ArrayList;
import java.util.List;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import org.apache.log4j.Logger;
import org.mobicents.examples.media.Announcement;
import org.mobicents.mscontrol.MsConnection;
import org.mobicents.mscontrol.MsLinkEvent;

/**
 *
 * @author Oleg Kulikov
 */
public abstract class ConfDemoSbb implements Sbb {
    
    private final static String WELCOME_MSG =
            "http://"+System.getProperty("jboss.bind.address", "127.0.0.1")+":8080/msdemo/audio/cnfannouncement.wav";
    
    private SbbContext sbbContext;
    private Logger logger = Logger.getLogger(ConfDemoSbb.class);
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.examples.media.Demo#startConversation(String, ActivityContextInterface).
     */
    public void startDemo(String endpointName) {
        this.setUserEndpoint(endpointName);
        ChildRelation childRelation = this.getAnnouncementSbb();
        try {
            Announcement announcement = (Announcement) childRelation.create();
            sbbContext.getActivities()[0].attach(announcement);
            List sequence = new ArrayList();
            sequence.add(WELCOME_MSG);
            announcement.play(endpointName, sequence, false);
        } catch (CreateException e) {
            logger.error("Unexpected error, Caused by", e);
            MsConnection connection = (MsConnection)
                    sbbContext.getActivities()[0].getActivity();
            connection.release();
        }
    }

    public void onAnnouncementComplete(MsLinkEvent evt, ActivityContextInterface aci) {
        try {
            ChildRelation childRelation = this.getForestSbb();
            Forest forest = (Forest) childRelation.create();
            sbbContext.getActivities()[0].attach(forest);
            forest.enter(this.getUserEndpoint());
            logger.info("Going to the forest");
        } catch (CreateException e) {
            MsConnection connection = (MsConnection)
                    sbbContext.getActivities()[0].getActivity();
            connection.release();
        }
    }

    

    /**
     * CMP field accessor
     *  
     * @return the name of the user's endpoint. 
     */
    public abstract String getUserEndpoint();

    /**
     * CMP field accessor
     *  
     * @param endpoint the name of the user's endpoint. 
     */
    public abstract void setUserEndpoint(String endpointName);

    /**
     * Relation to Welcome dialog
     * 
     * @return child relation object.
     */
    public abstract ChildRelation getAnnouncementSbb();

    /**
     * Relation with Loop back dialog.
     * 
     * @return child relation object.
     */
    public abstract ChildRelation getForestSbb();
    
    
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = sbbContext;
    }
    
    public void unsetSbbContext() {
    }

    public void sbbCreate() throws CreateException {
    }

    public void sbbPostCreate() throws CreateException {
    }

    public void sbbActivate() {
    }

    public void sbbPassivate() {
    }

    public void sbbLoad() {
    }

    public void sbbStore() {
    }

    public void sbbRemove() {
    }

    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
    }

    public void sbbRolledBack(RolledBackContext arg0) {
    }
}
