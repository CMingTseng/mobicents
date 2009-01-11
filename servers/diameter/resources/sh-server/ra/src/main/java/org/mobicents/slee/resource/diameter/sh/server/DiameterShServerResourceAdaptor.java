/**
 * Start time:17:53:55 2009-01-06<br>
 * Project: mobicents-diameter-parent<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.slee.resource.diameter.sh.server;

import static org.jdiameter.client.impl.helpers.Parameters.MessageTimeOut;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.slee.Address;
import javax.slee.UnrecognizedActivityException;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.management.UnrecognizedResourceAdaptorEntityException;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.BootstrapContext;
import javax.slee.resource.FailureReason;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.resource.ResourceException;
import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.CreateActivityException;
import net.java.slee.resource.diameter.base.DiameterActivity;
import net.java.slee.resource.diameter.base.events.DiameterMessage;
import net.java.slee.resource.diameter.base.events.ErrorAnswer;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentityAvp;
import net.java.slee.resource.diameter.sh.client.DiameterShAvpFactory;
import net.java.slee.resource.diameter.sh.client.events.PushNotificationRequest;
import net.java.slee.resource.diameter.sh.client.events.SubscribeNotificationsAnswer;
import net.java.slee.resource.diameter.sh.client.events.UserDataAnswer;
import net.java.slee.resource.diameter.sh.server.ShServerActivityContextInterfaceFactory;
import net.java.slee.resource.diameter.sh.server.ShServerMessageFactory;
import net.java.slee.resource.diameter.sh.server.ShServerProvider;
import net.java.slee.resource.diameter.sh.server.events.ProfileUpdateRequest;
import net.java.slee.resource.diameter.sh.server.events.PushNotificationAnswer;
import net.java.slee.resource.diameter.sh.server.handlers.ShServerSessionListener;

import org.apache.log4j.Logger;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerTable;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.sh.ClientShSession;
import org.jdiameter.api.sh.ServerShSession;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.server.impl.app.sh.ShServerSessionImpl;
import org.mobicents.diameter.stack.DiameterListener;
import org.mobicents.diameter.stack.DiameterStackMultiplexerMBean;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.ResourceAdaptorActivityContextInterfaceFactory;
import org.mobicents.slee.resource.ResourceAdaptorEntity;
import org.mobicents.slee.resource.ResourceAdaptorState;
import org.mobicents.slee.resource.diameter.base.DiameterActivityHandle;
import org.mobicents.slee.resource.diameter.base.DiameterActivityImpl;
import org.mobicents.slee.resource.diameter.base.DiameterAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.base.DiameterMessageFactoryImpl;
import org.mobicents.slee.resource.diameter.base.events.ErrorAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.avp.DiameterIdentityAvpImpl;
import org.mobicents.slee.resource.diameter.sh.client.DiameterShAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.ProfileUpdateAnswerImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.PushNotificationRequestImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.SubscribeNotificationsAnswerImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.UserDataAnswerImpl;
import org.mobicents.slee.resource.diameter.sh.server.events.ProfileUpdateRequestImpl;
import org.mobicents.slee.resource.diameter.sh.server.events.PushNotificationAnswerImpl;
import org.mobicents.slee.resource.diameter.sh.server.events.SubscribeNotificationsRequestImpl;
import org.mobicents.slee.resource.diameter.sh.server.events.UserDataRequestImpl;
import org.mobicents.slee.resource.diameter.sh.server.handlers.ShServerSessionFactory;


/**
 * Start time:17:53:55 2009-01-06<br>
 * Project: mobicents-diameter-parent<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class DiameterShServerResourceAdaptor  implements ResourceAdaptor, DiameterListener , ShServerSessionListener{
	private static final long serialVersionUID = 1L;
	  
  	private static transient Logger logger = Logger.getLogger(DiameterShServerResourceAdaptor.class);
	private Stack stack;
	private SessionFactory sessionFactory = null;
	private long messageTimeout = 5000;
	private ObjectName diameterMultiplexerObjectName = null;
	private DiameterStackMultiplexerMBean diameterMux=null;
	private ResourceAdaptorState state;
	
	
	/**
	 * The BootstrapContext provides the resource adaptor with the required
	 * capabilities in the SLEE to execute its work. The bootstrap context is
	 * implemented by the SLEE. The BootstrapContext object holds references to
	 * a number of objects that are of interest to many resource adaptors. For
	 * further information see JSLEE v1.1 Specification Page 305. The
	 * bootstrapContext will be set in entityCreated() method.
	 */
	private transient BootstrapContext bootstrapContext = null;

	/**
	 * The SLEE endpoint defines the contract between the SLEE and the resource
	 * adaptor that enables the resource adaptor to deliver events
	 * asynchronously to SLEE endpoints residing in the SLEE. This contract
	 * serves as a generic contract that allows a wide range of resources to be
	 * plugged into a SLEE environment via the resource adaptor architecture.
	 * For further information see JSLEE v1.1 Specification Page 307 The
	 * sleeEndpoint will be initialized in entityCreated() method.
	 */
	private transient SleeEndpoint sleeEndpoint = null;

	/**
	 * the EventLookupFacility is used to look up the event id of incoming
	 * events
	 */
	private transient EventLookupFacility eventLookup = null;
	public static final Map<Integer, String> events;
	
	static {
		Map<Integer, String> eventsTemp = new HashMap<Integer, String>();

		eventsTemp.put(ProfileUpdateRequest.commandCode, "ProfileUpdate");
		eventsTemp.put(PushNotificationAnswer.commandCode, "PushNotification");
		eventsTemp.put(UserDataAnswer.commandCode, "UserData");
		eventsTemp.put(SubscribeNotificationsAnswer.commandCode, "SubscribeNotifications");

		events = Collections.unmodifiableMap(eventsTemp);

	}
	
	private transient ShServerProviderImpl serverProvider = null;
	
	/**
	 * The list of activites stored in this resource adaptor. If this resource
	 * adaptor were a distributed and highly available solution, this storage
	 * were one of the candidates for distribution.
	 */
	private transient ConcurrentHashMap<ActivityHandle, DiameterActivity> activities = null;

	/**
	 * The activity context interface factory defined in
	 * DiameterRAActivityContextInterfaceFactoryImpl
	 */
	
	
	private transient ShServerActivityContextInterfaceFactory acif = null;
	private DiameterAvpFactoryImpl diameterAvpFactory = new DiameterAvpFactoryImpl();
	private DiameterShAvpFactory localFactory=null;
	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 301 for further information. <br>
	 * The SLEE calls this method to inform the resource adaptor that the SLEE
	 * has completed activity end processing for the activity represented by the
	 * activity handle. The resource adaptor should release any resource related
	 * to this activity as the SLEE will not ask for it again.
	 */
	public void activityEnded(ActivityHandle handle) {
		logger.info("Diameter ShServer RA :: activityEnded :: handle[" + handle + ".");

		if (this.activities != null) {
			synchronized (this.activities) {
				this.activities.remove(handle);
			}
		}
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 301 for further information. <br>
	 * The SLEE calls this method to inform the resource adaptor that the
	 * activity�s Activity Context object is no longer attached to any SBB
	 * entities and is no longer referenced by any SLEE Facilities. This enables
	 * the resource adaptor to implicitly end the Activity object.
	 */
	public void activityUnreferenced(ActivityHandle handle) {
		logger.info("Diameter ShServer RA :: activityUnreferenced :: handle[" + handle + "].");

		this.activityEnded(handle);
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor The JSLEE v1.1
	 * Specification does not include entityActivated(). However, the API
	 * description of JSLEE v1.1 does include this method already. So, the
	 * documentation follows the code. <br>
	 * This method is called in context of project Mobicents in context of
	 * resource adaptor activation. More precisely,
	 * org.mobicents.slee.resource.ResourceAdaptorEntity.activate() calls this
	 * method entityActivated(). This method signals the resource adaptor the
	 * transition from state "INACTIVE" to state "ACTIVE".
	 */
	public void entityActivated() throws ResourceException {
		logger.info("Diameter ShServer RA :: entityActivated.");

		try {
			logger.info("Activating Diameter ShServer RA Entity");

			// this.raProvider = new DiameterProviderImpl(this);

			initializeNamingContext();

			this.activities = new ConcurrentHashMap();

			this.state = ResourceAdaptorState.CONFIGURED;
		} catch (Exception e) {
			logger.error("Error Configuring Diameter ShServer RA Entity", e);
		}

		try {
			// Initialize the protocol stack
			this.diameterMultiplexerObjectName=new ObjectName("diameter.mobicents:service=DiameterStackMultiplexer");
			
			Object[] params = new Object[]{};

		    String[] signature = new String[]{};
		    
		    String operation = "getMultiplexerMBean";
		    
		    Object object = SleeContainer.lookupFromJndi().getMBeanServer().invoke( this.diameterMultiplexerObjectName, operation, params, signature );
		    
		    if(object instanceof DiameterStackMultiplexerMBean)
		      this.diameterMux = (DiameterStackMultiplexerMBean) object;
		
			initStack();

			// Resource Adaptor ready to rumble!
			this.state = ResourceAdaptorState.ACTIVE;
			this.sessionFactory = this.stack.getSessionFactory();

			((ISessionFactory) sessionFactory).registerAppFacory(ClientShSession.class, new ShServerSessionFactory(this.sessionFactory,this,this.messageTimeout));
		} catch (Exception e) {
			logger.error("Error Activating Diameter ShServer RA Entity", e);
		}
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 298 for further information. <br>
	 * This method is called by the SLEE when a resource adaptor object instance
	 * is bootstrapped, either when a resource adaptor entity is created or
	 * during SLEE startup. The SLEE implementation will construct the resource
	 * adaptor object and then invoke the entityCreated method before any other
	 * operations can be invoked on the resource adaptor object.
	 */
	public void entityCreated(BootstrapContext bootstrapContext) throws ResourceException {
		logger.info("Diameter ShServer RA :: entityCreated :: bootstrapContext[" + bootstrapContext + "].");

		this.bootstrapContext = bootstrapContext;
		this.sleeEndpoint = bootstrapContext.getSleeEndpoint();
		this.eventLookup = bootstrapContext.getEventLookupFacility();

		this.state = ResourceAdaptorState.UNCONFIGURED;
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor The JSLEE v1.1
	 * Specification does not include entityDeactivated(). However, the API
	 * description of JSLEE v1.1 does include this method already. So, the
	 * documentation follows the code. <br>
	 * This method is called in context of project Mobicents in context of
	 * resource adaptor deactivation. More precisely,
	 * org.mobicents.slee.resource.ResourceAdaptorEntity.deactivate() calls this
	 * method entityDeactivated(). The method call is done AFTER the call to
	 * entityDeactivating(). This method signals the resource adaptor the
	 * transition from state "STOPPING" to state "INACTIVE".
	 */
	public void entityDeactivated() {
		logger.info("Diameter ShServer RA :: entityDeactivated.");

		logger.info("Diameter ShServer RA :: Cleaning RA Activities.");

		synchronized (this.activities) {
			activities.clear();
		}
		activities = null;

		logger.info("Diameter ShServer RA :: Cleaning naming context.");

		try {
			cleanNamingContext();
		} catch (NamingException e) {
			logger.error("Diameter ShServer RA :: Cannot unbind naming context.");
		}

		logger.info("Diameter ShServer RA :: RA Stopped.");
	}

	/**
	 * This method is called in context of project Mobicents in context of
	 * resource adaptor deactivation. More precisely,
	 * org.mobicents.slee.resource.ResourceAdaptorEntity.deactivate() calls this
	 * method entityDeactivating() PRIOR to invoking entityDeactivated(). This
	 * method signals the resource adaptor the transition from state "ACTIVE" to
	 * state "STOPPING".
	 */
	public void entityDeactivating() {
		logger.info("Diameter ShServer RA :: entityDeactivating.");

		this.state = ResourceAdaptorState.STOPPING;

		try{
			diameterMux.unregisterListener(this);
		}catch (Exception e) 
		{
			logger.error("", e);
		}

		synchronized (this.activities) {
			for (ActivityHandle activityHandle : activities.keySet()) {
				try {
					logger.info("Ending activity [" + activityHandle + "]");

					activities.get(activityHandle).endActivity();

				} catch (Exception e) {
					logger.error("Error Deactivating Activity", e);
				}
			}

		}

		logger.info("Diameter ShServer RA :: entityDeactivating completed.");
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 299 for further information. <br>
	 * This method is called by the SLEE when a resource adaptor object instance
	 * is being removed, either when a resource adaptor entity is deleted or
	 * during SLEE shutdown. When receiving this invocation the resource adaptor
	 * object is expected to close any system resources it has allocated.
	 */
	public void entityRemoved() {
		// Stop the stack
		// this.stack.destroy();

		// Clean up!
		this.acif = null;
		this.activities = null;
		this.bootstrapContext = null;
		this.eventLookup = null;
		//this.raProvider = null;
		this.sleeEndpoint = null;
		this.stack = null;

		logger.info("Diameter ShServer RA :: entityRemoved.");
	}

	private void initializeNamingContext() throws NamingException {
		// get the reference to the SLEE container from JNDI
		SleeContainer container = SleeContainer.lookupFromJndi();

		// get the entities name
		String entityName = bootstrapContext.getEntityName();

		ResourceAdaptorEntity resourceAdaptorEntity;

		try {
			resourceAdaptorEntity = ((ResourceAdaptorEntity) container.getResourceAdaptorEntity(entityName));
		} catch (UnrecognizedResourceAdaptorEntityException uraee) {
			throw new NamingException("Failure setting up Naming Context. RA Entity not found.");
		}

		ResourceAdaptorTypeID raTypeId = resourceAdaptorEntity.getInstalledResourceAdaptor().getRaType().getResourceAdaptorTypeID();

		// create the ActivityContextInterfaceFactory
		acif = new ShServerActivityContextInterfaceFactoryImpl(resourceAdaptorEntity.getServiceContainer(), entityName);

		// set the ActivityContextInterfaceFactory
		resourceAdaptorEntity.getServiceContainer().getActivityContextInterfaceFactories().put(raTypeId, acif);

		try {
			if (this.acif != null) {
				// parse the string = java:slee/resources/RAFrameRA/raframeacif
				String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) acif).getJndiName();

				int begind = jndiName.indexOf(':');
				int toind = jndiName.lastIndexOf('/');

				String prefix = jndiName.substring(begind + 1, toind);
				String name = jndiName.substring(toind + 1);

				logger.info("Diameter ShServer RA :: Registering in JNDI :: Prefix[" + prefix + "], Name[" + name + "].");

				SleeContainer.registerWithJndi(prefix, name, this.acif);

				logger.info("Diameter ShServer RA :: Registered in JNDI successfully.");
			}
		} catch (IndexOutOfBoundsException iobe) {
			logger.info("Failure initializing name context.", iobe);
		}
	}

	/**
	 * Clean the JNDI naming context
	 */
	private void cleanNamingContext() throws NamingException {
		try {
			if (this.acif != null) {
				// parse the string = java:slee/resources/RAFrameRA/raframeacif
				String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) this.acif).getJndiName();

				// remove "java:" prefix
				int begind = jndiName.indexOf(':');
				String javaJNDIName = jndiName.substring(begind + 1);

				logger.info("Diameter ShServer RA :: Unregistering from JNDI :: Name[" + javaJNDIName + "].");

				SleeContainer.unregisterWithJndi(javaJNDIName);

				logger.info("Diameter ShServer RA :: Unregistered from JNDI successfully.");
			}
		} catch (IndexOutOfBoundsException iobe) {
			logger.error("Failure cleaning name context.", iobe);
		}
	}

	/**
	 * Initializes the RA Diameter Stack.
	 * 
	 * @throws Exception
	 */
	private synchronized void initStack() throws Exception {

		//proxy.startService(this.bootstrapContext.getEntityName());
		Set<Integer> codes = events.keySet();
		long[] command = new long[codes.size()];
		Iterator<Integer> it = codes.iterator();
		for (int i = 0; i < codes.size(); i++) {
			Integer ii = it.next();
			command[i] = ii.longValue();
		}

		this.diameterMux.registerListener(this, new ApplicationId[]{ApplicationId.createByAuthAppId(10415L, 16777217L)});
		this.stack=this.diameterMux.getStack();
		this.messageTimeout = stack.getMetaData().getConfiguration().getLongValue(MessageTimeOut.ordinal(), (Long) MessageTimeOut.defValue());
		this.serverProvider=new ShServerProviderImpl(this);
		this.localFactory=new DiameterShAvpFactoryImpl(this.diameterAvpFactory,this.stack);
		logger.info("Diameter ShServer RA :: Successfully initialized stack.");
	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 300 for further information. <br>
	 * The SLEE calls this method to inform the resource adaptor object that the
	 * specified event was processed unsuccessfully by the SLEE. Event
	 * processing can fail if, for example, the SLEE doesn�t have enough
	 * resource to process the event, a SLEE node fails during event processing
	 * or a system level failure prevents the SLEE from committing transactions.
	 */
	public void eventProcessingFailed(ActivityHandle handle, Object event, int eventID, Address address, int flags, FailureReason reason) {
		logger.info("Diameter ShServer RA :: eventProcessingFailed :: handle[" + handle + "], event[" + event + "], eventID[" + eventID + "], address[" + address + "], flags["
				+ flags + "], reason[" + reason + "].");

	}

	/**
	 * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	 * Specification Page 300 for further information. <br>
	 * The SLEE calls this method to inform the resource adaptor object that the
	 * specified event was processed successfully by the SLEE. An event is
	 * considered to be processed successfully if the SLEE has attempted to
	 * deliver the event to all interested SBBs.
	 */
	public void eventProcessingSuccessful(ActivityHandle handle, Object event, int eventID, Address address, int flags) {
		logger.info("Diameter ShServer RA :: eventProcessingSuccessful :: handle[" + handle + "], event[" + event + "], eventID[" + eventID + "], address[" + address + "], flags["
				+ flags + "].");

	}

	public Object getActivity(ActivityHandle arg0) {

		return this.activities.get(arg0);
	}

	public ActivityHandle getActivityHandle(Object activity) {
		logger.info("Diameter ShServer RA :: getActivityHandle :: activity[" + activity + "].");

		if (!(activity instanceof DiameterActivity))
			return null;

		DiameterActivity inActivity = (DiameterActivity) activity;

		for (Entry<ActivityHandle, DiameterActivity> activityInfo : this.activities.entrySet()) {
			Object curActivity = activityInfo.getValue();

			if (curActivity.equals(inActivity))
				return activityInfo.getKey();
		}

		return null;
	}

	public Marshaler getMarshaler() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getSBBResourceAdaptorInterface(String arg0) {
		return this.serverProvider;
	}
	
	
	
	public Answer processRequest(Request request) {
		DiameterActivity activity;

		try {
			activity = serverProvider.createActivity(request);

			//baranowb: do nothing here, if its valid it should be processed, f not we will get exception
		} catch (CreateActivityException e) {
			logger.error("", e);
		}

		// returning null so we can answer later
		return null;
	}

	public void receivedSuccessMessage(Request req, Answer ans) {
		logger.info("Diameter ShServer RA :: receivedSuccessMessage :: " + "Request[" + req + "], Answer[" + ans + "].");

		try {
			logger.info("Received Message Result-Code: " + ans.getResultCode().getUnsigned32());
		} catch (AvpDataException ignore) {
			// ignore, this was just for informational purposes...
		}
		// FIXME: alexandre: what should we do here? end activity?
	}

	public void timeoutExpired(Request req) {
		logger.info("Diameter Base RA :: timeoutExpired :: " + "Request[" + req + "].");

		// Message delivery timed out - we have to remove activity
		DiameterActivityHandle ah = new DiameterActivityHandle(req.getSessionId());

		try {
			activities.get(ah).endActivity();
		} catch (Exception e) {
			logger.error("Failure processing timeout message.", e);
		}
	}

	/**
	 * Method for obtaining the Peers the RA is currently conneceted to.
	 * 
	 * @return an array of DiameterIdentity AVPs representing the peers.
	 */
	public DiameterIdentityAvp[] getConnectedPeers()
	{
		if (this.stack != null)
		{
			try
			{
				// Get the list of peers from the stack
				List<Peer> peers = stack.unwrap(PeerTable.class).getPeerTable();
				
				DiameterIdentityAvp[] result = new DiameterIdentityAvp[peers.size()];

				int i = 0;

				// Get each peer from the list and make a DiameterIdentityAvp
				for (Peer peer : peers)
				{
					DiameterIdentityAvp identity = new DiameterIdentityAvpImpl(0, 0, 0, 0, peer.getUri().toString().getBytes());

					result[i++] = identity;
				}

				return result;
			}
			catch (Exception e)
			{
				logger.error("Failure getting peer list.", e);
			}
		}

		return null;
	}
	
	/**
	 * Method for firing event to SLEE
	 * 
	 * @param handle
	 *            the handle for the activity where event will be fired on
	 * @param name
	 *            the unqualified Event name
	 * @param request
	 *            the request that will be wrapped in the event, if any
	 * @param answer
	 *            the answer that will be wrapped in the event, if any
	 */
	public void fireEvent(ActivityHandle handle, String name, Request request, Answer answer) {
		try {
			int eventID = eventLookup.getEventID("net.java.slee.resource.diameter.sh." + name, "java.net", "0.8");

			DiameterMessage event = (DiameterMessage) createEvent(request, answer);
			sleeEndpoint.fireEvent(handle, event, eventID, null);
		} catch (Exception e) {
			logger.warn("Can not send event", e);
		}
	}

	/**
	 * Create Event object from request/answer
	 * 
	 * @param request
	 *            the request to create the event from, if any.
	 * @param answer
	 *            the answer to create the event from, if any.
	 * @return a DiameterMessage object wrapping the request/answer
	 * @throws OperationNotSupportedException
	 */
	public DiameterMessage createEvent(Request request, Answer answer) throws OperationNotSupportedException {
		if (request == null && answer == null)
			return null;

		int commandCode = (request != null ? request.getCommandCode() : answer.getCommandCode());

		switch (commandCode) {
		case PushNotificationRequestImpl.commandCode: // PNR/PNA
			return request != null ? new PushNotificationRequestImpl(request) : new PushNotificationAnswerImpl(answer);
		case ProfileUpdateRequestImpl.commandCode: // PUR/PUA
			return request != null ? new ProfileUpdateRequestImpl(request) : new ProfileUpdateAnswerImpl(answer);
		case SubscribeNotificationsRequestImpl.commandCode: // SNR/SNA
			return request != null ? new SubscribeNotificationsRequestImpl(request) : new SubscribeNotificationsAnswerImpl(answer);
		case net.java.slee.resource.diameter.sh.server.events.UserDataRequest.commandCode: // DWR
																							// /
																							// DWA
			return request != null ? new UserDataRequestImpl(request) : new UserDataAnswerImpl(answer);

		case ErrorAnswer.commandCode:
			if (answer != null) {
				return new ErrorAnswerImpl(answer);
			} else {
				throw new IllegalArgumentException("ErrorAnswer code set on request: " + request);
			}

			// FIXME: baranowb : should extension fall in here?
			// FIXME: baranowb: what about Error
		default:
			throw new OperationNotSupportedException("Not supported message code:" + commandCode + "\n" + (request != null ? request : answer));
		}
	}

	/**
	 * Method for performing tasks when activity is created, such as informing
	 * SLEE about it and storing into internal map.
	 * 
	 * @param ac
	 *            the activity that has been created
	 */
	private void activityCreated(DiameterActivity ac)
	{
		try
		{
			// Inform SLEE that Activity Started
			DiameterActivityImpl activity = (DiameterActivityImpl) ac;
			sleeEndpoint.activityStarted(activity.getActivityHandle());

			// Put it into our activites map
			activities.put(activity.getActivityHandle(), activity);

			logger.info("Activity started [" + activity.getActivityHandle() + "]");
		}
		catch (Exception e)
		{
			logger.error("Error creating activity", e);
			
			throw new RuntimeException("Error creating activity", e);
		}
	}
	
	
	// ################################
	// # SERVICE FILTERING #
	// ################################
	
	public void queryLiveness(ActivityHandle arg0) {
		// TODO Auto-generated method stub
		
	}

	public void serviceActivated(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void serviceDeactivated(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void serviceInstalled(String arg0, int[] arg1, String[] arg2) {
		// TODO Auto-generated method stub
		
	}

	public void serviceUninstalled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public void sessionDestroyed(String sessionId,ClientShSession session) {
		try {
			this.sleeEndpoint.activityEnding(getActivityHandle(sessionId));
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecognizedActivityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	protected DiameterActivityHandle getActivityHandle(String sessionId)
	{
		return new DiameterActivityHandle(sessionId);
	}
	
	class ShServerProviderImpl implements ShServerProvider{

		private DiameterShServerResourceAdaptor ra=null;
		public ShServerProviderImpl(
				DiameterShServerResourceAdaptor diameterShServerResourceAdaptor) {
			this.ra=diameterShServerResourceAdaptor;
		}

		//Here we only act on reqeusts, answer dont concern us ?
		public DiameterActivity createActivity(Request request) throws CreateActivityException {
			long commandCode=request.getCommandCode();
			String sessionId = request.getSessionId();
			if(!events.keySet().contains(commandCode))
			{
				throw new CreateActivityException("Cant create activity for nknown command code: "+commandCode);
			}
			
			try{
				//In this case we have to pass so session factory know what to tell to session listener
				ShServerSessionImpl session = ((ISessionFactory) sessionFactory)
				.getNewAppSession(request.getSessionId(), ApplicationId
					.createByAuthAppId(10415, 16777217),
					ServerShSession.class, new Object[]{request});
				// session.
				session.processRequest(request);
			}catch(Exception e)
			{
				throw new CreateActivityException(e);
			}
			 return   (DiameterActivity) getActivity(getActivityHandle(sessionId));
		}

		public ShServerMessageFactory getServerMessageFactory() {
			DiameterMessageFactoryImpl baseMsgFactory = new DiameterMessageFactoryImpl(null,stack);
		    ShServerMessageFactoryImpl ccaMsgFactory = new ShServerMessageFactoryImpl(baseMsgFactory, null, stack, localFactory);
		    
		    return ccaMsgFactory;
		}

		public DiameterShAvpFactory getAvpFactory() {
			return localFactory;
		}
		public PushNotificationAnswer pushNotificationRequest(
				PushNotificationRequest message) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		
		
		public DiameterIdentityAvp[] getConnectedPeers()
	    {
	      return ra.getConnectedPeers();
	    }

	    public int getPeerCount()
	    {
	      return ra.getConnectedPeers().length;
	    }
		
	}

	public void fireEvent(String sessionId, String name, Request request,
			Answer answer) {
		// TODO Auto-generated method stub
		
	}

	public void sessionCreated(ServerShSession session, boolean isSubscription) {
		// Make sure it's a new session and there's no activity created yet.
	    if(this.getActivity(getActivityHandle(session.getSessions().get(0).getSessionId())) != null)
	    {
	      logger.warn( "Activity found for created Credit-Control Server Session. Shouldn't exist. Aborting." );
	      return;
	    }

	    // Get Message Factories (for Base and CCA)
	    DiameterMessageFactoryImpl baseMsgFactory = new DiameterMessageFactoryImpl(session.getSessions().get(0),this.stack);
	    ShServerMessageFactoryImpl ccaMsgFactory = new ShServerMessageFactoryImpl(baseMsgFactory, session.getSessions().get(0), this.stack, this.localFactory);

	    // Create Server Activity
	    DiameterActivity activity=null;
	    if(isSubscription)
	    {
	    	ShServerSubscriptionActivityImpl _activity=new ShServerSubscriptionActivityImpl(ccaMsgFactory,this.localFactory,session,this.messageTimeout,null,null,this.sleeEndpoint);
	    	_activity.setSessionListener(this);
	    	 activity = _activity;
	    }else
	    {
	    	ShServerActivityImpl _activity=new ShServerActivityImpl(ccaMsgFactory,this.localFactory,session,this.messageTimeout,null,null,this.sleeEndpoint);
	    	_activity.setSessionListener(this);
	    	 activity = _activity;
	    }

	    //FIXME: baranowb: add basic session mgmt for base? or do we relly on responses?
	    //session.addStateChangeNotification(activity);
	    
	    activityCreated(activity);
		
	}

	public void sessionDestroyed(String sessionId, ServerShSession session) {
		  try
		    {
		      this.sleeEndpoint.activityEnding(getActivityHandle(sessionId));
		      
		    }
		    catch (Exception e) {
		      logger.error( "Failure Ending Activity with Session-Id[" + sessionId + "]", e );
		    }
		
	}
	
	
	

  public boolean sessionExists(String sessionId)
  {
    return this.activities.containsKey(new DiameterActivityHandle(sessionId));
  }

  public void sessionDestroyed(String sessionId, Object appSession)
  {
    try
    {
      this.sleeEndpoint.activityEnding(getActivityHandle(sessionId));
    }
    catch (Exception e) {
      logger.error( "Failure Ending Activity with Session-Id[" + sessionId + "]", e );
    }
  }

	
}
