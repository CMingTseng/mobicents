/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/

package org.mobicents.slee.container.management.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.slee.ComponentID;
import javax.slee.InvalidArgumentException;
import javax.slee.UnrecognizedComponentException;
import javax.slee.facilities.Level;
import javax.slee.facilities.TraceLevel;
import javax.slee.facilities.Tracer;
import javax.slee.management.ManagementException;
import javax.slee.management.NotificationSource;
import javax.slee.management.TraceMBean;
import javax.slee.management.TraceNotification;
import javax.slee.management.UnrecognizedNotificationSourceException;
import javax.transaction.SystemException;

import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBeanSupport;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.runtime.facilities.TraceFacilityImpl;
import org.mobicents.slee.runtime.facilities.TracerImpl;
import org.mobicents.slee.runtime.facilities.TracerStorage;
import org.mobicents.slee.runtime.transaction.TransactionalAction;

/**
 * Implementation of the Trace MBean.
 * 
 * @author M. Ranganathan
 * @author baranowb
 */
public class TraceMBeanImpl extends ServiceMBeanSupport implements TraceMBeanImplMBean {

	private TraceFacilityImpl traceFacility;

	private static Logger logger;

	static {
		logger = Logger.getLogger(TraceMBeanImpl.class);
	}

	/**
	 * Creates the trace mbean and registers itself in JNDI.
	 * 
	 * @throws NotCompliantMBeanException
	 */
	public TraceMBeanImpl() throws NotCompliantMBeanException {
		super(TraceMBeanImplMBean.class);
		this.traceFacility = new TraceFacilityImpl(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.slee.management.TraceMBean#setTraceLevel(javax.slee.ComponentID,
	 * javax.slee.facilities.Level)
	 */
	public void setTraceLevel(ComponentID componentId, Level level) throws NullPointerException, UnrecognizedComponentException, ManagementException {
		if (componentId == null)
			throw new NullPointerException("null component Id");
		this.traceFacility.checkComponentID(componentId);
		this.traceFacility.setTraceLevel(componentId, level);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.slee.management.TraceMBean#getTraceLevel(javax.slee.ComponentID)
	 */
	public Level getTraceLevel(ComponentID componentId) throws NullPointerException, UnrecognizedComponentException, ManagementException {
		if (componentId == null)
			throw new NullPointerException(" null component Id ");
		Level level = this.traceFacility.getTraceLevel(componentId);
		return level;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {

		if (this.traceFacility == null)
			return null;

		String[] notificationTypes = this.traceFacility.getNotificationTypes();
		MBeanNotificationInfo[] mbeanNotificationInfo = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notificationTypes, TraceMBean.TRACE_NOTIFICATION_TYPE,
				"SLEE Spec 1.0, #13.4. SBBs use the Trace Facility to generate trace messages intended for "
						+ "consumption by external management clients, such as a network management console or a management policy engine.") };
		return mbeanNotificationInfo;
	}

	/**
	 * the JNDI name where this facility is be bound under the facilities
	 * context. The name is fixed by the SLEE 1.0 spec, section 13.8
	 */
	public static final String JNDI_NAME = "trace";

	protected void startService() throws Exception {
		SleeContainer.registerFacilityWithJndi(JNDI_NAME, this.traceFacility);
		logger.info("TraceMBean started");
	}

	protected void stopService() throws Exception {
		SleeContainer.unregisterFacilityWithJndi(JNDI_NAME);
		logger.info("TraceMBean stopped");
	}

	public TraceLevel getTraceLevel(NotificationSource src, String tracerName) throws NullPointerException, InvalidArgumentException, UnrecognizedNotificationSourceException, ManagementException {
		if(src == null)
		{
			throw new NullPointerException("NotificationSource must nto be null!");
		}
		
		if(tracerName == null)
		{
			throw new NullPointerException("TracerName must not be null!");
		}
		if(!this.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source not recognized: "+src);
		}
		TracerImpl.checkTracerName(tracerName, src);
		
		if(!this.isTracerDefined(src,tracerName))
		{
			//FIXME: what is valid tracer name? JDOC contradicts that not existing tracer name is invalid
			this.createTracer(src,tracerName,false);
		}
		
		
		
		TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}
		try {
			return ts.getTracerLevel(tracerName);
		} catch (Exception e) {
			throw new ManagementException("Failed to unset trace level due to: ", e);
		}
	}

