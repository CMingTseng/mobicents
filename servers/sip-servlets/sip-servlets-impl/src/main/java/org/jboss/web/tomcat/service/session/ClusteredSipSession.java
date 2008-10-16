/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.web.tomcat.service.session;

import gov.nist.javax.sip.stack.SIPDialog;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.security.Principal;
import java.text.ParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.sip.address.URI;

import org.apache.catalina.Globals;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.metadata.WebMetaData;
import org.mobicents.servlet.sip.SipFactories;
import org.mobicents.servlet.sip.address.SipURIImpl;
import org.mobicents.servlet.sip.address.TelURLImpl;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
import org.mobicents.servlet.sip.core.session.SessionManagerUtil;
import org.mobicents.servlet.sip.core.session.SipApplicationSessionKey;
import org.mobicents.servlet.sip.core.session.SipSessionImpl;
import org.mobicents.servlet.sip.core.session.SipSessionKey;
import org.mobicents.servlet.sip.message.B2buaHelperImpl;
import org.mobicents.servlet.sip.message.SipFactoryImpl;
import org.mobicents.servlet.sip.proxy.ProxyImpl;

/**
 * Abstract base class for sip session clustering based on SipSessionImpl. Different session
 * replication strategy can be implemented such as session- field- or attribute-based ones.
 * 
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public abstract class ClusteredSipSession extends SipSessionImpl  
	implements Externalizable {

	private transient static final Log logger = LogFactory.getLog(ClusteredSipSession.class);

	/**
	 * Descriptive information describing this Session implementation.
	 */
	protected static final String info = "ClusteredSipSession/1.0";

	/**
	 * Set of attribute names which are not allowed to be replicated/persisted.
	 */
	protected static final String[] excludedAttributes = { Globals.SUBJECT_ATTR };

	/**
	 * Set containing all members of {@link #excludedAttributes}.
	 */
	protected static final Set replicationExcludes;
	static {
		HashSet set = new HashSet();
		for (int i = 0; i < excludedAttributes.length; i++) {
			set.add(excludedAttributes[i]);
		}
		replicationExcludes = Collections.unmodifiableSet(set);
	}

	protected int invalidationPolicy;

	/**
	 * If true, means the local in-memory session data contains changes that
	 * have not been published to the distributed cache.
	 * 
	 * @deprecated not used
	 */
	protected transient boolean isSessionModifiedSinceLastSave;

	/**
	 * If true, means the local in-memory session data contains metadata changes
	 * that have not been published to the distributed cache.
	 */
	protected transient boolean sessionMetadataDirty;

	/**
	 * If true, means the local in-memory session data contains attribute
	 * changes that have not been published to the distributed cache.
	 */
	protected transient boolean sessionAttributesDirty;

	/**
	 * The last version that was passed to {@link #setOutdatedVersion} or
	 * <code>0</code> if <code>setIsOutdated(false)</code> was subsequently
	 * called.
	 */
	protected transient int outdatedVersion;

	/**
	 * The last time {@link #setIsOutdated setIsOutdated(true)} was called or
	 * <code>0</code> if <code>setIsOutdated(false)</code> was subsequently
	 * called.
	 */
	protected transient long outdatedTime;

	/**
	 * Version number to track cache invalidation. If any new version number is
	 * greater than this one, it means the data it holds is newer than this one.
	 */
	protected int version;

	/**
	 * The session's id with any jvmRoute removed.
	 */
//	protected transient String realId;

	/**
	 * Whether JK is being used, in which case our realId will not match our id
	 */
	private transient boolean useJK;

	/**
	 * Timestamp when we were last replicated.
	 */
	protected transient long lastReplicated;

	/**
	 * Maximum percentage of the inactive interval this session should be
	 * allowed to go unreplicated if access to the session doesn't mark it as
	 * dirty. Drives the calculation of maxUnreplicatedInterval.
	 */
	protected transient int maxUnreplicatedFactor = 80;

	/**
	 * Maximum number of milliseconds this session should be allowed to go
	 * unreplicated if access to the session doesn't mark it as dirty.
	 */
	protected transient long maxUnreplicatedInterval;

	/**
	 * Whether any of this session's attributes implement
	 * HttpSessionActivationListener.
	 */
	protected transient Boolean hasActivationListener;

	/**
	 * Has this session only been accessed once?
	 */
	protected transient boolean firstAccess;
	
	protected transient SipApplicationSessionKey sipAppSessionParentKey;

	/**
	 * The string manager for this package.
	 */
	protected static StringManager sm = StringManager
			.getManager(ClusteredSession.class.getPackage().getName());


	protected ClusteredSipSession(SipSessionKey key, SipFactoryImpl sipFactoryImpl, MobicentsSipApplicationSession mobicentsSipApplicationSession, boolean useJK) {
		super(key, sipFactoryImpl, mobicentsSipApplicationSession);
		invalidationPolicy = ((AbstractJBossManager)mobicentsSipApplicationSession.getSipContext().getSipManager()).getInvalidateSessionPolicy();
		this.useJK = useJK;
		this.firstAccess = true;
		calcMaxUnreplicatedInterval();
	}

	/**
	 * Check to see if the session data is still valid. Outdated here means that
	 * the in-memory data is not in sync with one in the data store.
	 * 
	 * @return
	 */
	public boolean isOutdated() {
		return lastAccessedTime < outdatedTime;
	}

	/**
	 * Marks this session as outdated or up-to-date vis-a-vis the distributed
	 * cache.
	 * 
	 * @param outdated
	 * 
	 * @deprecated use {@link #setOutdatedVersion(int)} and
	 *             {@link #clearOutdated()}
	 */
	public void setIsOutdated(boolean outdated) {
		if (outdated)
			outdatedTime = System.currentTimeMillis();
		else
			clearOutdated();
	}

	public void setOutdatedVersion(int version) {
		this.outdatedVersion = version;
		outdatedTime = System.currentTimeMillis();
	}

	public void clearOutdated() {
		// Only overwrite the access time if access() hasn't been called
		// since setOutdatedVersion() was called
		//FIXME
//		if (outdatedTime > lastAccessedTime) {
//			lastAccessedTime = thisAccessedTime;
//			thisAccessedTime = outdatedTime;
//		}
		outdatedTime = 0;

		// Only overwrite the version if the outdated version is greater
		// Otherwise when we first unmarshal a session that has been
		// replicated many times, we will reset the version to 0
		if (outdatedVersion > version)
			version = outdatedVersion;

		outdatedVersion = 0;
	}

	public void updateAccessTimeFromOutdatedTime() {
		//FIXME
//		if (outdatedTime > thisAccessedTime) {
//			lastAccessedTime = thisAccessedTime;
//			thisAccessedTime = outdatedTime;
//		}
		outdatedTime = 0;
	}

	/**
	 * Gets the session id with any appended jvmRoute info removed.
	 * 
	 * @see #getUseJK()
	 */
