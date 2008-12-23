package org.mobicents.slee.resource.diameter.cca;

import static org.jdiameter.client.impl.helpers.Parameters.MessageTimeOut;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.slee.Address;
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
import net.java.slee.resource.diameter.base.events.AbortSessionAnswer;
import net.java.slee.resource.diameter.base.events.AccountingAnswer;
import net.java.slee.resource.diameter.base.events.DiameterMessage;
import net.java.slee.resource.diameter.base.events.ErrorAnswer;
import net.java.slee.resource.diameter.base.events.ExtensionDiameterMessage;
import net.java.slee.resource.diameter.base.events.ReAuthAnswer;
import net.java.slee.resource.diameter.base.events.SessionTerminationAnswer;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentityAvp;
import net.java.slee.resource.diameter.cca.CreditControlAVPFactory;
import net.java.slee.resource.diameter.cca.CreditControlActivityContextInterfaceFactory;
import net.java.slee.resource.diameter.cca.CreditControlClientSession;
import net.java.slee.resource.diameter.cca.CreditControlMessageFactory;
import net.java.slee.resource.diameter.cca.CreditControlProvider;
import net.java.slee.resource.diameter.cca.events.CreditControlAnswer;
import net.java.slee.resource.diameter.cca.events.CreditControlMessage;
import net.java.slee.resource.diameter.cca.events.CreditControlRequest;
import net.java.slee.resource.diameter.cca.handlers.CCASessionCreationListener;

import org.apache.log4j.Logger;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerTable;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.cca.ClientCCASession;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.mobicents.diameter.stack.DiameterListener;
import org.mobicents.diameter.stack.DiameterStackMultiplexerMBean;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.ResourceAdaptorActivityContextInterfaceFactory;
import org.mobicents.slee.resource.ResourceAdaptorEntity;
import org.mobicents.slee.resource.ResourceAdaptorState;
import org.mobicents.slee.resource.diameter.base.DiameterActivityHandle;
import org.mobicents.slee.resource.diameter.base.DiameterActivityImpl;
import org.mobicents.slee.resource.diameter.base.DiameterAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.base.DiameterBaseResourceAdaptor;
import org.mobicents.slee.resource.diameter.base.DiameterMessageFactoryImpl;
import org.mobicents.slee.resource.diameter.base.events.AbortSessionAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.AbortSessionRequestImpl;
import org.mobicents.slee.resource.diameter.base.events.AccountingAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.AccountingRequestImpl;
import org.mobicents.slee.resource.diameter.base.events.ErrorAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.ReAuthAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.ReAuthRequestImpl;
import org.mobicents.slee.resource.diameter.base.events.SessionTerminationAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.SessionTerminationRequestImpl;
import org.mobicents.slee.resource.diameter.base.events.avp.DiameterIdentityAvpImpl;
import org.mobicents.slee.resource.diameter.cca.events.CreditControlAnswerImpl;
import org.mobicents.slee.resource.diameter.cca.events.CreditControlRequestImpl;
import org.mobicents.slee.resource.diameter.cca.handlers.CreditControlSessionFactory;


public class CCAResourceAdaptor implements ResourceAdaptor, DiameterListener, CCASessionCreationListener{

		private static final long serialVersionUID = 1L;

	  private static transient Logger logger = Logger.getLogger(DiameterBaseResourceAdaptor.class);

	  
	  private ResourceAdaptorState state;

	  private Stack stack;
	  private SessionFactory sessionFactory = null;
	  
	  //private DiameterStackMultiplexerProxyMBeanImpl proxy=new DiameterStackMultiplexerProxyMBeanImpl();
	  private ObjectName diameterMultiplexerObjectName = null;
	  private DiameterStackMultiplexerMBean diameterMux=null;
	
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

	  /**
	   * The list of activites stored in this resource adaptor. If this resource
	   * adaptor were a distributed and highly available solution, this storage
	   * were one of the candidates for distribution.
	   */
	  private transient ConcurrentHashMap<ActivityHandle, DiameterActivity> activities = null;
	  
