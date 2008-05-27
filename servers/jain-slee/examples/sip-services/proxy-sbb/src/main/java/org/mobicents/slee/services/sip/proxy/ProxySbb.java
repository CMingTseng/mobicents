package org.mobicents.slee.services.sip.proxy;

import gov.nist.javax.sip.address.SipUri;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.FactoryException;
import javax.slee.InitialEventSelector;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.SbbID;
import javax.slee.SbbLocalObject;
import javax.slee.UnrecognizedActivityException;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceActivityFactory;

import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.sip.SipActivityContextInterfaceFactory;
import org.mobicents.slee.resource.sip.SipFactoryProvider;
import org.mobicents.slee.services.sip.common.ConfigurationProvider;
import org.mobicents.slee.services.sip.common.LocationInterface;
import org.mobicents.slee.services.sip.common.LocationSbbLocalObject;
import org.mobicents.slee.services.sip.common.LocationServiceException;
import org.mobicents.slee.services.sip.common.MessageHandlerInterface;
import org.mobicents.slee.services.sip.common.MessageUtils;
import org.mobicents.slee.services.sip.common.ProxyConfiguration;
import org.mobicents.slee.services.sip.common.RegistrationBinding;
import org.mobicents.slee.services.sip.common.SipLoopDetectedException;
import org.mobicents.slee.services.sip.common.SipSendErrorResponseException;
import org.mobicents.slee.services.sip.proxy.mbean.ProxyConfigurator;
import org.mobicents.slee.services.sip.proxy.mbean.ProxyConfiguratorMBean;

public abstract class ProxySbb implements Sbb {

	// We will use java loggin?
	private static Logger logger = Logger.getLogger(ProxySbb.class
			.getCanonicalName());

	// private static org.apache.log4j.Logger
	// log=org.apache.log4j.Logger.getLogger(ProxySbb.class);
	// protected static org.apache.log4j.Logger
	// dumpLogger=org.apache.log4j.Logger.getLogger("TMP_STACK_LOGGER");
	// ************************************************* SLEE STUFF
	private SbbContext context;

	// private TimerFacility timerFacility;

	// private AlarmFacility alarmFacility;

	// private ActivityContextNamingFacility namingFacility;

	private SbbID id;

	// private NullActivityFactory nullActivityFactory;

	// private NullActivityContextInterfaceFactory nullACIFactory;

	private Context myEnv;

	// **************************************************** STATICS - JNDI NAMES
	private static final String JNDI_SERVICEACTIVITY_FACTORY = "java:comp/env/slee/serviceactivity/factory";

	private static final String JNDI_SERVICEACTIVITYACI_FACTORY = "java:comp/env/slee/serviceactivity/activitycontextinterfacefactory";

	private static final String JNDI_NULL_ACTIVITY_FACTORY = "java:comp/env/slee/nullactivity/factory";

	private static final String JNDI_NULL_ACI_FACTORY = "java:comp/env/slee/nullactivity/activitycontextinterfacefactory";

	private static final String JNDI_ACTIVITY_CONTEXT_NAMING_FACILITY = "java:comp/env/slee/facilities/activitycontextnaming";

	private static final String JNDI_TRACE_FACILITY = "java:comp/env/slee/facilities/trace";

	private static final String JNDI_TIMER_FACILITY_NAME = "java:comp/env/slee/facilities/timer";

	private static final String JNDI_ALARM_FACILITY_NAME = "java:comp/env/slee/facilities/alarm";

	private static final String JNDI_PROFILE_FACILITY_NAME = "java:comp/env/slee/facilities/profile";

	private static final String JNDI_SIP_PROVIDER_NAME = "java:comp/env/slee/resources/jainsip/1.2/provider";

	private static final String JNDI_SIP_ACIF_NAME = "java:comp/env/slee/resources/jainsip/1.2/acifactory";

	// *************************************************** SIP RELATED
	private SipFactoryProvider fp;

	private SipProvider provider;

	private AddressFactory addressFactory;

	private HeaderFactory headerFactory;

	private MessageFactory messageFactory;

	private SipActivityContextInterfaceFactory acif;

	private String configurationName = null;

	/**
	 * Generate a custom convergence name so that events with the same call
	 * identifier will go to the same root SBB entity.
	 */
	public InitialEventSelector callIDSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		String callId = null;
		if (event instanceof ResponseEvent) {


			ies.setInitialEvent(false);
			return ies;

		} else if (event instanceof RequestEvent) {
			// If request event, the convergence name to callId
			Request request = ((RequestEvent) event).getRequest();


			if (!request.getMethod().equals(Request.ACK)) {
				callId = ((ViaHeader) request.getHeaders(ViaHeader.NAME).next())
						.getBranch();
			} else {
				callId = ((ViaHeader) request.getHeaders(ViaHeader.NAME).next())
						.getBranch()
						+ "_ACK";
			}

		}
		// Set the convergence name

		logger.log(Level.FINE, "Setting convergence name to: " + callId);

