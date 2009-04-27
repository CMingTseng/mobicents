package org.mobicents.slee.container.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.slee.InvalidArgumentException;
import javax.slee.SLEEException;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.management.DeploymentException;
import javax.slee.profile.ProfileSpecificationID;
import javax.slee.profile.UnrecognizedProfileSpecificationException;
import javax.slee.profile.UnrecognizedProfileTableNameException;
import javax.transaction.SystemException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.component.ProfileSpecificationComponent;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.MEnvEntry;
import org.mobicents.slee.container.deployment.profile.SleeProfileClassCodeGenerator;
import org.mobicents.slee.container.deployment.profile.jpa.JPAUtils;
import org.mobicents.slee.container.profile.ProfileCmpHandler;
import org.mobicents.slee.container.profile.ProfileManagementHandler;
import org.mobicents.slee.container.profile.ProfileObject;
import org.mobicents.slee.container.profile.ProfileTableConcrete;
import org.mobicents.slee.container.profile.ProfileTableConcreteImpl;
import org.mobicents.slee.runtime.cache.ProfileManagementCacheData;
import org.mobicents.slee.runtime.facilities.ProfileAlarmFacilityImpl;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

/**
 * 
 * Start time:16:56:21 2009-03-13<br>
 * Project: mobicents-jainslee-server-core<br>
 * Class that manages ProfileSpecification, profile tables, ProfileObjects. It
 * is responsible for setting up profile specification env.
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class SleeProfileTableManager {

	private static final Logger logger = Logger.getLogger(SleeProfileTableManager.class);
	private final static SleeProfileClassCodeGenerator sleeProfileClassCodeGenerator = new SleeProfileClassCodeGenerator();
  private static final String DEFAULT_PROFILE_DB_NAME = "";
	private SleeContainer sleeContainer = null;
	private SleeTransactionManager sleeTransactionManager = null;

	// FIXME: Alex this has to be moved into cache structure
	/**
	 * This map contains mapping - profieltable name ---> profile table concrete
	 * object. see 10.2.4 section of JSLEE 1.1 specs - there can be only single
	 * profile profile table in SLEE container
	 * 
	 */
	//private ConcurrentHashMap nameToProfileTableMap = new ConcurrentHashMap();
	private ProfileManagementCacheData nameToProfileTableMap;

	public SleeProfileTableManager(SleeContainer sleeContainer) {
		super();
		if (sleeContainer == null)
			throw new NullPointerException("Parameter must not be null");
		this.sleeContainer = sleeContainer;
		this.sleeTransactionManager = this.sleeContainer.getTransactionManager();
		this.nameToProfileTableMap=this.sleeContainer.getCache().getProfileManagementCacheData();

	}

	/**
	 * Installs profile into container, creates default profile and reads
	 * backend storage to search for other profiles - it creates MBeans for all
	 * 
	 * @param component
	 * @throws DeploymentException
	 *             - this exception is thrown in case of error FIXME: on check
	 *             to profile - if we have one profile table active and we
	 *             encounter another, what shoudl happen? is there auto init for
	 *             all in back end memory?
	 */
	public void installProfileSpecification(ProfileSpecificationComponent component) throws DeploymentException {

		if (logger.isDebugEnabled()) {
			logger.debug("Installing " + component);
		}

		// FIXME:
		this.sleeTransactionManager.mandateTransaction();
		final SleeTransactionManager sleeTransactionManager = sleeContainer.getTransactionManager();
		sleeTransactionManager.mandateTransaction();

		// change classloader
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(component.getClassLoader());

			this.createJndiSpace(component);
			// FIXME: we wont use trace and alarm in 1.0 way wont we?
			this.sleeProfileClassCodeGenerator.process(component);
		} catch (DeploymentException de) {
			throw de;
		} catch (Throwable t) {
			throw new DeploymentException("Bad throwable, possible bug - this should be handled properly.", t);
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}

	}

	public void uninstallProfileSpecification(ProfileSpecificationComponent component) throws UnrecognizedProfileSpecificationException {
		//FIXME: Alex ?
		Collection<String> profileTableNames = getDeclaredProfileTableNames(component.getProfileSpecificationID());
		
		for(String profileTableName:profileTableNames)
		{
			try {
				this.getProfileTable(profileTableName).removeProfileTable();
			} catch (TransactionRequiredLocalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SLEEException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecognizedProfileTableNameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	/**
	 * This creates
	 */
	private void createJndiSpace(ProfileSpecificationComponent component) throws Exception {
		Context ctx = (Context) new InitialContext().lookup("java:comp");

		if (logger.isDebugEnabled()) {
			logger.debug("Setting up SBB env. Initial context is " + ctx);
		}

		Context envCtx = null;
		try {
			envCtx = ctx.createSubcontext("env");
		} catch (NameAlreadyBoundException ex) {
			envCtx = (Context) ctx.lookup("env");
		}

		Context sleeCtx = null;

		try {
			sleeCtx = envCtx.createSubcontext("slee");
		} catch (NameAlreadyBoundException ex) {
			sleeCtx = (Context) envCtx.lookup("slee");
		}

		Context facilitiesCtx = null;

		try {
			facilitiesCtx = sleeCtx.createSubcontext("facilities");
		} catch (NameAlreadyBoundException ex) {
			facilitiesCtx = (Context) sleeCtx.lookup("facilities");
		}
		
		ProfileAlarmFacilityImpl alarmFacility = new ProfileAlarmFacilityImpl(this.sleeContainer.getAlarmFacility());
		// FIXME: Alexandre: This should be AlarmFacility.JNDI_NAME. Any problem if already bound?
		try
		{
		  facilitiesCtx.bind("alarm", alarmFacility);
		}
		catch (NamingException e) {
      // ignore.
    }
		
		
		for (MEnvEntry mEnvEntry : component.getDescriptor().getEnvEntries()) {
			Class type = null;

			if (logger.isDebugEnabled()) {
				logger.debug("Got an environment entry:" + mEnvEntry);
			}

			try {
				type = Thread.currentThread().getContextClassLoader().loadClass(mEnvEntry.getEnvEntryType());
			} catch (Exception e) {
				throw new DeploymentException(mEnvEntry.getEnvEntryType() + " is not a valid type for an environment entry");
			}
			Object entry = null;
			String s = mEnvEntry.getEnvEntryValue();

			try {
				if (type == String.class) {
					entry = new String(s);
				} else if (type == Character.class) {
					if (s.length() != 1) {
						throw new DeploymentException(s + " is not a valid value for an environment entry of type Character");
					}
					entry = new Character(s.charAt(0));
				} else if (type == Integer.class) {
					entry = new Integer(s);
				} else if (type == Boolean.class) {
					entry = new Boolean(s);
				} else if (type == Double.class) {
					entry = new Double(s);
				} else if (type == Byte.class) {
					entry = new Byte(s);
				} else if (type == Short.class) {
					entry = new Short(s);
				} else if (type == Long.class) {
					entry = new Long(s);
				} else if (type == Float.class) {
					entry = new Float(s);
				}
			} catch (NumberFormatException e) {
				throw new DeploymentException("Environment entry value " + s + " is not a valid value for type " + type);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Binding environment entry with name:" + mEnvEntry.getEnvEntryName() + " type  " + entry.getClass() + " with value:" + entry + ". Current classloader = "
						+ Thread.currentThread().getContextClassLoader());
			}
			try {
				envCtx.bind(mEnvEntry.getEnvEntryName(), entry);
			} catch (NameAlreadyBoundException ex) {
				logger.error("Name already bound ! ", ex);
			}
		}

	}

	public ProfileTableConcrete getProfileTable(String profileTableName) throws SLEEException, UnrecognizedProfileTableNameException
	{

		try {
			ProfileTableConcrete ptc = (ProfileTableConcrete) this.nameToProfileTableMap.get(profileTableName);
			if (ptc == null)
				throw new UnrecognizedProfileTableNameException();

			// FIXME: add activity check to see if we are beeing removed ?

			return ptc;
		} catch (UnrecognizedProfileTableNameException e) {
			throw e;
		} catch (Exception e) {
			throw new SLEEException("Failed to fetch ProfileTable due to unknown error, please report.", e);
		}
	}

	public SleeContainer getSleeContainer() {
		return sleeContainer;
	}

	public ProfileSpecificationComponent getProfileSpecificationComponent(ProfileSpecificationID profileSpecificationId) {
		// FIXME: we posbily dont need this.
		return this.sleeContainer.getComponentRepositoryImpl().getComponentByID(profileSpecificationId);
	}

	public ProfileTableConcrete addProfileTable(String profileTableName, ProfileSpecificationComponent component) throws TransactionRequiredLocalException, SystemException, ClassNotFoundException, NullPointerException, InvalidArgumentException
	{
		//throw new UnsupportedOperationException();

		this.sleeTransactionManager.mandateTransaction();
		//this.nameToProfileTableMap.add(profileTableName, null);

		ProfileTableConcreteImpl profileTable = new ProfileTableConcreteImpl(this, profileTableName, component.getProfileSpecificationID());
		this.nameToProfileTableMap.add(profileTableName, profileTable);
		profileTable.register();

		// 1. Instantiate CMP Impl
		try
    {
		  Thread.currentThread().setContextClassLoader( component.getClassLoader() );
		  
      Class profileCmpClass = Thread.currentThread().getContextClassLoader().loadClass(component.getProfileCmpInterfaceClass().getName() + "Impl");
      
      Constructor profileCmpConstructor = profileCmpClass.getConstructor(ProfileManagementHandler.class);

      // FIXME: Alexandre: Is this what we should be passing? 
      ProfileManagementHandler argPMH = new ProfileManagementHandler();
      argPMH.setProfileObject( new ProfileObject(profileTable, component.getProfileSpecificationID()));
      argPMH.setProfileCmpHandler( new ProfileCmpHandler() );
      
      Object profileCmp = profileCmpConstructor.newInstance(argPMH);
      
      Method mSetTableName = profileCmpClass.getMethod( "setTableName", String.class );
      mSetTableName.invoke( profileCmp, profileTableName );
      
      Method mSetProfileName = profileCmpClass.getMethod( "setProfileName", String.class );
      mSetProfileName.invoke( profileCmp, DEFAULT_PROFILE_DB_NAME );
      
      JPAUtils.INSTANCE.getEntityManager(component.getComponentID()).persist( profileCmp );
    }
    catch ( Exception e )
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
		
		// FIXME: mayeb here we shoudl add default profile?
		return profileTable;
	}

	public Collection<String> getDeclaredProfileTableNames() {
		return Collections.unmodifiableCollection(this.nameToProfileTableMap.getProfileTables());
	}

	public Collection<String> getDeclaredProfileTableNames(ProfileSpecificationID id) throws UnrecognizedProfileSpecificationException {

		if (this.sleeContainer.getComponentRepositoryImpl().getComponentByID(id) == null) {
			throw new UnrecognizedProfileSpecificationException("No such profile specification: " + id);
		}
		ArrayList<String> names = new ArrayList<String>();

		// FIXME: this will fail if done async to change, is this ok ?
		Iterator<String> it = this.getDeclaredProfileTableNames().iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (((ProfileTableConcrete) this.nameToProfileTableMap.get(name)).getProfileSpecificationComponent().getProfileSpecificationID().equals(id)) {
				names.add(name);
			}
		}

		return names;
	}

	public void removeProfileTable(ProfileTableConcreteImpl profileTableConcreteImpl) {
		// FIXME: add more ?

	  this.nameToProfileTableMap.remove(profileTableConcreteImpl.getProfileTableName());
	}

	public void startAllProfileTableActivities() {
		for (Object key : this.getDeclaredProfileTableNames()) {
			ProfileTableConcreteImpl pt = (ProfileTableConcreteImpl) this.nameToProfileTableMap.get((String)key);
			pt.register();
		}

	}

}
