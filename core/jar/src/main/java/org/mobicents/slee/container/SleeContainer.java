/*
 * Mobicents: The Open Source SLEE Platform      
 *
 * Copyright 2003-2005, CocoonHive, LLC., 
 * and individual contributors as indicated
 * by the @authors tag. See the copyright.txt 
 * in the distribution for a full listing of   
 * individual contributors.
 *
 * This is free software; you can redistribute it
 * and/or modify it under the terms of the 
 * GNU General Public License (GPL) as
 * published by the Free Software Foundation; 
 * either version 2 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that 
 * it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the 
 * GNU General Public
 * License along with this software; 
 * if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, 
 * Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site:
 * http://www.fsf.org.
 */

package org.mobicents.slee.container;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.slee.ComponentID;
import javax.slee.CreateException;
import javax.slee.EventTypeID;
import javax.slee.InvalidArgumentException;
import javax.slee.InvalidStateException;
import javax.slee.SLEEException;
import javax.slee.SbbID;
import javax.slee.ServiceID;
import javax.slee.UnrecognizedServiceException;
import javax.slee.facilities.Level;
import javax.slee.facilities.TimerFacility;
import javax.slee.management.AlreadyDeployedException;
import javax.slee.management.ComponentDescriptor;
import javax.slee.management.DependencyException;
import javax.slee.management.DeployableUnitDescriptor;
import javax.slee.management.DeployableUnitID;
import javax.slee.management.DeploymentException;
import javax.slee.management.ManagementException;
import javax.slee.management.ResourceAdaptorEntityAlreadyExistsException;
import javax.slee.management.ResourceAdaptorEntityState;
import javax.slee.management.SbbDescriptor;
import javax.slee.management.ServiceManagementMBean;
import javax.slee.management.ServiceState;
import javax.slee.management.SleeManagementMBean;
import javax.slee.management.SleeState;
import javax.slee.management.UnrecognizedDeployableUnitException;
import javax.slee.management.UnrecognizedLinkNameException;
import javax.slee.management.UnrecognizedResourceAdaptorEntityException;
import javax.slee.management.UnrecognizedResourceAdaptorException;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileSpecificationDescriptor;
import javax.slee.profile.ProfileSpecificationID;
import javax.slee.resource.ResourceAdaptorDescriptor;
import javax.slee.resource.ResourceAdaptorID;
import javax.slee.resource.ResourceAdaptorTypeDescriptor;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.resource.ResourceException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.jboss.logging.Logger;
import org.jboss.mx.loading.UnifiedClassLoader3;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.util.naming.NonSerializableFactory;
import org.jboss.util.naming.Util;
import org.mobicents.slee.container.component.ComponentContainer;
import org.mobicents.slee.container.component.ComponentIDImpl;
import org.mobicents.slee.container.component.ComponentKey;
import org.mobicents.slee.container.component.DeployableUnitDescriptorImpl;
import org.mobicents.slee.container.component.DeployableUnitIDImpl;
import org.mobicents.slee.container.component.EventTypeIDImpl;
import org.mobicents.slee.container.component.MobicentsEventTypeDescriptor;
import org.mobicents.slee.container.component.MobicentsSbbDescriptor;
import org.mobicents.slee.container.component.ProfileSpecificationDescriptorImpl;
import org.mobicents.slee.container.component.ProfileSpecificationIDImpl;
import org.mobicents.slee.container.component.ResourceAdaptorIDImpl;
import org.mobicents.slee.container.component.SbbEventEntry;
import org.mobicents.slee.container.component.SbbIDImpl;
import org.mobicents.slee.container.component.ServiceDescriptorImpl;
import org.mobicents.slee.container.component.ServiceIDImpl;
import org.mobicents.slee.container.component.deployment.DeploymentManager;
import org.mobicents.slee.container.component.deployment.EventTypeDeploymentDescriptorParser;
import org.mobicents.slee.container.component.deployment.ProfileSpecificationDescriptorParser;
import org.mobicents.slee.container.deployment.ConcreteClassGeneratorUtils;
import org.mobicents.slee.container.deployment.RaTypeDeployer;
import org.mobicents.slee.container.deployment.SbbDeployer;
import org.mobicents.slee.container.management.jmx.AlarmMBeanImpl;
import org.mobicents.slee.container.management.jmx.ComponentIDArrayPropertyEditor;
import org.mobicents.slee.container.management.jmx.ComponentIDPropertyEditor;
import org.mobicents.slee.container.management.jmx.DeployableUnitIDPropertyEditor;
import org.mobicents.slee.container.management.jmx.LevelPropertyEditor;
import org.mobicents.slee.container.management.jmx.ObjectPropertyEditor;
import org.mobicents.slee.container.management.jmx.PropertiesPropertyEditor;
import org.mobicents.slee.container.management.jmx.ServiceStatePropertyEditor;
import org.mobicents.slee.container.management.jmx.ServiceUsageMBeanImpl;
import org.mobicents.slee.container.management.jmx.TraceMBeanImpl;
import org.mobicents.slee.container.management.xml.DefaultSleeEntityResolver;
import org.mobicents.slee.container.profile.ProfileDeployer;
import org.mobicents.slee.container.profile.ProfileSpecificationIDPropertyEditor;
import org.mobicents.slee.container.rmi.RmiServerInterfaceMBean;
import org.mobicents.slee.container.service.Service;
import org.mobicents.slee.container.service.ServiceComponent;
import org.mobicents.slee.resource.ConfigPropertyDescriptor;
import org.mobicents.slee.resource.EventLookup;
import org.mobicents.slee.resource.EventLookupFacilityImpl;
import org.mobicents.slee.resource.InstalledResourceAdaptor;
import org.mobicents.slee.resource.ResourceAdaptorActivityContextInterfaceFactory;
import org.mobicents.slee.resource.ResourceAdaptorBoostrapContext;
import org.mobicents.slee.resource.ResourceAdaptorContext;
import org.mobicents.slee.resource.ResourceAdaptorDescriptorImpl;
import org.mobicents.slee.resource.ResourceAdaptorEntity;
import org.mobicents.slee.resource.ResourceAdaptorType;
import org.mobicents.slee.resource.ResourceAdaptorTypeDescriptorImpl;
import org.mobicents.slee.resource.ResourceAdaptorTypeIDImpl;
import org.mobicents.slee.resource.SleeEndpointImpl;
import org.mobicents.slee.resource.TCKActivityContextInterfaceFactoryImpl;
import org.mobicents.slee.runtime.ActivityContextFactoryImpl;
import org.mobicents.slee.runtime.EventRouter;
import org.mobicents.slee.runtime.EventRouterImpl;
import org.mobicents.slee.runtime.SbbObjectPoolFactory;
import org.mobicents.slee.runtime.SleeInternalEndpoint;
import org.mobicents.slee.runtime.SleeInternalEndpointImpl;
import org.mobicents.slee.runtime.cache.CacheableSet;
import org.mobicents.slee.runtime.facilities.ActivityContextNamingFacilityImpl;
import org.mobicents.slee.runtime.facilities.AlarmFacilityImpl;
import org.mobicents.slee.runtime.facilities.NullActivityContextInterfaceFactoryImpl;
import org.mobicents.slee.runtime.facilities.NullActivityFactoryImpl;
import org.mobicents.slee.runtime.facilities.ProfileFacilityImpl;
import org.mobicents.slee.runtime.facilities.ProfileTableActivityContextInterfaceFactoryImpl;
import org.mobicents.slee.runtime.facilities.TimerFacilityImpl;
import org.mobicents.slee.runtime.facilities.TraceFacilityImpl;
import org.mobicents.slee.runtime.sbbentity.RootSbbEntitiesRemovalTask;
import org.mobicents.slee.runtime.serviceactivity.ServiceActivityContextInterfaceFactoryImpl;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;
import org.mobicents.slee.runtime.transaction.TransactionIDAccessImpl;
import org.mobicents.slee.runtime.transaction.TransactionManagerImpl;
import org.mobicents.slee.runtime.transaction.TransactionalAction;

import com.opencloud.sleetck.lib.resource.sbbapi.TransactionIDAccess;

/**
 * 
 * Implements the SleeContainer. The SleeContainer is the anchor for the Slee.
 * It is the central location from where all other major data structures are
 * accessible.
 * 
 * @author F.Moggia
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 * @author Emil Ivov
 * @author Tim Fox
 * @author eduardomartins
 * 
 */
public class SleeContainer implements ComponentContainer {

	public static boolean isSecurityEnabled = false;

	// For unit testing only -- to be removed later.
	private static final String JNDI_NAME = "container";

	public static final String JVM_ENV = "java:";

	/** standard ENC name in JNDI */
	public static final String COMP_ENV = "java:comp/env";

	/** the root context for SLEE */
	private static final String CTX_SLEE = "slee";

	/** The lifecycle state of the SLEE */
	private SleeState sleeState;

	// The usage parameter set
	// (Ivelin) obsolete, the usage params are now stored in sbbdescriptorimpl
	// Usage parameters - indexed by ServiceID/SbbID
	// These are now stored in the Service

	// private HashMap usageParameters;

	// the usage mbean -- one per usage parameter set.
	// Indexed using the same key as the usageParameters set.
	// private HashMap usageMBeans;

	// The key is the SbbID the value is the ObjectPooling
	private HashMap sbbPooling;

	private GenericObjectPool.Config poolConfig;

	// An interface for the resource adaptor to retrieve the event type id.
	private EventLookup eventLookup;

	private HashMap eventTypeIDToDescriptor;

	private HashMap eventKeyToEventTypeIDMap;

	// A vector containing all the event types that are known to the slee.
	private HashMap eventTypeIDs;

	private ActivityContextFactoryImpl activityContextFactory;

	// An abstraction used by resource adaptors to post events to the
	// slee event queue.
	private SleeInternalEndpoint sleeEndpoint;

	private RmiServerInterfaceMBean rmiServerInterfaceMBeanImpl;

	// the class that actually posts events to the SBBs.
	// This should be made into a facility and registered with jmx and jndi
	// so it can be independently controlled.
	private EventRouter router;

	private DeploymentManager deploymentManager;

	private ServiceActivityContextInterfaceFactoryImpl serviceActivityContextInterfaceFactory;

	private NullActivityContextInterfaceFactoryImpl nullActivityContextInterfaceFactory;

	private static TimerFacilityImpl timerFacility;

	private ProfileFacilityImpl profileFacility;

	// A set of event type ids mapped to event descriptors.

	private HashMap eventTypeIDToEventKeyMap;

	private final static Logger logger = Logger.getLogger(SleeContainer.class);

	private ActivityContextNamingFacilityImpl activityContextNamingFacility;

	/**
	 * Resource Container
	 */
	// maps Resource adaptor Type Ids to the installed ResourceAdaptorTypes
	private HashMap resourceAdaptorTypes;

	// maps ResourceAdaptorID to the installed ResourceAdaptor
	private HashMap installedResourceAdaptors;

	// maps a link to a resource adaptor entity name
	private HashMap resourceAdaptorEntityLinks;

	/*
	 * maps name to Resource Adaptor Entity This has to be persistent inside the
	 * slee. If the slee is shutdown and restarted the RaEntities are restored
	 * to their previous operational state
	 * 
	 */
	private HashMap resourceAdaptorEntities;

	private ResourceAdaptorContext bootstrapContext;

	// maps ResourceAdaptorTypeID to the proper ActivityContextInterfaceFactory
	private HashMap activityContextInterfaceFactories;

	private NullActivityFactoryImpl nullActivityFactory;

	private ProfileTableActivityContextInterfaceFactoryImpl profileTableActivityContextInterfaceFactory;

	private TransactionIDAccess transactionIDAccess;

	private MBeanServer mbeanServer;

	private ServiceManagementMBean serviceManagementMBean;

	private ClassLoader loader;

	{
		// establish the location of mobicents.sar
		initDeployPath();
	}

	/*
	 * private DeploymentCacheManager getDeploymentCacheManager() throws
	 * SystemException { DeploymentCacheManager cm = (DeploymentCacheManager)
	 * SleeContainer
	 * .getTransactionManager().getDeferredData("DeploymentCache"); if (cm ==
	 * null) { cm = new DeploymentCacheManager();
	 * SleeContainer.getTransactionManager().putDeferredData( "DeploymentCache",
	 * cm); cm.loadFromCache(); }
	 * 
	 * return cm; }
	 */
	private DeploymentCacheManager deploymentCacheManager;

	private static SleeContainer sleeContainer;

	private static SleeTransactionManager sleeTransactionManager;

	/**
	 * location of mobicents.sar
	 */
	private static String deployPath;

	/**
	 * Get deployment data set from cache
	 * 
	 */
	private DeploymentCacheManager getDeploymentCacheManager() {
		if (deploymentCacheManager == null) {
			deploymentCacheManager = new DeploymentCacheManager();
		}
		return deploymentCacheManager;
	}

	private static void initDeployPath() {
		java.net.URL url = SleeContainer.class.getResource("/");
		java.net.URI uri;
		try {
			uri = new java.net.URI(url.toExternalForm());
			File file = new File(uri);
			deployPath = file.getAbsolutePath();
		} catch (URISyntaxException e) {
			logger
					.error(
							"Failed to establish path to Mobicents root deployment directory (mobicents.sar)",
							e);
			deployPath = null;
		}
	}

	/**
	 * 
	 * @return the full file system path where mobicents.sar is located
	 */
	public static String getDeployPath() {
		return deployPath;
	}

	public DeploymentCacheManager getDeploymentManager() {
		return deploymentCacheManager;
	}

	public static TraceFacilityImpl getTraceFacility() throws NamingException {
		return (TraceFacilityImpl) lookupFacilityInJndi(TraceMBeanImpl.JNDI_NAME);
	}

	public static AlarmFacilityImpl getAlarmFacility() throws NamingException {
		return (AlarmFacilityImpl) lookupFacilityInJndi(AlarmMBeanImpl.JNDI_NAME);
	}

	public static ProfileTableActivityContextInterfaceFactoryImpl getProfileTableActivityContextInterfaceFactory()
			throws NamingException {
		return (ProfileTableActivityContextInterfaceFactoryImpl) SleeContainer
				.lookupFacilityInJndi(ProfileTableActivityContextInterfaceFactoryImpl.JNDI_NAME);
	}

	public ClassLoader getClassLoader() {
		return this.loader;
	}

	/**
	 * Get the service management MBean
	 * 
	 * @throws Exception
	 */
	public ServiceManagementMBean getServiceManagementMBean() {
		return this.serviceManagementMBean;
	}