		ies.setCustomName(callId);
		return ies;
	}

	protected String getTraceMessageType() {
		return "ProxySbb";
	}

	// ****************************** ABSTRACT PARTS ********************
	// **** SLEE Children
	public abstract ChildRelation getRegistrarSbbChild();

	public abstract ChildRelation getLocationSbbChild();

	public abstract ProxySbbActivityContextInterface asSbbActivityContextInterface(
			ActivityContextInterface ac);

	// ***** CMPs

	public abstract void setConfiguration(Object pc);

	public abstract Object getConfiguration();

	/**
	 * This flag tells the SBB that the transaction has been terminated, and
	 * that any further messages on this transaction (eg. ACKs after the proxy
	 * returns 404 NOT_FOUND) should be ignored and not forwarded.
	 */
	public abstract boolean getServerTransactionTerminated();

	public abstract void setServerTransactionTerminated(
			boolean transactionTerminated);

	// This is required for cancels and acks, those need the same via as
	// invites?
	public abstract void setForwardedInviteViaHeader(ViaHeader via);

	public abstract ViaHeader getForwardedInviteViaHeader();

	// ****************************** Helper methods

	public ServerTransaction getServerTX(String method) {

		ActivityContextInterface myacis[] = getSbbContext().getActivities();

		for (int i = 0; i < myacis.length; i++) {

			Object activity = myacis[i].getActivity();
			if (activity instanceof ServerTransaction) {
				ServerTransaction stx = (ServerTransaction) activity;
				Request req = stx.getRequest();

				if (!req.getMethod().equals(Request.CANCEL)
						&& req.getMethod().equals(method))
					return stx;
			}

		}

		return null;
	}

	protected void prepareENV() {
		try {
			if (getConfiguration() == null)
				try {
					logger
							.finer("[SipProxy][^^^][No conf present, obtainign one]");
					Context myEnv = (Context) new InitialContext()
							.lookup("java:comp/env");

					// env-entries
					configurationName = (String) myEnv
							.lookup("configuration-MBEAN");
					ProxyConfiguration conf = (ProxyConfiguration) ConfigurationProvider
							.getCopy(ProxyConfiguratorMBean.MBEAN_NAME_PREFIX,
									configurationName);

					setConfiguration(conf);

				} catch (NamingException ne) {
					logger.warning("Could not set SBB context:"
							+ ne.getMessage());
				}
		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	protected void clearENV() {

		try {
			// if (getConfiguration() != null)
			// try {
			logger.info("Clearing environment, removing mbean");
			Context myEnv = (Context) new InitialContext()
					.lookup("java:comp/env");

			// env-entries
			configurationName = (String) myEnv.lookup("configuration-MBEAN");

			MBeanServer mbs = SleeContainer.lookupFromJndi().getMBeanServer();
			ObjectName on = new ObjectName(ProxyConfiguration.MBEAN_NAME_PREFIX
					+ configurationName);
			mbs.unregisterMBean(on);
			// } catch (NamingException ne) {
			// logger.warning("Could not set SBB context:"
			// + ne.getMessage());
			// }
		} catch (Exception e) {
			e.printStackTrace();
			// This will happen if event is ServiceStart ????
		}
	}

	/**
	 * Checks if proxys ancestor has processed call, if so it sets attribute
	 * aliased with <b>"handledByProxy"</b> to true and returns <b>false</b>.
	 * <br>
	 * If proxys ancestor hasnt processed this call it sets attribute aliased
	 * with <b>"handledByProxy"</b> to true and retuns <b>true</b> <br>
	 * <b> Variable alliased by "handledByProxy" is always set to true!!!
	 * 
	 * @param aci -
	 *            proxys aci.
	 * @return
	 *            <li><b>true</b> - if proxy should process this message.
	 *            <li><b>false</b> - otherwise.
	 */
	protected boolean proxyProcess(ProxySbbActivityContextInterface proxyACI) {

		// LETS CHECK IF OUR ANCESTOR HAS PROCESSED THIS CALL.
		proxyACI.setHandledByMe(true);
		if (proxyACI.getHandledByAncestor()) {
			// OUR ANCESTOR HAS PROCESSED THIS CALL, WE SHOULD INFORM OUR
			// DESCENDANTS IN CHAIN
			return false;
		} else {

			return true;
		}

	}

	protected void startMBeanConfigurator() {

		ProxyConfigurator proxyConfigurator = new ProxyConfigurator();

		proxyConfigurator.setSipHostName(fp.getHostAddress());
		proxyConfigurator.setSipPort(fp.getHostPort());
		proxyConfigurator.setSipTransports(fp.getTransports());

		String confValue = null;
		Context myEnv = null;
		try {
			logger.info("Building Configuration from ENV Entries");
			myEnv = (Context) new InitialContext().lookup("java:comp/env");

		} catch (NamingException ne) {

			logger.warning("Could not set SBB context:" + ne.getMessage());
			return;
		}

		// env-entries
		try {
			confValue = (String) myEnv.lookup("configuration-URI-SCHEMES");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (confValue == null) {
			proxyConfigurator.addSupportedURIScheme("sip");
			proxyConfigurator.addSupportedURIScheme("sips");
		} else {
			String[] tmp = confValue.split(";");
			for (int i = 0; i < tmp.length; i++)
				proxyConfigurator.addSupportedURIScheme(tmp[i]);
		}

		confValue = null;

		try {

			confValue = (String) myEnv.lookup("configuration-LOCAL-DOMAINS");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (confValue == null) {
			// Domains should be present in conf file, it shouldnt do much harm
			// to add those
			proxyConfigurator.addLocalDomain("nist.gov");
			proxyConfigurator.addLocalDomain("mobicents.org");
		} else {
			String[] tmp = confValue.split(";");
			for (int i = 0; i < tmp.length; i++)
				proxyConfigurator.addLocalDomain(tmp[i]);
		}

		String configurationName=null;
		try {
			configurationName = (String) myEnv
			.lookup("configuration-MBEAN");
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(configurationName!=null)
			proxyConfigurator.setName(configurationName);
		// GO ;] start service
		proxyConfigurator.startService();

	}

	// ***************************** EVENT HANLDERS

	public void onServiceStarted(
			javax.slee.serviceactivity.ServiceStartedEvent serviceEvent,
			ActivityContextInterface aci) {
		// This is called when ANY service is started.
		logger.fine("Got a Service Started Event!");
		logger.fine("CREATING CONFIGURRATION");

		startMBeanConfigurator();

	}

	public void onActivityEndEvent(ActivityEndEvent event,
			ActivityContextInterface aci) {
		try {
			Object activity = aci.getActivity();
			if (activity instanceof ServiceActivity) {
				Context myEnv = (Context) new InitialContext()
						.lookup("java:comp/env");
				// check if it's my service aci that is ending
				ServiceActivity sa = ((ServiceActivityFactory) myEnv
						.lookup("slee/serviceactivity/factory")).getActivity();
				if (sa.equals(activity)) {
					logger.finest("Service aci ending, removing mbean");
					// lets remove our mbean
					clearENV();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ****************** EVENT HANLDERS

	public void onRegisterEvent(RequestEvent event, ActivityContextInterface ac) {
		// getDefaultSbbUsageParameterSet().incrementNumberOfRegister(1);
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received REGISTER request, class="
					+ event.getClass());
		try {

			// is local domain?

			// LETS CHECK IF THIS CALL SHOULD BE PROCESSED BY PROXY
			ProxySbbActivityContextInterface sipaci = asSbbActivityContextInterface(ac);

			if (getServerTransactionTerminated()) {
				return;
			}

			if (!proxyProcess(sipaci)) {

				if (logger.isLoggable(Level.FINE))
					logger.log(Level.FINE,
							"\n===============\nLEAVEING CALL:|\n===============\n"
									+ ((CallIdHeader) event.getRequest()
											.getHeader(CallIdHeader.NAME))
											.getCallId()
									+ "\n==============================");
				// WE SHOULD NOT PROCESS THIS RESPONSE
				return;
			}
			if (logger.isLoggable(Level.FINER))
				logger.log(Level.FINER,
						"\n================+\nPROCESSING CALL:|\n================\n"
								+ ((CallIdHeader) event.getRequest().getHeader(
										CallIdHeader.NAME)).getCallId()
								+ "\n================================");

			// TODO: CHECK FOR DOMAIN

			// create registrar child SBB
			ChildRelation relation = getRegistrarSbbChild();
			SbbLocalObject child = null;
			if (relation.size() == 0)
				child = relation.create();
			else
				child = (SbbLocalObject) relation.iterator().next();

			// attach child to this activity
			ac.attach(child);

			// detach myself
			ac.detach(getSbbContext().getSbbLocalObject());

			// Event router will pass this event to child SBB

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onRegisterEvent", e);
		}
	}

	public void onInviteEvent(RequestEvent event, ActivityContextInterface ac) {
		// getDefaultSbbUsageParameterSet().incrementNumberOfInvite(1);
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received INVITE request");
		try {
			Request request = event.getRequest();

			ServerTransaction serverTransaction = (ServerTransaction) ac
					.getActivity();
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Server transacton is "
						+ serverTransaction);
			processRequest(serverTransaction, request, ac);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onInviteEvent", e);
		}
	}

	public void onByeEvent(RequestEvent event, ActivityContextInterface ac) {

		// getDefaultSbbUsageParameterSet().incrementNumberOfBye(1);
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received BYE request");
		try {

			Request request = event.getRequest();
			ServerTransaction serverTransaction = (ServerTransaction) ac
					.getActivity();

			processRequest(serverTransaction, request, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onByeEvent", e);
		}

	}

	public void onCancelEvent(RequestEvent event, ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received CANCEL request");
		try {

			Request request = event.getRequest();
			ServerTransaction serverTransaction = event.getServerTransaction();

			// CANCELs are hop-by-hop, so here we respond immediately on the
			// server txn, if the RA didn't do it
			if ((serverTransaction.getState() != TransactionState.TERMINATED)
					&& (serverTransaction.getState() != TransactionState.COMPLETED)
					&& (serverTransaction.getState() != TransactionState.CONFIRMED)) {
				serverTransaction.sendResponse(messageFactory.createResponse(
						Response.OK, request));
			}

			// This will generate a new CANCEL request that originates from the
			// proxy
			processRequest(serverTransaction, request, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onCancelEvent", e);
		}

	}

	public void onAckEvent(RequestEvent event, ActivityContextInterface ac) {
		// getDefaultSbbUsageParameterSet().incrementNumberOfAck(1);
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received ACK request");
		// logger.info("[PROXY onAckEvent] \n"+event.getRequest());

		try {

			Request request = event.getRequest();
			ServerTransaction serverTransaction = event.getServerTransaction();
			processRequest(serverTransaction, request, ac);
			// this.setTimeStarted(System.currentTimeMillis());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onAckEvent", e);
		}

	}

	public void onMessageEvent(RequestEvent event, ActivityContextInterface ac) {
		// getDefaultSbbUsageParameterSet().incrementNumberOfMessage(1);
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received MESSAGE request");
		try {
			Request request = event.getRequest();
			ServerTransaction serverTransaction = event.getServerTransaction();
			processRequest(serverTransaction, request, ac);
			// this.setTimeStarted(System.currentTimeMillis());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onMessageEvent", e);
		}
	}

	public void onOptionsEvent(RequestEvent event, ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received OPTIONS request");
		try {

			Request request = event.getRequest();
			ServerTransaction serverTransaction = event.getServerTransaction();
			ProxyConfiguration proxyConfiguration = (ProxyConfiguration) getConfiguration();
			// check if it's for me, in that case reply 501
			SipUri localNodeURI = new SipUri();
			localNodeURI.setHost(proxyConfiguration.getSipHostname());
			localNodeURI.setPort(proxyConfiguration.getSipPort());
			if (request.getRequestURI().equals(localNodeURI)) {
				if (request.getHeader(MaxForwardsHeader.NAME) == null) {
					request
							.addHeader(headerFactory
									.createMaxForwardsHeader(69));
				}
				serverTransaction.sendResponse(messageFactory.createResponse(
						Response.NOT_IMPLEMENTED, request));
			} else {
				processRequest(serverTransaction, request, ac);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onOptionsEvent", e);
		}
	}

	public void onInfoRespEvent(ResponseEvent event, ActivityContextInterface ac) {

		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 1xx (FINER) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onInfoRespEvent", e);
		}
	}

	public void onSuccessRespEvent(ResponseEvent event,
			ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 2xx (SUCCESS) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onSuccessRespEvent", e);
		}
	}

	public void onRedirRespEvent(ResponseEvent event,
			ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 3xx (REDIRECT) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception during onRedirRespEvent", e);
		}
	}

	public void onClientErrorRespEvent(ResponseEvent event,
			ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 4xx (CLIENT ERROR) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING,
					"Exception during onClientErrorRespEvent", e);
		}
	}

	public void onServerErrorRespEvent(ResponseEvent event,
			ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 5xx (SERVER ERROR) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING,
					"Exception during onServerErrorRespEvent", e);
		}
	}

	public void onGlobalFailureRespEvent(ResponseEvent event,
			ActivityContextInterface ac) {
		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received 6xx (GLOBAL FAILURE) response");
		try {

			Response response = event.getResponse();
			ClientTransaction clientTransaction = event.getClientTransaction();
			processResponse(clientTransaction, response, ac);

		} catch (Exception e) {
			logger.log(Level.WARNING,
					"Exception during onGlobalFailureRespEvent", e);
		}
	}

	/*
	 * Timeouts
	 */

	public void onTransactionTimeoutEvent(TimeoutEvent event,
			ActivityContextInterface ac) {
		ClientTransaction clientTransaction = event.getClientTransaction();
		ServerTransaction serverTransaction = event.getServerTransaction();

		if (logger.isLoggable(Level.FINER))
			logger.log(Level.FINER, "Received transaction timeout event, tid="
					+ clientTransaction);
		if (serverTransaction != null) {

			try {
				serverTransaction.sendResponse(messageFactory.createResponse(
						Response.REQUEST_TIMEOUT, serverTransaction
								.getRequest()));
				setServerTransactionTerminated(true);
			} catch (FactoryException e) {

				e.printStackTrace();
			} catch (NullPointerException e) {

				e.printStackTrace();

			} catch (SipException e) {

				e.printStackTrace();
			} catch (InvalidArgumentException e) {

				e.printStackTrace();
			} catch (ParseException e) {

				e.printStackTrace();
			}

		}
	}

	public ServerTransaction getServerTransaction(
			ClientTransaction clientTransaction) {

		return getServerTX(clientTransaction.getRequest().getMethod());

	}

	// ***** SENDER METHODS

	// ***** SENDER METHODS

	public ClientTransaction sendRequest(Request request, boolean attach)
			throws SipException {
		if (request.getHeader(MaxForwardsHeader.NAME) == null) {
			try {
				request.addHeader(headerFactory.createMaxForwardsHeader(69));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ClientTransaction ct = provider.getNewClientTransaction(request);
		if (attach) {
			try {
				ActivityContextInterface aci = acif
						.getActivityContextInterface(ct);
				aci.attach(getSbbContext().getSbbLocalObject());
			} catch (UnrecognizedActivityException e) {
				logger.log(Level.WARNING,
						"unable to attach to client transaction", e);
			}
		}

		ct.sendRequest();
		return ct;
	}

	public void sendStatelessRequest(Request request) throws SipException {
		provider.sendRequest(request);
	}

	public void sendStatelessResponse(Response response) throws SipException {
		provider.sendResponse(response);
	}

	// ** PROXY PART

	private void processRequest(ServerTransaction serverTransaction,
			Request request, ActivityContextInterface ac) {
		if (logger.isLoggable(Level.INFO))
			logger.log(Level.INFO, "processRequest: request = \n"
					+ request.toString());
		// log.error("===> REQUEST METHOD["+request.getMethod()+"]
		// CALLID["+((CallID)request.getHeader(CallID.NAME)).getCallId()+"]
		// TO["+((ToHeader)request.getHeader(ToHeader.NAME)).getAddress()+"]
		// BRANCH["+serverTransaction.getBranchId()+"]");
		try {

			if (getServerTransactionTerminated()) {
				if (logger.isLoggable(Level.INFO))
					logger.info("[PROXY MACHINE] txTERM \n" + request);
				return;
			}

			ProxySbbActivityContextInterface sipaci = asSbbActivityContextInterface(ac);

			// LETS CHECK IF THIS CALL SHOULD BE PROCESSED BY PROXY
			if (!proxyProcess(sipaci)) {
				if (logger.isLoggable(Level.FINE))
					logger.log(Level.FINE,
							"\n===============\nLEAVEING CALL:|\n===============\n"
									+ ((CallIdHeader) request
											.getHeader(CallIdHeader.NAME))
											.getCallId()
									+ "\n==============================");
				// WE SHOULD NOT PROCESS THIS REQUEST
				return;
			}
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE,
						"\n================+\nPROCESSING CALL:|\n================\n"
								+ ((CallIdHeader) request
										.getHeader(CallIdHeader.NAME))
										.getCallId()
								+ "\n================================");
			// Create a worker to process this event
			// MySipProxy proxy = new MySipProxy(this, sipaci);

			LocationSbbLocalObject locationInterface = null;
			if (getLocationSbbChild().size() == 0)
				locationInterface = (LocationSbbLocalObject) getLocationSbbChild()
						.create();
			else
				locationInterface = (LocationSbbLocalObject) getLocationSbbChild()
						.iterator().next();

			prepareENV();

			// if (getServerTX() == null)
			// setServerTX(serverTransaction);
			// Go - if it is invite here, serverTransaction can be CANCEL
			// transaction!!!! so we dont want to overwrite it above
			ProxyMachine proxyMachine = new ProxyMachine(
					(ProxyConfiguration) getConfiguration(), locationInterface,
					this.addressFactory, this.headerFactory,
					this.messageFactory, this.provider);
			proxyMachine.processRequest(serverTransaction, request);

		} catch (Exception e) {
			// Send error response so client can deal with it
			logger.log(Level.WARNING, "Exception during processRequest", e);
			try {
				serverTransaction.sendResponse(messageFactory.createResponse(
						Response.SERVER_INTERNAL_ERROR, request));
			} catch (Exception ex) {
				logger.log(Level.WARNING, "Exception during processRequest", e);
			}
		}

	}

	private void processResponse(ClientTransaction clientTransaction,
			Response response, ActivityContextInterface ac) {
		if (logger.isLoggable(Level.INFO))
			logger.log(Level.INFO, "processResponse: response = \n"
					+ response.toString());
		// log.error("===> RESPONSE CODE["+response.getStatusCode()+"]
		// METHOD["+((CSeq)response.getHeader(CSeq.NAME)).getMethod()+"]
		// CALLID["+((CallID)response.getHeader(CallID.NAME)).getCallId()+"]
		// TO["+((ToHeader)response.getHeader(ToHeader.NAME)).getAddress()+"]
		// BRANCH["+clientTransaction.getBranchId()+"]");
		try {

			if (getServerTransactionTerminated()) {
				return;
			}

			ProxySbbActivityContextInterface sipaci = asSbbActivityContextInterface(ac);

			// LETS CHECK IF THIS CALL SHOULD BE PROCESSED BY PROXY
			if (!proxyProcess(sipaci)) {
				if (logger.isLoggable(Level.FINE))
					logger.log(Level.FINE,
							"\n===============\nLEAVEING CALL:|\n===============\n"
									+ ((CallIdHeader) response
											.getHeader(CallIdHeader.NAME))
											.getCallId()
									+ "\n==============================");
				// WE SHOULD NOT PROCESS THIS RESPONSE
				return;
			}
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE,
						"\n================+\nPROCESSING CALL:|\n================\n"
								+ ((CallIdHeader) response
										.getHeader(CallIdHeader.NAME))
										.getCallId()
								+ "\n================================");
			// Create a worker to process this event
			// MySipProxy proxy = new MySipProxy(this, sipaci);
			// clientTransaction.setApplicationData(response);
			LocationSbbLocalObject locationInterface = null;
			if (getLocationSbbChild().size() == 0)
				locationInterface = (LocationSbbLocalObject) getLocationSbbChild()
						.create();
			else
				locationInterface = (LocationSbbLocalObject) getLocationSbbChild()
						.iterator().next();

			prepareENV();

			// Go
			ServerTransaction serverTransaction = getServerTransaction(clientTransaction);
			if (serverTransaction != null) {
				ProxyMachine proxyMachine = new ProxyMachine(
						(ProxyConfiguration) getConfiguration(),
						locationInterface, this.addressFactory,
						this.headerFactory, this.messageFactory, this.provider);
				proxyMachine.processResponse(serverTransaction,
						clientTransaction, response);
			} else {
				logger.warning("Weird got null tx for[" + response + "]");
			}

		} catch (Exception e) {
			// Send error response so client can deal with it
			logger.log(Level.WARNING, "Exception during processResponse", e);
		}

	}

	// ** STRICT RFC 3261 Proxy part

	// *********** SBB SLEE METHODS

	public void sbbActivate() {

	}

	public void sbbCreate() throws CreateException {

	}

	public void sbbExceptionThrown(Exception arg0, Object arg1,
			ActivityContextInterface arg2) {

	}

	public void sbbLoad() {

	}

	public void sbbPassivate() {

	}

	public void sbbPostCreate() throws CreateException {

	}

	public void sbbRemove() {

	}

	public void sbbRolledBack(RolledBackContext arg0) {

	}

	public void sbbStore() {

	}

	protected SbbContext getSbbContext() {
		// TODO Auto-generated method stub
		return this.context;
	}

	public void setSbbContext(SbbContext context) {

		this.context = context;
		try {
			id = context.getSbb();

			myEnv = new InitialContext();

			// timerFacility = (TimerFacility) myEnv
			// .lookup(JNDI_TIMER_FACILITY_NAME);
			// alarmFacility = (AlarmFacility) myEnv
			// .lookup(JNDI_ALARM_FACILITY_NAME);

			// namingFacility = (ActivityContextNamingFacility) myEnv
			// .lookup(JNDI_ACTIVITY_CONTEXT_NAMING_FACILITY);
			// nullACIFactory = (NullActivityContextInterfaceFactory) myEnv
			// .lookup(JNDI_NULL_ACI_FACTORY);
			// nullActivityFactory = (NullActivityFactory) myEnv
			// .lookup(JNDI_NULL_ACTIVITY_FACTORY);

			fp = (SipFactoryProvider) myEnv.lookup(JNDI_SIP_PROVIDER_NAME);

			provider = fp.getSipProvider();
			messageFactory = fp.getMessageFactory();
			headerFactory = fp.getHeaderFactory();
			addressFactory = fp.getAddressFactory();

			acif = (SipActivityContextInterfaceFactory) myEnv
					.lookup(JNDI_SIP_ACIF_NAME);
		} catch (NamingException ne) {
			logger.log(java.util.logging.Level.WARNING,
					"Could not set SBB context: ", ne);
		}

	}

	public void unsetSbbContext() {

		this.context = null;
	}

	// Inner class - this is pojo, but it needs access to some SLEE stuff, its
	// more conveniant to do this like this, since otherwise we would have
	// to either pass whole sbb or interface
	class ProxyMachine extends MessageUtils implements MessageHandlerInterface {
		protected final Logger log = Logger.getLogger("ProxyMachine.class");

		// We can use those variables from top level class, but let us have our
		// own.

		protected LocationInterface reg = null;

		protected AddressFactory af = null;

		protected HeaderFactory hf = null;

		protected MessageFactory mf = null;

		protected SipProvider provider = null;

		protected HashSet<URI> localMachineInterfaces = new HashSet<URI>();

		protected ProxyConfiguration pc = null;

		protected ProxyConfiguration config = null;

		public ProxyMachine(ProxyConfiguration config,
				LocationInterface registrarAccess, AddressFactory af,
				HeaderFactory hf, MessageFactory mf, SipProvider prov)
				throws ParseException {
			super(config);
			reg = registrarAccess;
			this.mf = mf;
			this.af = af;
			this.hf = hf;
			this.provider = prov;
			this.config = config;
			SipUri localMachineURI = new SipUri();
			localMachineURI.setHost(this.config.getSipHostname());
			localMachineURI.setPort(this.config.getSipPort());
			this.localMachineInterfaces.add(localMachineURI);
		}

		public void processRequest(ServerTransaction stx, Request req) {
			log.entering(this.getClass().getName(), "processRequest");
			try {
				Request tmpNewRequest = (Request) req.clone();

				// 16.3 Request Validation
				validateRequest(stx, tmpNewRequest);

				// 16.4 Route Information Preprocessing
				routePreProcess(tmpNewRequest);

				// logger.fine("Server transaction " + stx);
				// 16.5 Determining Request Targets
				List targets = determineRequestTargets(tmpNewRequest);

				Iterator it = targets.iterator();
				while (it.hasNext()) {
					Request newRequest = (Request) tmpNewRequest.clone();
					URI target = (URI) it.next();

					// Part of loop detection, here we will stop initial reqeust
					// that makes loop in local stack
					if (isLocalMachine(target)) {

						continue;
					}
					// logger.fine("SIP Proxy Forwarding: "
					// + req.getMethod() + " to URI target: " + target);
					// 16.6 Request Forwarding
					// 1. Copy request

					// 2. Request-URI
					if (target.isSipURI() && !((SipUri) target).hasLrParam())
						newRequest.setRequestURI(target);

					// *NEW* CANCEL processing
					// CANCELs are hop-by-hop, so here must remove any existing
					// Via
					// headers,
					// Record-Route headers. We insert Via header below so we
					// will
					// get response.
					if (newRequest.getMethod().equals(Request.CANCEL)) {
						newRequest.removeHeader(ViaHeader.NAME);
						newRequest.removeHeader(RecordRouteHeader.NAME);
					} else {
						// 3. Max-Forwards
						decrementMaxForwards(newRequest);
						// 4. Record-Route
						addRecordRouteHeader(newRequest);
					}

					// 5. Add Additional Header Fields
					// TBD
					// 6. Postprocess routing information
					// TBD
					// 7. Determine Next-Hop Address, Port and Transport
					// TBD

					// 8. Add a Via header field value
					addViaHeader(newRequest);

					// 9. Add a Content-Leangth header field if necessary
					// TBD

					// 10. Forward Request

					ClientTransaction ctx = forwardRequest(stx, newRequest);

					// 11. Set timer C

				}

			} catch (SipSendErrorResponseException se) {
				se.printStackTrace();
				int statusCode = se.getStatusCode();
				sendErrorResponse(stx, req, statusCode);
			} catch (SipLoopDetectedException slde) {
				log.warning("Loop detected, droping message.");
				slde.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void processResponse(ServerTransaction stx,
				ClientTransaction ctx, Response resp) {

			// Now check if we really want to send it right away

			// log.entering(this.getClass().getName(), "processResponse");

			try {

				Response newResponse = (Response) resp.clone();

				// 16.7 Response Processing
				// 1. Find appropriate response context

				// 2. Update timer C for provisional responses

				// 3. Remove topmost via
				Iterator viaHeaderIt = newResponse.getHeaders(ViaHeader.NAME);
				viaHeaderIt.next();
				viaHeaderIt.remove();
				if (!viaHeaderIt.hasNext())
					return; // response was meant for this proxy

				// 4. Add the response to the response context

				// 5. Check to see if this response should be forwarded
				// immediately
				if (newResponse.getStatusCode() == Response.TRYING) {
					return;
				}

				// 6. When necessary, choose the best final response from the

				// 7. Aggregate authorization header fields if necessary

				// 8. Optionally rewrite Record-Route header field values

				// 9. Forward the response
				forwardResponse(stx, newResponse);

				// 10. Generate any necessary CANCEL requests

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public ClientTransaction forwardRequest(
				ServerTransaction serverTransaction, Request request) {
			ClientTransaction toReturn = null;
			if (log.isLoggable(Level.FINER))
				log.finer("Forwarding request " + request.getMethod()
						+ " of server tx " + serverTransaction.getBranchId());

			// ProxySbb.log.error("===> REQUEST FWD
			// METHOD["+request.getMethod()+"]
			// CALLID["+((CallID)request.getHeader(CallID.NAME)).getCallId()+"]
			// BRANCH["+serverTransaction.getBranchId()+"]");
			// log.info("PRXY forwardReqeust\n"+request);
			try {

				if (request.getMethod().equals(Request.ACK)) {

					sendStatelessRequest(request);

				} else if (request.getMethod().equals(Request.CANCEL)) {

					sendRequest(request, false);
				} else {

					ClientTransaction clientTransaction = sendRequest(request,
							true);

					return clientTransaction;
				}

			} catch (Exception e) {
				// log.info( "Exception during forwardRequest-->"+ e);
				e.printStackTrace();
				if (!serverTransaction.getRequest().getMethod().endsWith(
						Request.CANCEL)) {
					// send back error if it's nt a cancel, because that one
					// already got a 200 ok
					sendErrorResponse(serverTransaction, serverTransaction
							.getRequest(), Response.SERVER_INTERNAL_ERROR);
				}
			}
			return toReturn;
		}

		public void sendErrorResponse(ServerTransaction txn, Request request,
				int statusCode) {
			try {

				// sipaci.setTransactionTerminated(true);
				setServerTransactionTerminated(true);
				Response response = mf.createResponse(statusCode, request);
				if (response.getHeader(MaxForwardsHeader.NAME) == null) {
					response.addHeader(hf.createMaxForwardsHeader(69));
				}
				txn.sendResponse(response);
			} catch (Exception e) {
				// trace(Level.WARNING, "Exception during sendErrorResponse",
				// e);
				e.printStackTrace();
			}
		}

		public void forwardResponse(ServerTransaction txn, Response response) {
			if (log.isLoggable(Level.FINER))
				log.finer("Forwarding response " + response.getStatusCode()
						+ " of server tx " + txn.getBranchId());

			// log.info("PRXY forwardResponse\n"+response);
			// try{
			// ProxySbb.log.error("===> RESPONSE FWD
			// CODE["+response.getStatusCode()+"]
			// METHOD["+((CSeq)response.getHeader(CSeq.NAME)).getMethod()+"]
			// CALLID["+((CallID)response.getHeader(CallID.NAME)).getCallId()+"]
			// BRANCH["+txn==null?"GO TNULL":txn.getBranchId()+"]");
			// }catch(Exception e)
			// {
			// e.printStackTrace();
			// }
			try {
				// trace(Level.FINEST, "Forwarding response:\n" + response);
				if (txn != null) {
					txn.sendResponse(response);
				} else {
					// forward statelessly anyway
					sendStatelessResponse(response);
				}
			} catch (Exception e) {
				log.severe("Exception during forwardResponse[\n" + response
						+ "\n] TXBRANCH[" + txn.getBranchId() + "] TXR[\n"
						+ txn.getRequest() + "\n]:" + e);
			}
		}

		/**
		 * Performs request validation as per RFC 3261 section 16.3. If a
		 * request fails validation, throw exception to cause appropriate error
		 * response to client.
		 * 
		 * @param txn
		 *            the server transaction of the request.
		 * @param request
		 *            the SIP request to be validated.
		 * @throws SipLoopDetectedException -
		 *             error that is beeing thrown when local stack goes into
		 *             loop
		 * @throws SipSendErrorResponseException -
		 *             thrown uri of passed message is not supported
		 */
		public void validateRequest(ServerTransaction txn, Request request)
				throws SipSendErrorResponseException, SipLoopDetectedException {
			// 1. Reasonable syntax

			// 2. URI scheme
			URI requestURI = null;
			requestURI = request.getRequestURI();

			boolean supportedURIScheme = false;
			supportedURIScheme = isSupportedURIScheme(requestURI);

			if (!supportedURIScheme) {
				throw new SipSendErrorResponseException(
						"Unsupported URI scheme",
						Response.UNSUPPORTED_URI_SCHEME);
			}

			// 3. Max-Forwards
			checkMaxForwards(txn, request);

			// 4. Loop Detection - TBD
			detectLoop(request);
			// 5. Proxy-Require - TBD
			// 6. Proxy-Authorization - TBD
		}

		public void detectLoop(Request request) throws SipLoopDetectedException {

			URI requestURI = null;
			requestURI = request.getRequestURI();
			// , now its simple, so we wont spin requests
			// to local node
			SipUri localNodeURI = new SipUri();
			try {
				localNodeURI.setHost(this.config.getSipHostname());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			localNodeURI.setPort(this.config.getSipPort());

			// This only works if UAC conforms to rfc 3261
			if (requestURI.equals(localNodeURI)) {
				// throw new SipSendErrorResponseException("Possible local
				// looping on node",Response.LOOP_DETECTED);
				// throw new SipLoopDetectedException(
				// "Possible loop detected on LOCAL["+localNodeURI+"]
				// MSG["+requestURI+"] message:n" + request
				// + "\n====================================");
				// this can be a loop, we will only warn as this is uncertain at
				// this point
				// if You know more, please patch :]

				if (log.isLoggable(Level.FINE)) {
					log.fine("Possible loop detected on LOCAL[" + localNodeURI
							+ "] MSG[" + requestURI + "] message:n" + request
							+ "\n====================================");
				}

			}

			// do more:

			ListIterator lit = request.getHeaders(ViaHeader.NAME);

			if (lit != null && lit.hasNext()) {
				int found = 0;

				do {
					ViaHeader vh = (ViaHeader) lit.next();
					if (vh.getHost().equals(localNodeURI.getHost())
							&& vh.getPort() == localNodeURI.getPort()) {
						found++;
					}

					if (found >= 2) {
						throw new SipLoopDetectedException(
								"Possible loop detected[mutliple via headers] on message:n"
										+ request
										+ "\n====================================");
					}
				} while (lit.hasNext());

			}

		}

		/**
		 * Validate the max-forwards header throw a user error exception (too
		 * many hops) if max-forwards reaches 0.
		 * 
		 * @param txn
		 * @param request
		 * @throws SipSendErrorResponseException
		 */
		public void checkMaxForwards(ServerTransaction txn, Request request)
				throws SipSendErrorResponseException {
			MaxForwardsHeader mfh = (MaxForwardsHeader) request
					.getHeader(MaxForwardsHeader.NAME);
			if (mfh == null)
				return;

			int maxForwards = 0;
			maxForwards = ((MaxForwardsHeader) request
					.getHeader(MaxForwardsHeader.NAME)).getMaxForwards();

			if (maxForwards > 0) {
				return;
			} else {
				// MAY respond to OPTIONS, otherwise return 483 Too Many Hops
				throw new SipSendErrorResponseException("Too many hops",
						Response.TOO_MANY_HOPS);
			}

		}

		/**
		 * Attempts to find a locally registered contact address for the given
		 * URI, using the location service interface.
		 */
		public LinkedList findLocalTarget(URI uri)
				throws SipSendErrorResponseException {
			String addressOfRecord = uri.toString();

			Map<String, RegistrationBinding> bindings = null;
			LinkedList listOfTargets = new LinkedList();
			try {
				bindings = reg.getUserBindings(addressOfRecord);
			} catch (LocationServiceException e) {

				e.printStackTrace();
				return listOfTargets;
			}

			if (bindings == null) {
				throw new SipSendErrorResponseException("User not found",
						Response.NOT_FOUND);
			}
			if (bindings.isEmpty()) {
				throw new SipSendErrorResponseException(
						"User temporarily unavailable",
						Response.TEMPORARILY_UNAVAILABLE);
			}

			Iterator it = bindings.values().iterator();
			URI target = null;
			while (it.hasNext()) {
				RegistrationBinding binding = (RegistrationBinding) it.next();
				// logger.fine("BINDINGS: " + binding);
				ContactHeader header = binding.getContactHeader(af, hf);
				// logger.fine("CONTACT HEADER: " + header);
				if (header == null) { // entry expired
					continue; // see if there are any more contacts...
				}
				Address na = header.getAddress();
				// logger.fine("Address: " + na);
				// target = na.getURI();
				// break;
				listOfTargets.add(na.getURI());

			}
			if (listOfTargets.size() == 0) {
				// logger.fine("findLocalTarget: No contacts for "
				// + addressOfRecord + " found.");
				throw new SipSendErrorResponseException(
						"User temporarily unavailable",
						Response.TEMPORARILY_UNAVAILABLE);
			}
			return listOfTargets;
		}

		/**
		 * Adds a default Via header to the request. Override to provide a
		 * different Via header.
		 */
		public void addViaHeader(Request request)
				throws SipSendErrorResponseException {
			ViaHeader via = null;
			try {
				// ViaHeader via = hf.createViaHeader(config.getSipHostname(),
				// config.getSipPort(), config.getSipTransport(), null);

				// if (request.getMethod().equals(Request.CANCEL)
				// || request.getMethod().equals(Request.ACK)) {
				// For now we cant do rfc 3261 ch 17.1.1.3
				if (request.getMethod().equals(Request.CANCEL)) {
					via = getForwardedInviteViaHeader();

					if (via == null) {
						throw new SipSendErrorResponseException(
								"Couldnt add via [" + via + "] to msg[\n"
										+ request + "\n]", Response.BAD_REQUEST);
					}

				} else {
					via = hf.createViaHeader(config.getSipHostname(), config
							.getSipPort(), config.getSipTransports()[0],
							"z9hG4bK" + System.currentTimeMillis() + "_"
									+ Math.random() + "_"
									+ System.currentTimeMillis());
					if (request.getMethod().equals(Request.INVITE)) {
						setForwardedInviteViaHeader(via);
					}
				}

				// THIS: config.getSipTransports()[0] // has to be changed!!!
				log.finer("[&&&] addViaHeader\n" + via + "");
				// via.setParameter("ID",
				// ""+System.currentTimeMillis()+"_"+Math.random()+"_"+config.getSipHostname()+":"+config.getSipPort());
				request.addHeader(via);

			} catch (Exception e) {
				throw new SipSendErrorResponseException("Couldnt add via ["
						+ via + "] to msg[\n" + request + "\n]",
						Response.SERVER_INTERNAL_ERROR);
			}
		}

		/**
		 * Adds a default Record-Route header to the request. Override to
		 * provide a different Record-Route header.
		 */
		public void addRecordRouteHeader(Request request) {
			try {
				// Add our record-route header to list...
				SipURI myURI = af.createSipURI(null, config.getSipHostname());
				myURI.setPort(config.getSipPort());
				myURI.setMAddrParam(config.getSipHostname());
				myURI.setTransportParam(config.getSipTransports()[0]);
				myURI.setParameter("cluster", "mobi-cents");
				myURI.setParameter("lr", "");
				Address myName = af.createAddress(myURI);

				RecordRouteHeader myHeader = hf.createRecordRouteHeader(myName);
				request.addFirst(myHeader);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * Decrement the value of max-forwards. If no max-forwards header
		 * present, create a max-forwards header with the RFC3261 recommended
		 * default value
		 * 
		 * @param request
		 * @throws SipSendErrorResponseException
		 */
		public void decrementMaxForwards(Request request)
				throws SipSendErrorResponseException {
			MaxForwardsHeader max = (MaxForwardsHeader) request
					.getHeader(MaxForwardsHeader.NAME);
			try {
				if (max == null) {
					// add max-forwards with default 70 hops
					max = hf.createMaxForwardsHeader(70);
					request.setHeader(max);
				} else {
					// decrement max-forwards
					int maxForwards = max.getMaxForwards();
					maxForwards--;
					max.setMaxForwards(maxForwards);
					request.setHeader(max);
				}
			} catch (Exception e) {
				throw new SipSendErrorResponseException(
						"Error updating max-forwards",
						Response.SERVER_INTERNAL_ERROR);
			}
		}

		/**
		 * Check for strict-routing style route headers and swap with
		 * Request-URI if applicable.
		 */
		public void routePreProcess(Request request)
				throws SipSendErrorResponseException {
			URI requestURI = null;
			requestURI = request.getRequestURI();

			// TODO: add check on multiple names!!!!!
			if ((requestURI.isSipURI())
					&& (((SipURI) requestURI).getUser() == null)
					&& (((SipURI) requestURI).getHost().equalsIgnoreCase(config
							.getSipHostname()))) {
				// client is a strict router, replace request-URI with last
				// value in Route header field...
				try {
					ListIterator it = request.getHeaders(RouteHeader.NAME);
					LinkedList l = new LinkedList();
					// need last value in list
					while (it.hasNext()) {
						RouteHeader r = (RouteHeader) it.next();
						l.add(r);
					}
					if (l.size() == 0)
						return;

					RouteHeader route = (RouteHeader) l.getLast();

					l.removeLast(); // Remove the last route header from the
					// list,
					// possibly leaving an empty list

					request.removeHeader(RouteHeader.NAME); // Remove all route
					// headers from the
					// message

					// Re-add the remaining headers to the message
					for (int i = 0; i < l.size(); i++) {
						RouteHeader routeHeader = (RouteHeader) l.get(i);
						request.addHeader(routeHeader);
					}

					URI newURI = route.getAddress().getURI();
					request.setRequestURI(newURI);

				} catch (Exception e) {
					e.printStackTrace();
					throw new SipSendErrorResponseException(
							"Error updating route headers",
							Response.SERVER_INTERNAL_ERROR);

				}
			}
			// From RFC3261 16.4:
			// If the first value in the Route header field indicates this
			// proxy,
			// the proxy MUST remove that value from the request.
			Iterator routeHeaders = request.getHeaders(RouteHeader.NAME);
			if (routeHeaders.hasNext()) {
				RouteHeader r = (RouteHeader) routeHeaders.next();
				// is this route header for our hostname & port?
				URI uri = r.getAddress().getURI();
				if (uri.isSipURI()) {
					SipURI sipURI = (SipURI) uri;
					int uriPort = sipURI.getPort();
					if (uriPort <= 0)
						uriPort = 5060; // WARNING: Assumes stack impl returns
					// <= 0
					// if port not specified in URI
					String cluster = sipURI.getParameter("cluster");

					boolean isMobicents = (cluster != null && cluster
							.equals("mobi-cents"));

					// JAIN SIP does not specify but NIST SIP returns -1
					if (((sipURI.getHost().equalsIgnoreCase(config
							.getSipHostname())) && (uriPort == config
							.getSipPort()))
							|| isMobicents) {
						// logger.fine("Cluster = " + cluster);
						// remove this route header
						routeHeaders.remove();
						// if this was the last one, remove the header entirely
						// (why
						// isn't this automatic?)
						if (!routeHeaders.hasNext())
							request.removeHeader(RouteHeader.NAME);
					}
				}
			}

		}

		/**
		 * Determines target SIP URI(s) for request, using location service or
		 * other criteria.
		 * 
		 * TODO: Forking (return more than one target)
		 * 
		 * @param request
		 *            the SIP request being forwarded
		 * @return a list of URIs
		 */
		public List determineRequestTargets(Request request)
				throws SipSendErrorResponseException {
			LinkedList targets = null;

			URI requestURI = null;
			URI target = null;
			boolean localDomain = false;
			requestURI = request.getRequestURI();
			localDomain = isLocalDomain(requestURI);

			if (request.getMethod().equals(Request.ACK)
					|| request.getMethod().equals(Request.BYE)) {
				RouteHeader rh = (RouteHeader) request
						.getHeader(RouteHeader.NAME);
				if (rh != null) {
					target = rh.getAddress().getURI();
				} else
					target = request.getRequestURI();
				targets = new LinkedList();
				targets.add(target);
			} else if (localDomain) {
				// determine local SIP target(s) using location service etc
				targets = findLocalTarget(requestURI);
				// This is done in findLocalTarget
				// if (target == null) { // not found (or not currently
				// registered)
				// throw new SipSendErrorResponseException("User not
				// registered",
				// Response.TEMPORARILY_UNAVAILABLE);
				// }
			} else {
				// destination addr is outside our domain
				target = requestURI;
				targets = new LinkedList();
				targets.add(target);
			}

			return targets;
		}

		public boolean isLocalMachine(URI hostURI) {

			return this.localMachineInterfaces.contains(hostURI);

		}

	}

}
