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
package org.mobicents.examples.media.recorder;

import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.AddressPlan;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.UnrecognizedActivityException;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TimerPreserveMissed;

import org.apache.log4j.Logger;
import org.mobicents.mscontrol.MsConnection;
import org.mobicents.mscontrol.MsConnectionEvent;
import org.mobicents.mscontrol.MsEndpoint;
import org.mobicents.mscontrol.MsLink;
import org.mobicents.mscontrol.MsLinkEvent;
import org.mobicents.mscontrol.MsLinkMode;
import org.mobicents.mscontrol.MsNotifyEvent;
import org.mobicents.mscontrol.MsProvider;
import org.mobicents.mscontrol.MsSession;
import org.mobicents.mscontrol.events.MsEventAction;
import org.mobicents.mscontrol.events.MsEventFactory;
import org.mobicents.mscontrol.events.MsRequestedEvent;
import org.mobicents.mscontrol.events.MsRequestedSignal;
import org.mobicents.mscontrol.events.ann.MsPlayRequestedSignal;
import org.mobicents.mscontrol.events.audio.MsRecordRequestedSignal;
import org.mobicents.mscontrol.events.pkg.MsAnnouncement;
import org.mobicents.mscontrol.events.pkg.MsAudio;
import org.mobicents.slee.resource.media.ratype.MediaRaActivityContextInterfaceFactory;

/**
 * 
 * @author Oleg Kulikov
 */
public abstract class RecorderDemoSbb implements Sbb {

