package org.mobicents.slee.service;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.SbbContext;

import org.apache.log4j.Logger;
import org.mobicents.slee.resource.lab.message.Message;
import org.mobicents.slee.resource.lab.message.MessageEvent;
import org.mobicents.slee.resource.lab.ratype.MessageActivity;

public abstract class ChildSbb extends CommonSbb {
	private Logger logger = Logger.getLogger(ChildSbb.class);

	/** Creates a new instance of BounceSbb */
	public ChildSbb() {
		super();
	}

	/**
	 * EventHandler method for incoming events of type "AnyEvent". AnyEvent is
	 * defined in the deployment descriptor "sbb-jar.xml". This method is
	 * invoked by the SLEE if an event of type ANY is received and fired by the
	 * resource adaptor.
	 */
	public void onAnyEvent(MessageEvent event, ActivityContextInterface ac) {
		logger.info("BounceSbb: " + this
				+ ": received an incoming Request. CallID = "
				+ event.getMessage().getId() + ". Command = "
				+ event.getMessage().getCommand());

		int noOfAnyMessages = getNoOfAnyMessages() + 1;
		setNoOfAnyMessages(noOfAnyMessages);

		logger.info("This is " + getNoOfAnyMessages()
				+ " number of ANY Message");
		try {
			MessageActivity activity = (MessageActivity) ac.getActivity();
			// change the activity - here only for demonstration purpose, but
			// could be valuable for other Sbbs
			activity.anyReceived();
			logger.info("ANY Event: INIT:" + activity.getInitCounter()
					+ " ANY:" + activity.getAnyCounter() + " END:"
					+ activity.getEndCounter() + " Valid state: "
					+ activity.isValid(event.getMessage().getCommandId()));
		} catch (Exception e) {
			logger.warn("Exception during onAnyEvent: ", e);
		}

		// send an answer back to the resource adaptor / stack / invokee
		// generate a message object and ...
		Message answer = getMessageResourceAdaptorSbbInterface()
				.getMessageFactory().createMessage(
						event.getMessage().getId(),
						getCustomMessageFromEnv()
								+ event.getMessage().getCommand());
		// ... send it using the resource adaptor
		getMessageResourceAdaptorSbbInterface().send(answer);
	}

	/**
	 * Synchronous call using the SbbLocalObject interface
	 * 
	 */
	public void synchronousCall() {
		logger.info("synchronousCall of ChildSbb");

	}

	// 'noOfAnyMessages' CMP field setter
	public abstract void setNoOfAnyMessages(int value);

	// 'noOfAnyMessages' CMP field getter
	public abstract int getNoOfAnyMessages();

	private String getCustomMessageFromEnv() {
		String customMessage = null;
		try {
			Context initCtx = new InitialContext();
			Context myEnv = (Context) initCtx.lookup("java:comp/env");
			customMessage = (String) myEnv.lookup("customMessage");

		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		}
		return customMessage;
	}

}
