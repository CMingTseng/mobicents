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
package org.mobicents.mgcp.demo;

import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.text.ParseException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.FactoryException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.UnrecognizedActivityException;

import net.java.slee.resource.mgcp.JainMgcpProvider;
import net.java.slee.resource.mgcp.MgcpActivityContextInterfaceFactory;
import net.java.slee.resource.mgcp.MgcpConnectionActivity;
import net.java.slee.resource.mgcp.event.TransactionTimeout;
import net.java.slee.resource.sip.DialogActivity;
import net.java.slee.resource.sip.SipActivityContextInterfaceFactory;
import net.java.slee.resource.sip.SleeSipProvider;

import org.apache.log4j.Logger;

/**
 * 
 * @author amit.bhayani
 */
public abstract class RQNTSbb implements Sbb {

	public final static String ENDPOINT_NAME = "media/trunk/Announcement/$";

	public final static String JBOSS_BIND_ADDRESS = System.getProperty("jboss.bind.address", "127.0.0.1");

	public final static String HELLO_WORLD = "http://" + JBOSS_BIND_ADDRESS + ":8080/mgcpdemo/audio/RQNT-ULAW.wav";

	private SbbContext sbbContext;

	// SIP
	private SleeSipProvider provider;

	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;
	private MessageFactory messageFactory;
	private SipActivityContextInterfaceFactory acif;

	// MGCP
	private JainMgcpProvider mgcpProvider;
	private MgcpActivityContextInterfaceFactory mgcpAcif;

	private static final Logger logger = Logger.getLogger(RQNTSbb.class);

	/** Creates a new instance of CallSbb */
	public RQNTSbb() {
	}

