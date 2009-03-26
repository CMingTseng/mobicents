package org.mobicents.slee.container.profile;

import java.util.Collections;
import java.util.LinkedList;

import javax.slee.SLEEException;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.profile.UnrecognizedProfileTableNameException;
import javax.transaction.SystemException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.runtime.facilities.MNotificationSource;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

/**
 * 
 * Start time:17:47:16 2009-03-25<br>
 * Project: mobicents-jainslee-server-core<br>
 * This is class that carefully records calls to profile classes. On calls
 * within transaction this records profile name that has been called. In case of
 * reentrant profile we look for:
 * <ul>
 * <li>loop call - if currently called profile is reentrant && call names
 * contain this profile && last call name is not this profile</li>
 * <li>calls from different thread</li> --- how should we do that ? lol
 * </ul>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * 
 */
public class ProfileCallRecorderTransactionData {

	// FIXME: Shoudl we also look up threads ?
	private final static SleeContainer sleeContainer = SleeContainer.lookupFromJndi();
	private static Logger logger = Logger.getLogger(ProfileCallRecorderTransactionData.class);
	private static final String TRANSACTION_CONTEXT_KEY = "pctd";

	/**
	 * a linked list with the which contains string values representing profile
	 * table - profile pairs that has been called within transaction.
	 */
	private final LinkedList<String> ivokedProfiles = new LinkedList<String>();
	/**
	 * Stores profile table name. This is required for alarma factility, as
	 * source changes with call to different profile table+profile pair.
	 */
	private final LinkedList<String> ivokedProfileTablesNames = new LinkedList<String>();

	/**
	 * Adds call to this profile.
	 * 
	 * @param po
	 * @throws SLEEException
	 */
	public static void addProfileCall(ProfileConcrete pc) throws TransactionRequiredLocalException, SLEEException {
		if (logger.isDebugEnabled()) {
			logger.debug("Recording call to profile, stored key: " + makeKey(pc));
		}
		SleeTransactionManager sleeTransactionManaget = sleeContainer.getTransactionManager();
		try {
			sleeTransactionManaget.mandateTransaction();

			ProfileCallRecorderTransactionData data = (ProfileCallRecorderTransactionData) sleeTransactionManaget.getTransactionContext().getData().get(TRANSACTION_CONTEXT_KEY);
			if (data == null) {
				data = new ProfileCallRecorderTransactionData();
				sleeTransactionManaget.getTransactionContext().getData().put(TRANSACTION_CONTEXT_KEY, data);
			}
			if (!pc.getProfileObject().isProfileReentrant()) {
				String key = makeKey(pc);
				// we need to check
				if (data.ivokedProfiles.contains(key) && data.ivokedProfiles.getLast().compareTo(key) != 0) {
					throw new SLEEException("Detected loopback call. Call sequence: " + data.ivokedProfiles);
				}
				data.ivokedProfiles.add(key);
				data.ivokedProfileTablesNames.add(pc.getProfileTableConcrete().getProfileTableName());
			}

		} catch (SystemException e) {
			throw new SLEEException("Failed to verify reentrancy due to some system level errror.", e);
		}

	}

	public static void removeProfileCall(ProfileConcrete pc) throws TransactionRequiredLocalException, SLEEException {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing call to profile, stored key: " + makeKey(pc));
		}
		SleeTransactionManager sleeTransactionManaget = sleeContainer.getTransactionManager();
		try {
			sleeTransactionManaget.mandateTransaction();

			ProfileCallRecorderTransactionData data = (ProfileCallRecorderTransactionData) sleeTransactionManaget.getTransactionContext().getData().get(TRANSACTION_CONTEXT_KEY);
			if (data == null) {
				throw new SLEEException("No Profile call recorder in memory, this is a bug.");

			}
			if (logger.isDebugEnabled()) {
				logger.debug("Removing call to profile, stored key: " + makeKey(pc) + ", last active table: " + data.ivokedProfileTablesNames.getLast());
			}
			if (!pc.getProfileObject().isProfileReentrant()) {
				String key = makeKey(pc);
				// we need to check
				String lastKey = data.ivokedProfiles.getLast();
				if (lastKey.compareTo(key) != 0) {
					{
						// logger.error("Last called profile does not match current: "
						// + key + ", last call: " + lastKey +
						// ". Please report this, it is a bug.");
						throw new SLEEException("Last called profile does not match current: " + key + ", last call: " + lastKey);
					}
				} else {
					data.ivokedProfiles.removeLast();
					data.ivokedProfileTablesNames.removeLast();
					if (data.ivokedProfiles.size() == 0) {
						sleeTransactionManaget.getTransactionContext().getData().remove(TRANSACTION_CONTEXT_KEY);
					}
				}

			}
		} catch (SystemException e) {
			throw new SLEEException("Failed to verify reentrancy due to some system level errror.", e);
		}
	}

	public static MNotificationSource getCurrentNotificationSource() throws TransactionRequiredLocalException, SLEEException{
		if(logger.isDebugEnabled())
		{
			logger.debug("Trying to get Notification source for profile table.");
		}
		SleeTransactionManager sleeTransactionManaget = sleeContainer.getTransactionManager();
		ProfileCallRecorderTransactionData data;
		try {
			data = (ProfileCallRecorderTransactionData) sleeTransactionManaget.getTransactionContext().getData().get(TRANSACTION_CONTEXT_KEY);
			if (data == null) {
				throw new SLEEException("No Profile call recorder in memory, this is a bug.");

			}

			//IF data is present, there is something in it.
			String tableName = data.ivokedProfileTablesNames.getLast();
			//FIXME: should we create new object? or lookup table? Lets do lookup
			ProfileTableConcrete ptc = sleeContainer.getSleeProfileTableManager().getProfileTable(tableName);
			return ptc.getProfileTableNotification();
		} catch (SystemException e) {
			throw new SLEEException("Failed to fetch notification source due to some system level errror.", e);
		} catch (UnrecognizedProfileTableNameException e) {
			throw new SLEEException("Failed to fetch notification source due to some system level errror.", e);
		}
		
	}

	private static String makeKey(ProfileConcrete po) {
		// FIXME: do we need ProfileConcrete.toString(); ??
		return po.getProfileTableConcrete().getProfileTableName() + "-" + po.getProfileName() + "-" + po.toString();
	}
}