	  //protected transient SessionFactory proxySessionFactory=null;
	  protected transient CreditControlAVPFactory localFactory=null;
	  protected DiameterAvpFactoryImpl diameterAvpFactory = new DiameterAvpFactoryImpl();
	  protected transient CreditControlProviderImpl raProvider=null;
	  public static final Map<Integer, String> events;
	 
	  
	  static
	  {
	    Map<Integer, String> eventsTemp = new HashMap<Integer, String>();

	    eventsTemp.put(CreditControlAnswer.commandCode, "CreditControl");
	    eventsTemp.put(ReAuthAnswer.commandCode, "ReAuth");
	    eventsTemp.put(AbortSessionAnswer.commandCode, "AbortSession");
	    eventsTemp.put(AccountingAnswer.commandCode, "Accounting");
	    eventsTemp.put(ErrorAnswer.commandCode, "Error");
	    eventsTemp.put(SessionTerminationAnswer.commandCode, "SessionTermination");
	    // FIXME: baranowb - make sure its compilant with xml
	    eventsTemp.put(ExtensionDiameterMessage.commandCode, "ExtensionDiameterMessage");
	    
	    events = Collections.unmodifiableMap(eventsTemp);
	    
	   
	  }
	  
	  //Factories
	  protected CreditControlSessionFactory ccaSessionFactory=null;
	  
	  //ACIF
	  protected CreditControlActivityContextInterfaceFactory acif=null;
	  

	 
	  
	  //PROVISIONING
	  private long messageTimeout = 5000;
	  protected int defaultDirectDebitingFailureHandling = 0;
	  protected int defaultCreditControlFailureHandling = 0;		
		//its seconds
		protected long defaultValidityTime=30;
		protected long defaultTxTimerValue=10;
	  
