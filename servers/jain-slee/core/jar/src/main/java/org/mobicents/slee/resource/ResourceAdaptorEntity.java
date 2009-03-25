package org.mobicents.slee.resource;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import javax.slee.EventTypeID;
import javax.slee.InvalidArgumentException;
import javax.slee.InvalidStateException;
import javax.slee.SLEEException;
import javax.slee.ServiceID;
import javax.slee.facilities.AlarmFacility;
import javax.slee.management.NotificationSource;
import javax.slee.management.ResourceAdaptorEntityNotification;
import javax.slee.management.ResourceAdaptorEntityState;
import javax.slee.management.SleeState;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorID;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.jboss.logging.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.component.ResourceAdaptorComponent;
import org.mobicents.slee.container.component.ResourceAdaptorTypeComponent;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.references.MEventTypeRef;
import org.mobicents.slee.container.management.jmx.ResourceUsageMBeanImpl;
import org.mobicents.slee.runtime.activity.ActivityContext;
import org.mobicents.slee.runtime.activity.ActivityContextHandle;
import org.mobicents.slee.runtime.activity.ActivityType;
import org.mobicents.slee.runtime.eventrouter.DeferredEvent;
import org.mobicents.slee.runtime.facilities.AbstractAlarmFacilityImpl;
import org.mobicents.slee.runtime.facilities.DefaultAlarmFacilityImpl;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

/**
 * 
 * Implementation of the logical Resource Adaptor Entity and its life cycle.
 * 
 * @author Eduardo Martins
 */
public class ResourceAdaptorEntity {

	private static final Logger logger = Logger
			.getLogger(ResourceAdaptorEntity.class);

	/**
	 * the ra entity name
	 */
	private final String name;

	/**
	 * the ra component related to this entity
	 */
	private final ResourceAdaptorComponent component;

	/**
	 * the ra entity state
	 */
	private ResourceAdaptorEntityState state;

	/**
	 * the ra object
	 */
	private final ResourceAdaptorObject object;

	/**
	 * the slee container
	 */
	private SleeContainer sleeContainer;

	/**
	 * Notification source of this RA Entity
	 */
	private ResourceAdaptorEntityNotification notificationSource;

	/**
	 * Alarm facility serving this RA entity(notification source)
	 */
	private AbstractAlarmFacilityImpl alarmFacility;

	/**
	 * the resource usage mbean for this ra, may be null
	 */
	private ResourceUsageMBeanImpl usageMbean;

	/**
	 * the ra context for this entity
	 */
	private final ResourceAdaptorContextImpl resourceAdaptorContext;

	/**
	 * the ra allowed event types, cached here for optimal runtime performance
	 */
	private final Set<EventTypeID> allowedEventTypes;

	/**
	 * Creates a new entity with the specified name, for the specified ra
	 * component and with the provided entity config properties. The entity
	 * creation is complete after instantianting the ra object, and then setting
	 * its ra context and configuration.
	 * 
	 * @param name
	 * @param component
	 * @param entityProperties
	 * @param sleeContainer
	 * @throws InvalidConfigurationException
	 * @throws InvalidArgumentException
	 */
	public ResourceAdaptorEntity(String name,
			ResourceAdaptorComponent component,
			ConfigProperties entityProperties, SleeContainer sleeContainer,
			ResourceAdaptorEntityNotification notificationSource)
			throws InvalidConfigurationException, InvalidArgumentException {
		this.name = name;
		this.component = component;
		this.sleeContainer = sleeContainer;
		this.notificationSource = notificationSource;
		this.alarmFacility = new DefaultAlarmFacilityImpl(notificationSource,
				this.sleeContainer.getAlarmFacility());
		// create ra object
		try {
			Constructor cons = this.component.getResourceAdaptorClass()
					.getConstructor(null);
			ResourceAdaptor ra = (ResourceAdaptor) cons.newInstance(null);
			object = new ResourceAdaptorObject(ra, component
					.getDefaultConfigPropertiesInstance());
		} catch (Exception e) {
			throw new SLEEException(
					"unable to create instance of ra object for " + component);
		}
		// create ra context
		resourceAdaptorContext = new ResourceAdaptorContextImpl(this,
				sleeContainer);
		// cache runtime data
		if (!component.getDescriptor().getIgnoreRaTypeEventTypeCheck()) {
			// the slee endpoint will filter events fired, build a set with the
			// event types allowed
			allowedEventTypes = new HashSet<EventTypeID>();
			for (ResourceAdaptorTypeID raTypeID : resourceAdaptorContext
					.getResourceAdaptorTypes()) {
				ResourceAdaptorTypeComponent raTypeComponent = sleeContainer
						.getComponentRepositoryImpl()
						.getComponentByID(raTypeID);
				for (MEventTypeRef eventTypeRef : raTypeComponent
						.getDescriptor().getEventTypeRefs()) {
					allowedEventTypes.add(eventTypeRef.getComponentID());
				}
			}
		} else {
			allowedEventTypes = null;
		}
		// set ra context and configure it
		try {
			object.setResourceAdaptorContext(resourceAdaptorContext);
		} catch (InvalidStateException e) {
			logger
					.error(
							"should not happen, setting ra context on ra entity creation",
							e);
			throw new SLEEException(e.getMessage(), e);
		}
		object.raConfigure(entityProperties);
		// process to inactive state
		this.state = ResourceAdaptorEntityState.INACTIVE;
	}