    private final static String INFO_MSG = "http://" + System.getProperty("jboss.bind.address", "127.0.0.1") + ":8080/msdemo/audio/recorder.wav";
    private final static String RECORDER = "test.wav";
    private final static String IVR_ENDPOINT = "media/trunk/IVR/$";
    private SbbContext sbbContext;
    private MsProvider msProvider;
    private MediaRaActivityContextInterfaceFactory mediaAcif;
    private TimerFacility timerFacility;
    private Logger logger = Logger.getLogger(RecorderDemoSbb.class);

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.examples.media.Demo#startConversation(String,
     *      ActivityContextInterface).
     */
    public void startDemo(String endpointName) {
        logger.info("Joining " + endpointName + " with " + IVR_ENDPOINT);

        MsConnection connection = (MsConnection) getConnectionActivity().getActivity();
        MsSession session = connection.getSession();
        MsLink link = session.createLink(MsLinkMode.FULL_DUPLEX);

        ActivityContextInterface linkActivity = null;
        try {
            linkActivity = mediaAcif.getActivityContextInterface(link);
        } catch (UnrecognizedActivityException ex) {
        }

        linkActivity.attach(sbbContext.getSbbLocalObject());
        link.join(endpointName, IVR_ENDPOINT);

    }

    public void onIVRConnected(MsLinkEvent evt, ActivityContextInterface aci) {
        logger.info("Joined IVR connected, Starting announcement and recorder");
        MsLink link = evt.getSource();
        setUserEndpoint(link.getEndpoints()[1].getLocalName());

        play(INFO_MSG, link);
        try {
            startTimer(aci, 35);
            logger.info("Timer started");
        } catch (NamingException e) {
            logger.error("Unexpected error", e);
        }

    }
    
    private void play(String url, MsLink link) {
        MsEventFactory eventFactory = msProvider.getEventFactory();
        MsPlayRequestedSignal play = (MsPlayRequestedSignal) eventFactory.createRequestedSignal(MsAnnouncement.PLAY);
        play.setURL(url);

        MsRequestedEvent onCompleted = eventFactory.createRequestedEvent(MsAnnouncement.COMPLETED);
        onCompleted.setEventAction(MsEventAction.NOTIFY);

        MsRequestedEvent onFailed = eventFactory.createRequestedEvent(MsAnnouncement.FAILED);
        onFailed.setEventAction(MsEventAction.NOTIFY);

        MsRequestedSignal[] requestedSignals = new MsRequestedSignal[]{play};
        MsRequestedEvent[] requestedEvents = new MsRequestedEvent[]{onCompleted, onFailed};

        link.getEndpoints()[1].execute(requestedSignals, requestedEvents, link);
    }

    public void onAnnouncementComplete(MsNotifyEvent evt, ActivityContextInterface aci) {
        MsLink link = (MsLink) evt.getSource();
        MsEndpoint ivr = link.getEndpoints()[1];

        MsEventFactory eventFactory = msProvider.getEventFactory();
        MsRecordRequestedSignal record = (MsRecordRequestedSignal) eventFactory.createRequestedSignal(MsAudio.RECORD);
        record.setFile(RECORDER);


        MsRequestedEvent onFailed = eventFactory.createRequestedEvent(MsAudio.FAILED);
        onFailed.setEventAction(MsEventAction.NOTIFY);

        MsRequestedSignal[] requestedSignals = new MsRequestedSignal[]{record};
        MsRequestedEvent[] requestedEvents = new MsRequestedEvent[]{onFailed};
        
        ivr.execute(requestedSignals, requestedEvents, link);
    }
    

    public void onTimerEvent(TimerEvent event, ActivityContextInterface aci) {
        // disable recorder
        MsLink link = (MsLink) aci.getActivity();
        logger.info("Timer event,play back recorder file");
        String url = "file://" + System.getProperty("jboss.server.data.dir") + "/" + RECORDER;

        MsEventFactory eventFactory = msProvider.getEventFactory();
        MsPlayRequestedSignal play = (MsPlayRequestedSignal) eventFactory.createRequestedSignal(MsAnnouncement.PLAY);
        play.setURL(url);


        MsRequestedSignal[] requestedSignals = new MsRequestedSignal[]{play};
        MsRequestedEvent[] requestedEvents = new MsRequestedEvent[]{};

        link.getEndpoints()[1].execute(requestedSignals, requestedEvents, link);
    }


    public void onUserDisconnected(MsConnectionEvent evt, ActivityContextInterface aci) {
        logger.info("Finita la commedia");
        ActivityContextInterface activities[] = sbbContext.getActivities();
        for (int i = 0; i < activities.length; i++) {
            if (activities[i].getActivity() instanceof MsLink) {
                ((MsLink) activities[i].getActivity()).release();
            } 
        }

    }

    private ActivityContextInterface getConnectionActivity() {
        ActivityContextInterface[] activities = sbbContext.getActivities();
        for (int i = 0; i < activities.length; i++) {
            if (activities[i].getActivity() instanceof MsConnection) {
                return activities[i];
            }
        }

        return null;
    }

    private MsLink getLink() {
        ActivityContextInterface[] activities = sbbContext.getActivities();
        for (int i = 0; i < activities.length; i++) {
            if (activities[i].getActivity() instanceof MsLink) {
                return (MsLink) activities[i].getActivity();
            }
        }

        return null;
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
     * @param endpoint
     *            the name of the user's endpoint.
     */
    public abstract void setUserEndpoint(String endpointName);

    /**
     * Relation to Welcome dialog
     * 
     * @return child relation object.
     */
    public abstract ChildRelation getAnnouncementSbb();

    private void startTimer(ActivityContextInterface aci, int duration) throws NamingException {
        Context ctx = (Context) new InitialContext().lookup("java:comp/env");
        timerFacility = (TimerFacility) ctx.lookup("slee/facilities/timer");

        TimerOptions options = new TimerOptions(false, 1000 * duration, TimerPreserveMissed.NONE);
        Address address = new Address(AddressPlan.IP, "127.0.0.1");
        Date now = new Date();

        timerFacility.setTimer(aci, address, now.getTime() + 1000 * duration, options);
    }

    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = sbbContext;
        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
            timerFacility = (TimerFacility) ctx.lookup("slee/facilities/timer");
            msProvider = (MsProvider) ctx.lookup("slee/resources/media/1.0/provider");
            mediaAcif = (MediaRaActivityContextInterfaceFactory) ctx.lookup("slee/resources/media/1.0/acifactory");
        } catch (Exception e) {
            logger.error("Could not set SBB context", e);
        }
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
