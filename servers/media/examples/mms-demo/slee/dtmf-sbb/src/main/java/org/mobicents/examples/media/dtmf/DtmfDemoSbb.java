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

package org.mobicents.examples.media.dtmf;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.UnrecognizedActivityException;

import org.apache.log4j.Logger;
import org.mobicents.examples.media.Announcement;
import org.mobicents.media.server.impl.common.events.EventCause;
import org.mobicents.media.server.impl.common.events.EventID;
import org.mobicents.mscontrol.MsConnection;
import org.mobicents.mscontrol.MsConnectionEvent;
import org.mobicents.mscontrol.MsNotifyEvent;
import org.mobicents.mscontrol.MsProvider;
import org.mobicents.mscontrol.MsSignalDetector;
import org.mobicents.slee.resource.media.ratype.MediaRaActivityContextInterfaceFactory;

/**
 * 
 * @author Oleg Kulikov
 */
public abstract class DtmfDemoSbb implements Sbb {

	private final static String WELCOME_MSG = "http://localhost:8080/msdemo/audio/welcome.wav";
	private final static String DTMF_0 = "http://localhost:8080/msdemo/audio/dtmf0.wav";
	private final static String DTMF_1 = "http://localhost:8080/msdemo/audio/dtmf1.wav";
	private final static String DTMF_2 = "http://localhost:8080/msdemo/audio/dtmf2.wav";
	private final static String DTMF_3 = "http://localhost:8080/msdemo/audio/dtmf3.wav";
	private final static String DTMF_4 = "http://localhost:8080/msdemo/audio/dtmf4.wav";
	private final static String DTMF_5 = "http://localhost:8080/msdemo/audio/dtmf5.wav";
	private final static String DTMF_6 = "http://localhost:8080/msdemo/audio/dtmf6.wav";
	private final static String DTMF_7 = "http://localhost:8080/msdemo/audio/dtmf7.wav";
	private final static String DTMF_8 = "http://localhost:8080/msdemo/audio/dtmf8.wav";
	private final static String DTMF_9 = "http://localhost:8080/msdemo/audio/dtmf9.wav";

	private MsProvider msProvider;
	private MediaRaActivityContextInterfaceFactory mediaAcif;

	private SbbContext sbbContext;
	private Logger logger = Logger.getLogger(DtmfDemoSbb.class);

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.examples.media.Demo#startConversation(String,
	 *      ActivityContextInterface).
	 */
	public void startDemo(String endpointName) {
		this.setUserEndpoint(endpointName);
		logger.info("Playing welcome message: " + WELCOME_MSG);
		this.play(WELCOME_MSG);

		logger.info("Initializing DTMF detector");
		this.initDtmfDetector(getConnection());
	}

	public void onDtmf(MsNotifyEvent evt, ActivityContextInterface aci) {
		this.initDtmfDetector(getConnection());
		EventCause cause = evt.getCause();
		if (cause == EventCause.DTMF_DIGIT_0) {
			play(DTMF_0);
		} else if (cause == EventCause.DTMF_DIGIT_1) {
			play(DTMF_1);
		} else if (cause == EventCause.DTMF_DIGIT_2) {
			play(DTMF_2);
		} else if (cause == EventCause.DTMF_DIGIT_3) {
			play(DTMF_3);
		} else if (cause == EventCause.DTMF_DIGIT_4) {
			play(DTMF_4);
		} else if (cause == EventCause.DTMF_DIGIT_5) {
			play(DTMF_5);
		} else if (cause == EventCause.DTMF_DIGIT_6) {
			play(DTMF_6);
		} else if (cause == EventCause.DTMF_DIGIT_7) {
			play(DTMF_7);
		} else if (cause == EventCause.DTMF_DIGIT_8) {
			play(DTMF_8);
		} else if (cause == EventCause.DTMF_DIGIT_9) {
			play(DTMF_9);
		}
	}

	private void play(String url) {
		ChildRelation childRelation = this.getAnnouncementSbb();
		try {
			Announcement announcement = (Announcement) childRelation.create();
			this.getConnectionActivityContext().attach(announcement);
			List sequence = new ArrayList();
			sequence.add(url);
			announcement.play(this.getUserEndpoint(), sequence, false);
		} catch (CreateException e) {
			logger.error("Unexpected error, Caused by", e);
			MsConnection connection = getConnection();
			connection.release();
		}
	}

	private void initDtmfDetector(MsConnection connection) {
		MsSignalDetector dtmfDetector = msProvider.getSignalDetector(this.getUserEndpoint());
		try {
			ActivityContextInterface dtmfAci = mediaAcif.getActivityContextInterface(dtmfDetector);
			dtmfAci.attach(sbbContext.getSbbLocalObject());
			dtmfDetector.receive(EventID.DTMF, connection, new String[] {});
		} catch (UnrecognizedActivityException e) {
		}
	}

	public void onUserDisconnected(MsConnectionEvent evt, ActivityContextInterface aci) {
		ActivityContextInterface activities[] = sbbContext.getActivities();
		for (int i = 0; i < activities.length; i++) {
			if (activities[i].getActivity() instanceof MsSignalDetector) {
				((MsSignalDetector) activities[i].getActivity()).release();
			}
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

	private MsConnection getConnection() {
		ActivityContextInterface[] activities = sbbContext.getActivities();
		for (int i = 0; i < activities.length; i++) {
			if (activities[i].getActivity() instanceof MsConnection) {
				return (MsConnection) activities[i].getActivity();
			}
		}
		return null;
	}

	private ActivityContextInterface getConnectionActivityContext() {
		ActivityContextInterface[] activities = sbbContext.getActivities();
		for (int i = 0; i < activities.length; i++) {
			if (activities[i].getActivity() instanceof MsConnection) {
				return activities[i];
			}
		}
		return null;
	}

	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = sbbContext;
		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			msProvider = (MsProvider) ctx.lookup("slee/resources/media/1.0/provider");
			mediaAcif = (MediaRaActivityContextInterfaceFactory) ctx.lookup("slee/resources/media/1.0/acifactory");
		} catch (Exception ne) {
			logger.error("Could not set SBB context:", ne);
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

	public void sbbExceptionThrown(Exception ex, Object arg1, ActivityContextInterface aci) {
	}

	public void sbbRolledBack(RolledBackContext context) {
	}

}