	/**
	 * Retrieves ra component related to this entity
	 * 
	 * @return
	 */
	public ResourceAdaptorComponent getComponent() {
		return component;
	}

	/**
	 * Retrieves the ra entity name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the ra entity state
	 * 
	 * @return
	 */
	public ResourceAdaptorEntityState getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == this.getClass()) {
			return ((ResourceAdaptorEntity) obj).name.equals(this.name);
		} else {
			return false;
		}
	}

	// --------- ra entity/object logic

	/**
	 * Updates the ra entity config properties
	 */
	public void updateConfigurationProperties(ConfigProperties properties)
			throws InvalidConfigurationException, InvalidStateException {
		if (!component.getDescriptor().getSupportsActiveReconfiguration()
				&& (sleeContainer.getSleeState() == SleeState.RUNNING
						&& sleeContainer.getSleeState() == SleeState.STOPPING && sleeContainer
						.getSleeState() == SleeState.STARTING)
				&& (state == ResourceAdaptorEntityState.ACTIVE || state == ResourceAdaptorEntityState.STOPPING)) {
			throw new InvalidStateException(
					"the value of the supports-active-reconfiguration attribute of the resource-adaptor-class element in the deployment descriptor of the Resource Adaptor of the resource adaptor entity is False and the resource adaptor entity is in the Active or Stopping state and the SLEE is in the Starting, Running, or Stopping state");
		} else {
			object.raConfigurationUpdate(properties);
		}
	}

	/**
	 * Signals that the container is in RUNNING state
	 */
	public void sleeRunning() throws InvalidStateException {
		// if entity is active then activate the ra object
		if (this.state.isActive()) {
			object.raActive();
		}
	}

	/**
	 * Signals that the container is in STOPPING state
	 */
	public void sleeStopping() throws InvalidStateException {
		if (this.state.isActive()) {
			object.raStopping();
			endAllActivities();
			object.raInactive();
		}
	}

	/**
	 * Activates the ra entity
	 * 
	 * @throws InvalidStateException
	 *             if the entity is not in INACTIVE state
	 */
	public void activate() throws InvalidStateException {
		if (!this.state.isInactive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
					+ this.state);
		}
		this.state = ResourceAdaptorEntityState.ACTIVE;
		// if slee is running then activate ra object
		if (sleeContainer.getSleeState().isRunning()) {
			object.raActive();
		}
	}

	/**
	 * Deactivates the ra entity
	 * 
	 * @throws InvalidStateException
	 *             if the entity is not in ACTIVE state
	 */
	public void deactivate() throws InvalidStateException {
		if (!this.state.isActive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
					+ this.state);
		}
		object.raStopping();
		this.state = ResourceAdaptorEntityState.STOPPING;
		endAllActivities();
		object.raInactive();
		this.state = ResourceAdaptorEntityState.INACTIVE;
	}

	private void endAllActivities() {

		// end all activities
		SleeTransactionManager txManager = sleeContainer
				.getTransactionManager();
		boolean rb = true;
		try {
			txManager.begin();
			for (ActivityContextHandle handle : sleeContainer
					.getActivityContextFactory()
					.getAllActivityContextsHandles()) {
				if (handle.getActivityType() == ActivityType.externalActivity
						&& handle.getActivitySource().equals(name)) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Ending activity " + handle);
						}
						ActivityContext ac = sleeContainer
								.getActivityContextFactory()
								.getActivityContext(handle, false);
						if (ac != null) {
							ac.endActivity();
						}
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("Failed to end activity " + handle, e);
						}
					}
				}
			}
			rb = false;
		} catch (Exception e) {
			logger.error("Exception while ending all activities for ra entity "
					+ name, e);

		} finally {
			try {
				if (rb) {
					txManager.rollback();
				} else {
					txManager.commit();
				}
			} catch (Exception e) {
				logger.error(
						"Error in tx management while ending all activities for ra entity "
								+ name, e);
			}
		}
	}

	/**
	 * Removes the entity, it will unconfigure and unset the ra context, the
	 * entity object can not be reused
	 * 
	 * @throws InvalidStateException
	 */
	public void remove() throws InvalidStateException {
		if (!this.state.isInactive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
					+ this.state);
		}
		object.raUnconfigure();
		object.unsetResourceAdaptorContext();
		this.sleeContainer.getTraceFacility().getTraceMBeanImpl()
				.deregisterNotificationSource(this.getNotificationSource());
		state = null;
	}

	/**
	 * Retrieves the active config properties for the entity
	 * 
	 * @return
	 */
	public ConfigProperties getConfigurationProperties() {
		return object.getConfigProperties();
	}

	/**
	 * Retrieves the id of the resource adaptor for this entity
	 * 
	 * @return
	 */
	public ResourceAdaptorID getResourceAdaptorID() {
		return component.getResourceAdaptorID();
	}

	/**
	 * Retrieves the ra object
	 * 
	 * @return
	 */
	public ResourceAdaptorObject getResourceAdaptorObject() {
		return object;
	}

	/**
	 * Retrieves the ra interface for this entity and the specified ra type
	 * 
	 * @param raType
	 * @return
	 */
	public Object getResourceAdaptorInterface(ResourceAdaptorTypeID raType) {
		return object.getResourceAdaptorInterface(sleeContainer
				.getComponentRepositoryImpl().getComponentByID(raType)
				.getDescriptor().getResourceAdaptorInterface()
				.getResourceAdaptorInterfaceName());
	}

	/**
	 * Retrieves the marshaller from the ra object, if exists
	 * 
	 * @return
	 */
	public Marshaler getMarshaler() {
		return object.getMarshaler();
	}

	/**
	 * Indicates a service was activated, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceActive(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceActive(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Indicates a service is stopping, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceStopping(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceStopping(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Indicates a service was deactivated, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceInactive(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceInactive(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Return Notification source representing this RA Entity
	 * 
	 * @return
	 */
	public NotificationSource getNotificationSource() {
		return this.notificationSource;
	}

	public AlarmFacility getAlarmFacility() {
		return alarmFacility;
	}

	/**
	 * Retrieves the resource usage mbean for this ra, may be null
	 * 
	 * @return
	 */
	public ResourceUsageMBeanImpl getResourceUsageMBean() {
		return usageMbean;
	}

	/**
	 * Sets the resource usage mbean for this ra, may be null
	 */
	public void setResourceUsageMBean(ResourceUsageMBeanImpl usageMbean) {
		this.usageMbean = usageMbean;
	}

	/**
	 * Retrieves a set containing event types allowed to be fire by this entity
	 * 
	 * @return null if the ra ignores event type checking
	 */
	public Set<EventTypeID> getAllowedEventTypes() {
		return allowedEventTypes;
	}

	/**
	 * Callback to notify the ra object that the processing for specified event
	 * succeed
	 * 
	 * @see ResourceAdaptorObject#eventProcessingSuccessful(javax.slee.resource.ActivityHandle,
	 *      javax.slee.resource.FireableEventType, Object, javax.slee.Address,
	 *      ReceivableService, int)
	 * 
	 * @param deferredEvent
	 */
	public void eventProcessingSucceed(DeferredEvent deferredEvent) {
		object.eventProcessingSuccessful(deferredEvent
				.getActivityContextHandle().getActivityHandle(),
				getFireableEventType(deferredEvent), deferredEvent.getEvent(),
				deferredEvent.getAddress(),
				getReceivableService(deferredEvent), deferredEvent
						.getEventFlags());
	}

	/**
	 * Callback to notify the ra object that the processing for specified event
	 * failed.
	 * 
	 * @see ResourceAdaptorObject#eventProcessingFailed(javax.slee.resource.ActivityHandle,
	 *      javax.slee.resource.FireableEventType, Object, javax.slee.Address,
	 *      ReceivableService, int, FailureReason)
	 * 
	 * @param deferredEvent
	 * @param failureReason
	 */
	public void eventProcessingFailed(DeferredEvent deferredEvent,
			FailureReason failureReason) {
		object.eventProcessingFailed(deferredEvent.getActivityContextHandle()
				.getActivityHandle(), getFireableEventType(deferredEvent),
				deferredEvent.getEvent(), deferredEvent.getAddress(),
				getReceivableService(deferredEvent), deferredEvent
						.getEventFlags(), failureReason);
	}

	private FireableEventType getFireableEventType(DeferredEvent deferredEvent) {
		FireableEventType eventType = null;
		try {
			eventType = resourceAdaptorContext.getEventLookupFacility()
					.getFireableEventType(deferredEvent.getEventTypeId());
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		return eventType;
	}

	private ReceivableService getReceivableService(DeferredEvent deferredEvent) {
		ReceivableService receivableService = null;
		try {
			receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(
							deferredEvent.getService());
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		return receivableService;
	}

	/**
	 * Callback to notify the ra object that the specified event is now
	 * unreferenced
	 * 
	 * @param deferredEvent
	 */
	public void eventUnreferenced(DeferredEvent deferredEvent) {
		object.eventUnreferenced(deferredEvent.getActivityContextHandle()
				.getActivityHandle(), getFireableEventType(deferredEvent),
				deferredEvent.getEvent(), deferredEvent.getAddress(),
				getReceivableService(deferredEvent), deferredEvent
						.getEventFlags());
	}
}