	public String[] getTracersSet(NotificationSource src) throws NullPointerException, UnrecognizedNotificationSourceException, ManagementException {
		if(src == null)
		{
			throw new NullPointerException("NotificationSource must nto be null!");
		}
		if(!this.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source not recognized: "+src);
		}

			
		TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}
		return ts.getDefinedTracerNames();
	}

	public String[] getTracersUsed(NotificationSource src) throws NullPointerException, UnrecognizedNotificationSourceException, ManagementException {
		if(src == null)
		{
			throw new NullPointerException("NotificationSource must nto be null!");
		}
		if(!this.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source not recognized: "+src);
		}
		TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}
		return ts.getRequestedTracerNames();
	}

	public void setTraceLevel(NotificationSource src, final String tracerName, TraceLevel lvl) throws NullPointerException, UnrecognizedNotificationSourceException, InvalidArgumentException,
			ManagementException {
		if(src == null)
		{
			throw new NullPointerException("NotificationSource must nto be null!");
		}
		
		if(tracerName == null)
		{
			throw new NullPointerException("TracerName must not be null!");
		}
		
		if(lvl == null)
		{
			throw new NullPointerException("TraceLevel must not be null!");
		}
		if
		(!this.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source not recognized: "+src);
		}
		TracerImpl.checkTracerName(tracerName, src);
		
		if(!this.isTracerDefined(src,tracerName))
		{
			//FIXME: what is valid tracer name? JDOC contradicts that not existing tracer name is invalid
			this.createTracer(src,tracerName,false);
		}
		
		final TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}
		try {
			
			
			try {
				if(SleeContainer.lookupFromJndi().getTransactionManager().getTransaction()!=null)
				{
					final TraceLevel _oldLevel=ts.getTracerLevel(tracerName);
					TransactionalAction action = new TransactionalAction() {
						TraceLevel oldLevel = _oldLevel;
				
						String name = tracerName;
						public void execute() {
							try {
								ts.setTracerLevel(oldLevel, tracerName);
							} catch (InvalidArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					};
					SleeContainer.lookupFromJndi().getTransactionManager().addAfterRollbackAction(action);
				}
			} catch (SystemException e) {
				
				e.printStackTrace();
			}
			
			ts.setTracerLevel(lvl, tracerName);
			
			
		} catch (Exception e) {
			throw new ManagementException("Failed to set trace level due to: ", e);
		}

		
	}

	public void unsetTraceLevel(NotificationSource src, final String tracerName) throws NullPointerException, UnrecognizedNotificationSourceException, InvalidArgumentException, ManagementException {
		if(src == null)
		{
			throw new NullPointerException("NotificationSource must nto be null!");
		}
		
		if(tracerName == null)
		{
			throw new NullPointerException("TracerName must not be null!");
		}
		if(!this.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source not recognized: "+src);
		}
		TracerImpl.checkTracerName(tracerName, src);
		
		if(!this.isTracerDefined(src,tracerName))
		{
			//FIXME: what is valid tracer name? JDOC contradicts that not existing tracer name is invalid
			this.createTracer(src,tracerName,false);
		}
		
		final TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}
		try {
			
			
			
			try {
				if(SleeContainer.lookupFromJndi().getTransactionManager().getTransaction()!=null)
				{
					final TraceLevel _oldLevel=ts.getTracerLevel(tracerName);
					TransactionalAction action = new TransactionalAction() {
						TraceLevel oldLevel = _oldLevel;
				
						String name = tracerName;
						public void execute() {
							try {
								ts.setTracerLevel(oldLevel, tracerName);
							} catch (InvalidArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					};
					SleeContainer.lookupFromJndi().getTransactionManager().addAfterRollbackAction(action);
				}
			} catch (SystemException e) {
				
				e.printStackTrace();
			}
			
			
			
			
			
			ts.unsetTracerLevel(tracerName);
		} catch (Exception e) {
			throw new ManagementException("Failed to unset trace level due to: ", e);
		}
		
	}

	
	//Non MBEAN methods
	
	// 1.1 TracerImpl are. We define internal private class to hide details :)

	private Map<NotificationSource, TracerStorage> tracerStorage = new HashMap<NotificationSource, TracerStorage>();

	/**
	 * This checks if tracer name is ok. It must not be null;
	 * 
	 * @param split
	 * @throws IllegalArgumentException
	 */
	public static void checkTracerName(String tracerName, NotificationSource notificationSource) throws IllegalArgumentException {

		if (tracerName.compareTo("") == 0) {
			// This is root
			return;
		}

		// String[] splitName = tracerName.split("\\.");
		StringTokenizer stringTokenizer = new StringTokenizer(tracerName, ".", true);

		int fqdnPartIndex = 0;

		// if(splitName.length==0)
		// {
		// throw new IllegalArgumentException("Passed tracer:" + tracerName +
		// ", name for source: " + notificationSource + ", is illegal");
		// }

		String lastToken = null;

		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if (lastToken == null) {
				// this is start
				lastToken = token;
			}

			if (lastToken.compareTo(token) == 0 && token.compareTo(".") == 0) {
				throw new IllegalArgumentException("Passed tracer:" + tracerName + ", name for source: " + notificationSource + ", is illegal");
			}

			if (token.compareTo(".") != 0) {
				for (int charIndex = 0; charIndex < token.length(); charIndex++) {
					Character c = token.charAt(charIndex);
					if (c.isLetter(c) || c.isDigit(c)) {
						// Its ok?
					} else {
						throw new IllegalArgumentException("Passed tracer:" + tracerName + " Token[" + token + "], name for source: " + notificationSource
								+ ", is illegal, contains illegal character: " + charIndex);
					}

				}

				fqdnPartIndex++;
			}
			lastToken = token;

		}

		if (lastToken.compareTo(".") == 0) {
			throw new IllegalArgumentException("Passed tracer:" + tracerName + ", name for source: " + notificationSource + ", is illegal");
		}

	}

	public void registerNotificationSource(final NotificationSource src) {
		if (this.tracerStorage.containsKey(src)) {

		} else {
			TracerStorage ts = new TracerStorage(src, this);
			this.tracerStorage.put(src, ts);
			
		}
		
		
		try {
			if(SleeContainer.lookupFromJndi().getTransactionManager().getTransaction()!=null)
			{
				TransactionalAction action = new TransactionalAction() {
					NotificationSource notiSrc = src;
					public void execute() {
						tracerStorage.remove(notiSrc);
					}
				};
				SleeContainer.lookupFromJndi().getTransactionManager().addAfterRollbackAction(action);
			}
		} catch (SystemException e) {
			
			e.printStackTrace();
		}
		
		
		
	}

	/**
	 * This method shoudl be called on on removal of notification source from
	 * slee
	 * 
	 * @param src
	 */
	public void deregisterNotificationSource(final NotificationSource src) {
		
		final TracerStorage st=this.tracerStorage.remove(src);
		
		try {
			if(SleeContainer.lookupFromJndi().getTransactionManager().getTransaction()!=null)
			{
				TransactionalAction action = new TransactionalAction() {
					NotificationSource notiSrc = src;
					TracerStorage storage = st;
					public void execute() {
						tracerStorage.put(src,storage);
					}
				};
				SleeContainer.lookupFromJndi().getTransactionManager().addAfterRollbackAction(action);
			}
		} catch (SystemException e) {
			
			e.printStackTrace();
		}
	}

	public boolean isNotificationSourceDefined(NotificationSource src) {
		return this.tracerStorage.containsKey(src);
	}

	public boolean isTracerDefined(NotificationSource src, String tracerName) throws ManagementException {
		TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}

		return ts.isTracerDefined(tracerName);
	}

	/**
	 * This method creates tracer for specified source, with specified name.
	 * boolean flag indicates that tracer has been requested by src, else, its
	 * just management operation. This method can me invoked multiple times.
	 * Only difference is boolean flag.
	 * 
	 * @param src
	 *            - notification source
	 * @param tracerName
	 *            - tracer name
	 * @param createdBySource
	 *            - flag indicating that src requested this tracer. In case
	 *            tracer already exists (created by mgmt operation) and this
	 *            method is invoked from NotificationSource this flag is set to
	 *            true. This alters state of tracer. It can not be changed back
	 * @return Tracer object. Either newly created or one that previously
	 *         existed.
	 * 
	 */
	public Tracer createTracer(NotificationSource src, String tracerName, boolean createdBySource) throws ManagementException {

		TracerStorage ts = this.tracerStorage.get(src);
		if (ts == null) {
			throw new ManagementException("NotificationSource has been uninstalled from SLEE. Can not create tracer.");
		}

		return ts.createTracer(tracerName, createdBySource);

	}
}