//	public String getRealId() {
//		return realId;
//	}

//	private void parseRealId(String sessionId) {
//		String newId = null;
//		if (useJK)
//			newId = Util.getRealId(sessionId);
//		else
//			newId = sessionId;
//
//		// realId is used in a lot of map lookups, so only replace it
//		// if the new id is actually different -- preserve object identity
//		if (!newId.equals(realId))
//			realId = newId;
//	}

	/**
	 * This is called specifically for failover case using mod_jk where the new
	 * session has this node name in there. As a result, it is safe to just
	 * replace the id since the backend store is using the "real" id without the
	 * node name.
	 * 
	 * @param id
	 */
	//FIXME we don't need this for SIP since the route is the call id and is handled by the LB,
	//is it worth encoding it in the SIP message
//	public void resetIdWithRouteInfo(String id) {
//		this.id = id;
//		parseRealId(id);
//	}

	public boolean getUseJK() {
		return useJK;
	}

	/**
	 * Check to see if the input version number is greater than I am. If it is,
	 * it means we will need to invalidate the in-memory cache.
	 * 
	 * @param version
	 * @return
	 */
	public boolean isNewData(int version) {
		return (this.version < version);
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * There are couple ways to generate this version number. But we will stick
	 * with the simple one of incrementing for now.
	 * 
	 * @return the new version
	 */
	public int incrementVersion() {
		return version++;
	}

	/**
	 * Gets the maximum percentage of the <code>maxInactiveInterval</code>
	 * beyond which a session should be replicated upon access even if it isn't
	 * dirty. Used to ensure that even a read-only session gets replicated
	 * before it expires, so that it isn't removed from other nodes.
	 * 
	 * @return an int between 1 and 100, or -1 if replicating on access is
	 *         disabled
	 */
	public int getMaxUnreplicatedFactor() {
		return maxUnreplicatedFactor;
	}

	/**
	 * Sets the maximum percentage of the <code>maxInactiveInterval</code>
	 * beyond which a session should be replicated upon access even if it isn't
	 * dirty. Used to ensure that even a read-only session gets replicated
	 * before it expires, so that it isn't removed from other nodes.
	 * 
	 * @param maxUnreplicatedFactor
	 *            an int between 1 and 100, or -1 to disable replicating on
	 *            access
	 * 
	 * @throws IllegalArgumentException
	 *             if the factor isn't -1 or between 1 and 100
	 */
	public void setMaxUnreplicatedFactor(int factor) {
		if ((factor != -1 && factor < 1) || factor > 100)
			throw new IllegalArgumentException("Invalid factor " + factor
					+ " -- must be between 1 and 100 or -1");
		this.maxUnreplicatedFactor = factor;
		calcMaxUnreplicatedInterval();
	}

	/**
	 * Gets the time {@link #updateLastReplicated()} was last called, or
	 * <code>0</code> if it has never been called.
	 */
	public long getLastReplicated() {
		return lastReplicated;
	}

	/**
	 * Sets the {@link #getLastReplicated() lastReplicated} field to the current
	 * time.
	 */
	public void updateLastReplicated() {
		lastReplicated = System.currentTimeMillis();
	}

	public long getMaxUnreplicatedInterval() {
		return maxUnreplicatedInterval;
	}

	public boolean getExceedsMaxUnreplicatedInterval() {
		boolean result = false;

		if (maxUnreplicatedInterval > 0) // -1 means ignore; 0 means expire now
		{
			result = ((System.currentTimeMillis() - lastReplicated) >= maxUnreplicatedInterval);
		}

		return result;
	}

	private void calcMaxUnreplicatedInterval() {
//		if (maxInactiveInterval < 0 || maxUnreplicatedFactor < 0)
			maxUnreplicatedInterval = -1;
//		else {
//			// Ignoring casting would be (mII * 1000) * (mUF / 100)
//			maxUnreplicatedInterval = maxInactiveInterval
//					* maxUnreplicatedFactor * 10;
//		}
	}

	/**
	 * This is called after loading a session to initialize the transient
	 * values.
	 * 
	 * @param manager
	 */
	public abstract void initAfterLoad(JBossCacheSipManager manager);

	/**
	 * Propogate session to the internal store.
	 */
	public abstract void processSessionRepl();

	/**
	 * Remove myself from the internal store.
	 */
	public abstract void removeMyself();

	/**
	 * Remove myself from the <t>local</t> internal store.
	 */
	public abstract void removeMyselfLocal();

	// ----------------------------------------------- Overridden Public Methods

	public void access() {
		super.access();

		// JBAS-3528. If it's not the first access, make sure
		// the 'new' flag is correct
		//SipSession doesn't have the isNew flag
//		if (!firstAccess && isNew) {
//			setNew(false);
//		}

		if (invalidationPolicy == WebMetaData.SESSION_INVALIDATE_ACCESS) {
			this.sessionMetadataDirty();
		}
	}

//	public void endAccess() {
//		super.endAccess();
//
//		if (firstAccess) {
//			firstAccess = false;
//			// Tomcat marks the session as non new, but that's not really
//			// accurate per SRV.7.2, as the second request hasn't come in yet
//			// So, we fix that
//			isNew = true;
//		}
//	}

	public Object getAttribute(String name) {

		if (!isValid())
			throw new IllegalStateException(sm
					.getString("clusteredSession.getAttribute.ise"));

		return getAttributeInternal(name);
	}

	public Enumeration getAttributeNames() {
		if (!isValid())
			throw new IllegalStateException(sm
					.getString("clusteredSession.getAttributeNames.ise"));

		return (new Enumerator(getAttributesInternal().keySet(), true));
	}

	public void setAttribute(String name, Object value) {
		// Name cannot be null
		if (name == null)
			throw new IllegalArgumentException(sm
					.getString("clusteredSession.setAttribute.namenull"));

		// Null value is the same as removeAttribute()
		if (value == null) {
			removeAttribute(name);
			return;
		}

		// Validate our current state
		if (!isValid())
			throw new IllegalStateException(sm
					.getString("clusteredSession.setAttribute.ise"));
		if ((getSipApplicationSession().getSipContext().getSipManager() != null) && getSipApplicationSession().getSipContext().getSipManager().getDistributable()
				&& !(canAttributeBeReplicated(value)))
			throw new IllegalArgumentException(sm
					.getString("clusteredSession.setAttribute.iae"));

		// Construct an event with the new value
		SipSessionBindingEvent event = null;

		// Call the valueBound() method if necessary
		if (value instanceof SipSessionBindingListener) {
			event = new SipSessionBindingEvent(this, name);
			try {
				((SipSessionBindingListener) value).valueBound(event);
			} catch (Throwable t) {
				sipApplicationSession.getSipContext().getLogger().error(
						sm.getString("standardSession.bindingEvent"), t);
			}
		}

		// Replace or add this attribute
		Object unbound = setInternalAttribute(name, value);

		// Call the valueUnbound() method if necessary
		if ((unbound != null) && (unbound != value)
				&& (unbound instanceof SipSessionBindingListener)) {
			try {
				((SipSessionBindingListener) unbound)
						.valueUnbound(new SipSessionBindingEvent(this,
								name));
			} catch (Throwable t) {
				sipApplicationSession.getSipContext().getLogger().error(
						sm.getString("standardSession.bindingEvent"), t);
			}
		}

		// Notify interested application event listeners
		List<SipSessionAttributeListener> listeners = getSipApplicationSession().getSipContext().getListeners().getSipSessionAttributeListeners();
		if (listeners == null)
			return;
		for (SipSessionAttributeListener listener : listeners) {
			try {
				if (unbound != null) {
//					fireContainerEvent(context,
//							"beforeSessionAttributeReplaced", listener);
					if (event == null) {
						event = new SipSessionBindingEvent(this, name);
					}
					listener.attributeReplaced(event);
//					fireContainerEvent(context,
//							"afterSessionAttributeReplaced", listener);
				} else {
//					fireContainerEvent(context, "beforeSessionAttributeAdded",
//							listener);
					if (event == null) {
						event = new SipSessionBindingEvent(this, name);
					}
					listener.attributeAdded(event);
//					fireContainerEvent(context, "afterSessionAttributeAdded",
//							listener);
				}
			} catch (Throwable t) {
				try {
//					if (unbound != null) {
//						fireContainerEvent(context,
//								"afterSessionAttributeReplaced", listener);
//					} else {
//						fireContainerEvent(context,
//								"afterSessionAttributeAdded", listener);
//					}
				} catch (Exception e) {
					;
				}
				sipApplicationSession.getSipContext().getLogger().error(
						sm.getString("standardSession.attributeEvent"), t);
			}
		}
	}

	/**
	 * Returns whether the attribute's type is one that can be replicated.
	 * 
	 * @param attribute
	 *            the attribute
	 * @return <code>true</code> if <code>attribute</code> is <code>null</code>,
	 *         <code>Serializable</code> or an array of primitives.
	 */
	protected boolean canAttributeBeReplicated(Object attribute) {
		if (attribute instanceof Serializable || attribute == null)
			return true;
		Class clazz = attribute.getClass().getComponentType();
		return (clazz != null && clazz.isPrimitive());
	}

	/**
	 * Invalidates this session and unbinds any objects bound to it. Overridden
	 * here to remove across the cluster instead of just expiring.
	 * 
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated session
	 */
//	public void invalidate() {
//		if (!isValid())
//			throw new IllegalStateException(sm
//					.getString("clusteredSession.invalidate.ise"));

		// Cause this session to expire globally
//		boolean notify = true;
//		boolean localCall = true;
//		boolean localOnly = false;
//		expire(notify, localCall, localOnly);
//	}

	/**
	 * Overrides the {@link StandardSession#isValid() superclass method} to call @
	 * #isValid(boolean) isValid(true)} .
	 */
	public boolean isValid() {
		return isValid(true);
	}

	/**
	 * Returns whether the current session is still valid, but only calls
	 * {@link #expire(boolean)} for timed-out sessions if
	 * <code>expireIfInvalid</code> is <code>true</code>.
	 * 
	 * @param expireIfInvalid
	 *            <code>true</code> if sessions that have been timed out should
	 *            be expired
	 */
	public boolean isValid(boolean expireIfInvalid) {
//		if (this.expiring) {
//			return true;
//		}

		if (!this.isValid) {
			return false;
		}

//		if (ACTIVITY_CHECK && accessCount.get() > 0) {
//			return true;
//		}

//		if (maxInactiveInterval >= 0) {
//			long timeNow = System.currentTimeMillis();
//			int timeIdle = (int) ((timeNow - thisAccessedTime) / 1000L);
//			if (timeIdle >= maxInactiveInterval) {
//				if (expireIfInvalid)
//					expire(true);
//				else
//					return false;
//			}
//		}

		return (this.isValid);

	}

	/**
	 * Expires the session, but in such a way that other cluster nodes are
	 * unaware of the expiration.
	 * 
	 * @param notify
	 */
	public void expire(boolean notify) {
		boolean localCall = true;
		boolean localOnly = true;
		expire(notify, localCall, localOnly);
	}

	/**
	 * Expires the session, notifying listeners and possibly the manager.
	 * <p>
	 * <strong>NOTE:</strong> The manager will only be notified of the
	 * expiration if <code>localCall</code> is <code>true</code>; otherwise it
	 * is the responsibility of the caller to notify the manager that the
	 * session is expired. (In the case of JBossCacheManager, it is the manager
	 * itself that makes such a call, so it of course is aware).
	 * </p>
	 * 
	 * @param notify
	 *            whether servlet spec listeners should be notified
	 * @param localCall
	 *            <code>true</code> if this call originated due to local
	 *            activity (such as a session invalidation in user code or an
	 *            expiration by the local background processing thread);
	 *            <code>false</code> if the expiration originated due to some
	 *            kind of event notification from the cluster.
	 * @param localOnly
	 *            <code>true</code> if the expiration should not be announced to
	 *            the cluster, <code>false</code> if other cluster nodes should
	 *            be made aware of the expiration. Only meaningful if
	 *            <code>localCall</code> is <code>true</code>.
	 */
	public void expire(boolean notify, boolean localCall, boolean localOnly) {
		if (logger.isDebugEnabled()) {
			logger.debug("The session has expired with id: " + key
					+ " -- is it local? " + localOnly);
		}

		// If another thread is already doing this, stop
//		if (expiring)
//			return;

		synchronized (this) {
			// If we had a race to this sync block, another thread may
			// have already completed expiration. If so, don't do it again
			if (!isValid)
				return;

			if (getSipApplicationSession().getSipContext().getSipManager() == null)
				return;

//			expiring = true;

			// Notify interested application event listeners
			// FIXME - Assumes we call listeners in reverse order
			List<SipSessionListener> listeners = getSipApplicationSession().getSipContext().getListeners().getSipSessionListeners();
			if (notify && (listeners != null)) {
				SipSessionEvent event = new SipSessionEvent(this);
				for (SipSessionListener listener : listeners) {
					try {
//						fireContainerEvent(context, "beforeSessionDestroyed",
//								listener);
						listener.sessionDestroyed(event);
//						fireContainerEvent(context, "afterSessionDestroyed",
//								listener);
					} catch (Throwable t) {
//						try {
//							fireContainerEvent(context,
//									"afterSessionDestroyed", listener);
//						} catch (Exception e) {
//							;
//						}
						getSipApplicationSession().getSipContext().getLogger()
								.error(sm.getString("standardSession.sessionEvent"),t);
					}
				}
			}
//			if (ACTIVITY_CHECK) {
//				accessCount.set(0);
//			}

			// Notify interested session event listeners.
//			if (notify) {
//				fireSessionEvent(Session.SESSION_DESTROYED_EVENT, null);
//			}

			// JBAS-1360 -- Unbind any objects associated with this session
			String keys[] = keys();
			for (int i = 0; i < keys.length; i++)
				removeAttributeInternal(keys[i], localCall, localOnly, notify);

			// Remove this session from our manager's active sessions
			removeFromManager(localCall, localOnly);

			// We have completed expire of this session
//			setValid(false);
//			expiring = false;
		}

	}

	/**
	 * Advise our manager to remove this expired session.
	 * 
	 * @param localCall
	 *            whether this call originated from local activity or from a
	 *            remote invalidation. In this default implementation, this
	 *            parameter is ignored.
	 * @param localOnly
	 *            whether the rest of the cluster should be made aware of the
	 *            removal
	 */
	protected void removeFromManager(boolean localCall, boolean localOnly) {
		if (localOnly) {
			((JBossCacheSipManager) getSipApplicationSession().getSipContext().getSipManager()).removeLocal(this);
		} else {
			getSipApplicationSession().getSipContext().getSipManager().removeSipSession(key);
		}
	}

	public void passivate() {
		// Notify interested session event listeners
//		fireSessionEvent(Session.SESSION_PASSIVATED_EVENT, null);

		if (hasActivationListener != Boolean.FALSE) {
			boolean hasListener = false;

			// Notify ActivationListeners
			SipSessionEvent event = null;
			String keys[] = keys();
			Map attrs = getAttributesInternal();
			for (int i = 0; i < keys.length; i++) {
				Object attribute = attrs.get(keys[i]);
				if (attribute instanceof SipSessionActivationListener) {
					hasListener = true;

					if (event == null)
						event = new SipSessionEvent(this);
					try {
						((SipSessionActivationListener) attribute)
								.sessionWillPassivate(event);
					} catch (Throwable t) {
						getSipApplicationSession().getSipContext().getSipManager()
								.getContainer()
								.getLogger()
								.error(
										sm
												.getString("clusteredSession.attributeEvent"),
										t);
					}
				}
			}

			hasActivationListener = hasListener ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	public void activate() {
		// Notify interested session event listeners
//		fireSessionEvent(Session.SESSION_ACTIVATED_EVENT, null);

		if (hasActivationListener != Boolean.FALSE) {
			// Notify ActivationListeners

			boolean hasListener = false;

			SipSessionEvent event = null;
			String keys[] = keys();
			Map attrs = getAttributesInternal();
			for (int i = 0; i < keys.length; i++) {
				Object attribute = attrs.get(keys[i]);
				if (attribute instanceof SipSessionActivationListener) {
					hasListener = true;
					if (event == null)
						event = new SipSessionEvent(this);
					try {
						((SipSessionActivationListener) attribute)
								.sessionDidActivate(event);
					} catch (Throwable t) {
						getSipApplicationSession().getSipContext().getSipManager()
								.getContainer()
								.getLogger()
								.error(
										sm
												.getString("clusteredSession.attributeEvent"),
										t);
					}
				}
			}

			hasActivationListener = hasListener ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	// TODO uncomment when work on JBAS-1900 is completed
	// public void removeNote(String name)
	// {
	// // FormAuthenticator removes the username and password because
	// // it assumes they are not needed if the Principal is cached,
	// // but they are needed if the session fails over, so ignore
	// // the removal request.
	// // TODO discuss this on Tomcat dev list to see if a better
	// // way of handling this can be found
	// if (Constants.SESS_USERNAME_NOTE.equals(name)
	// || Constants.SESS_PASSWORD_NOTE.equals(name))
	// {
	// if (logger.isDebugEnabled())
	// {
	// logger.debug("removeNote(): ignoring removal of note " + name);
	// }
	// }
	// else
	// {
	// super.removeNote(name);
	// }
	//	      
	// }

	// TODO uncomment when work on JBAS-1900 is completed
	// public void setNote(String name, Object value)
	// {
	// super.setNote(name, value);
	//	      
	// if (Constants.SESS_USERNAME_NOTE.equals(name)
	// || Constants.SESS_PASSWORD_NOTE.equals(name))
	// {
	// sessionIsDirty();
	// }
	// }

	/**
	 * Override the superclass to additionally reset this class' fields.
	 * <p>
	 * <strong>NOTE:</strong> It is not anticipated that this method will be
	 * called on a ClusteredSession, but we are overriding the method to be
	 * thorough.
	 * </p>
	 */
//	public void recycle() {
//		super.recycle();
//
//		// Fields that the superclass isn't clearing
//		listeners.clear();
//		support = new PropertyChangeSupport(this);
//
//		invalidationPolicy = 0;
//		outdatedTime = 0;
//		outdatedVersion = 0;
//		sessionAttributesDirty = false;
//		sessionMetadataDirty = false;
//		realId = null;
//		useJK = false;
//		version = 0;
//		hasActivationListener = null;
//		lastReplicated = 0;
//		maxUnreplicatedFactor = 80;
//		calcMaxUnreplicatedInterval();
//	}

	/**
	 * Set the creation time for this session. This method is called by the
	 * Manager when an existing Session instance is reused.
	 * 
	 * @param time
	 *            The new creation time
	 */
//	public void setCreationTime(long time) {
//		super.setCreationTime(time);
//		sessionMetadataDirty();
//	}

	/**
	 * Overrides the superclass method to also set the {@link #getRealId()
	 * realId} property.
	 */
//	public void setId(String id) {
//		// Parse the real id first, as super.setId() calls add(),
//		// which depends on having the real id
//		parseRealId(id);
//		super.setId(id);
//	}

	/**
	 * Set the authenticated Principal that is associated with this Session.
	 * This provides an <code>Authenticator</code> with a means to cache a
	 * previously authenticated Principal, and avoid potentially expensive
	 * <code>Realm.authenticate()</code> calls on every request.
	 * 
	 * @param principal
	 *            The new Principal, or <code>null</code> if none
	 */
	public void setPrincipal(Principal principal) {

		Principal oldPrincipal = this.userPrincipal;
		this.userPrincipal = principal;
//		support.firePropertyChange("principal", oldPrincipal, this.principal);

		if ((oldPrincipal != null && !oldPrincipal.equals(principal))
				|| (oldPrincipal == null && principal != null))
			sessionMetadataDirty();

	}

//	public void setNew(boolean isNew) {
//		super.setNew(isNew);
//		// Don't replicate metadata just 'cause its the second request
//		// The only effect of this is if someone besides a request
//		// deserializes metadata from the distributed cache, this
//		// field may be out of date.
//		// If a request accesses the session, the access() call will
//		// set isNew=false, so the request will see the correct value
//		// sessionMetadataDirty();
//	}

//	public void setValid(boolean isValid) {
//		super.setValid(isValid);
//		sessionMetadataDirty();
//	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("key: " + key)
				.append(" lastAccessedTime: " + lastAccessedTime).append(
						" version: " + version).append(
						" lastOutdated: " + outdatedTime);

		return buf.toString();
	}

	// --------------------------------------------------------- Externalizable

	/**
	 * Reads all non-transient state from the ObjectOutput <i>except the
	 * attribute map</i>. Subclasses that wish the attribute map to be read
	 * should override this method and {@link #writeExternal(ObjectOutput)
	 * writeExternal()}.
	 * 
	 * <p>
	 * This method is deliberately public so it can be used to reset the
	 * internal state of a session object using serialized contents replicated
	 * from another JVM via JBossCache.
	 * </p>
	 * 
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		if(logger.isDebugEnabled()) {
			logger.debug("reading sip session from the cache ");
		}
		synchronized (this) {
			// From SipSessionImpl
			String fromAddress = in.readUTF();
			String fromTag = in.readUTF();
			String toAddress = in.readUTF();
			String toTag = in.readUTF();
			if("".equals(toTag)) {
				toTag = null;
			}
			String callId = in.readUTF();
			String applicationName = in.readUTF();
			key = new SipSessionKey(fromAddress,fromTag,toAddress, toTag, callId, applicationName);
			if(logger.isDebugEnabled()) {
				logger.debug("reading sip session from the cache. sip session key = " + key);
			}
			String parentAppSessionKey = in.readUTF();
			try {
				sipAppSessionParentKey = SessionManagerUtil.parseSipApplicationSessionKey(parentAppSessionKey);
			} catch (ParseException e) {
				logger.error("Couldn't parse the store parent app session key " + parentAppSessionKey, e);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("reading sip session from the cache. sip app session key = " + sipAppSessionParentKey);
			}
			routingRegion = (SipApplicationRoutingRegion) in.readObject();
			stateInfo = (Serializable) in.readObject();
			handlerServlet = in.readUTF();
			String subscriberURIStringified = in.readUTF();
			if("".equals(subscriberURIStringified)) {
				subscriberURIStringified = null;
			} else {
				try {
					URI jsipSubscriberUri = SipFactories.addressFactory.createURI(subscriberURIStringified);				
					if(jsipSubscriberUri instanceof javax.sip.address.SipURI) {
						subscriberURI = new SipURIImpl((javax.sip.address.SipURI)jsipSubscriberUri);
					} else if (jsipSubscriberUri instanceof javax.sip.address.TelURL) {
						subscriberURI = new TelURLImpl((javax.sip.address.TelURL)jsipSubscriberUri);
					}
				} catch (ParseException pe) {
					logger.error("Impossible to parse the subscriber URI " 
							+ subscriberURIStringified, pe);
				}
			}
			sessionCreatingDialog = (SIPDialog) in.readObject();
			if(logger.isDebugEnabled()) {
				logger.debug("deserialized dialog for the sip session "+ sessionCreatingDialog);
			}
			invalidateWhenReady = in.readBoolean();
			readyToInvalidate = in.readBoolean();
			
			creationTime = in.readLong();
			lastAccessedTime = in.readLong();					
			
//			maxInactiveInterval = in.readInt();
//			isNew = in.readBoolean();
			isValid = in.readBoolean();
			lastAccessedTime = in.readLong();

			boolean proxySerialized = in.readBoolean();
			if(proxySerialized) {
				proxy = (ProxyImpl) in.readObject();
			} 
			boolean b2buaSerialized = in.readBoolean();
			if(b2buaSerialized) {
				b2buaHelper = (B2buaHelperImpl) in.readObject();
			}
			// From ClusteredSession
			invalidationPolicy = in.readInt();
			version = in.readInt();

			// Get our id without any jvmRoute appended
//			parseRealId(id);

			// We no longer know if we have an activationListener
			hasActivationListener = null;

			// If the session has been replicated, any subsequent
			// access cannot be the first.
			this.firstAccess = false;

			if(logger.isDebugEnabled()) {
				logger.debug("sip session has just been read from the cache ");
				logger.debug("sip session executor service " + executorService);
			}
			// TODO uncomment when work on JBAS-1900 is completed
			// // Session notes -- for FORM auth apps, allow replicated session
			// // to be used without requiring a new login
			// // We use the superclass set/removeNote calls here to bypass
			// // the custom logic we've added
			// String username = (String) in.readObject();
			// if (username != null)
			// {
			// super.setNote(Constants.SESS_USERNAME_NOTE, username);
			// }
			// else
			// {
			// super.removeNote(Constants.SESS_USERNAME_NOTE);
			// }
			// String password = (String) in.readObject();
			// if (password != null)
			// {
			// super.setNote(Constants.SESS_PASSWORD_NOTE, password);
			// }
			// else
			// {
			// super.removeNote(Constants.SESS_PASSWORD_NOTE);
			// }
		}
	}

	/**
	 * Writes all non-transient state to the ObjectOutput <i>except the
	 * attribute map</i>. Subclasses that wish the attribute map to be written
	 * should override this method and append it.
	 * 
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		synchronized (this) {
			// From SipSessionimpl			
			out.writeUTF(key.getFromAddress());
			out.writeUTF(key.getFromTag());
			out.writeUTF(key.getToAddress());
			if(key.getToTag() != null) {
				out.writeUTF(key.getToTag());
			} else {
				out.writeUTF("");
			}
			out.writeUTF(key.getCallId());
			out.writeUTF(key.getApplicationName());
			
			out.writeUTF(sipApplicationSession.getKey().toString());
			out.writeObject(routingRegion);
			out.writeObject(stateInfo);
			out.writeUTF(handlerServlet);
			if(subscriberURI != null) {
				out.writeUTF(subscriberURI.toString());
			} else {
				out.writeUTF("");
			}
			if(logger.isDebugEnabled()) {
				logger.debug("serializing dialog for the sip session "+ sessionCreatingDialog);
			}
			out.writeObject((SIPDialog)sessionCreatingDialog);
			out.writeBoolean(invalidateWhenReady);
			out.writeBoolean(readyToInvalidate);
			
			out.writeLong(creationTime);
			out.writeLong(lastAccessedTime);
//			out.writeInt(maxInactiveInterval);
//			out.writeBoolean(isNew);
			out.writeBoolean(isValid);
			out.writeLong(lastAccessedTime);
			
			if(proxy == null) {
				out.writeBoolean(false);
			} else {
				out.writeBoolean(true);
				out.writeObject(proxy);
			}
			
			if(b2buaHelper == null) {
				out.writeBoolean(false);
			} else {
				out.writeBoolean(true);
				out.writeObject(b2buaHelper);
			}
			
			// From ClusteredSession
			out.writeInt(invalidationPolicy);
			out.writeInt(version);

			// TODO uncomment when work on JBAS-1900 is completed
			// // Session notes -- for FORM auth apps, allow replicated session
			// // to be used without requiring a new login
			// String username = (String) getNote(Constants.
			// SESS_USERNAME_NOTE);
			// logger.debug(Constants.SESS_USERNAME_NOTE + " = " + username);
			// out.writeObject(username);
			// String password = (String) getNote(Constants.SESS_PASSWORD_NOTE);
			// logger.debug(Constants.SESS_PASSWORD_NOTE + " = " + password);
			// out.writeObject(password);
		}
	}

	// ----------------------------------------------------- Protected Methods

	/**
	 * Removes any attribute whose name is found in {@link #excludedAttributes}
	 * from <code>attributes</code> and returns a Map of all such attributes.
	 * 
	 * @param attributes
	 *            source map from which excluded attributes are to be removed.
	 * 
	 * @return Map that contains any attributes removed from
	 *         <code>attributes</code>, or <code>null</code> if no attributes
	 *         were removed.
	 */
	protected static Map removeExcludedAttributes(Map attributes) {
		Map excluded = null;
		for (int i = 0; i < excludedAttributes.length; i++) {
			Object attr = attributes.remove(excludedAttributes[i]);
			if (attr != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Excluding attribute " + excludedAttributes[i]
							+ " from replication");
				}
				if (excluded == null) {
					excluded = new HashMap();
				}
				excluded.put(excludedAttributes[i], attr);
			}
		}

		return excluded;
	}

	// -------------------------------------- Internal protected method override

	/**
	 * Method inherited from Tomcat. Return zero-length based string if not
	 * found.
	 */
	protected String[] keys() {
		return ((String[]) getAttributesInternal().keySet()
				.toArray(new String[0]));
	}

	/**
	 * Called by super.removeAttribute().
	 * 
	 * @param name
	 *            the attribute name
	 * @param notify
	 *            <code>true</code> if listeners should be notified
	 */
	protected void removeAttributeInternal(String name, boolean notify) {
		boolean localCall = true;
		boolean localOnly = false;
		removeAttributeInternal(name, localCall, localOnly, notify);
	}

	/**
	 * Remove the attribute from the local cache and possibly the distributed
	 * cache, plus notify any listeners
	 * 
	 * @param name
	 *            the attribute name
	 * @param localCall
	 *            <code>true</code> if this call originated from local activity
	 *            (e.g. a removeAttribute() in the webapp or a local session
	 *            invalidation/expiration), <code>false</code> if it originated
	 *            due to an remote event in the distributed cache.
	 * @param localOnly
	 *            <code>true</code> if the removal should not be replicated
	 *            around the cluster
	 * @param notify
	 *            <code>true</code> if listeners should be notified
	 */
	protected void removeAttributeInternal(String name, boolean localCall,
			boolean localOnly, boolean notify) {

		// Remove this attribute from our collection
		Object value = removeAttributeInternal(name, localCall, localOnly);

		// Do we need to do valueUnbound() and attributeRemoved() notification?
		if (!notify || (value == null)) {
			return;
		}

		// Call the valueUnbound() method if necessary
		SipSessionBindingEvent event = null;
		if (value instanceof SipSessionBindingListener) {
			event = new SipSessionBindingEvent(this, name);
			((SipSessionBindingListener) value).valueUnbound(event);
		}

		// Notify interested application event listeners
		List<SipSessionAttributeListener> listeners = getSipApplicationSession().getSipContext().getListeners().getSipSessionAttributeListeners();
		if (listeners == null)
			return;
		for (SipSessionAttributeListener listener : listeners) {
			try {
//				fireContainerEvent(context, "beforeSessionAttributeRemoved",
//						listener);
				if (event == null) {
					event = new SipSessionBindingEvent(this, name);
				}
				listener.attributeRemoved(event);
//				fireContainerEvent(context, "afterSessionAttributeRemoved",
//						listener);
			} catch (Throwable t) {
//				try {
//					fireContainerEvent(context, "afterSessionAttributeRemoved",
//							listener);
//				} catch (Exception e) {
//					;
//				}
				sipApplicationSession.getSipContext().getLogger().error(
						sm.getString("standardSession.attributeEvent"), t);
			}
		}

	}

	/**
	 * Exists in this class solely to act as an API-compatible bridge to the
	 * deprecated {@link #removeJBossInternalAttribute(String)}.
	 * JBossCacheClusteredSession subclasses will override this to call their
	 * own methods that make use of localCall and localOnly
	 * 
	 * @param name
	 * @param localCall
	 * @param localOnly
	 * @return
	 * 
	 * @deprecated will be replaced by removeJBossInternalAttribute(String,
	 *             boolean, boolean)
	 */
	protected Object removeAttributeInternal(String name, boolean localCall,
			boolean localOnly) {
		return removeJBossInternalAttribute(name);
	}

	protected Object getAttributeInternal(String name) {
		return getJBossInternalAttribute(name);
	}

	protected Map getAttributesInternal() {
		return getJBossInternalAttributes();
	}

	protected Object setInternalAttribute(String name, Object value) {
		if (value instanceof SipSessionActivationListener)
			hasActivationListener = Boolean.TRUE;

		return setJBossInternalAttribute(name, value);
	}

	// ------------------------------------------ JBoss internal abstract method

	protected abstract Object getJBossInternalAttribute(String name);

	/**
	 * @deprecated will be replaced by removeJBossInternalAttribute(String,
	 *             boolean, boolean)
	 */
	protected abstract Object removeJBossInternalAttribute(String name);

	protected abstract Map getJBossInternalAttributes();

	protected abstract Object setJBossInternalAttribute(String name,
			Object value);

	// ------------------------------------------------ Session Package Methods

	protected void sessionAttributesDirty() {
		sessionAttributesDirty = true;
	}

	protected boolean getSessionAttributesDirty() {
		return sessionAttributesDirty;
	}

	protected void sessionMetadataDirty() {
		sessionMetadataDirty = true;
	}

	protected boolean getSessionMetadataDirty() {
		return sessionMetadataDirty;
	}

	/**
	 * Calls {@link #sessionAttributesDirty()} and
	 * {@link #sessionMetadataDirty()}.
	 * 
	 * @deprecated use one of the more fine-grained methods.
	 */
	protected void sessionDirty() {
		sessionAttributesDirty();
		sessionMetadataDirty();
	}

	public boolean isSessionDirty() {
		return sessionAttributesDirty || sessionMetadataDirty;
	}

	public boolean getReplicateSessionBody() {
		return sessionMetadataDirty || getExceedsMaxUnreplicatedInterval();
	}

	protected boolean isGetDirty(Object attribute) {
		boolean result = false;
		switch (invalidationPolicy) {
		case (WebMetaData.SESSION_INVALIDATE_SET_AND_GET):
			result = true;
			break;
		case (WebMetaData.SESSION_INVALIDATE_SET_AND_NON_PRIMITIVE_GET):
			result = isMutable(attribute);
			break;
		default:
			// result is false
		}
		return result;
	}

	protected boolean isMutable(Object attribute) {
		return attribute != null
				&& !(attribute instanceof String || attribute instanceof Number
						|| attribute instanceof Character || attribute instanceof Boolean);
	}	
}