	  public CCAResourceAdaptor()
	  {
	    logger.info("Diameter CCA RA :: DiameterBaseResourceAdaptor.");
	    
	   
	  }
	  
	  
	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 301 for further information. <br>
	   * The SLEE calls this method to inform the resource adaptor that the SLEE
	   * has completed activity end processing for the activity represented by the
	   * activity handle. The resource adaptor should release any resource related
	   * to this activity as the SLEE will not ask for it again.
	   */
	  public void activityEnded(ActivityHandle handle)
	  {
	    logger.info("Diameter CCA RA :: activityEnded :: handle[" + handle + ".");
	    
	    if(this.activities != null)
	    {
	      synchronized (this.activities)
	      {
	        this.activities.remove(handle);
	      }
	    }
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 301 for further information. <br>
	   * The SLEE calls this method to inform the resource adaptor that the
	   * activitys Activity Context object is no longer attached to any SBB
	   * entities and is no longer referenced by any SLEE Facilities. This enables
	   * the resource adaptor to implicitly end the Activity object.
	   */
	  public void activityUnreferenced(ActivityHandle handle)
	  {
	    logger.info("Diameter CCA RA :: activityUnreferenced :: handle[" + handle + "].");

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
	  public void entityActivated() throws ResourceException
	  {
	    logger.info("Diameter CCA RA :: entityActivated.");

	    try
	    {
	      logger.info("Activating Diameter CCA RA Entity");


	        this.diameterMultiplexerObjectName=new ObjectName("diameter.mobicents:service=DiameterStackMultiplexer");
	        
	        Object[] params = new Object[]{};

	          String[] signature = new String[]{};
	          
	          String operation = "getMultiplexerMBean";
	          
	          Object object = SleeContainer.lookupFromJndi().getMBeanServer().invoke( this.diameterMultiplexerObjectName, operation, params, signature );
	          
	          if(object instanceof DiameterStackMultiplexerMBean)
	            this.diameterMux = (DiameterStackMultiplexerMBean) object;
	      
	      
	      
	      this.raProvider = new CreditControlProviderImpl(this);

	      initializeNamingContext();

	      this.activities = new ConcurrentHashMap();

	      this.state = ResourceAdaptorState.CONFIGURED;
	    
	      // Initialize the protocol stack
	      initStack();

	      
	      
	      
	      // Resource Adaptor ready to rumble!
	      this.state = ResourceAdaptorState.ACTIVE;
	      this.sessionFactory = this.stack.getSessionFactory();
	      this.ccaSessionFactory=new CreditControlSessionFactory( sessionFactory,
	  			 this,  messageTimeout,
				 defaultDirectDebitingFailureHandling,
				 defaultCreditControlFailureHandling,  defaultValidityTime,
				 defaultTxTimerValue);
	      
	      //this.proxySessionFactory=this.sessionFactory;
	      
	    
	      
	      // Register CCA App Session Factories
	      ((ISessionFactory) sessionFactory).registerAppFacory(ServerCCASession.class, this.ccaSessionFactory);
	      ((ISessionFactory) sessionFactory).registerAppFacory(ClientCCASession.class, this.ccaSessionFactory);

	    
	    }
	    catch (Exception e)
	    {
	      logger.error("Error Activating Diameter CCA RA Entity", e);
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
	  public void entityCreated(BootstrapContext bootstrapContext) throws ResourceException
	  {
	    logger.info("Diameter CCA RA :: entityCreated :: bootstrapContext[" + bootstrapContext + "].");

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
	  public void entityDeactivated()
	  {
	    logger.info("Diameter CCA RA :: entityDeactivated.");

	    logger.info("Diameter CCA RA :: Cleaning RA Activities.");

	    synchronized (this.activities)
	    {
	      activities.clear();
	    }
	    activities = null;

	    logger.info("Diameter CCA RA :: Cleaning naming context.");

	    try
	    {
	      cleanNamingContext();
	    }
	    catch (NamingException e)
	    {
	      logger.error("Diameter CCA RA :: Cannot unbind naming context.");
	    }

	    // Stop the stack
	    //try
	    //{
	    //  stack.stop(5, TimeUnit.SECONDS);
	    //}
	    //catch (Exception e)
	    //{
	    //  logger.error("Diameter CCA RA :: Failure while stopping ");
	    //}

	    //proxy.stopService(this.bootstrapContext.getEntityName());
	    
	    logger.info("Diameter CCA RA :: RA Stopped.");
	  }

	  /**
	   * This method is called in context of project Mobicents in context of
	   * resource adaptor deactivation. More precisely,
	   * org.mobicents.slee.resource.ResourceAdaptorEntity.deactivate() calls this
	   * method entityDeactivating() PRIOR to invoking entityDeactivated(). This
	   * method signals the resource adaptor the transition from state "ACTIVE" to
	   * state "STOPPING".
	   */
	  public void entityDeactivating()
	  {
	    logger.info("Diameter CCA RA :: entityDeactivating.");
	    
	    this.state = ResourceAdaptorState.STOPPING;
	    
	    //try 
	    //{
	    //  Network network = stack.unwrap(Network.class);

	    //  Iterator<ApplicationId> appIdsIt = stack.getMetaData().getLocalPeer().getCommonApplications().iterator();
	    //  
	    //  while(appIdsIt.hasNext())
	    //  {
	    //    network.removeNetworkReqListener(appIdsIt.next());
	        
	        // Update the iterator (avoid ConcurrentModificationException)
	    //    appIdsIt = stack.getMetaData().getLocalPeer().getCommonApplications().iterator();
	    //  }
	    //}
	    //catch (InternalException e) 
	    //{
	    //  logger.error("", e);
	    //}

	    try{
	      diameterMux.unregisterListener(this);
	    }catch (Exception e) 
	    {
	      logger.error("", e);
	    }
	    
	    synchronized (this.activities)
	    {
	      for (ActivityHandle activityHandle : activities.keySet())
	      {
	        try
	        {
	          logger.info("Ending activity [" + activityHandle + "]");

	          activities.get(activityHandle).endActivity();

	        }
	        catch (Exception e)
	        {
	          logger.error("Error Deactivating Activity", e);
	        }
	      }

	    }
	    
	    logger.info("Diameter CCA RA :: entityDeactivating completed.");
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 299 for further information. <br>
	   * This method is called by the SLEE when a resource adaptor object instance
	   * is being removed, either when a resource adaptor entity is deleted or
	   * during SLEE shutdown. When receiving this invocation the resource adaptor
	   * object is expected to close any system resources it has allocated.
	   */
	  public void entityRemoved()
	  {
	    // Stop the stack
	    //this.stack.destroy();

	    // Clean up!
		this.acif=null;
	    this.activities = null;
	    this.bootstrapContext = null;
	    this.eventLookup = null;
	    this.raProvider = null;
	    this.sleeEndpoint = null;
	    this.stack = null;

	    logger.info("Diameter CCA RA :: entityRemoved.");
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 300 for further information. <br>
	   * The SLEE calls this method to inform the resource adaptor object that the
	   * specified event was processed unsuccessfully by the SLEE. Event
	   * processing can fail if, for example, the SLEE doesnt have enough
	   * resource to process the event, a SLEE node fails during event processing
	   * or a system level failure prevents the SLEE from committing transactions.
	   */
	  public void eventProcessingFailed(ActivityHandle handle, Object event, int eventID, Address address, int flags, FailureReason reason)
	  {
	    logger.info("Diameter CCA RA :: eventProcessingFailed :: handle[" + handle + "], event[" + event + "], eventID[" + eventID + "], address[" + address + "], flags[" + flags + 
	        "], reason[" + reason + "].");
	    

	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 300 for further information. <br>
	   * The SLEE calls this method to inform the resource adaptor object that the
	   * specified event was processed successfully by the SLEE. An event is
	   * considered to be processed successfully if the SLEE has attempted to
	   * deliver the event to all interested SBBs.
	   */
	  public void eventProcessingSuccessful(ActivityHandle handle, Object event, int eventID, Address address, int flags)
	  {
	    logger.info("Diameter CCA RA :: eventProcessingSuccessful :: handle[" + handle + "], event[" + event + "], eventID[" + eventID + "], address[" + address + "], flags[" + 
	        flags + "].");

	    DiameterActivity activity = activities.get(handle);
	    
	    if(activity instanceof CreditControlClientSessionImpl)
	    {
	      CreditControlClientSessionImpl ccaClientActivity = (CreditControlClientSessionImpl) activity;
	      
	      if(ccaClientActivity.getTerminateAfterAnswer())
	        ccaClientActivity.endActivity();
	    }

	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 301 for further information. <br>
	   * The SLEE calls this method to get access to the underlying activity for
	   * an activity handle. The resource adaptor is expected to pass back a
	   * non-null object.
	   */
	  public Object getActivity(ActivityHandle handle)
	  {
	    logger.info("Diameter CCA RA :: getActivity :: handle[" + handle + "].");

	    return this.activities.get(handle);
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 301 for further information. <br>
	   * The SLEE calls this method to get an activity handle for an activity
	   * created by the underlying resource. This method is invoked by the SLEE
	   * when it needs to construct an activity context for an activity via an
	   * activity context interface factory method invoked by an SBB.
	   */
	  public ActivityHandle getActivityHandle(Object activity)
	  {
	    logger.info("Diameter CCA RA :: getActivityHandle :: activity[" + activity + "].");

	    if (!(activity instanceof DiameterActivity))
	      return null;

	    DiameterActivity inActivity = (DiameterActivity) activity;
	    
	    for (Map.Entry<ActivityHandle, DiameterActivity> activityInfo : this.activities.entrySet())
	    {
	      Object curActivity = activityInfo.getValue();
	      
	      if (curActivity.equals(inActivity))
	        return activityInfo.getKey();
	    }

	    return null;
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 302 for further information. <br>
	   * The SLEE calls this method to get reference to the Marshaler object. The
	   * resource adaptor implements the Marshaler interface. The Marshaler is
	   * used by the SLEE to convert between object and distributable forms of
	   * events and event handles.
	   */
	  public Marshaler getMarshaler()
	  {
	    logger.info("Diameter CCA RA :: getMarshaler");

	    return null;
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 302 for further information. <br>
	   * The SLEE calls this method to get access to the underlying resource
	   * adaptor interface that enables the SBB to invoke the resource adaptor, to
	   * send messages for example.
	   */
	  public Object getSBBResourceAdaptorInterface(String className)
	  {
	    logger.info("Diameter CCA RA :: getSBBResourceAdaptorInterface :: className[" + className + "].");

	    return this.raProvider;
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 301 for further information. <br>
	   * The SLEE calls this method to query if a specific activity belonging to
	   * this resource adaptor object is alive.
	   */
	  public void queryLiveness(ActivityHandle handle)
	  {
	    logger.info("Diameter CCA RA :: queryLiveness :: handle[" + handle + "].");

	    DiameterActivityImpl activity = (DiameterActivityImpl) activities.get(handle);

	    if (activity != null && !activity.isValid())
	    {
	      try
	      {
	        sleeEndpoint.activityEnding(handle);
	      }
	      catch (Exception e)
	      {
	        logger.error("", e);
	      }
	    }
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 303 for further information. <br>
	   * The SLEE calls this method to inform the resource adaptor that a service
	   * has been activated and is interested in the event types associated to the
	   * service key. The service must be installed with the resource adaptor via
	   * the serviceInstalled method before it can be activated.
	   */
	  public void serviceActivated(String serviceKey)
	  {
	    logger.info("Diameter CCA RA :: serviceActivated :: serviceKey[" + serviceKey + "].");
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 304 for further information. <br>
	   * The SLEE calls this method to inform the SLEE that a service has been
	   * deactivated and is no longer interested in the event types associated to
	   * the service key.
	   */
	  public void serviceDeactivated(String serviceKey)
	  {
	    logger.info("Diameter CCA RA :: serviceDeactivated :: serviceKey[" + serviceKey + "].");
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 302 for further information. <br>
	   * The SLEE calls this method to signify to the resource adaptor that a
	   * service has been installed and is interested in a specific set of events.
	   * The SLEE passes an event filter which identifies a set of event types
	   * that services in the SLEE are interested in. The SLEE calls this method
	   * once a service is installed.
	   */
	  public void serviceInstalled(String serviceKey, int[] eventIDs, String[] resourceOptions)
	  {
	    logger.info("Diameter CCA RA :: serviceInstalled :: serviceKey[" + serviceKey + "], eventIDs[" + eventIDs + "], resourceOptions[" + resourceOptions + "].");
	  }

	  /**
	   * implements javax.slee.resource.ResourceAdaptor Please refer to JSLEE v1.1
	   * Specification Page 303 for further information. <br>
	   * The SLEE calls this method to signify that a service has been
	   * un-installed in the SLEE. The event types associated to the service key
	   * are no longer of interest to a particular application.
	   */
	  public void serviceUninstalled(String serviceKey)
	  {
	    logger.info("Diameter CCA RA :: serviceUninstalled :: serviceKey[" + serviceKey + "].");
	  }

	  /**
	   * Set up the JNDI naming context
	   */
	  private void initializeNamingContext() throws NamingException
	  {
	    // get the reference to the SLEE container from JNDI
	    SleeContainer container = SleeContainer.lookupFromJndi();

	    // get the entities name
	    String entityName = bootstrapContext.getEntityName();

	    ResourceAdaptorEntity resourceAdaptorEntity;

	    try
	    {
	      resourceAdaptorEntity = ((ResourceAdaptorEntity) container.getResourceAdaptorEntity(entityName));
	    }
	    catch (UnrecognizedResourceAdaptorEntityException uraee)
	    {
	      throw new NamingException("Failure setting up Naming Context. RA Entity not found.");
	    }

	    ResourceAdaptorTypeID raTypeId = resourceAdaptorEntity.getInstalledResourceAdaptor().getRaType().getResourceAdaptorTypeID();

	    // create the ActivityContextInterfaceFactory
	    acif = new CreditControlActivityContextInterfaceFactoryImpl(resourceAdaptorEntity.getServiceContainer(), entityName);

	    // set the ActivityContextInterfaceFactory
	    resourceAdaptorEntity.getServiceContainer().getActivityContextInterfaceFactories().put(raTypeId, acif);

	    try
	    {
	      if (this.acif != null)
	      {
	        // parse the string = java:slee/resources/RAFrameRA/raframeacif
	        String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) acif).getJndiName();
	        
	        int begind = jndiName.indexOf(':');
	        int toind = jndiName.lastIndexOf('/');
	        
	        String prefix = jndiName.substring(begind + 1, toind);
	        String name = jndiName.substring(toind + 1);

	        logger.info("Diameter CCA RA :: Registering in JNDI :: Prefix[" + prefix + "], Name[" + name + "].");

	        SleeContainer.registerWithJndi(prefix, name, this.acif);
	        
	        logger.info("Diameter CCA RA :: Registered in JNDI successfully.");
	      }
	    }
	    catch (IndexOutOfBoundsException iobe)
	    {
	      logger.info("Failure initializing name context.", iobe);
	    }
	  }

	  /**
	   * Clean the JNDI naming context
	   */
	  private void cleanNamingContext() throws NamingException
	  {
	    try
	    {
	      if (this.acif != null)
	      {
	        // parse the string = java:slee/resources/RAFrameRA/raframeacif
	        String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) this.acif).getJndiName();

	        // remove "java:" prefix
	        int begind = jndiName.indexOf(':');
	        String javaJNDIName = jndiName.substring(begind + 1);

	        logger.info("Diameter CCA RA :: Unregistering from JNDI :: Name[" + javaJNDIName + "].");

	        SleeContainer.unregisterWithJndi(javaJNDIName);
	        
	        logger.info("Diameter CCA RA :: Unregistered from JNDI successfully.");
	      }
	    }
	    catch (IndexOutOfBoundsException iobe)
	    {
	      logger.error("Failure cleaning name context.", iobe);
	    }
	  }

	  /**
	   * Initializes the RA Diameter Stack.
	   * 
	   * @throws Exception
	   */
	  private synchronized void initStack() throws Exception
	  {
	    //FIXME: Fetch stack
	    // Set message timeout accordingly to stack definition
	    
	    // FIXME: This should come from config.. adding manually
	    // <ApplicationID>
	    // <VendorId value="193"/>
	    // <AuthApplId value="0"/>
	    // <AcctApplId value="19302"/>
	    // </ApplicationID>
	    //appIds.add(ApplicationId.createByAccAppId(193L, 19302L));

	    // <ApplicationID>
	    // <VendorId value="193"/>
	    // <AuthApplId value="19301"/>
	    // <AcctApplId value="0"/>
	    // </ApplicationID>
	    //appIds.add(ApplicationId.createByAuthAppId(193L, 19301L));
	    //DiameterStackMultiplexerProxyMBeanImpl proxy=new DiameterStackMultiplexerProxyMBeanImpl();
	    //proxy.startService(this.bootstrapContext.getEntityName());
	    Set<Integer> codes=events.keySet();
	    long[] command=new long[codes.size()];
	    Iterator<Integer> it=codes.iterator();
	    for(int i=0;i<codes.size();i++)
	    {
	      Integer ii=it.next();
	      command[i]=ii.longValue();
	    }
	    this.diameterMux.registerListener( this, new ApplicationId[]{ApplicationId.createByAuthAppId(0, 4)});
	    this.stack=this.diameterMux.getStack();
	    this.messageTimeout = stack.getMetaData().getConfiguration().getLongValue(MessageTimeOut.ordinal(), (Long) MessageTimeOut.defValue());
	    
	    this.localFactory=new CreditControlAVPFactoryImpl(this.diameterAvpFactory,this.stack);
	    logger.info("Diameter CCA RA :: Successfully initialized stack.");
	  }


	  /**
	   * Create the Diameter Activity Handle for an given session id
	   * 
	   * @param sessionId the session identifier to create the activity handle from
	   * @return a DiameterActivityHandle for the provided sessionId
	   */
	  protected DiameterActivityHandle getActivityHandle(String sessionId)
	  {
	    return new DiameterActivityHandle(sessionId);
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

	  
	 

	  
	//  ///////////////////////////
	//  // JDiam handelr methods //
	//  ///////////////////////////

	public Answer processRequest(Request request) {
		logger.info("-------- CCA GOT REQUEST: "+request.getCommandCode());
		//Here we receive initial request for which session does not exist!!!
		//Valid messages are:
		// * CCR - if we act as server, this is the message we receive
		// * NO other message should make it here, if it gets its an errro ?
		//FIXME: baranowb: check if ACR is vald here also
		if(request.getCommandCode()==CreditControlRequest.commandCode)
		{
			DiameterActivity activity;

			try {
				activity = raProvider.createActivity(request);
				if(activity==null)
				{
					logger.error("Diameter CCA RA :: failed to create session, Request code: "+request.getCommandCode()+", sessionId: "+request.getSessionId());
				}else
				{
					//we can only have server session?, but for sake errorcatching
					if(activity instanceof ServerCCASession)
					{
						ServerCCASessionImpl session=(ServerCCASessionImpl) activity;
						session.processRequest(request);
					}
				}
				
				//baranowb: do nothing here, if its valid it should be processed, f not we will get exception
			} catch (CreateActivityException e) {
				logger.error("", e);
			}

			// returning null so we can answer later
			return null;
		}else
		{
			logger.info("Diameter CCA RA :: Received bad request - either its not CCR or session should exist to handle this, Request code: "+request.getCommandCode()+", sessionId: "+request.getSessionId());
		}
		
		
		return null;
	}


	public void receivedSuccessMessage(Request request, Answer answer) {

		//NO answer should make it here, its an error, report it 
		logger.error("Diameter CCA RA :: Received bad answer - RA should not get this - its error, session should exist to handel it, Answer code: "+answer.getCommandCode()+", sessionId: "+answer.getSessionId());
		
	}


	public void timeoutExpired(Request request) {
		
		//NO Timeout shoudl occur here, session should exist, its and error
		logger.error("Diameter CCA RA :: Received timeout message - RA should not get this - its error, session should exist to handel it, Request code: "+request.getCommandCode()+", sessionId: "+request.getSessionId());
		
	}

	//  ///////////////////////////
	//  // fire events methods   //
	//  ///////////////////////////


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
	public void fireEvent(String sessionId, String name, Request request, Answer answer) {
		DiameterActivityHandle handle=this.getActivityHandle(sessionId);
		this.fireEvent(handle, name, request, answer);
	}
	
	public void fireEvent(ActivityHandle handle, String name, Request request,
			Answer answer) {
		try {
			
			int eventID=-1;
			int commandCode=(request==null?answer.getCommandCode():request.getCommandCode());
			if(!this.events.containsKey(commandCode))
			{
				logger.error("Diameter CCA RA :: Received bad command code - RA should not get this - its error, command code: "+commandCode);
				return;
			}
			if(commandCode ==CreditControlMessage.commandCode)
			{
			 eventID = eventLookup.getEventID("net.java.slee.resource.diameter.cca.events." + name, "java.net", "0.8");
			}else
			{
				//its a base
				eventID = eventLookup.getEventID("net.java.slee.resource.diameter.base.events." + name, "java.net", "0.8");
			}
			
			
			DiameterMessage event = (DiameterMessage) createEvent(request, answer);
			logger.info("Diameter CCA RA :: FIRE EVENT: command code: "+commandCode+" EVENTID: " +eventID+" Handle: "+handle);
			sleeEndpoint.fireEvent(handle, event, eventID, null);
		} catch (Exception e) {
			logger.warn("Can not send event", e);
		}
		
	
	}
	
	public DiameterMessage createEvent(Request request, Answer answer) throws OperationNotSupportedException {
		if (request == null && answer == null)
			return null;

		int commandCode = (request != null ? request.getCommandCode() : answer.getCommandCode());

		switch (commandCode) {
		case CreditControlMessage.commandCode: // CCR/CCA
			return request != null ? new CreditControlRequestImpl(request) : new CreditControlAnswerImpl(answer);
		case AbortSessionAnswer.commandCode: // ASR/ASA
			return request != null ? new AbortSessionRequestImpl(request) : new AbortSessionAnswerImpl(answer);
		case SessionTerminationAnswer.commandCode: // STR/STA
			return request != null ? new SessionTerminationRequestImpl(request) : new SessionTerminationAnswerImpl(answer);
		case ReAuthAnswer.commandCode: // DWR																			// RAR/RAA
			return request != null ? new ReAuthRequestImpl(request) : new ReAuthAnswerImpl(answer);
		case AccountingAnswer.commandCode: // ACR/ACA
			return request != null ? new AccountingRequestImpl(request) : new AccountingAnswerImpl(answer);
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
	
	
	
	
	//  ///////////////////////////////////
	//  // Session mgmt callback methods //
	//  ///////////////////////////////////

	public void sessionCreated(ClientCCASession ccClientSession) {
		
		
		if(this.getActivity(getActivityHandle(ccClientSession.getSessions().get(0).getSessionId()))!=null)
		{
			//FIXME: baranowb: log
			return;
		}
		
		DiameterMessageFactoryImpl baseFactory=new DiameterMessageFactoryImpl(ccClientSession.getSessions().get(0),this.stack);
		CreditControlMessageFactory ccaMsgFactory=new CreditControlMessageFactoryImpl(baseFactory,ccClientSession.getSessions().get(0),this.stack,this.localFactory);
		
	    CreditControlClientSessionImpl activity=new CreditControlClientSessionImpl(ccaMsgFactory,this.localFactory,ccClientSession,this.messageTimeout,null,null,this.sleeEndpoint);
	    
	    //FIXME: baranowb: add basic session mgmt for base? or do we relly on responses?
	    //session.addStateChangeNotification(activity);
	    activity.setSessionListener(this);
	    activityCreated(activity);
		
		
	}


	public void sessionCreated(ServerCCASession ccServerSession) {
		
		
		if(this.getActivity(getActivityHandle(ccServerSession.getSessions().get(0).getSessionId()))!=null)
		{
			//FIXME: baranowb: log
			return;
		}
		DiameterMessageFactoryImpl baseFactory=new DiameterMessageFactoryImpl(ccServerSession.getSessions().get(0),this.stack);
		CreditControlMessageFactory ccaMsgFactory=new CreditControlMessageFactoryImpl(baseFactory,ccServerSession.getSessions().get(0),this.stack,this.localFactory);
	    CreditControlServerSessionImpl activity=new CreditControlServerSessionImpl(ccaMsgFactory,this.localFactory,ccServerSession,this.messageTimeout,null,null,this.sleeEndpoint);
	    
	    //FIXME: baranowb: add basic session mgmt for base? or do we relly on responses?
	    //session.addStateChangeNotification(activity);
	    activity.setSessionListener(this);
	    activityCreated(activity);
		
	}

	
	
	
	public boolean sessionExists(String sessionId) {
		
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
	
	private class CreditControlProviderImpl implements CreditControlProvider
	{
		
		
		
		public CreditControlProviderImpl(CCAResourceAdaptor ra) {
			super();
			this.ra = ra;
		}

		protected CCAResourceAdaptor ra=null;
		public CreditControlClientSession createClientSession()
				throws CreateActivityException {
			
			try
			{
			ClientCCASession session = ((ISessionFactory) stack.getSessionFactory()).getNewAppSession(null, ApplicationId.createByAuthAppId(CreditControlMessageFactory._CCA_VENDOR, CreditControlMessageFactory._CCA_AUTH_APP_ID), ClientCCASession.class, null);
			
			if (session == null)
			{
				logger.error("Failure creating CCA Server Session (null).");
				return null;
			}
			
			return  (CreditControlClientSession) getActivity(getActivityHandle(session.getSessions().get(0).getSessionId()));
			
			}
			catch (InternalException e)
			{
				throw new CreateActivityException(e);
			}
			catch (IllegalDiameterStateException e)
			{
				throw new CreateActivityException(e);
			}
		}
		
		//This method should be called only for CCR
		protected DiameterActivity createActivity(Request request) throws CreateActivityException{
			
			//No session exists....
			try
			{
				Set<ApplicationId> appIds=request.getApplicationIdAvps();
				if(appIds==null|| appIds.size()==0)
				{
					throw new CreateActivityException("No App ids present in message");
				}
				
				//FIXME: add lookup for appids in req
				ServerCCASession session = ((ISessionFactory) stack.getSessionFactory()).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(CreditControlMessageFactory._CCA_VENDOR, CreditControlMessageFactory._CCA_AUTH_APP_ID), ServerCCASession.class, null);
				
				if (session == null)
				{
					logger.error("Failure creating CCA Server Session (null).");
					return null;
				}
				
				return (DiameterActivity) getActivity(getActivityHandle(request.getSessionId()));
			}
			catch (InternalException e)
			{
				logger.error("", e);
				return null;
			}
			catch (IllegalDiameterStateException e)
			{
				logger.error("", e);
				return null;
			}
			
		
		}

		public CreditControlClientSession createClientSession(
				DiameterIdentityAvp destinationHost,
				DiameterIdentityAvp destinationRealm)
				throws CreateActivityException {


			CreditControlClientSessionImpl clientSession=(CreditControlClientSessionImpl) this.createClientSession();
			clientSession.setDestinationHost(destinationHost);
			clientSession.setDestinationRealm(destinationRealm);
			return clientSession;
		}

		public CreditControlAVPFactory getCreditControlAVPFactory() {
			return localFactory;
		}

		public CreditControlMessageFactory getCreditControlMessageFactory() {
			DiameterMessageFactoryImpl baseFactory=new DiameterMessageFactoryImpl(null,stack);
			CreditControlMessageFactory ccaMsgFactory=new CreditControlMessageFactoryImpl(baseFactory,null,stack,localFactory);
			return ccaMsgFactory;
		}

		public DiameterIdentityAvp[] getConnectedPeers() {
			return ra.getConnectedPeers();
		}

		public int getPeerCount() {
			return ra.getConnectedPeers().length;
		}
		
	}

	
	
	
	// ################
	// # PROVISIONING #
	// ################
	
	public long getMessageTimeout() {
		return messageTimeout;
	}


	public void setMessageTimeout(long messageTimeout) {
		this.messageTimeout = messageTimeout;
	}


	public int getDefaultDirectDebitingFailureHandling() {
		return defaultDirectDebitingFailureHandling;
	}


	public void setDefaultDirectDebitingFailureHandling(
			int defaultDirectDebitingFailureHandling) {
		this.defaultDirectDebitingFailureHandling = defaultDirectDebitingFailureHandling;
	}


	public int getDefaultCreditControlFailureHandling() {
		return defaultCreditControlFailureHandling;
	}


	public void setDefaultCreditControlFailureHandling(
			int defaultCreditControlFailureHandling) {
		this.defaultCreditControlFailureHandling = defaultCreditControlFailureHandling;
	}


	public long getDefaultValidityTime() {
		return defaultValidityTime;
	}


	public void setDefaultValidityTime(long defaultValidityTime) {
		this.defaultValidityTime = defaultValidityTime;
	}


	public long getDefaultTxTimerValue() {
		return defaultTxTimerValue;
	}


	public void setDefaultTxTimerValue(long defaultTxTimerValue) {
		this.defaultTxTimerValue = defaultTxTimerValue;
	}
	
	
	// ##################
	// # HELPER METHODS #
	// ##################	
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

		return new DiameterIdentityAvp[0];
	}
	  
}