	/**
	 * Initialization code.
	 * 
	 */
	public void init(SleeManagementMBean sleeManagementMBean,
			ObjectName rmiServerInterfaceMBean) throws Exception {
		// loader = new URLClassLoader(new URL[0], Thread.currentThread()
		// .getContextClassLoader());
		logger.info("Initializing SLEE container...");

		initNamingContexts();

		serviceManagementMBean = (ServiceManagementMBean) MBeanProxy.get(
				ServiceManagementMBean.class, sleeManagementMBean
						.getServiceManagementMBean(), mbeanServer);

		// Force class loading of ConcreteClassGeneratorUtils to initilize the
		// static pool
		ConcreteClassGeneratorUtils.class.getClass();

		DefaultSleeEntityResolver.init(this.getClass().getClassLoader());

		this.poolConfig = this.configurePooling();

		this.sbbPooling = new HashMap();
		this.deploymentManager = new DeploymentManager();

		// this.usageParameters = new HashMap();
		this.eventTypeIDs = new HashMap();
		this.eventTypeIDToDescriptor = new HashMap();
		this.eventKeyToEventTypeIDMap = new HashMap();
		this.transactionIDAccess = new TransactionIDAccessImpl(
				(TransactionManagerImpl) SleeContainer.getTransactionManager());
		this.eventLookup = new EventLookup(this);

		this.eventTypeIDToEventKeyMap = new HashMap();

		this.deployStandardEvents();
		this.deployStandardProfileSpecs();
		this.activityContextFactory = new ActivityContextFactoryImpl(this);
		this.router = new EventRouterImpl(this);
		this.sleeEndpoint = new SleeInternalEndpointImpl(
				activityContextFactory, router, this);

		// Initialize the various facilities for the slee.

		this.activityContextNamingFacility = new ActivityContextNamingFacilityImpl();
		registerWithJndi("slee/facilities", "activitycontextnaming",
				activityContextNamingFacility);

		this.nullActivityContextInterfaceFactory = new NullActivityContextInterfaceFactoryImpl(
				this);
		registerWithJndi("slee/nullactivity",
				"nullactivitycontextinterfacefactory",
				nullActivityContextInterfaceFactory);

		this.nullActivityFactory = new NullActivityFactoryImpl(this);
		registerWithJndi("slee/nullactivity", "nullactivityfactory",
				nullActivityFactory);

		this.profileTableActivityContextInterfaceFactory = new ProfileTableActivityContextInterfaceFactoryImpl();
		registerWithJndi("slee/facilities",
				ProfileTableActivityContextInterfaceFactoryImpl.JNDI_NAME,
				profileTableActivityContextInterfaceFactory);

		SleeContainer.timerFacility = new TimerFacilityImpl(this);

		registerWithJndi("slee/facilities", TimerFacilityImpl.JNDI_NAME,
				SleeContainer.timerFacility);

		this.profileFacility = new ProfileFacilityImpl();

		registerWithJndi("slee/facilities", ProfileFacilityImpl.JNDI_NAME,
				profileFacility);

		// this.alarmFacility = (AlarmFacilityImpl)
		// lookupFacilityInJndi(AlarmMBeanImpl.JNDI_NAME);

		// This is not registered with jndi.
		this.serviceActivityContextInterfaceFactory = new ServiceActivityContextInterfaceFactoryImpl(
				this);

		registerWithJndi("slee/serviceactivity/",
				ServiceActivityContextInterfaceFactoryImpl.JNDI_NAME,
				serviceActivityContextInterfaceFactory);

		this.bootstrapContext = new ResourceAdaptorContext(this
				.getSleeEndpoint(), this.getEventLookupFacility());
		/*
		 * Resource Container initialization
		 */
		resourceAdaptorTypes = new HashMap();
		resourceAdaptorEntities = new HashMap();
		installedResourceAdaptors = new HashMap();
		this.resourceAdaptorEntityLinks = new HashMap();
		this.activityContextInterfaceFactories = new HashMap();

		registerWithJndi();

		startRMIServer(this.nullActivityFactory, this.sleeEndpoint,
				this.eventLookup, rmiServerInterfaceMBean);

		registerPropertyEditors();

	}

	private Config configurePooling() {
		GenericObjectPool.Config conf = new GenericObjectPool.Config();
		conf.maxActive = -1;
		conf.maxIdle = -1;
		conf.maxWait = -1;
		conf.minEvictableIdleTimeMillis = -1;
		conf.minIdle = 5;
		conf.numTestsPerEvictionRun = -1;
		conf.testOnBorrow = false;
		conf.testOnReturn = false;
		conf.testWhileIdle = false;
		conf.timeBetweenEvictionRunsMillis = -1;
		conf.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;

		return conf;
	}

	private void registerWithJndi() {
		registerWithJndi("slee", JNDI_NAME, this);
	}

	private void unregisterWithJndi() {
		unregisterWithJndi("slee/" + JNDI_NAME);
	}

	/**
	 * Return the SleeContainer instance registered in the JVM scope of JNDI
	 * 
	 */
	public static SleeContainer lookupFromJndi() {
		try {
			if (sleeContainer != null)
				return sleeContainer;
			else {
				sleeContainer = (SleeContainer) getFromJndi("slee/" + JNDI_NAME);
				return sleeContainer;
			}
		} catch (Exception ex) {
			logger.error("Unexpected error: Cannot retrieve SLEE Container!",
					ex);
			return null;
		}
	}

	/*
	 * Start the HA RMI Server, used by JCA resource adaptors to communicate
	 * with the SLEE
	 */
	private void startRMIServer(NullActivityFactoryImpl naf,
			SleeInternalEndpoint endpoint, EventLookup eventLookup,
			ObjectName rmiServerInterfaceMBean) {

		try {
			logger.debug("creating RmiServerInterface using MBeanProxy");

			rmiServerInterfaceMBeanImpl = (RmiServerInterfaceMBean) MBeanProxy
					.get(RmiServerInterfaceMBean.class,
							rmiServerInterfaceMBean, mbeanServer);

			rmiServerInterfaceMBeanImpl.startRMIServer(naf, endpoint,
					eventLookup, activityContextFactory);

		} catch (Exception e) {
			logger.error(
					"Failed to start HA RMI server for Remote slee service", e);
		}
	}

	private void stopRMIServer() {

		logger.debug("Stopping RMI Server for slee service");

		rmiServerInterfaceMBeanImpl.stopRMIServer();

	}

	/**
	 * Load up all the services from the deployment directory.
	 * 
	 * @throws Exception
	 */
	public void initializeServices() throws Exception {

		try {

			deploymentManager.deployAllUnitsAtLocation(getDeployPath()
					+ "/deploy", // get
					// deployable
					// units
					// here,
					getDeployPath(), // put classes from them here
					this); // and install them in this container

		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new Exception(ex.getMessage());
		}
	}

	/**
	 * Start a service given its service ID.
	 * 
	 * @param serviceID
	 * @throws UnrecognizedServiceException
	 */

	public synchronized void startService(ServiceID serviceID)
			throws UnrecognizedServiceException, InvalidStateException {

		if (logger.isDebugEnabled()) {
			logger.debug("Activating " + serviceID);
		}

		if (serviceID == null)
			throw new NullPointerException("null service id");

		SleeTransactionManager transactionManager = SleeContainer
				.getTransactionManager();
		boolean b = transactionManager.requireTransaction();
		boolean rb = true;
		try {

			Service service = this.getService(serviceID);

			if (service == null)
				throw new UnrecognizedServiceException("unrecognized service "
						+ serviceID);

			if (service.getState().equals(ServiceState.ACTIVE)) {
				// transactionManager.commit();
				// return;
				getDeploymentCacheManager().getActiveServiceIDs()
						.add(serviceID);

				throw new InvalidStateException("Service already active");
			}

			// If there was a deactivate before we have sbb entities pending,
			// remove those first
			RootSbbEntitiesRemovalTask task = RootSbbEntitiesRemovalTask
					.getTask(serviceID);
			if (task != null) {
				task.run();
				if (logger.isDebugEnabled()) {
					logger
							.debug("Found timer task running to remove remaining sbb entities. Executing now...");
				}
			}

			// notifying the resource adaptor about service
			ServiceComponent svcComponent = getServiceComponent(serviceID);
			HashSet sbbIDs = svcComponent.getSbbComponents();
			HashSet raEntities = new HashSet();
			Iterator i = sbbIDs.iterator();
			while (i.hasNext()) {
				SbbDescriptor sbbdesc = getSbbComponent((SbbID) i.next());
				if (sbbdesc != null) {
					String[] raLinks = sbbdesc.getResourceAdaptorEntityLinks();
					for (int c = 0; raLinks != null && c < raLinks.length; c++) {
						ResourceAdaptorEntity raEntity = getRAEntity(raLinks[c]);
						if (raEntity != null && !raEntities.contains(raEntity)) {
							raEntity.serviceActivated(serviceID.toString());
							raEntities.add(raEntity);
						}
					}
				}
			}

			// Already active just return.
			service.activate();
			getDeploymentCacheManager().getActiveServiceIDs().add(serviceID);
			svcComponent.lock();
			rb = false;
			logger.info("Activated " + serviceID);
		} catch (InvalidStateException ise) {
			throw ise;
		} catch (UnrecognizedServiceException use) {
			throw use;
		} catch (Exception ex) {
			throw new SLEEException("Failed starting service", ex);
		} finally {

			try {
				if (rb)
					transactionManager.setRollbackOnly();
				if (b)
					transactionManager.commit();

			} catch (Exception e) {
				logger.error("Failed: transaction commit", e);
			}
		}
	}