	public void onCallCreated(RequestEvent evt, ActivityContextInterface aci) {
		Request request = evt.getRequest();

		FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
		ToHeader to = (ToHeader) request.getHeader(ToHeader.NAME);

		logger.info("Incoming call " + from + " " + to);

		// create Dialog and attach SBB to the Dialog Activity
		ActivityContextInterface daci = null;
		try {
			Dialog dialog = provider.getNewDialog(evt.getServerTransaction());
			dialog.terminateOnBye(true);
			daci = acif.getActivityContextInterface((DialogActivity) dialog);
			daci.attach(sbbContext.getSbbLocalObject());
		} catch (Exception e) {
			logger.error("Error during dialog creation", e);
			respond(evt, Response.SERVER_INTERNAL_ERROR);
			return;
		}

		// respond(evt, Response.RINGING);

		CallIdentifier callID = mgcpProvider.getUniqueCallIdentifier();
		EndpointIdentifier endpointID = new EndpointIdentifier(ENDPOINT_NAME, JBOSS_BIND_ADDRESS + ":2729");

		CreateConnection createConnection = new CreateConnection(this, callID, endpointID, ConnectionMode.SendRecv);

		try {
			String sdp = new String(evt.getRequest().getRawContent());
			createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp));
		} catch (ConflictingParameterException e) {
			// should never happen
		}

		int txID = mgcpProvider.getUniqueTransactionHandler();
		createConnection.setTransactionHandle(txID);

		MgcpConnectionActivity connectionActivity = null;
		try {
			connectionActivity = mgcpProvider.getConnectionActivity(txID, endpointID);
			ActivityContextInterface epnAci = mgcpAcif.getActivityContextInterface(connectionActivity);
			epnAci.attach(sbbContext.getSbbLocalObject());
		} catch (FactoryException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		} catch (UnrecognizedActivityException ex) {
			ex.printStackTrace();
		}

		mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });
	}

	public void onCreateConnectionResponse(CreateConnectionResponse event, ActivityContextInterface aci)
			throws ParseException {
		logger.info("Receive CRCX response: " + event.getTransactionHandle());

		ServerTransaction txn = getServerTransaction();
		Request request = txn.getRequest();

		ReturnCode status = event.getReturnCode();

		this.setEndpointName(event.getSpecificEndpointIdentifier().getLocalEndpointName());
		logger.info("***&& " + this.getEndpointName());

		switch (status.getValue()) {
		case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:

			ConnectionIdentifier connectionIdentifier = event.getConnectionIdentifier();

			this.setConnectionIdentifier(connectionIdentifier.toString());
			String sdp = event.getLocalConnectionDescriptor().toString();

			ContentTypeHeader contentType = null;
			try {
				contentType = headerFactory.createContentTypeHeader("application", "sdp");
			} catch (ParseException ex) {
			}

			String localAddress = provider.getListeningPoints()[0].getIPAddress();
			int localPort = provider.getListeningPoints()[0].getPort();

			Address contactAddress = null;
			try {
				contactAddress = addressFactory.createAddress("sip:" + localAddress + ":" + localPort);
			} catch (ParseException ex) {
			}
			ContactHeader contact = headerFactory.createContactHeader(contactAddress);

			EndpointIdentifier endpointID = new EndpointIdentifier(this.getEndpointName(), JBOSS_BIND_ADDRESS + ":2729");

			NotificationRequest notificationRequest = new NotificationRequest(this, endpointID, mgcpProvider
					.getUniqueRequestIdentifier());

			EventName[] signalRequests = { new EventName(PackageName.Announcement, MgcpEvent.ann.withParm(HELLO_WORLD),
					connectionIdentifier) };
			notificationRequest.setSignalRequests(signalRequests);

			RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

			RequestedEvent[] requestedEvents = {
					new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.oc, connectionIdentifier),
							actions),
					new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.of, connectionIdentifier),
							actions) };

			notificationRequest.setRequestedEvents(requestedEvents);
			notificationRequest.setTransactionHandle(mgcpProvider.getUniqueTransactionHandler());

			NotifiedEntity notifiedEntity = new NotifiedEntity(JBOSS_BIND_ADDRESS, JBOSS_BIND_ADDRESS, 2728);
			notificationRequest.setNotifiedEntity(notifiedEntity);

			// MgcpEndpointActivity endpointActivity = null;
			// try {
			// endpointActivity = mgcpProvider.getEndpointActivity(endpointID);
			// ActivityContextInterface epnAci =
			// mgcpAcif.getActivityContextInterface(endpointActivity);
			// epnAci.attach(sbbContext.getSbbLocalObject());
			// } catch (FactoryException ex) {
			// ex.printStackTrace();
			// } catch (NullPointerException ex) {
			// ex.printStackTrace();
			// } catch (UnrecognizedActivityException ex) {
			// ex.printStackTrace();
			// }

			mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { notificationRequest });

			logger.info(" NotificationRequest sent");

			Response response = null;
			try {
				response = messageFactory.createResponse(Response.OK, request, contentType, sdp.getBytes());
			} catch (ParseException ex) {
				logger.error("ParseException while trying to create OK Response", ex);
			}

			response.setHeader(contact);
			try {
				txn.sendResponse(response);
			} catch (InvalidArgumentException ex) {
				logger.error("InvalidArgumentException while trying to send OK Response", ex);
			} catch (SipException ex) {
				logger.error("SipException while trying to send OK Response", ex);
			}
			break;
		default:
			try {
				response = messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, request);
				txn.sendResponse(response);
			} catch (Exception ex) {
				logger.error("Exception while trying to send SERVER_INTERNAL_ERROR Response", ex);
			}
		}
	}

	public void onNotificationRequestResponse(NotificationRequestResponse event, ActivityContextInterface aci) {
		logger.info("onNotificationRequestResponse");

	}

	public void onNotifyRequest(Notify event, ActivityContextInterface aci) {
		logger.info("onNotifyRequest");

		EventName[] observedEvents = event.getObservedEvents();

		for (EventName observedEvent : observedEvents) {
			if (observedEvent.getEventIdentifier().intValue() == MgcpEvent.REPORT_ON_COMPLETION) {
				NotificationRequestResponse response = new NotificationRequestResponse(event.getSource(),
						ReturnCode.Transaction_Executed_Normally);
				response.setTransactionHandle(event.getTransactionHandle());

				mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { response });

				logger.info("Announcemnet Completed NTFY received");
			}
		}

	}

	public void onTransactionTimeout(TransactionTimeout event, ActivityContextInterface aci) {
		logger.info("onTransactionTimeout");
	}

	public void onCallTerminated(RequestEvent evt, ActivityContextInterface aci) {
		EndpointIdentifier endpointID = new EndpointIdentifier(this.getEndpointName(), JBOSS_BIND_ADDRESS + ":2729");
		DeleteConnection deleteConnection = new DeleteConnection(this, endpointID);

		deleteConnection.setConnectionIdentifier(new ConnectionIdentifier(this.getConnectionIdentifier()));

		deleteConnection.setTransactionHandle(mgcpProvider.getUniqueTransactionHandler());
		mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { deleteConnection });

		ServerTransaction tx = evt.getServerTransaction();
		Request request = evt.getRequest();

		try {
			Response response = messageFactory.createResponse(Response.OK, request);
			tx.sendResponse(response);
		} catch (Exception e) {
		}
	}

	private void respond(RequestEvent evt, int cause) {
		Request request = evt.getRequest();
		ServerTransaction tx = evt.getServerTransaction();
		try {
			Response response = messageFactory.createResponse(cause, request);
			tx.sendResponse(response);
		} catch (Exception e) {
			logger.warn("Unexpected error: ", e);
		}
	}

	private ServerTransaction getServerTransaction() {
		ActivityContextInterface[] activities = sbbContext.getActivities();
		for (ActivityContextInterface activity : activities) {
			if (activity.getActivity() instanceof ServerTransaction) {
				return (ServerTransaction) activity.getActivity();
			}
		}
		return null;
	}

	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = sbbContext;
		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");

			// initialize SIP API
			provider = (SleeSipProvider) ctx.lookup("slee/resources/jainsip/1.2/provider");

			addressFactory = provider.getAddressFactory();
			headerFactory = provider.getHeaderFactory();
			messageFactory = provider.getMessageFactory();
			acif = (SipActivityContextInterfaceFactory) ctx.lookup("slee/resources/jainsip/1.2/acifactory");

			// initialize media api

			mgcpProvider = (JainMgcpProvider) ctx.lookup("slee/resources/jainmgcp/2.0/provider/demo");
			mgcpAcif = (MgcpActivityContextInterfaceFactory) ctx.lookup("slee/resources/jainmgcp/2.0/acifactory/demo");

		} catch (Exception ne) {
			logger.error("Could not set SBB context:", ne);
		}
	}

	public abstract String getConnectionIdentifier();

	public abstract void setConnectionIdentifier(String connectionIdentifier);

	public abstract String getCallIdentifier();

	public abstract void setCallIdentifier(String CallIdentifier);

	public abstract String getRemoteSdp();

	public abstract void setRemoteSdp(String remoteSdp);

	public abstract String getEndpointName();

	public abstract void setEndpointName(String endpointName);

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

	public void sbbExceptionThrown(Exception exception, Object object, ActivityContextInterface activityContextInterface) {
	}

	public void sbbRolledBack(RolledBackContext rolledBackContext) {
	}
}