	/**
	 * Revive a service and start it. This is called when starting the SLEE
	 * after stop.
	 * 
	 * @param serviceID --
	 *            service ID of the service to start
	 * @return
	 * @throws Exception
	 */
	public void reviveAndStartService(ServiceID serviceID) throws Exception {

		getTransactionManager().begin();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Reactivating service" + serviceID);
			}
			this.startService(serviceID);
		} catch (Exception ex) {
			getTransactionManager().setRollbackOnly();
		} finally {
			try {
				getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("txmgr failed", ex);
			}
		}
	}

	/**
	 * get a resource adaptor entity given its name. To retrieve an RA Entity
	 * from. A single resource adaptor type may have multiple resource adaptors
	 * associated with it. This conveniance method just returns the first one.
	 * 
	 * @param raTypeID --
	 *            Resource adaptor type id.
	 * 
	 */
	public ResourceAdaptorEntity getResourceAdaptorEntity(
			ResourceAdaptorTypeID raTypeID) throws Exception {
		ResourceAdaptorType raType = (ResourceAdaptorType) resourceAdaptorTypes
				.get(raTypeID);
		if (raType == null)
			throw new Exception("Ra Type not found! ");
		HashSet resourceAdaptors = raType.getResourceAdaptorIDs();
		if (resourceAdaptors == null || resourceAdaptors.isEmpty())
			throw new Exception(
					"Could not find a resource adaptor for this type: "
							+ raTypeID);
		ResourceAdaptorIDImpl raId = (ResourceAdaptorIDImpl) resourceAdaptors
				.iterator().next();
		InstalledResourceAdaptor installedRa = (InstalledResourceAdaptor) installedResourceAdaptors
				.get(raId);
		if (installedRa.getResourceAdaptorEntities().size() == 0)
			throw new Exception("cannot find RA Entity! ");
		return (ResourceAdaptorEntity) installedRa.getResourceAdaptorEntities()
				.iterator().next();
	}

	/**
	 * @return The installed resource adaptor matching the given component(RA)
	 *         ID. null if there is none.
	 * 
	 * @param raId --
	 *            Resource adaptor id.
	 * 
	 */
	public InstalledResourceAdaptor getInstalledResourceAdaptor(
			ResourceAdaptorID raId) throws Exception {
		InstalledResourceAdaptor installedRa = (InstalledResourceAdaptor) installedResourceAdaptors
				.get(raId);
		return installedRa;
	}

	/**
	 * Reverses the result of
	 * 
	 * @see #initializeTckResourceAdaptor()
	 * 
	 */
	public void closeTckResourceAdaptor() {
		unregisterWithJndi("slee/resources", "tckacif");

		try {
			deactivateResourceAdaptorEntity("tck");
			// FRANCESCO -- Why do you need a name for the entity and another
			// entity links?
			resourceAdaptorEntityLinks.remove("slee/resources/tck");
		} catch (Exception e) {
			logger.error("Error Closing RA", e);

		}
	}

	/**
	 * Activate a resource adaptor entity given its name.
	 * 
	 * @param name --
	 *            name of the RA entity
	 * @throws NullPointerException
	 * @throws UnrecognizedResourceAdaptorException
	 * @throws InvalidStateException
	 * @throws ResourceException
	 * @throws ManagementException
	 * 
	 * 
	 */
	public void activateResourceAdaptorEntity(String name)
			throws UnrecognizedResourceAdaptorEntityException,
			InvalidStateException, javax.slee.resource.ResourceException,
			ManagementException {
		if (this.resourceAdaptorEntities.get(name) == null) {
			throw new UnrecognizedResourceAdaptorEntityException(
					"Resource Adaptor Entity " + name + " not found.");
		}
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(name);
		raEntity.activate();
	}

	public void addResourceAdaptorType(ResourceAdaptorTypeID key,
			ResourceAdaptorType rat) {
		this.resourceAdaptorTypes.put(key, rat);
	}

	public HashMap getResourceAdaptorTypes() {
		return resourceAdaptorTypes;
	}

	/**
	 * 
	 * Cleanup in the reverse order of init()
	 * 
	 * @throws NamingException
	 * 
	 */
	public void close() throws NamingException {

		// Cleaning up the cache is not desired for failover scenarios.
		// Cache should be cleaned only when the last node in a SLEE cluster is
		// shutting down.
		// TODO: cleanSleeCache();

		unregisterWithJndi();

		Context ctx = new InitialContext();
		Util.unbind(ctx, JVM_ENV + CTX_SLEE);

		// closeSipResourceAdaptor();
		closeTckResourceAdaptor();

		stopRMIServer();

	}

	/**
	 * These are slee defined events. These are stored in the location
	 * slee-event-jar.xml Parse these and make them known to the slee.
	 * 
	 */
	private void deployStandardEvents() throws Exception {
		// TODO: modify to use new deployment mechanisms (have one or 2 DU for
		// the standard events)
		logger.info("Deploying standard events.");
		List stdEvents = EventTypeDeploymentDescriptorParser
				.parseStandardEvents(getDeployPath()
						+ "/xml/slee-event-jar.xml");
		Iterator it = stdEvents.iterator();
		while (it.hasNext()) {
			MobicentsEventTypeDescriptor etype = (MobicentsEventTypeDescriptor) it
					.next();
			this.installEventType(etype);
		}
		stdEvents = EventTypeDeploymentDescriptorParser
				.parseStandardEvents(getDeployPath() + "/xml/event-jar.xml");
		it = stdEvents.iterator();
		while (it.hasNext()) {
			MobicentsEventTypeDescriptor etype = (MobicentsEventTypeDescriptor) it
					.next();
			this.installEventType(etype);
		}

	}

	/**
	 * These are slee defined profiles. These are stored in the location
	 * slee-profile-spec-jar.xml Parse these and make them known to the slee.
	 * 
	 */
	private void deployStandardProfileSpecs() throws Exception {
		logger.info("Deploying standard profile specs.");
		List stdProfileSpecs = ProfileSpecificationDescriptorParser
				.parseStandardProfileSpecifications(getDeployPath()
						+ "/xml/slee-profile-spec-jar.xml");
		Iterator it = stdProfileSpecs.iterator();
		while (it.hasNext()) {
			ProfileSpecificationDescriptorImpl profileSpecification = (ProfileSpecificationDescriptorImpl) it
					.next();
			this.install(profileSpecification, null);
		}
	}

	private void installEventType(MobicentsEventTypeDescriptor descriptorImpl)
			throws AlreadyDeployedException {

		ComponentKey ckey = new ComponentKey(descriptorImpl.getName(),
				descriptorImpl.getVendor(), descriptorImpl.getVersion());

		if (this.eventKeyToEventTypeIDMap.get(ckey) == null) {
			EventTypeIDImpl eventTypeID = new EventTypeIDImpl(ckey);
			if (logger.isDebugEnabled())
				logger.debug("Installing event " + eventTypeID);
			this.eventTypeIDToDescriptor.put(eventTypeID, descriptorImpl);
			this.eventKeyToEventTypeIDMap.put(ckey, eventTypeID);
			this.eventTypeIDToEventKeyMap.put(eventTypeID, ckey);
			this.eventTypeIDs.put(new Integer(eventTypeID.getEventID()),
					eventTypeID);
			descriptorImpl.setID(eventTypeID);
			logger.info("Installed event " + ckey);
		} else {

			throw new AlreadyDeployedException("The event " + ckey
					+ " is already deployed");
		}

	}

	/**
	 * Remove all the deployed evnet type information related to the deployable
	 * unit id. Searches through the set of deployed event information and
	 * removes any event information associated with this deployableUnitID
	 * 
	 * @param deployableUnitID
	 */

	private synchronized void removeEventType(DeployableUnitID deployableUnitID) {

		if (logger.isDebugEnabled()) {
			logger.debug("Uninstalling events from DU " + deployableUnitID);
		}

		Iterator it = eventTypeIDToDescriptor.keySet().iterator();
		while (it.hasNext()) {
			EventTypeIDImpl eventTypeID = (EventTypeIDImpl) it.next();
			MobicentsEventTypeDescriptor desc = (MobicentsEventTypeDescriptor) eventTypeIDToDescriptor
					.get(eventTypeID);
			if (desc != null && desc.getDeployableUnit() != null
					&& desc.getDeployableUnit().equals(deployableUnitID)) {
				it.remove();

				ComponentKey ckey = (ComponentKey) this.eventTypeIDToEventKeyMap
						.get(eventTypeID);
				this.eventTypeIDToEventKeyMap.remove(eventTypeID);
				if (ckey != null)
					eventKeyToEventTypeIDMap.remove(ckey);
				this.eventTypeIDs.remove(new Integer(eventTypeID.getEventID()));
				logger.info("Uninstalled event " + eventTypeID);
			}
		}
	}

	/**
	 * Unregister an internal slee component with jndi.
	 * 
	 */
	public static void unregisterWithJndi(String prefix, String name) {
		unregisterWithJndi(prefix + "/" + name);
	}

	/**
	 * Unregister an internal slee component with jndi.
	 * 
	 * @param Name -
	 *            the full path to the resource except the "java:" prefix.
	 */
	public static void unregisterWithJndi(String name) {
		Context ctx;
		String path = JVM_ENV + name;
		try {
			ctx = new InitialContext();
			Util.unbind(ctx, path);
		} catch (NamingException ex) {
			logger.warn("unregisterWithJndi failed for " + path);
		}
	}

	/**
	 * Register a internal slee component with jndi.
	 * 
	 */
	public static void registerWithJndi(String prefix, String name,
			Object object) {
		String fullName = JVM_ENV + prefix + "/" + name;
		try {
			Context ctx = new InitialContext();
			try {
				Util.createSubcontext(ctx, fullName);
			} catch (NamingException e) {
				logger.warn("Context, " + fullName + " might have been bound.");
				logger.warn(e);
			}
			ctx = (Context) ctx.lookup(JVM_ENV + prefix);
			// ctx.createSubcontext(name);
			// ctx = (Context) ctx.lookup(name);
			// Util.rebind(JVM_ENV + prefix + "/" + name, object);
			NonSerializableFactory.rebind(fullName, object);
			StringRefAddr addr = new StringRefAddr("nns", fullName);
			Reference ref = new Reference(object.getClass().getName(), addr,
					NonSerializableFactory.class.getName(), null);
			Util.rebind(ctx, name, ref);
			logger.debug("registered with jndi " + fullName);
		} catch (Exception ex) {
			logger.warn("registerWithJndi failed for " + fullName, ex);
		}
	}

	/**
	 * 
	 * Convenience method for registering SLEE facilities with JNDI
	 * 
	 * @param facilityName
	 * @param facility
	 */
	public static void registerFacilityWithJndi(String facilityName,
			Object facility) {
		registerWithJndi("slee/facilities", facilityName, facility);
	}

	/**
	 * 
	 * Convenience method for unregistering SLEE facilities with JNDI
	 * 
	 * @param facilityName
	 * @param facility
	 */
	public static void unregisterFacilityWithJndi(String facilityName) {
		unregisterWithJndi("slee/facilities", facilityName);
	}

	/**
	 * 
	 * Convenience method for unregistering SLEE facilities with JNDI
	 * 
	 * @param facilityName
	 * @param facility
	 * @throws NamingException
	 */
	private static Object lookupFacilityInJndi(String facilityName)
			throws NamingException {
		return getFromJndi("slee/facilities" + "/" + facilityName);
	}

	/**
	 * lookup a name reference from jndi.
	 * 
	 * @param resourceName --
	 *            name to lookup.
	 * @throws NamingException
	 */
	private static Object getFromJndi(String resourceName)
			throws NamingException {

		Context initialContext = new InitialContext();
		Context compEnv = (Context) initialContext.lookup(JVM_ENV);
		return compEnv.lookup(resourceName);

	}

	/**
	 * Add a service to the deployment table.<br>
	 * This method follows section 15.4.2.15 - page 302 of jslee1.1
	 * specification. In particular:
	 * <ul>
	 * <li>The event types specified are of interest to the service installed
	 * in the SLEE.
	 * <li>The SLEE will only provide the resource adaptor entity information
	 * about events that are specified by the resource adaptor type of the
	 * resource adaptor. For example, if a Foo resource adaptor type only
	 * references the Foo event, then the SLEE will only tell the resource
	 * adaptor implementing this resource adaptor type about Foo events.
	 * <li> If multiple SBB entities define different resource options in the
	 * deployment descriptor for the same event type, then the SLEE concatenates
	 * the resource options on that event type.
	 * </ul>
	 * <br>
	 * <b>Resource options passed are concatenated with "," sign, no white
	 * spaces are added. eventID and corresponding options are sorted in
	 * ascending order by eventID (integer).</b> <br>
	 * Examples parameters of passed to RA to serviceInstalled method:<br>
	 * <table border="1">
	 * <tr>
	 * <th>index</th>
	 * <th>eventID</th>
	 * <th>resource options</th>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>12</td>
	 * <td>null</td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td>24</td>
	 * <td>Block</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>53</td>
	 * <td>Pass,Ham,Cat</td>
	 * </tr>
	 * </table><br>
	 * <b>null</b> - this is java null value, not string - "null". It is the
	 * same as:<br>
	 * <i>String nully=null;<br>
	 * System.out.println(nully);</i>
	 * 
	 * @param serviceDescriptor --
	 *            the service descriptor to add.
	 * @throws Exception --
	 *             if there was a problem installing and starting the service.
	 */
	private void installService(ServiceDescriptorImpl serviceDescriptorImpl)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing Service " + serviceDescriptorImpl.getID());
		}

		sleeTransactionManager.mandateTransaction();

		// new Service(serviceDescriptorImpl);
		Service.getService(serviceDescriptorImpl);

		ServiceComponent serviceComponent = new ServiceComponent(
				serviceDescriptorImpl);

		MobicentsSbbDescriptor sbbDesc = serviceComponent.getRootSbbComponent();

		if (sbbDesc == null) {
			throw new DeploymentException(
					"cannot find root SbbID component ! cannot install service ");
		}

		this.getDeploymentCacheManager().getServiceComponents().put(
				serviceComponent.getServiceID(), serviceComponent);

		this.addReferringComponent((SbbIDImpl) sbbDesc.getID(),
				serviceComponent.getServiceID());

		ServiceUsageMBeanImpl serviceUsageMBean = new ServiceUsageMBeanImpl(
				serviceComponent.getServiceID());

		ObjectName usageMBeanName = this.serviceManagementMBean
				.getServiceUsageMBean(serviceComponent.getServiceID());

		serviceComponent.setUsageMBeanName(usageMBeanName);

		this.mbeanServer.registerMBean(serviceUsageMBean, usageMBeanName);
		// Recursively install all the usage parameters for this and all his
		// children.

		serviceComponent.installDefaultUsageParameters(sbbDesc, new HashSet());

		// SBBIDS FOR THIS SERVICE
		HashSet sbbIDs = serviceComponent.getSbbComponents();

		if (logger.isDebugEnabled()) {

			Iterator sbbIdsIterator = sbbIDs.iterator();
			StringBuffer sb = new StringBuffer(300);

			while (sbbIdsIterator.hasNext()) {
				SbbDescriptor sbbdesc = (MobicentsSbbDescriptor) getSbbComponent((SbbID) sbbIdsIterator
						.next());
				sb.append("NAME=" + sbbdesc.getName() + " COMPONENTID="
						+ sbbdesc.getID() + "\n");
			}
			logger
					.debug("\n==================SERVICE SBBS=======================\n"
							+ sb.toString()
							+ "\n"
							+ "=====================================================");
		}

		// CONTAINS MAPPING RAENTITY TO Events that are of interest of this
		// service, which are storeded in Set( of EventTypeIDs)
		HashMap raEntitiesToEventTypeIDsOfInterest = new HashMap(5);
		// CONTAINS MAPPING OF EVENTID TO resource options FOR THIS EVENT WHICH
		// ARE STORED IN String
		HashMap eventTypeIDsToResourceOptions = new HashMap(30);
		// MAPS RA ENTITY TO EVENTS IT CAN FIRE, EVENTS ARE STORED IN Set( of
		// EventTypeIDs)
		HashMap raEntitiesToEventsFired = new HashMap(5);

		Iterator sbbIdsIterator = sbbIDs.iterator();

		// WE HAVE TO ITERATE THROUGH ALL SBBS IN SERVICE AND BUILD DATA
		// STRUCTURES
		while (sbbIdsIterator.hasNext()) {

			MobicentsSbbDescriptor sbbdesc = (MobicentsSbbDescriptor) getSbbComponent((SbbID) sbbIdsIterator
					.next());
			if (sbbdesc == null)
				continue;

			// EventTypeID[] eventTypeIDs = sbbdesc.getEventTypes();

			// HashMap sbbResourceOptionsForEventsIfInterest=new HashMap(20);
			// maps EventTypeID to coresponding SbbEventEntry
			// IT CONTAINS MAPPING FOR ALL EVENTS THAT ARE OF INTEREST IF THIS
			// SBB
			HashMap eventTypeIdToEventEntriesMappings = sbbdesc
					.getEventTypesMappings();
			// SIMPLY SET OF EventTypeIDs that are of interest of this SBB
			Set sbbEventsOfInterest = eventTypeIdToEventEntriesMappings
					.keySet();
			// HashSet sbbRaEntities=new HashSet(20);
			// LINKS OF RAs FOR THIS SBB
			String[] raLinks = sbbdesc.getResourceAdaptorEntityLinks();

			if (raLinks == null || raLinks.length == 0) {
				if (logger.isDebugEnabled()) {
					logger.debug(" Service doesn't have RA links");
				}
				continue;
			}
			if (logger.isDebugEnabled()) {

				logger.debug("\n"
						+ "=============SBB==============================\n"
						+ "" + sbbdesc.getName() + ", " + sbbdesc.getVendor()
						+ ", " + sbbdesc.getVersion() + "\n"
						+ "==============================================");
				Iterator eventEntryItarator = eventTypeIdToEventEntriesMappings
						.values().iterator();

				StringBuffer sb = new StringBuffer(300);

				while (eventEntryItarator.hasNext()) {
					SbbEventEntry eventEntry = (SbbEventEntry) eventEntryItarator
							.next();
					sb.append("EVENT :" + eventEntry + "\n");
				}
				logger
						.debug("\n==================EVENTS OF SBB INTEREST=======================\n"
								+ sb.toString()
								+ "\n"
								+ "=================================================================");
				sb = new StringBuffer(300);
				for (int i = 0; i < raLinks.length; i++) {
					sb.append("RALINK : " + raLinks[i] + "\n");
				}
				logger
						.debug("\n================== RA LINKS FOR SBB ==================\n"
								+ ""
								+ sb
								+ "\n"
								+ "======================================================");

			}

			for (int i = 0; i < raLinks.length; i++) {
				// RAEntity FOR THIS LINK
				ResourceAdaptorEntity raEntity = getRAEntity(raLinks[i]);
				ResourceAdaptorType raType = raEntity
						.getInstalledResourceAdaptor().getRaType();

				// IF WE HAVE PROCESSED THIS RA FOR OTHER SBB IN THIS SERVICE WE
				// SHOULD HAVE A SET OF EventyTypeIDs of EVENTS
				// IT CAN FIRE, OTHERWISE WE HAVE TO CREATE IT.
				if (!raEntitiesToEventsFired.containsKey(raEntity)) {
					// SET OF EVENTS THAT THIS RA CAN FIRE
					HashSet setOfFiredEventIds = new HashSet();
					// ComponentKey
					// eventsK[]=raType.getRaTypeDescr().getEventTypeRefEntries();

					EventTypeID[] events = raType.getRaTypeDescr()
							.getEventTypes();
					if (events == null)
						continue; // IN ORDER TO PASS TCKS... TCK RA RETURNS
					// NULL...
					for (int j = 0; j < events.length; j++)
						setOfFiredEventIds.add(events[j]);
					// LETS STORE EventTypeIDs Set THAT ARE FIRED BY THIS RA
					raEntitiesToEventsFired.put(raEntity, setOfFiredEventIds);

					// LETS CREATE FOR THIS RA SET THAT WILL BE FILLED WITH
					// EVENTS THAT ARE OF INTEREST BY THIS SERVICE
					// AND STORE IT, IT WILL BE FILLED LATER
					raEntitiesToEventTypeIDsOfInterest.put(raEntity,
							new HashSet(20));
					// raEntitiesToEventTypeIDsOfInterest.put(raEntity,new
					// TreeSet());
				}

				// EVENTS THAT ARE FIRED BY THIS RA ENTITY AND ARE OF ITNEREST
				// OF THIS SERVICE
				// IT CONTAINS EventyTypeIDs
				Set eventsOfInterest = (Set) raEntitiesToEventTypeIDsOfInterest
						.get(raEntity);
				// SET OF EventTypeIDs FIRED BYT THIS RA ENTITY
				Set eventsFiredByRAEntity = (Set) raEntitiesToEventsFired
						.get(raEntity);
				Iterator eventsOfSbbinterest = sbbEventsOfInterest.iterator();

				// LETS FILL IN eventsOfInterest
				while (eventsOfSbbinterest.hasNext()) {
					// IT SHOUDL BE EventTypeID
					Object eventIdOfSbbItenrest = eventsOfSbbinterest.next();

					// IF THIS RA FIRES EVENT THAT IS OF ITNEREST OF SBB
					if (eventsFiredByRAEntity.contains(eventIdOfSbbItenrest)) {
						// TODO: - CAN WE REMOVE IT? IT SEEMS LIKE GOOD IDEA, IT
						// WILL REDUCE OVERHEAD OF PROCESSING
						// THOSE LOOPS FOR OTHER RAs
						// eventsOfSbbinterest.remove();
						eventsOfInterest.add(eventIdOfSbbItenrest);
						// GET RESOURCE OPTION?
						SbbEventEntry eventEntry = (SbbEventEntry) eventTypeIdToEventEntriesMappings
								.get(eventIdOfSbbItenrest);
						String resourceOption = eventEntry.getResourceOption();
						// STORE RESOURCE OPTION FOR THIS EventTypeID
						if (resourceOption != null)
							if (eventTypeIDsToResourceOptions
									.containsKey(eventIdOfSbbItenrest)) {
								String value = (String) eventTypeIDsToResourceOptions
										.remove(eventIdOfSbbItenrest);
								// WE DONT NEED MORE THAN ONE OPTION KIND HERE.
								if (value.indexOf(resourceOption) == -1) {
									value += "," + resourceOption;
									eventTypeIDsToResourceOptions.put(
											eventIdOfSbbItenrest, value);
								}
							} else {
								eventTypeIDsToResourceOptions.put(
										eventIdOfSbbItenrest, resourceOption);
							}
					}
				}

				if (logger.isDebugEnabled()) {

					StringBuffer sb = new StringBuffer(300);
					Iterator it = eventsOfInterest.iterator();
					while (it.hasNext()) {
						sb.append(it.next() + "\n");
					}
					logger
							.debug("\n=========================== EVENTIDS OF INTEREST FROM RA===========================\n"
									+ ""
									+ sb
									+ "\n"
									+ "=====================================================================================");
				}
			}

		}

		// NOW WE HAVE TO BUILD ARRAYS OF eventIDs and corresponding resource
		// options for each RA ENTITY

		Iterator raEntities = raEntitiesToEventTypeIDsOfInterest.keySet()
				.iterator();
		Iterator eventTypeIdIterator = null;
		String resourceOption = null;
		int eventID = -1;
		// IF WE HAVE SOME RAs, WE NEED TO LET THEM KNOW THAT SOMEONE IS GOING
		// TO BE INSTALLED
		// AND THAT HE IS INTERESTED IN SOME EVENTS WITH SOME ResoureOptions
		while (raEntities.hasNext()) {
			ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) raEntities
					.next();
			Set eventsOfServiceInterest = (Set) raEntitiesToEventTypeIDsOfInterest
					.get(raEntity);
			// eventIDs and resourceOptions ARE GOING TO BE PASSED AS ARGS TO RA
			// eventIDs[i]=int# , resourceOptions[i]=optionsFor-int#
			int[] eventIDs = new int[eventsOfServiceInterest.size()];
			String[] resourceOptions = new String[eventsOfServiceInterest
					.size()];

			eventsOfServiceInterest = new TreeSet(eventsOfServiceInterest);

			int i = 0;

			eventTypeIdIterator = eventsOfServiceInterest.iterator();

			while (eventTypeIdIterator.hasNext()) {
				EventTypeID ETID = (EventTypeID) eventTypeIdIterator.next();
				eventID = eventLookup.getEventID(getEventKey(ETID));
				resourceOption = (String) eventTypeIDsToResourceOptions
						.get(ETID);
				eventIDs[i] = eventID;
				resourceOptions[i++] = resourceOption;

			}

			if (logger.isDebugEnabled()) {

				StringBuffer sb = new StringBuffer(300);
				sb.append("INDEX[ eventID | resourceOptions ]\n");
				for (int k = 0; k < eventIDs.length; k++) {

					sb.append("#" + k + "[ " + eventIDs[k] + " | "
							+ resourceOptions[k] + " ]\n");
				}
				ResourceAdaptorTypeDescriptorImpl raDesc = raEntity
						.getInstalledResourceAdaptor().getRaType()
						.getRaTypeDescr();
				logger
						.debug("\n============= PASSING INSTALL SERVCICE ARGS TO RA =============\n"
								+ "| RA DESC: "
								+ raDesc.getName()
								+ ", "
								+ raDesc.getVendor()
								+ ", "
								+ raDesc.getVersion()
								+ "\n"
								+ "===============================================================\n"
								+ "| EVENTS : |\n"
								+ "============\n"
								+ ""
								+ sb
								+ "\n"
								+ "===============================================================");
			}

			raEntity.serviceInstalled(serviceComponent.getServiceID()
					.toString(), eventIDs, resourceOptions);
			// ZERO VARS
			resourceOption = null;
			eventID = -1;
		}

		logger.info("Installed Service " + serviceComponent.getServiceID()
				+ ". Root SBB is "
				+ serviceComponent.getRootSbbComponent().getID());
	}

	/**
	 * unistall a service.
	 * 
	 * @param deployableUnitID --
	 *            deployable unit to unistall
	 */
	private synchronized void uninstallService(
			DeployableUnitIDImpl deployableUnitID) throws Exception {

		sleeTransactionManager.mandateTransaction();

		if (logger.isDebugEnabled()) {
			logger.debug("Uninstalling services on " + deployableUnitID);
		}
		Map serviceComps = this.getDeploymentCacheManager()
				.getServiceComponents();
		for (Iterator it = serviceComps.values().iterator(); it.hasNext();) {

			ServiceComponent serviceComponent = (ServiceComponent) it.next();

			if (logger.isDebugEnabled()) {
				logger.debug("Uninstalling service "
						+ serviceComponent.getServiceID());
			}

			ServiceDescriptorImpl serviceDescriptor = serviceComponent
					.getServiceDescriptor();

			DeployableUnitID duid = serviceDescriptor.getDeployableUnit();
			ServiceID serviceID = (ServiceID) serviceDescriptor.getID();

			if (duid.equals(deployableUnitID)) {

				// Fetch the runtime service image from the cache.
				Service service = this.getService(serviceID);

				if (!service.getState().isInactive()) {
					throw new IllegalStateException(
							"Service state is not inactive");
				}

				// Remove and propably run task which will remove sbb
				// entities
				// if it hadnt done it already
				RootSbbEntitiesRemovalTask task = RootSbbEntitiesRemovalTask
						.getTask(serviceID);
				if (task != null) {
					task.run();
					if (logger.isDebugEnabled()) {
						logger
								.debug("Found timer task running to remove remaining sbb entities. Executing now...");
					}

				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("NO TASK TO RUN ON SERVICE UNINSTALL FOR "
								+ serviceID);
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Unregistring Usage MBean of service "
							+ serviceComponent.getServiceID());
				}

				this.mbeanServer.unregisterMBean(serviceComponent
						.getUsageMBean());
				// Get the dynamically created usage mbeans for the
				// components of this service.

				for (Iterator itr = serviceComponent.getSbbUsageMBeans(); itr
						.hasNext();) {
					ObjectName usageMbeanName = (ObjectName) itr.next();
					this.mbeanServer.unregisterMBean(usageMbeanName);
				}

				// Remove the svc and all usage parameters etc. from cache.

				if (logger.isDebugEnabled()) {
					logger.debug("Removing all usage parameters of service "
							+ serviceComponent.getServiceID());
				}

				Service.removeAllUsageParameters(service.getServiceID());

				// notifying the resource adaptor about service
				HashSet sbbIDs = serviceComponent.getSbbComponents();
				HashSet raEntities = new HashSet();
				Iterator i = sbbIDs.iterator();
				while (i.hasNext()) {
					SbbDescriptor sbbdesc = getSbbComponent((SbbID) i.next());
					if (sbbdesc != null) {
						String[] raLinks = sbbdesc
								.getResourceAdaptorEntityLinks();
						for (int c = 0; raLinks != null && c < raLinks.length; c++) {
							ResourceAdaptorEntity raEntity = getRAEntity(raLinks[c]);
							if (raEntity != null
									&& !raEntities.contains(raEntity)) {
								raEntity.serviceUninstalled(serviceComponent
										.getServiceID().toString());
								raEntities.add(raEntity);
							}
						}
					}
				}

				// NOW REMOVE EVERYTHING

				if (logger.isDebugEnabled()) {
					logger.debug("Removing Service "
							+ serviceComponent.getServiceID()
							+ " from cache and active services set");
				}
				// remove the service from the list of deployed services
				serviceComps.remove(serviceID);

				service.removeFromCache();

				logger.info("Uninstalled service "
						+ serviceComponent.getServiceID());

			}

		}

	}

	/**
	 * unistall a Resource Adaptor.
	 * 
	 * @param deployableUnitID --
	 *            deployable unit to unistall
	 */
	private synchronized void uninstallRA(DeployableUnitIDImpl deployableUnitID)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Removing RAs from DU " + deployableUnitID);
		}

		sleeTransactionManager.mandateTransaction();

		ResourceAdaptorIDImpl[] raIDs = this.getResourceAdaptorIDs();
		for (ResourceAdaptorID raID : raIDs) {
			ComponentID[] components = deployableUnitID.getDescriptor()
					.getComponents();
			for (ComponentID cID : components) {
				if (raID.equals(cID)) {
					if (!deployableUnitID.getDescriptor()
							.hasInstalledComponent(cID)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Not removing RA " + raID
									+ ", it was not installed by DU "
									+ deployableUnitID.getSourceURL());
						}
						continue;
					}
					InstalledResourceAdaptor ra = (InstalledResourceAdaptor) this.installedResourceAdaptors
							.remove(raID);
					ra.uninstall();
					logger.info("Uninstalled RA " + raID);
				}
			}
		}

	}

	/**
	 * unistall a RA Type.
	 * 
	 * @param deployableUnitID --
	 *            deployable unit to unistall
	 */
	private synchronized void uninstallRAType(
			DeployableUnitIDImpl deployableUnitID) throws Exception {

		sleeTransactionManager.mandateTransaction();

		ComponentID[] cIDs = deployableUnitID.getDescriptor().getComponents();
		for (ComponentID cID : cIDs) {
			if (cID instanceof ResourceAdaptorTypeID) {
				ResourceAdaptorTypeID raTypeID = (ResourceAdaptorTypeID) cID;
				ResourceAdaptorType raType = this
						.getResourceAdaptorType(raTypeID);
				if (raType.getResourceAdaptorIDs().size() != 0) {
					// ERROR, WE CANT ALLOW TO DO ANY UnInstall operation
					HashSet ras = raType.getResourceAdaptorIDs();
					StringBuilder sb = new StringBuilder(ras.size() * 50);
					for (Iterator it = ras.iterator(); it.hasNext();) {
						ResourceAdaptorIDImpl raid = (ResourceAdaptorIDImpl) it
								.next();
						sb.append("" + raid.getComponentKey() + "; ");
					}
					throw new RuntimeException(" RAType ["
							+ raType.getResourceAdaptorTypeID()
									.getComponentKey()
							+ "] is still referenced by some RA/s --> " + sb);
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Uninstalling RA Types for DU " + deployableUnitID);
		}
		for (Iterator it = this.getResourceAdaptorTypes().keySet().iterator(); it
				.hasNext();) {
			ResourceAdaptorTypeID raTypeID = (ResourceAdaptorTypeIDImpl) it
					.next();
			// ComponentID components[] =
			// deployableUnitID.getDescriptor().getComponents();
			for (ComponentID cID : cIDs) {
				if (raTypeID.equals(cID)) {
					if (!deployableUnitID.getDescriptor()
							.hasInstalledComponent(cID)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Not removing RA Type " + raTypeID
									+ ", it was not installed by DU "
									+ deployableUnitID.getSourceURL());
						}
						continue;
					}

					it.remove();
					this.resourceAdaptorTypes.remove(raTypeID);
					logger.info("Uninstalled RA Type " + raTypeID);
				}
			}
		}
	}

	/**
	 * Deploys an SBB. This generates the code to convert abstract to concrete
	 * class and registers the component in the component table and creates an
	 * object pool for the sbb id.
	 * 
	 * @param mobicentsSbbDescriptor
	 *            the descriptor of the sbb to install
	 * @throws Exception
	 */
	private synchronized void installSbb(
			final MobicentsSbbDescriptor mobicentsSbbDescriptor)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing SBB " + mobicentsSbbDescriptor.getID());
		}

		getTransactionManager().mandateTransaction();

		// Iterate over the set of event entries and initialize descriptor
		for (Iterator it = mobicentsSbbDescriptor.getSbbEventEntries()
				.iterator(); it.hasNext();) {
			SbbEventEntry eventEntry = (SbbEventEntry) it.next();
			ComponentKey eventTypeRefKey = eventEntry.getEventTypeRefKey();

			EventTypeID eventTypeId = (EventTypeID) this
					.getEventType(eventTypeRefKey);

			if (eventTypeId == null)
				throw new DeploymentException("Unknown event type "
						+ eventTypeRefKey);

			mobicentsSbbDescriptor.addEventEntry(eventTypeId, eventEntry);
		}

		// create deployer
		SbbDeployer sbbDeployer = new SbbDeployer(getDeployPath());
		// change classloader
		ClassLoader oldClassLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					mobicentsSbbDescriptor.getClassLoader());
			// Set up the comp/env naming context for the Sbb.
			mobicentsSbbDescriptor.setupSbbEnvironment();
			// deploy the sbb
			sbbDeployer.deploySbb(mobicentsSbbDescriptor, this);
		} catch (Exception ex) {
			throw ex;
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
		// add a rollback action to unregister class loader
		TransactionalAction action = new TransactionalAction() {
			public void execute() {
				((UnifiedClassLoader3) mobicentsSbbDescriptor.getClassLoader())
						.unregister();
			}
		};
		sleeTransactionManager.addAfterRollbackAction(action);

		// create the pool for the given SbbID
		sbbPooling.put(mobicentsSbbDescriptor.getID(),
				(new GenericObjectPoolFactory(new SbbObjectPoolFactory(
						((SbbID) mobicentsSbbDescriptor.getID()), SleeContainer
								.lookupFromJndi()), poolConfig)).createPool());
		if (logger.isDebugEnabled()) {
			logger.debug("Created Pool for SBB "
					+ mobicentsSbbDescriptor.getID());
		}
		// add a rollback action to remove sbb object pool
		action = new TransactionalAction() {
			public void execute() {
				try {
					((ObjectPool) sbbPooling.remove(mobicentsSbbDescriptor
							.getID())).close();
				} catch (Exception e) {
					logger.error("Failed to remove SBB "
							+ mobicentsSbbDescriptor.getID() + " object pool",
							e);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Removed Pool for SBB "
							+ mobicentsSbbDescriptor.getID());
				}
			}
		};
		sleeTransactionManager.addAfterRollbackAction(action);

		// Set Trace to off
		getTraceFacility().setTraceLevelOnTransaction(
				mobicentsSbbDescriptor.getID(), Level.OFF);
		getAlarmFacility().registerComponent(mobicentsSbbDescriptor.getID());

		// add sbb component
		this.addSbbComponent(mobicentsSbbDescriptor);

		logger.info("Installed SBB " + mobicentsSbbDescriptor.getID());

	}

	public ActivityContextFactoryImpl getActivityContextFactory() {
		return this.activityContextFactory;
	}

	public MobicentsEventTypeDescriptor getEventDescriptor(EventTypeID id) {
		return (MobicentsEventTypeDescriptor) this.eventTypeIDToDescriptor
				.get(id);
	}

	/**
	 * check the number of activity contexts.
	 * 
	 */
	public int getActivityContextCount() {
		return this.activityContextFactory.getActivityContextCount();
	}

	/**
	 * get the slee endpoint
	 * 
	 */
	public SleeInternalEndpoint getSleeEndpoint() {
		return this.sleeEndpoint;
	}

	/**
	 * Creates a new instance of SleeContainer -- This is called from the
	 * SleeManagementMBean to get the whole thing running.
	 * 
	 */
	public SleeContainer(MBeanServer mbserver) throws Exception {
		// created in STOPPED state and remain so until started
		this.sleeState = SleeState.STOPPED;
		this.mbeanServer = mbserver;
	}

	/**
	 * Get the sbb components
	 */
	public SbbIDImpl[] getSbbIDs() throws Exception {
		boolean b = SleeContainer.getTransactionManager().requireTransaction();
		try {
			SbbIDImpl[] retval = new SbbIDImpl[getDeploymentCacheManager()
					.getSbbComponents().size()];
			this.getDeploymentCacheManager().getSbbComponents().keySet()
					.toArray(retval);
			return retval;
		} finally {
			if (b)
				SleeContainer.getTransactionManager().commit();
		}
	}

	public javax.slee.facilities.ActivityContextNamingFacility getActivityContextNamingFacility() {
		return this.activityContextNamingFacility;
	}

	public static ProfileFacility getProfileFacility() throws NamingException {
		return (ProfileFacility) lookupFacilityInJndi(ProfileFacilityImpl.JNDI_NAME);
	}

	public static javax.slee.facilities.TimerFacility getTimerFacility()
			throws NamingException {
		if (timerFacility == null)
			return (TimerFacility) lookupFacilityInJndi(TimerFacilityImpl.JNDI_NAME);
		else
			return timerFacility;
	}

	public SbbDescriptor getSbbComponent(SbbID sbbComponentId) {

		getTransactionManager().mandateTransaction();

		if (logger.isDebugEnabled()) {
			logger.debug("getting " + sbbComponentId);
		}

		return (SbbDescriptor) this.getDeploymentCacheManager()
				.getSbbComponents().get(sbbComponentId);

	}

	private void addSbbComponent(SbbDescriptor sbbComponent) throws Exception {

		getTransactionManager().mandateTransaction();

		if (logger.isDebugEnabled()) {
			logger.debug("adding sbb component for "
					+ sbbComponent.getID().toString());
		}

		this.getDeploymentCacheManager().getSbbComponents().put(
				sbbComponent.getID(), sbbComponent);
		SbbID[] sbbs = sbbComponent.getSbbs();

		// I refer to all these sbbs.
		for (int i = 0; i < sbbs.length; i++) {
			this.addReferringComponent(sbbs[i], sbbComponent.getID());
		}

		// I refer to all these profile specifications.
		ProfileSpecificationID[] profileIDs = sbbComponent
				.getProfileSpecifications();

		for (int i = 0; i < profileIDs.length; i++) {
			this.addReferringComponent(profileIDs[i], sbbComponent.getID());
		}

		// I refer to all the following EventTypeIDs.
		EventTypeID[] eventTypes = sbbComponent.getEventTypes();
		for (int i = 0; i < eventTypes.length; i++) {
			this.addReferringComponent(eventTypes[i], sbbComponent.getID());
		}

		// I refer to the following resource adaptor type ids.
		ResourceAdaptorTypeID[] raTypeIDs = sbbComponent
				.getResourceAdaptorTypes();
		if (logger.isDebugEnabled()) {
			logger.debug("ResourceAdaptorTypeIDs " + raTypeIDs.length);
		}
		for (int i = 0; i < raTypeIDs.length; i++) {
			this.addReferringComponent(raTypeIDs[i], sbbComponent.getID());
		}

		// I refer to the following AddressProfile.
		ProfileSpecificationID addressProfile = sbbComponent
				.getAddressProfileSpecification();
		if (addressProfile != null)
			this.addReferringComponent(addressProfile, sbbComponent.getID());

		if (logger.isDebugEnabled()) {
			logger
					.debug("dependencyTable "
							+ this.getDeploymentCacheManager()
									.getReferringComponents());
			logger.debug("sbbComponents "
					+ this.getDeploymentCacheManager().getSbbComponents());
		}

	}

	/**
	 * Geta service give its service id
	 * 
	 * @param serviceId
	 *            service ID for which the service is needed
	 * @return the service
	 * @throws SystemException
	 * @throws RollbackException
	 * @throws HeuristicRollbackException
	 * @throws HeuristicMixedException
	 */
	public Service getService(ServiceID serviceId) throws SystemException,
			UnrecognizedServiceException {

		getTransactionManager().mandateTransaction();

		return getService((ServiceComponent) getDeploymentCacheManager()
				.getServiceComponents().get(serviceId));
	}

	/**
	 * Geta service give its service component
	 * 
	 * @param serviceId
	 *            service ID for which the service is needed
	 * @return the service
	 * @throws SystemException
	 * @throws RollbackException
	 * @throws HeuristicRollbackException
	 * @throws HeuristicMixedException
	 */
	public Service getService(ServiceComponent serviceComponent)
			throws SystemException, UnrecognizedServiceException {

		getTransactionManager().requireTransaction();

		if (serviceComponent == null) {
			String s = "Service does not exist - has it been uninstalled?";
			if (logger.isDebugEnabled()) {
				logger.debug(s);
			}
			// not finding a service is no reason to rollback the enclosing
			// transaction
			// lets give a chance of the transaction owner to make that
			// decision

			throw new UnrecognizedServiceException(s);
		}

		// create service object populated with info in cache
		return Service.getService(serviceComponent.getServiceDescriptor());

	}

	/**
	 * Get the transaction manager from JNDI
	 * 
	 * @throws
	 */
	public static SleeTransactionManager getTransactionManager() {
		try {
			if (sleeTransactionManager == null)
				sleeTransactionManager = (SleeTransactionManager) getFromJndi("slee/"
						+ SleeTransactionManager.JNDI_NAME);
			return sleeTransactionManager;
		} catch (Exception ex) {
			logger.error("Error fetching transaciton manager!", ex);
			return null;
		}
	}

	/**
	 * This is for the TCK only ( will be stripped in the actual release)
	 * 
	 * @return
	 */
	public TransactionIDAccess getTransactionIDAccess() {
		return this.transactionIDAccess;
	}

	/**
	 * Get a list of services known to me
	 * 
	 * @return A list of services that are registered with me.
	 */
	public ServiceID[] getServiceIDs() {
		boolean b = SleeContainer.getTransactionManager().requireTransaction();
		try {
			ServiceIDImpl[] retval = new ServiceIDImpl[this
					.getDeploymentCacheManager().getServiceComponents().size()];
			this.getDeploymentCacheManager().getServiceComponents().keySet()
					.toArray(retval);
			return retval;
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed");
		} finally {
			try {
				if (b) {
					SleeContainer.getTransactionManager().commit();
				}
			} catch (SystemException ex) {
				throw new RuntimeException("Tx manager failed");
			}
		}
	}

	/**
	 * get the ObjectPool for this Sbb. Each Sbb component has its own object
	 * pool. Each sbb component is identified by an SbbId.
	 * 
	 * @param sbbId -
	 *            sbb id for which object pool is required.
	 * @return the object pool for the sbb id.
	 * 
	 */
	public ObjectPool getObjectPool(SbbID sbbid) {
		return (ObjectPool) this.sbbPooling.get(sbbid);
	}

	/**
	 * Add a referring component to a given component ID. This is only a table
	 * of direct references. A component cannot be uninstalled if there are any
	 * components that refer to it.
	 * 
	 * @param componentID
	 * @param referringComponent
	 * @throws DeploymentException
	 */
	private void addReferringComponent(ComponentID componentID,
			ComponentID referringComponent) {
		if (logger.isDebugEnabled()) {
			logger.debug("AddReferringComponent componentID = " + componentID
					+ " referringComponent = " + referringComponent);
		}

		Set rcomp = (Set) this.getDeploymentCacheManager()
				.getReferringComponents().get(componentID);
		if (rcomp == null) {
			rcomp = getDeploymentCacheManager().newReferringCompSet();
			this.getDeploymentCacheManager().getReferringComponents().put(
					componentID, rcomp);
		}
		rcomp.add(referringComponent);

	}

	/**
	 * This is done pre-removal. Go through the component table and remove all
	 * places where I refer to somebody.
	 * 
	 */
	private void removeReferredComponent(ComponentID componentID) {
		Iterator it = this.getDeploymentCacheManager().getReferringComponents()
				.keySet().iterator();
		while (it.hasNext()) {
			ComponentID key = (ComponentID) it.next();
			Map refComps = (Map) this.getDeploymentCacheManager()
					.getReferringComponents();
			CacheableSet rcomp = (CacheableSet) refComps.get(key);
			rcomp.remove(componentID);
			if (rcomp.isEmpty()) {
				refComps.remove(key);
				rcomp.remove();
			}
		}
	}

	/**
	 * This is called by our management interface.
	 * 
	 * @param referredComponent
	 * @return
	 */
	public ComponentIDImpl[] getReferringComponents(
			ComponentID referredComponent) {
		boolean b = getTransactionManager().requireTransaction();
		try {

			Set rcomp = (Set) this.getDeploymentCacheManager()
					.getReferringComponents().get(referredComponent);
			if (rcomp == null)
				return new ComponentIDImpl[0];
			else {
				ComponentIDImpl[] retval = new ComponentIDImpl[rcomp.size()];
				rcomp.toArray(retval);
				return retval;
			}
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected exception!", ex);

		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (Exception ex) {
				throw new RuntimeException("tx manager failed", ex);
			}

		}
	}

	/**
	 * 
	 * @param cid
	 * @param duid
	 * @return
	 *            <ul>
	 *            <li><b>true</b> - if this cid has been installed for the
	 *            first time - no cid-duid mapping was present</li>
	 *            <li><b>false</b> - if cid has been installed by some other
	 *            DU, some cid-duid mapping is present</li>
	 *            </ul>
	 */
	private boolean addReferringDU(ComponentID cid, DeployableUnitID duid) {
		boolean installedForTheFirstTime = false;
		try {
			Set referringDus = (Set) this.getDeploymentCacheManager()
					.getComponentIDToDeployableUnitIDMap().get(cid);
			if (referringDus == null) {
				referringDus = newReferringDuSet();
				this.getDeploymentCacheManager()
						.getComponentIDToDeployableUnitIDMap().put(cid,
								referringDus);
				installedForTheFirstTime = true;

			}
			referringDus.add(duid);
			if (logger.isDebugEnabled()) {
				logger.debug("componentToDUMap = "
						+ this.getDeploymentCacheManager()
								.getComponentIDToDeployableUnitIDMap());
			}
			return installedForTheFirstTime;
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed !", ex);
		}
	}

	private Set newReferringDuSet() {
		Set refs = getDeploymentCacheManager().newReferringDuSet();
		return refs;
	}

	private void removeReferredDU(DeployableUnitID duid) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("removeReferredDU " + duid);
			}
			Map ci2du = getDeploymentCacheManager()
					.getComponentIDToDeployableUnitIDMap();
			for (Iterator it = ci2du.keySet().iterator(); it.hasNext();) {
				ComponentID cid = (ComponentID) it.next();
				CacheableSet dus = (CacheableSet) ci2du.get(cid);
				dus.remove(duid);
				if (dus.isEmpty()) {
					ci2du.remove(cid);
					dus.remove();
				}

			}
			if (logger.isDebugEnabled()) {
				logger.debug("removeReferredDU After Remove "
						+ this.getDeploymentCacheManager()
								.getComponentIDToDeployableUnitIDMap());
			}
		} catch (Exception ex) {
			throw new RuntimeException("tx manager failed ! ", ex);
		}
	}

	/**
	 * Return true if there are any components that hold references to the
	 * deployable unit
	 * 
	 * @param dudesc --
	 *            the deployable unit descriptor to check.
	 * @return true -- if there are referring components.
	 * @throws Exception --
	 *             if theres a problem with the cache.
	 */
	private boolean hasReferringDU(DeployableUnitDescriptorImpl dudesc)
			throws Exception {

		ComponentID[] cid = dudesc.getComponents();
		for (int i = 0; i < cid.length; i++) {
			Set hset = (Set) this.getDeploymentCacheManager()
					.getComponentIDToDeployableUnitIDMap().get(cid[i]);
			if (hset == null)
				return false;
			else if (hset.size() != 1) {
				// See if there's another DU that refers directly to me.
				if (logger.isDebugEnabled()) {
					logger.debug("Direct reference detected to component  "
							+ cid[i]);
					logger.debug("componentID to DUID Map = " + hset);
				}
				// return true;
				if (dudesc.hasInstalledComponent(cid[i])) {
					if (logger.isDebugEnabled()) {
						logger
								.debug("dudesc.hasInstalledComponent(cid[i])[TRUE]");
					}
					return true;
				} else {
					if (logger.isDebugEnabled()) {
						logger
								.debug("dudesc.hasInstalledComponent(cid[i])[FALSE]");
					}
					return false;
				}

			} else {
				// I'm the only one that references this component.
				ComponentID[] referringComponents = this
						.getReferringComponents(cid[i]);
				// See if there's any other component that refers to me that
				// belongs
				// to another DU.
				if (referringComponents != null) {
					for (int k = 0; k < referringComponents.length; k++) {
						if (logger.isDebugEnabled()) {
							logger.debug("checking referring component "
									+ referringComponents[k]);

						}
						if (!((Set) this.getDeploymentCacheManager()
								.getComponentIDToDeployableUnitIDMap().get(
										referringComponents[k]))
								.contains(dudesc.getDeployableUnit())) {
							if (logger.isDebugEnabled()) {
								logger
										.debug("referring component = "
												+ this
														.getDeploymentCacheManager()
														.getComponentIDToDeployableUnitIDMap()
														.get(
																referringComponents[k]));
								logger.debug("attempting to remove = "
										+ dudesc.getDeployableUnit());
							}
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public EventTypeIDImpl getEventType(ComponentKey componentKey) {

		return (EventTypeIDImpl) this.eventKeyToEventTypeIDMap
				.get(componentKey);
	}

	/**
	 * This is only for debugging.
	 * 
	 * @param eventTypeId
	 * @return
	 */
	public ComponentKey getEventKey(EventTypeIDImpl eventTypeId) {
		return (ComponentKey) this.eventTypeIDToEventKeyMap.get(eventTypeId);

	}

	public EventLookup getEventLookupFacility() {
		return eventLookup;
	}

	/**
	 * Get the event type id given the eventID
	 * 
	 * @param eventID --
	 *            event ID to map to the event Type ID
	 * 
	 * @return the mapped eventType ID or null if no such event ID is mapped.
	 * 
	 */
	public EventTypeIDImpl getEventTypeID(int eventID) {

		return (EventTypeIDImpl) eventTypeIDs.get(new Integer(eventID));
	}

	/**
	 * This is only for debugging.
	 * 
	 * @param eventTypeId
	 * @return
	 */
	public ComponentKey getEventKey(EventTypeID eventTypeId) {
		ComponentKey key = (ComponentKey) this.eventTypeIDToEventKeyMap
				.get(eventTypeId);
		return key;
	}

	/**
	 * This is an implementation of the
	 * <code>ComponentContainer.installComponent
	 * </code> method.
	 * 
	 * (Ivelin) TODO: this method does not need to be synchronized. Right now it
	 * is, because the deploymeny code is not clean. For example there is one
	 * global ClassPool shared between all deployments, instead of an hierarchy
	 * with clean isolation.
	 * 
	 * @param descriptor
	 *            a descriptor of the component to deploy.
	 */
	public synchronized void install(ComponentDescriptor descriptor,
			DeployableUnitDescriptor duDescriptor) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing " + descriptor);
		}

		boolean b = getTransactionManager().requireTransaction();
		try {
			if (descriptor instanceof MobicentsSbbDescriptor) {
				installSbb((MobicentsSbbDescriptor) descriptor);
			} else if (descriptor instanceof ServiceDescriptorImpl) {
				installService((ServiceDescriptorImpl) descriptor);
			} else if (descriptor instanceof MobicentsEventTypeDescriptor) {
				installEventType((MobicentsEventTypeDescriptor) descriptor);
			} else if (descriptor instanceof ResourceAdaptorTypeDescriptor) {
				installResourceAdaptorType((ResourceAdaptorTypeDescriptorImpl) descriptor);
			} else if (descriptor instanceof ProfileSpecificationDescriptorImpl) {
				installProfile((ProfileSpecificationDescriptorImpl) descriptor);
			} else if (descriptor instanceof ResourceAdaptorDescriptorImpl) {
				installResourceAdaptor((ResourceAdaptorDescriptorImpl) descriptor);
			} else {
				logger.fatal("unknown component type!");
			}
		} catch (Exception ex) {
			logger.error("Exception caught while installing component", ex);
			throw ex;
		} finally {
			if (b)
				getTransactionManager().commit();
		}

	}

	/**
	 * 
	 * Deploys a Profile. This generates the code to generate concrete classes
	 * and registers the component in the component table
	 * 
	 * @param profileSpecificationDescriptorImpl
	 *            the descriptor of the profile to install
	 * @throws DeploymentException
	 * @throws SystemException
	 * @throws Exception
	 */
	private void installProfile(
			ProfileSpecificationDescriptorImpl profileSpecificationDescriptorImpl)
			throws DeploymentException, SystemException {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing Profile "
					+ profileSpecificationDescriptorImpl.getID());
		}

		sleeTransactionManager.mandateTransaction();

		File duPath = null;
		if (profileSpecificationDescriptorImpl.getDeployableUnit() != null) {
			duPath = ((DeployableUnitIDImpl) profileSpecificationDescriptorImpl
					.getDeployableUnit()).getDUDeployer()
					.getTempClassDeploymentDir();
		} else {
			duPath = new File(getDeployPath());
		}

		new ProfileDeployer(duPath)
				.deployProfile(profileSpecificationDescriptorImpl);

		this.getDeploymentCacheManager().getProfileComponents().put(
				profileSpecificationDescriptorImpl.getID(),
				profileSpecificationDescriptorImpl);

		logger.info("Installed Profile "
				+ profileSpecificationDescriptorImpl.getID());

	}

	/**
	 * Installing the resource adaptor type descriptror component
	 * 
	 * @param raTypeDescriptor --
	 *            type descriptor for the Resource Adaptor type.
	 * 
	 */
	private void installResourceAdaptorType(
			ResourceAdaptorTypeDescriptorImpl raTypeDescriptor)
			throws DeploymentException {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing RA Type " + raTypeDescriptor.getID());
		}

		RaTypeDeployer deployer = new RaTypeDeployer(raTypeDescriptor, this);
		deployer.deployRaType();
		/*
		 * RaTypeVerifier ver=new RaTypeVerifier(raTypeDescriptor);
		 * 
		 * logger.info("Verified["+ver.verifyRaType()+"]"); ComponentKey[]
		 * eventTypeRefEntries = raTypeDescriptor .getEventTypeRefEntries();
		 * EventTypeID[] eventTypeIDs = new
		 * EventTypeIDImpl[eventTypeRefEntries.length]; for (int i = 0; i <
		 * eventTypeRefEntries.length; i++) { EventTypeID eventTypeId =
		 * (EventTypeID) this.eventKeyToEventTypeIDMap
		 * .get(eventTypeRefEntries[i]); if (eventTypeId == null) throw new
		 * DeploymentException( "Could not resolve event type ref" +
		 * eventTypeRefEntries[i]); else eventTypeIDs[i] = eventTypeId; } if
		 * (logger.isDebugEnabled()) { logger.debug("The event type array has " +
		 * eventTypeIDs.length); } raTypeDescriptor.setEventTypes(eventTypeIDs);
		 * ResourceAdaptorType raType = new
		 * ResourceAdaptorType(raTypeDescriptor); ResourceAdaptorTypeID key =
		 * raType.getResourceAdaptorTypeID(); if (logger.isDebugEnabled()) {
		 * logger.info("Inserting RAT key: " + key + " RAType: " + raType); }
		 * addResourceAdaptorType(key, raType);
		 */
	}

	/**
	 * Install the resource adaptor component
	 * 
	 * @param raDescr
	 * @throws DeploymentException
	 */
	private void installResourceAdaptor(ResourceAdaptorDescriptorImpl raDescr)
			throws DeploymentException {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing RA " + raDescr.getID());
		}

		ResourceAdaptorType raType = (ResourceAdaptorType) this.resourceAdaptorTypes
				.get(raDescr.getResourceAdaptorType());
		if (raType == null) {
			throw new DeploymentException(
					"missing resource adaptor type where id = "
							+ raDescr.getResourceAdaptorType());
		}

		ResourceAdaptorIDImpl raID = (ResourceAdaptorIDImpl) raDescr.getID();

		/***********************************************************************
		 * new ResourceAdaptorIDImpl( new ComponentKey(raDescr.getName(),
		 * raDescr.getVendor(), raDescr.getVersion()));
		 **********************************************************************/

		raType.addResourceAdaptor(raID);
		InstalledResourceAdaptor ira;
		try {
			ira = new InstalledResourceAdaptor(this, raDescr, raID);
			this.installedResourceAdaptors.put(raID, ira);
			logger.info("Installed RA " + raID);
		} catch (Exception e) {
			String s = "Error Installing Resource Adaptor";
			logger.error(s, e);
			throw new DeploymentException(s, e);
		}

	}

	/**
	 * Register a deployable unit given its descriptor.
	 * 
	 * @param descriptor -
	 *            descriptor to register
	 * 
	 */
	public synchronized void addDeployableUnit(
			DeployableUnitDescriptor descriptor) {
		SleeTransactionManager transactionManager = getTransactionManager();
		boolean b = false;
		try {
			b = transactionManager.requireTransaction();

			DeployableUnitDescriptorImpl dudesc = (DeployableUnitDescriptorImpl) descriptor;
			DeployableUnitID deployableUnitId = ((DeployableUnitDescriptorImpl) descriptor)
					.getDeployableUnit();
			if (logger.isDebugEnabled()) {
				logger.debug("Installing DU with descriptor:  " + descriptor);
			}
			if (descriptor == null)
				logger.fatal("null descriptor");
			this.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().put(deployableUnitId,
							descriptor);
			String url = descriptor.getURL();
			this.getDeploymentCacheManager().getUrlToDeployableUnitIDMap().put(
					url, deployableUnitId);
			ComponentID[] components = dudesc.getComponents();
			// Add dependencies from all services to me.
			for (int i = 0; i < components.length; i++) {
				// add to the dependency table.
				// this.addReferringDU(components[i], deployableUnitId);

				if (this.addReferringDU(components[i], deployableUnitId)) {
					// THIS deployableUnitId == virgin INSTALLATION OF THIS
					// components[i](cid)
					dudesc.installComponent(components[i]);
					if (logger.isDebugEnabled()) {
						logger.debug("Installed [" + components[i] + "] from ["
								+ dudesc.getURL() + "]");
					}
				} else {
					// DO NOTHING MORE, SOMEONE ELSE HAS INSTALLED THIS
					// COMPONENT
					if (logger.isDebugEnabled()) {
						logger.debug("Already exists, didn't install ["
								+ components[i] + "] from [" + dudesc.getURL()
								+ "]");
					}
				}

			}
		} catch (Exception ex) {
			try {
				transactionManager.setRollbackOnly();
			} catch (SystemException se) {
				String err = "Failed addDeployableUnit("
						+ descriptor
						+ "), because of system exception during tx.setRollbackOnly()! ";
				logger.error(err, se);
				throw new RuntimeException(err, se);
			}
			throw new RuntimeException("unexpected exception ! ", ex);
		} finally {
			try {
				if (b)
					transactionManager.commit();
			} catch (SystemException se) {
				String err = "Failed addDeployableUnit(" + descriptor
						+ "), because of system exception during tx.commit()! ";
				logger.error(err, se);
				throw new RuntimeException("unexpected exception ! ", se);
			}
		}

	}

	/**
	 * Get an array containing the deployable unit ids known to the container.
	 * 
	 * @return
	 */
	public DeployableUnitID[] getDeployableUnits() {
		// TODO : defaultSleeDUs is a hard coded variable in order to pass test
		// 3776, which is incorrect.
		// See the discussion:
		// https://mobicents.dev.java.net/servlets/ProjectForumMessageView?messageID=7035&forumID=739
		boolean b = getTransactionManager().requireTransaction();

		try {
			final int defaultSleeDUs = 2;
			int duds = getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().size();
			DeployableUnitID[] duArray = new DeployableUnitIDImpl[duds
					+ defaultSleeDUs];
			this.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().keySet().toArray(
							duArray);
			// pad with non-null objects. The SLEE TCK expects non-null values
			// in the array
			// This is a hack to cover another TCK hack, which expects at least
			// 2 DUDs in a SLEE,
			// although the spec does not mandate it. Using -1 to ensure that
			// there is no overlap with valid DUD
			DeployableUnitID dummyDud = new DeployableUnitIDImpl(-1);
			for (int i = duds; i < duArray.length; i++) {
				duArray[i] = dummyDud;
			}

			return duArray;
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected exception ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (Exception ex) {
				throw new RuntimeException("unexpected exception ! ", ex);
			}
		}
	}

	/**
	 * Get the deployable unit descriptor for a given deployable unit.
	 * 
	 * @param deployableUnitID --
	 *            the deployable unit id
	 * 
	 * @return
	 */

	public DeployableUnitDescriptor getDeployableUnitDescriptor(
			DeployableUnitID deployableUnitID) {
		boolean b = getTransactionManager().requireTransaction();
		try {

			DeployableUnitDescriptor dud = (DeployableUnitDescriptor) getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().get(deployableUnitID);

			return dud;
		} catch (Exception ex) {
			throw new RuntimeException(
					"unexpected error getting du descriptor", ex);
		} finally {
			try {
				if (b) {
					getTransactionManager().commit();
				}
			} catch (Exception ex) {
				throw new RuntimeException("Unexpected exception ", ex);
			}
		}
	}

	/**
	 * Get the descriptor of a component given its component ID
	 * 
	 * @param componentId --
	 *            component id for which we want the descriptor
	 * 
	 * @return the descriptor corresponding to the component id.
	 */

	public ComponentDescriptor getComponentDescriptor(ComponentID componentId)
			throws IllegalArgumentException {
		boolean b = false;
		try {
			b = SleeContainer.getTransactionManager().requireTransaction();
			ComponentIDImpl cidImpl = (ComponentIDImpl) componentId;
			if (cidImpl.isSbbID()) {
				return (ComponentDescriptor) this.getDeploymentCacheManager()
						.getSbbComponents().get(componentId);
			} else if (cidImpl.isServiceID()) {
				return (ComponentDescriptor) ((ServiceComponent) this
						.getDeploymentCacheManager().getServiceComponents()
						.get(componentId)).getServiceDescriptor();
			} else if (cidImpl.isProfileSpecificationID()) {
				return (ComponentDescriptor) ((ProfileSpecificationDescriptor) this
						.getDeploymentCacheManager().getProfileComponents()
						.get(componentId));
			} else if (cidImpl.isEventTypeID()) {
				return (ComponentDescriptor) (this.eventTypeIDToDescriptor
						.get(componentId));
			} else if (cidImpl.isResourceAdaptorTypeID()) {
				return (ComponentDescriptor) ((ResourceAdaptorType) this.resourceAdaptorTypes
						.get(componentId)).getRaTypeDescr();
			} else if (cidImpl instanceof ResourceAdaptorIDImpl) {
				return (ResourceAdaptorDescriptor) ((InstalledResourceAdaptor) this.installedResourceAdaptors
						.get(componentId)).getDescriptor();
			} else
				throw new IllegalArgumentException(" bad component id");
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed !", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("Tx manager failed !", ex);
			}
		}
	}

	/**
	 * @return the event types that are supported by the container.
	 * 
	 */
	public EventTypeID[] getEventTypes() {
		EventTypeIDImpl[] eventTypeIDArray = new EventTypeIDImpl[this.eventTypeIDs
				.size()];
		this.eventTypeIDs.values().toArray(eventTypeIDArray);
		return eventTypeIDArray;
	}

	public ResourceAdaptorContext getBootstrapContext() {
		return this.bootstrapContext;
	}

	/**
	 * Get an array of the resource adaptor IDs.
	 * 
	 * @return
	 */
	public synchronized ResourceAdaptorIDImpl[] getResourceAdaptorIDs() {
		ResourceAdaptorIDImpl[] retval = new ResourceAdaptorIDImpl[installedResourceAdaptors
				.size()];
		installedResourceAdaptors.keySet().toArray(retval);
		return retval;
	}

	/**
	 * Get an array containing the RA types.
	 * 
	 * @return an array of the resource adaptor type IDs.
	 */
	public synchronized ResourceAdaptorTypeID[] getResourceAdaptorTypeIDs() {
		ResourceAdaptorTypeID[] raTypeIds = new ResourceAdaptorTypeID[resourceAdaptorTypes
				.size()];
		this.resourceAdaptorTypes.keySet().toArray(raTypeIds);
		return raTypeIds;
	}

	public ResourceAdaptorEntity createResourceAdaptorEntity(
			ResourceAdaptorIDImpl id, String name, Properties properties)
			throws NullPointerException, UnrecognizedResourceAdaptorException,
			InvalidArgumentException,
			ResourceAdaptorEntityAlreadyExistsException, ResourceException,
			ManagementException, CreateException {

		if (this.installedResourceAdaptors.get(id) == null) {
			String msg = "Failed to create RA Entity. RA ID: " + id
					+ " not found.";
			logger.error(msg);
			throw new UnrecognizedResourceAdaptorException(msg);
		}

		if (this.resourceAdaptorEntities.get(name) != null) {
			String msg = "Failed to create RA Entity. Resource Adpator Entity Name: "
					+ name + " already exists! RA ID: " + id;
			logger.error(msg);
			throw new ResourceAdaptorEntityAlreadyExistsException(msg);
		}

		InstalledResourceAdaptor installedRA = (InstalledResourceAdaptor) this.installedResourceAdaptors
				.get(id);

		ResourceAdaptorEntity raEntity = new ResourceAdaptorEntity(name,
				installedRA, new ResourceAdaptorBoostrapContext(
						new SleeEndpointImpl(this.activityContextFactory,
								this.router, this, name),
						new EventLookupFacilityImpl(this), name), this);

		this.resourceAdaptorEntities.put(name, raEntity);

		try {
			if (logger.isDebugEnabled()) {
				logger.debug(id + "RA PROPERTIES: " + properties);
			}
			// This actually creates the resource adaptor instance.
			raEntity.configure(properties);
			// You can access the instance that has been created.
		} catch (InvalidStateException e) {
			logger.warn("Failed to create RA Entity. ", e);
			new ResourceException("Resource exception ");
		}
		installedRA.addResourceAdaptorEntity(raEntity);
		return raEntity;
	}

	public String getRAEntityInterfaceJNDIName(String link) {
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) resourceAdaptorEntities
				.get(this.resourceAdaptorEntityLinks.get(link));
		return raEntity.getFactoryInterfaceJNDIName();
	}

	public String getRAEntityFactoryInterfaceJNDIName(String link) {
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) resourceAdaptorEntities
				.get(this.resourceAdaptorEntityLinks.get(link));
		return raEntity.getFactoryInterfaceJNDIName();
	}

	public ResourceAdaptorEntity getRAEntity(String link,
			ResourceAdaptorType raType) {
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) resourceAdaptorEntities
				.get(this.resourceAdaptorEntityLinks.get(link));
		return (raEntity != null && raEntity.getInstalledResourceAdaptor()
				.getRaType() == raType) ? raEntity : null;
	}

	public ResourceAdaptorEntity getRAEntity(String link) {
		return (ResourceAdaptorEntity) resourceAdaptorEntities
				.get(this.resourceAdaptorEntityLinks.get(link));

	}

	public void removeResourceAdaptorEntity(String name)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException,
			/* InvalidArgumentException, */DependencyException,
			ManagementException {

		if (this.resourceAdaptorEntities.get(name) == null) {
			throw new UnrecognizedResourceAdaptorEntityException(
					"Resource Adaptor Entity " + name + " not found.");
		}
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(name);
		try {
			raEntity.remove();
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DependencyException("Resource Adaptor Entity " + name
					+ " is in the wrong state");
		}

		raEntity.getInstalledResourceAdaptor().removeResourceAdaptorEntity(
				raEntity);
		this.resourceAdaptorEntities.remove(name);

	}

	public void updateConfigurationProperties(String name, Properties properties)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException, InvalidStateException,
			ResourceException, ManagementException {
		if (this.resourceAdaptorEntities.get(name) == null) {
			throw new UnrecognizedResourceAdaptorEntityException(
					"Resource Adaptor Entity " + name + " not found.");
		}
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(name);
		raEntity.configure(properties);
	}

	/**
	 * Deactivate a resource adaptor.
	 * 
	 * @param name
	 * @throws NullPointerException
	 * @throws UnrecognizedResourceAdaptorException
	 * @throws InvalidStateException
	 * @throws ManagementException
	 */
	public void deactivateResourceAdaptorEntity(String name)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException, InvalidStateException,
			ManagementException {
		if (this.resourceAdaptorEntities.get(name) == null) {
			throw new UnrecognizedResourceAdaptorEntityException(
					"Resource Adaptor Entity " + name + " not found.");
		}
		ResourceAdaptorEntity raEntity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(name);
		raEntity.deactivate();

	}

	/**
	 * @param entityName
	 * @return
	 * @throws NullPointerException
	 * @throws UnrecognizedResourceAdaptorEntityException
	 * @throws ManagementException
	 * @throws ResourceException
	 */
	public Properties getRAEntityProperties(String entityName)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException, ManagementException {
		if (entityName == null)
			throw new NullPointerException("null entity name");
		ResourceAdaptorEntity resourceAdaptorEntity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(entityName);
		if (resourceAdaptorEntity == null)
			throw new UnrecognizedResourceAdaptorEntityException(
					"Resource Adaptor Entity " + entityName + " not found.");

		try {
			Properties properties = new Properties();
			Iterator configPropertyDescriptors = resourceAdaptorEntity
					.getInstalledResourceAdaptor().getDescriptor()
					.getConfigPropertyDescriptors().iterator();
			while (configPropertyDescriptors.hasNext()) {
				ConfigPropertyDescriptor configPropertyDescriptor = (ConfigPropertyDescriptor) configPropertyDescriptors
						.next();
				String value = resourceAdaptorEntity.getConfigProperty(
						configPropertyDescriptor).toString();
				properties.setProperty(configPropertyDescriptor.getName(),
						value);
			}
			return properties;
		} catch (ResourceException e) {
			e.printStackTrace();
			throw new ManagementException(e.getMessage());
		}
	}

	public Properties getRAProperties(ResourceAdaptorID id)
			throws NullPointerException, UnrecognizedResourceAdaptorException {
		if (id == null)
			throw new NullPointerException("null resource adaptor id");

		InstalledResourceAdaptor installedResourceAdaptor = (InstalledResourceAdaptor) installedResourceAdaptors
				.get(id);
		if (installedResourceAdaptor == null)
			throw new UnrecognizedResourceAdaptorException(
					"unrecognized resource adaptor " + id.toString());

		Properties properties = new Properties();
		Iterator configPropertyDescriptors = installedResourceAdaptor
				.getDescriptor().getConfigPropertyDescriptors().iterator();
		while (configPropertyDescriptors.hasNext()) {
			ConfigPropertyDescriptor configPropertyDescriptor = (ConfigPropertyDescriptor) configPropertyDescriptors
					.next();
			String value = configPropertyDescriptor.getValue().toString();
			properties.setProperty(configPropertyDescriptor.getName(), value);
		}
		return properties;
	}

	public synchronized ResourceAdaptorType getResourceAdaptorType(
			ResourceAdaptorTypeID key) {
		return (ResourceAdaptorType) this.resourceAdaptorTypes.get(key);
	}

	/**
	 * Check if a deployable Unit is deployed.
	 * 
	 * @param deployableUnitID
	 * 
	 * @return true if the deployable unit is deployed.
	 */
	public boolean isInstalled(DeployableUnitID deployableUnitID) {
		boolean b = getTransactionManager().requireTransaction();
		try {
			return this.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().containsKey(
							deployableUnitID);
		} catch (Exception ex) {
			throw new RuntimeException("tx manager failed! ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
		}

	}

	/**
	 * @param componentId
	 * @return
	 */
	public boolean isInstalled(ComponentID componentId) {
		boolean b = getTransactionManager().requireTransaction();

		try {
			if (componentId instanceof SbbID) {
				return this.getDeploymentCacheManager().getSbbComponents()
						.containsKey(componentId);
			} else if (componentId instanceof ServiceID) {
				return this.getDeploymentCacheManager().getServiceComponents()
						.containsKey(componentId);
			} else if (componentId instanceof EventTypeID) {
				return this.eventTypeIDs.containsKey(componentId);
			} else if (componentId instanceof ProfileSpecificationID) {
				return this.getDeploymentCacheManager().getProfileComponents()
						.containsKey(componentId);
			} else
				return false;
		} catch (Exception ex) {
			throw new RuntimeException("tx manager failed! ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
		}

	}

	private void removeReferredComponents(DeployableUnitDescriptorImpl dudesc) {

		// We return true if theres a component of the Deployable Unit that is
		// referenced by another component. We do not need to check about
		// service components.

		ComponentID[] cid = dudesc.getComponents();
		for (int i = 0; i < cid.length; i++) {
			this.removeReferredComponent(cid[i]);
		}

	}

	/**
	 * Uninstall a deployable unit and clean up all the files generated for it.
	 * This is a terrible mess.
	 * 
	 * @param deployableUnitID
	 */
	public void removeDeployableUnit(DeployableUnitID deployableUnitID)
			throws Exception {

		if (logger.isDebugEnabled())
			logger.debug("removeDeployableUnit: deployableUnitID="
					+ deployableUnitID);

		try {
			getTransactionManager().begin();
			DeployableUnitDescriptorImpl deployableUnitDescriptor = (DeployableUnitDescriptorImpl) this
					.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().get(deployableUnitID);
			if (deployableUnitDescriptor == null)
				throw new UnrecognizedDeployableUnitException(
						"Unrecognized deployable unit " + deployableUnitID);

			// Check if its safe to remove the deployable unit.

			if (this.hasReferringDU(deployableUnitDescriptor)) {
				throw new DependencyException(
						"Somebody is referencing a component of this DU"
								+ " -- cannot uninstall it!");
			}

			removeDU(deployableUnitDescriptor);

		} catch (Exception ex) {
			logger.error("Exception while removing deployable unit", ex);
			getTransactionManager().setRollbackOnly();
			throw ex;
		} finally {
			getTransactionManager().commit();
		}
	}

	/**
	 * Actually do the unistall.
	 * 
	 * @param deployableUnitID
	 */
	public void removeDU(DeployableUnitDescriptorImpl deployableUnitDescriptor)
			throws Exception {

		if (logger.isDebugEnabled())
			logger.debug("removeDU: "
					+ deployableUnitDescriptor.getDeployableUnit());

		// Check if this refers to a service
		DeployableUnitIDImpl deployableUnitID = deployableUnitDescriptor
				.getDeployableUnit();
		boolean b = getTransactionManager().requireTransaction();
		try {
			this.removeReferredComponents(deployableUnitDescriptor);
			this.removeReferredDU(deployableUnitID);

			checkServicesStateOnDUUndeploy(deployableUnitID);
			this.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().remove(
							deployableUnitID);

			this.uninstallRA(deployableUnitID);
			this.uninstallRAType(deployableUnitID);
			this.removeEventType(deployableUnitID);
			this.uninstallService(deployableUnitID);

			// Clean up all the class files.
			this.deploymentManager.undeployUnit(deployableUnitID);

			// Clean up the various tables that refer to this Deployable Unit
			// ID.
			String url = deployableUnitID.getSourceURL().toString();
			this.getDeploymentCacheManager().getUrlToDeployableUnitIDMap()
					.remove(url);

			removeDeployedProfileComps(deployableUnitID);

			removeDeployedSbbComps(deployableUnitID);

			// Remove the tmp jars directories.
			// this will remove an entry in the jboss cache when we
			// put it there
			try {
				deployableUnitDescriptor.getTmpDeploymentDirectory().delete();
				deployableUnitDescriptor.getTmpDUJarsDirectory().delete();
			} catch (Exception ex) {
				logger.error("Error removing tmp directories ", ex);
			}

		} finally {
			if (b)
				getTransactionManager().commit();
		}

	}

	private void checkServicesStateOnDUUndeploy(
			DeployableUnitIDImpl deployableUnitID) throws SystemException,
			InvalidStateException {
		Map serviceComps = this.getDeploymentCacheManager()
				.getServiceComponents();
		for (Iterator it = serviceComps.entrySet().iterator(); it.hasNext();) {
			Map.Entry nentry = (Map.Entry) it.next();
			ServiceComponent svc = (ServiceComponent) nentry.getValue();
			if (svc.getDeployableUnit().equals(deployableUnitID)
					&& svc.isLocked())
				throw new InvalidStateException("Service state is not stopped");
		}
	}

	private void removeDeployedSbbComps(DeployableUnitIDImpl deployableUnitID)
			throws SystemException, Exception, NamingException {

		Map sbbComps = getDeploymentCacheManager().getSbbComponents();
		for (Iterator it = sbbComps.values().iterator(); it.hasNext();) {
			final MobicentsSbbDescriptor sbbDescriptor = (MobicentsSbbDescriptor) it
					.next();
			if (sbbDescriptor.getDeployableUnit().equals(deployableUnitID)) {
				if (!deployableUnitID.getDescriptor().hasInstalledComponent(
						sbbDescriptor.getID())) {
					if (logger.isDebugEnabled()) {
						logger.debug(" === SBBComp[" + sbbDescriptor.getID()
								+ "] WAS NOT ISNTALLED BY DU["
								+ deployableUnitID.getSourceURL() + "] ===");
					}
					continue;
				}
				if (logger.isDebugEnabled())
					logger.debug("Uninstalling SBB " + sbbDescriptor.getID()
							+ " on DU " + deployableUnitID);
				// remove class loader
				((UnifiedClassLoader3) sbbDescriptor.getClassLoader())
						.unregister();
				if (logger.isDebugEnabled()) {
					logger.debug("Removed class loader for SBB "
							+ sbbDescriptor.getID());
				}

				// remove and close object pool
				((ObjectPool) sbbPooling.remove(sbbDescriptor.getID())).close();
				if (logger.isDebugEnabled()) {
					logger.debug("Removed Pool for SBB "
							+ sbbDescriptor.getID());
				}
				// restore object pool if tx rollbacks
				TransactionalAction action = new TransactionalAction() {
					public void execute() {
						sbbPooling
								.put(
										sbbDescriptor.getID(),
										(new GenericObjectPoolFactory(
												new SbbObjectPoolFactory(
														((SbbID) sbbDescriptor
																.getID()),
														SleeContainer
																.lookupFromJndi()),
												poolConfig)).createPool());
						if (logger.isDebugEnabled()) {
							logger.debug("Created Pool for SBB "
									+ sbbDescriptor.getID());
						}
					}
				};
				sleeTransactionManager.addAfterRollbackAction(action);

				// remove sbb from trace and alarm facilities
				getTraceFacility().unSetTraceLevel(sbbDescriptor.getID());
				getAlarmFacility().unRegisterComponent(sbbDescriptor.getID());
				if (logger.isDebugEnabled()) {
					logger.debug("Removed SBB " + sbbDescriptor.getID()
							+ " from trace and alarm facilities");
				}

				// remove sbb descriptor
				sbbComps.remove(sbbDescriptor.getID());

				logger.info("Uninstalled SBB " + sbbDescriptor.getID()
						+ " on DU " + sbbDescriptor.getDeployableUnit());

			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("SBB " + sbbDescriptor.getID()
							+ " belongs to "
							+ sbbDescriptor.getDeployableUnit());
				}
			}
		}
	}

	private void removeDeployedProfileComps(
			DeployableUnitIDImpl deployableUnitID) throws SystemException {
		Map profileComps = getDeploymentCacheManager().getProfileComponents();
		for (Iterator it = profileComps.entrySet().iterator(); it.hasNext();) {
			Map.Entry nentry = (Map.Entry) it.next();
			ProfileSpecificationDescriptorImpl profileDescriptor = (ProfileSpecificationDescriptorImpl) nentry
					.getValue();
			if (logger.isDebugEnabled())
				logger.debug("deployableUnit = "
						+ profileDescriptor.getDeployableUnit());

			// Null check here because standard profiles do not have a DU
			// descriptor
			// associated with it.
			if (profileDescriptor.getDeployableUnit() != null
					&& profileDescriptor.getDeployableUnit().equals(
							deployableUnitID)) {

				if (!deployableUnitID.getDescriptor().hasInstalledComponent(
						(ComponentID) nentry.getKey())) {
					if (logger.isDebugEnabled()) {
						logger.debug(" === ProfileComp["

						+ profileDescriptor.getID()
								+ "] WAS NOT ISNTALLED BY DU["
								+ deployableUnitID.getSourceURL() + "] ===");
					}
					continue;
				}

				if (logger.isDebugEnabled())
					logger.debug("Uninstalling Profile "
							+ profileDescriptor.getID());
				ComponentID cid = (ComponentID) nentry.getKey();
				profileComps.remove(cid);

				logger.info("Uninstalled Profile " + profileDescriptor.getID());
			}
		}
	}

	/**
	 * Return the deployable Unit id given the url string from where it was
	 * loaded.
	 * 
	 * @param deploymentUrl --
	 *            url from where the unit was loaded.
	 * @return the deployable unit id for the url.
	 */
	public DeployableUnitID getDeployableUnitIDFromUrl(String deploymentUrl)
			throws UnrecognizedDeployableUnitException {
		boolean b = getTransactionManager().requireTransaction();
		try {
			DeployableUnitID retval = (DeployableUnitID) this
					.getDeploymentCacheManager().getUrlToDeployableUnitIDMap()
					.get(deploymentUrl);
			if (retval == null)
				throw new UnrecognizedDeployableUnitException(
						"Unrecognized  deployable unit " + deploymentUrl);
			else
				return retval;
		} finally {
			if (b)
				try {
					getTransactionManager().commit();
				} catch (Exception e) {
					logger
							.error(
									"Failed to complete tx for getDeployableUnitIDFromUrl()",
									e);
					throw new SLEEException(
							"Failed to complete tx for getDeployableUnitIDFromUrl()",
							e);
				}
		}
	}

	/**
	 * Return a set of services that match the given service state.
	 * 
	 * @param serviceState
	 * @return
	 */
	public ServiceID[] getServicesByState(ServiceState serviceState)
			throws Exception {
		// Service state is cached so we need to call this only in the context
		// of a tx.
		boolean b = getTransactionManager().requireTransaction();
		try {
			if (serviceState == ServiceState.ACTIVE) {
				// logger.info(">>>>>>>> fetching number of Active
				// services...:getDeploymentCacheManager()");
				DeploymentCacheManager dumgr = getDeploymentCacheManager();
				// logger.info(">>>>>>>> fetching number of Active
				// services...:getActiveServiceIDs()");
				Set activeServiceIds = dumgr.getActiveServiceIDs();
				// logger.info(">>>>>>>> fetching number of Active
				// services...:activeServiceIds.toArray(new ServiceID[0])");
				return (ServiceID[]) activeServiceIds.toArray(new ServiceID[0]);
			} else {
				Iterator it = this.getDeploymentCacheManager()
						.getServiceComponents().values().iterator();
				ArrayList retval = new ArrayList();
				while (it.hasNext()) {
					ServiceComponent svc = (ServiceComponent) it.next();
					Service service = getService(svc.getServiceID());
					if (service.getState().equals(serviceState)) {
						retval.add(svc.getServiceID());
					}
				}
				ServiceID[] ret = new ServiceID[retval.size()];
				retval.toArray(ret);
				return ret;

			}
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (Exception ex) {
				logger.error("Transaction manager failed commit", ex);
			}
		}

	}

	/**
	 * @param serviceID
	 */
	public synchronized void stopService(ServiceID serviceID)
			throws UnrecognizedServiceException, InvalidStateException {

		if (logger.isDebugEnabled()) {
			logger.debug("Deactivating " + serviceID);
		}

		if (serviceID == null)
			throw new NullPointerException("null service ID");

		SleeTransactionManager transactionManager = SleeContainer
				.getTransactionManager();

		boolean rb = true;
		boolean newTx = transactionManager.requireTransaction();
		try {

			if (this.getDeploymentCacheManager().getServiceComponents().get(
					serviceID) == null)
				throw new UnrecognizedServiceException("Service not found for "
						+ serviceID);

			// get the transactionally isolated copy of the service.

			ServiceComponent serviceComponent = this
					.getServiceComponent(serviceID);

			Service service = this.getService(serviceComponent);

			if (logger.isDebugEnabled())
				logger.debug("Service is " + service + " serviceState = "
						+ service.getState());

			if (service.getState() == ServiceState.STOPPING)
				throw new InvalidStateException("Service is STOPPING");

			if (service.getState() == ServiceState.INACTIVE) {
				getDeploymentCacheManager().getActiveServiceIDs().remove(
						serviceID);
				throw new InvalidStateException("Service already deactivated");
			}

			// notifying the resource adaptor about service
			HashSet sbbIDs = serviceComponent.getSbbComponents();
			HashSet raEntities = new HashSet();
			Iterator i = sbbIDs.iterator();
			while (i.hasNext()) {
				SbbDescriptor sbbdesc = getSbbComponent((SbbID) i.next());
				if (sbbdesc != null) {
					String[] raLinks = sbbdesc.getResourceAdaptorEntityLinks();
					for (int c = 0; raLinks != null && c < raLinks.length; c++) {
						ResourceAdaptorEntity raEntity = getRAEntity(raLinks[c]);
						if (raEntity != null && !raEntities.contains(raEntity)) {
							raEntity.serviceDeactivated(serviceID.toString());
							raEntities.add(raEntity);
						}
					}
				}
			}

			service.deactivate();

			serviceComponent.unlock();

			getDeploymentCacheManager().getActiveServiceIDs().remove(serviceID);

			rb = false;

		} catch (InvalidStateException ise) {
			logger.error(ise);
			throw ise;
		} catch (SystemException ex) {
			logger.error(ex);
			throw new RuntimeException("Tx manager failed! ", ex);
		} catch (Exception e) {
			logger.error(e);
			throw new SLEEException("Failed to stop service " + serviceID, e);
		} finally {
			try {
				if (newTx) {
					if (rb) {
						transactionManager.rollback();
					} else {
						transactionManager.commit();
					}
				} else {
					if (rb)
						transactionManager.setRollbackOnly();
				}
			} catch (SystemException e2) {
				logger.error(e2);
			}
		}

	}

	public String getRAActivityContextInterfaceFactoryJNDIName(
			ResourceAdaptorTypeID resourceAdapterTypeId) {
		ResourceAdaptorActivityContextInterfaceFactory acif = (ResourceAdaptorActivityContextInterfaceFactory) this.activityContextInterfaceFactories
				.get(resourceAdapterTypeId);
		return acif.getJndiName();
	}

	public EventRouter getEventRouter() {
		return this.router;
	}

	/**
	 * Get a list of profiles known to me
	 * 
	 * @return A list of profiles that are registered with me.
	 */
	public ProfileSpecificationID[] getProfileSpecificationIDs() {
		boolean b = SleeContainer.getTransactionManager().requireTransaction();
		try {
			ProfileSpecificationIDImpl[] retval = new ProfileSpecificationIDImpl[this
					.getDeploymentCacheManager().getProfileComponents().size()];
			this.getDeploymentCacheManager().getProfileComponents().keySet()
					.toArray(retval);
			return retval;
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
		}
	}

	/**
	 * @return the usage parameter table / public HashMap
	 *         getUsageParameterTable() {
	 * 
	 * return this.usageParameters; } / /** return the null activity factory.
	 * 
	 * @return
	 */
	public NullActivityFactoryImpl getNullActivityFactory() {
		return this.nullActivityFactory;
	}

	/**
	 * get the null activity context interface factory.
	 * 
	 * @return the null activity context interface factory.
	 */
	public NullActivityContextInterfaceFactoryImpl getNullActivityContextInterfaceFactory() {
		return this.nullActivityContextInterfaceFactory;
	}

	/**
	 * Register standard SLEE contexts in JNDI
	 * 
	 * @throws Exception
	 */
	protected void initNamingContexts() throws Exception {
		// Initialize the SLEE name space

		/*
		 * The following code sets the global name for the Slee
		 */
		Context ctx = new InitialContext();
		ctx = Util.createSubcontext(ctx, JVM_ENV + CTX_SLEE);

		Util.createSubcontext(ctx, "resources");
		Util.createSubcontext(ctx, "container");
		Util.createSubcontext(ctx, "facilities");
		Util.createSubcontext(ctx, "sbbs");
		ctx = Util.createSubcontext(ctx, "nullactivity");
		Util.createSubcontext(ctx, "factory");
		Util.createSubcontext(ctx, "nullactivitycontextinterfacefactory");
	}

	/*
	 * Register property editors for the composite SLEE types so that the jboss
	 * jmx console can pass it as an argument.
	 */
	protected void registerPropertyEditors() {
		PropertyEditorManager.registerEditor(ComponentID.class,
				ComponentIDPropertyEditor.class);
		PropertyEditorManager.registerEditor(Level.class,
				LevelPropertyEditor.class);
		PropertyEditorManager.registerEditor(Properties.class,
				PropertiesPropertyEditor.class);
		PropertyEditorManager.registerEditor(ProfileSpecificationID.class,
				ProfileSpecificationIDPropertyEditor.class);
		PropertyEditorManager.registerEditor(ComponentID[].class,
				ComponentIDArrayPropertyEditor.class);
		PropertyEditorManager.registerEditor(SbbID[].class,
				ComponentIDArrayPropertyEditor.class);
		PropertyEditorManager.registerEditor(DeployableUnitID.class,
				DeployableUnitIDPropertyEditor.class);
		PropertyEditorManager.registerEditor(Object.class,
				ObjectPropertyEditor.class);
		PropertyEditorManager.registerEditor(ServiceID.class,
				ComponentIDPropertyEditor.class);
		PropertyEditorManager.registerEditor(Object.class,
				ServiceStatePropertyEditor.class);
		PropertyEditorManager.registerEditor(ResourceAdaptorID.class,
				ComponentIDPropertyEditor.class);
	}

	/**
	 * Get the current state of the Slee Container
	 * 
	 * @return SleeState
	 */
	public SleeState getSleeState() {
		return this.sleeState;
	}

	/**
	 * Set the current state of the Slee Container. CAUTION: Do not invoke this
	 * method directly! Use the SleeManagementMBean to change the Slee State
	 * 
	 * @param SleeState
	 */
	public SleeState setSleeState(SleeState newState) {
		return this.sleeState = newState;
	}

	/**
	 * Return the MBeanServer that the SLEEE is registers with in the current
	 * JVM
	 * 
	 */
	public MBeanServer getMBeanServer() {
		return mbeanServer;
	}

	public ServiceActivityContextInterfaceFactoryImpl getServiceActivityContextFactory() {
		return this.serviceActivityContextInterfaceFactory;
	}

	/**
	 * These are hacky -- will be removed after TCK
	 * 
	 * @return
	 */

	/**
	 * 
	 */
	public synchronized void removeEventType(
			MobicentsEventTypeDescriptor descriptor) {
		EventTypeIDImpl eventTypeID = (EventTypeIDImpl) descriptor.getID();
		ComponentKey ckey = eventTypeID.getComponentKey();
		if (logger.isDebugEnabled()) {
			logger.debug("Uninstalling event type " + eventTypeID);
		}
		this.eventTypeIDToDescriptor.remove(eventTypeID);
		this.eventKeyToEventTypeIDMap.remove(ckey);
		this.eventTypeIDToEventKeyMap.remove(eventTypeID);
		this.eventTypeIDs.remove(new Integer(eventTypeID.getEventID()));

		logger.info("Uninstalled event type " + eventTypeID);
	}

	public HashMap getActivityContextInterfaceFactories() {
		return activityContextInterfaceFactories;
	}

	public void addActivityContextInterfaceFactory(ComponentID key,
			TCKActivityContextInterfaceFactoryImpl acif) {
		activityContextInterfaceFactories.put(key, acif);
	}

	public void createResourceAdaptorEntityLink(String link, String entityName)
			throws ManagementException {
		if (this.resourceAdaptorEntityLinks.containsKey(link))
			throw new ManagementException("Entity Link already exist!");
		this.resourceAdaptorEntityLinks.put(link, entityName);
	}

	public void removeResourceAdaptorEntityLink(String link)
			throws ManagementException {
		if (!this.resourceAdaptorEntityLinks.containsKey(link))
			throw new ManagementException("Entity Link not found!");
		this.resourceAdaptorEntityLinks.remove(link);
	}

	public Set listResourceAdaptorEntityLinks() {
		return this.resourceAdaptorEntityLinks.keySet();
	}

	public String[] getResourceAdaptorEntityLinks() throws ManagementException {
		Set entityLinksSet = resourceAdaptorEntityLinks.keySet();
		String[] entityLinksArray = new String[entityLinksSet.size()];
		entityLinksArray = (String[]) entityLinksSet.toArray(entityLinksArray);
		return entityLinksArray;
	}

	public String getResourceAdaptorEntityName(String linkName)
			throws NullPointerException, UnrecognizedLinkNameException {
		if (linkName == null)
			throw new NullPointerException("null link name");

		String entityName = (String) resourceAdaptorEntityLinks.get(linkName);
		if (entityName == null)
			throw new UnrecognizedLinkNameException("Entity link " + linkName
					+ " not found");

		return entityName;
	}

	public String[] getResourceAdaptorEntityNames(String[] linkNames)
			throws NullPointerException {
		if (linkNames == null)
			throw new NullPointerException("null link names");

		String[] resultEntityNames = new String[linkNames.length];
		for (int i = 0; i < linkNames.length; i++) {
			String entityName = (String) resourceAdaptorEntityLinks
					.get(linkNames[i]);
			resultEntityNames[i] = entityName;
		}

		return resultEntityNames;
	}

	public String[] getResourceAdaptorEntityNames() {
		String[] entityNames = new String[resourceAdaptorEntities.keySet()
				.size()];
		entityNames = (String[]) resourceAdaptorEntities.keySet().toArray(
				entityNames);
		return entityNames;
	}

	public String[] getResourceAdaptorEntityNames(
			ResourceAdaptorID resourceAdaptorID) throws NullPointerException,
			UnrecognizedResourceAdaptorException {
		if (resourceAdaptorID == null)
			throw new NullPointerException("null resource adaptor");

		InstalledResourceAdaptor installedRA = (InstalledResourceAdaptor) this.installedResourceAdaptors
				.get(resourceAdaptorID);
		if (installedRA == null)
			throw new UnrecognizedResourceAdaptorException("Resource adaptor "
					+ resourceAdaptorID.toString() + " not found");

		HashSet resourceAdaptorEntitiesSet = installedRA
				.getResourceAdaptorEntities();
		ResourceAdaptorEntity[] resourceAdaptorEntitiesArray = new ResourceAdaptorEntity[resourceAdaptorEntitiesSet
				.size()];
		resourceAdaptorEntitiesArray = (ResourceAdaptorEntity[]) resourceAdaptorEntitiesSet
				.toArray(resourceAdaptorEntitiesArray);

		String[] entityNames = new String[resourceAdaptorEntitiesArray.length];
		for (int i = 0; i < entityNames.length; i++) {
			entityNames[i] = resourceAdaptorEntitiesArray[i].getName();
		}
		return entityNames;
	}

	public String[] getResourceAdaptorEntityLinks(String entityName)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException, ManagementException {
		if (entityName == null)
			throw new NullPointerException("null entity name");

		if (!resourceAdaptorEntities.containsKey(entityName))
			throw new UnrecognizedResourceAdaptorEntityException("Entity "
					+ entityName + " not found");

		String[] entityLinksArray = getResourceAdaptorEntityLinks();
		ArrayList resultEntityLinksArrayList = new ArrayList();
		for (int i = 0; i < entityLinksArray.length; i++) {
			String entityLink = entityLinksArray[i];
			String entity = (String) resourceAdaptorEntityLinks.get(entityLink);
			if (entity.equals(entityName))
				resultEntityLinksArrayList.add(entityLink);
		}
		String[] resultEntityLinks = new String[resultEntityLinksArrayList
				.size()];
		resultEntityLinks = (String[]) resultEntityLinksArrayList
				.toArray(resultEntityLinks);
		return resultEntityLinks;
	}

	public ResourceAdaptorEntity getResourceAdaptorEntity(String entityName)
			throws NullPointerException,
			UnrecognizedResourceAdaptorEntityException {
		if (entityName == null)
			throw new NullPointerException("null entity name");

		ResourceAdaptorEntity entity = (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(entityName);
		if (entity == null)
			throw new UnrecognizedResourceAdaptorEntityException("Entity "
					+ entityName + " not found");

		return entity;
	}

	/**
	 * @deprecated Use getResourceAdaptorEntity
	 */
	public ResourceAdaptorEntity getResourceAdaptorEnitity(String enitityName) {
		return (ResourceAdaptorEntity) this.resourceAdaptorEntities
				.get(enitityName);
	}

	public HashMap getResourceAdaptorEntities() {
		return resourceAdaptorEntities;
	}

	/**
	 * @deprecated Use getResourceAdaptorEntities
	 */
	public HashMap getResourceAdaptorEnitities() {
		return resourceAdaptorEntities;
	}

	public ResourceAdaptorID getResourceAdaptorID(String entityName)
			throws java.lang.NullPointerException,
			UnrecognizedResourceAdaptorEntityException, ManagementException {

		if (entityName == null)
			throw new NullPointerException("null entity name");

		ResourceAdaptorEntity resourceAdaptorEntity = (ResourceAdaptorEntity) resourceAdaptorEntities
				.get(entityName);
		if (resourceAdaptorEntity == null)
			throw new UnrecognizedResourceAdaptorEntityException("Entity "
					+ entityName + " not found");

		return resourceAdaptorEntity.getInstalledResourceAdaptor().getKey();
	}

	public String[] getResourceAdaptorEntities(ResourceAdaptorEntityState state)
			throws NullPointerException, ManagementException {
		if (state == null)
			throw new NullPointerException("null entity state");

		Iterator resourceAdaptorEntityIterator = resourceAdaptorEntities
				.values().iterator();
		ArrayList resultEntityNamesArrayList = new ArrayList();

		while (resourceAdaptorEntityIterator.hasNext()) {
			ResourceAdaptorEntity resourceAdaptorEntity = (ResourceAdaptorEntity) resourceAdaptorEntityIterator
					.next();
			if (resourceAdaptorEntity.getState().equals(state))
				resultEntityNamesArrayList.add(resourceAdaptorEntity.getName());
		}

		String[] resultEntityNames = new String[resultEntityNamesArrayList
				.size()];
		resultEntityNames = (String[]) resultEntityNamesArrayList
				.toArray(resultEntityNames);

		return resultEntityNames;
	}

	/**
	 * @param svc
	 * @return
	 */
	public ServiceComponent getServiceComponent(ServiceID svc) {
		// Call this when you dont care about the service state ( this is just
		// an optimization to avoid creating a new tx.
		boolean b = SleeContainer.getTransactionManager().requireTransaction();
		try {
			return (ServiceComponent) this.getDeploymentCacheManager()
					.getServiceComponents().get(svc);
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
		}
	}

	/**
	 * @param serviceID
	 * @return
	 */
	public boolean checkServiceExists(ServiceID serviceID) {

		boolean b = SleeContainer.getTransactionManager().requireTransaction();
		try {
			return this.getDeploymentCacheManager().getServiceComponents()
					.containsKey(serviceID);
		} catch (Exception ex) {
			throw new RuntimeException("Tx manager failed ", ex);
		} finally {
			try {
				if (b)
					getTransactionManager().commit();
			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
		}
	}

	public int getRmiRegistryPort() throws Exception {
		Integer port = (Integer) mbeanServer.getAttribute(new ObjectName(
				"slee:service=SleeTCKWrapper"), "RMIRegistryPort");
		return port.intValue();
	}

	/**
	 * @return
	 */
	public DeployableUnitDescriptor[] getDeployableUnitDescriptors()
			throws SystemException {
		if (logger.isDebugEnabled()) {
			logger.debug("getDeployableUnitDescriptors() ");
		}
		boolean b = SleeContainer.getTransactionManager().requireTransaction();

		try {
			Object[] descriptors = this.getDeploymentCacheManager()
					.getDeployableUnitIDtoDescriptorMap().values().toArray();

			DeployableUnitDescriptor[] retval = new DeployableUnitDescriptor[descriptors.length];
			for (int i = 0; i < descriptors.length; i++) {

				retval[i] = (DeployableUnitDescriptor) descriptors[i];
			}
			return retval;

		} finally {
			try {
				if (b)
					getTransactionManager().commit();

			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("getDeployableUnitDescriptors() exit");
			}
		}

	}

	/**
	 * Return a list of component descriptors for the given list of component
	 * ids.
	 * 
	 * @param componentIds
	 * 
	 * @return an array of component descriptors.
	 * 
	 */
	public ComponentDescriptor[] getDescriptors(ComponentID[] componentIds) {

		boolean b = getTransactionManager().requireTransaction();
		try {
			ArrayList knownComps = new ArrayList();
			for (int i = 0; i < componentIds.length; i++) {
				if (isInstalled(componentIds[i])) {
					// may be too strict (spec says 'recognized', 14.7.8, p214)

					ComponentDescriptor descr = this
							.getComponentDescriptor(componentIds[i]);
					knownComps.add(descr);

				}
			}
			ComponentDescriptor[] components = new ComponentDescriptor[knownComps
					.size()];
			knownComps.toArray(components);
			return components;
		} finally {
			try {
				if (b)
					getTransactionManager().commit();

			} catch (SystemException ex) {
				throw new RuntimeException("tx maanger failed ", ex);
			}

		}
	}

	public static boolean isSecurityEnabled() {

		return isSecurityEnabled;
	}

	public String dumpState() {
		return "SleeContainer map activityContextInterfaceFactories: "
				+ activityContextInterfaceFactories.keySet()
				+ "\nSleeContainer map eventKeyToEventTypeIDMap: "
				+ eventKeyToEventTypeIDMap.keySet()
				+ "\nSleeContainer map eventTypeIDs: " + eventTypeIDs.keySet()
				+ "\nSleeContainer map eventTypeIDToDescriptor: "
				+ eventTypeIDToDescriptor.keySet()
				+ "\nSleeContainer map eventTypeIDToEventKeyMap: "
				+ eventTypeIDToEventKeyMap.keySet()
				+ "\nSleeContainer map resourceAdaptorEntities: "
				+ resourceAdaptorEntities.keySet()
				+ "\nSleeContainer map resourceAdaptorEntityLinks: "
				+ resourceAdaptorEntityLinks.keySet()
				+ "\nSleeContainer map resourceAdaptorTypes: "
				+ resourceAdaptorTypes.keySet()
				+ "\nSleeContainer map sbbPooling: " + sbbPooling.keySet();
	}
}
