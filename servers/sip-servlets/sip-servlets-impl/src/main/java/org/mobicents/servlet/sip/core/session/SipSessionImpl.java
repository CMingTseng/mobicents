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
package org.mobicents.servlet.sip.core.session;

import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionUnavailableException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

import org.apache.catalina.Container;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.servlet.sip.JainSipUtils;
import org.mobicents.servlet.sip.SipFactories;
import org.mobicents.servlet.sip.address.AddressImpl;
import org.mobicents.servlet.sip.address.SipURIImpl;
import org.mobicents.servlet.sip.core.dispatchers.MessageDispatcher;
import org.mobicents.servlet.sip.message.SipFactoryImpl;
import org.mobicents.servlet.sip.message.SipServletRequestImpl;
import org.mobicents.servlet.sip.message.SipServletResponseImpl;
import org.mobicents.servlet.sip.message.TransactionApplicationData;
import org.mobicents.servlet.sip.proxy.ProxyBranchImpl;
import org.mobicents.servlet.sip.proxy.ProxyImpl;
import org.mobicents.servlet.sip.startup.SipContext;

/**
 * 
 * <p>Implementation of the SipSession interface.
 * An instance of this sip session can only be retrieved through the Session Manager
 * (extended class from Tomcat's manager classes implementing the <code>Manager</code> interface)
 * to constrain the creation of sip session and to make sure that all sessions created
 * can be retrieved only through the session manager</p> 
 *
 * <p>
 * As a SipApplicationSession represents a dialog, 
 * the call id and from header URI, from tag, to Header (and to Tag to identify forked requests) 
 * are used as a unique key for a given SipSession instance. 
 * </p>
 *
 * @author vralev
 * @author mranga
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A>
 */
public class SipSessionImpl implements MobicentsSipSession {
	
	protected static enum SipSessionEventType {
		CREATION, DELETION, READYTOINVALIDATE;
	}
	
	private transient static final Log logger = LogFactory.getLog(SipSessionImpl.class);
	
	protected transient MobicentsSipApplicationSession sipApplicationSession;			
	
	protected transient ProxyBranchImpl proxyBranch;

	protected Map<String, Object> sipSessionAttributeMap;
	
	protected SipSessionKey key;
	
	protected transient Principal userPrincipal;
	
	/**
	 * Creation time.
	 */
	protected long creationTime;
	
	/**
	 * Last access time.
	 */
	protected long lastAccessedTime;
	
	/**
	 * Routing region per session/dialog.
	 */
	protected SipApplicationRoutingRegion routingRegion;
	
	/**
	 * AR state info
	 */
	protected Serializable stateInfo;
	
	/**
	 * Current state of the session, one of INTITIAL, EARLY, ESTABLISHED and TERMINATED.
	 */
	protected State state;
	
	/**
	 * Is the session valid.
	 */
	protected boolean isValid;
	
	/**
	 * The name of the servlet withing this same app to handle all subsequent requests.
	 */
	protected String handlerServlet;
		
	/**
	 * Subscriber URI should be set for outbound sessions, from requests created in the container.
	 */
	protected URI subscriberURI;
	
	/**
	 * Outbound interface is one of the allowed values in the Servlet Context attribute
	 * "javax.servlet.ip.outboundinterfaces"
	 * This one is not serialized, it has to be reset by the app on sessionActivated listener method
	 */
	protected transient SipURI outboundInterface;
	
	
	// === THESE ARE THE OBJECTS A SIP SESSION CAN BE ASSIGNED TO ===
	// TODO: Refactor this into two Session classes to avoid nulls
	// and branching on nulls
	/**
	 * We use this for dialog-related requests. In this case the dialog
	 * directly corresponds to the session.
	 */
	protected Dialog sessionCreatingDialog;
	
	/**
	 * We use this for REGISTER, where a dialog doesn't exist to carry the session info.
	 * In this case the session only spans a single transaction.
	 */
	protected transient Transaction sessionCreatingTransaction;
	// =============================================================
		
	protected transient Set<Transaction> ongoingTransactions;
	
	protected boolean supervisedMode;

	protected transient ConcurrentHashMap<String, MobicentsSipSession> derivedSipSessions;

	/*
	 * The almighty provider
	 */
	protected transient SipFactoryImpl sipFactory;
	
	protected boolean invalidateWhenReady = true;
	
	protected boolean readyToInvalidate = false;

	/*
	 * If this is a derived session, have a pointer to the parent session.
	 */
	protected transient MobicentsSipSession parentSession = null;
	
	protected SipSessionImpl (SipSessionKey key, SipFactoryImpl sipFactoryImpl, MobicentsSipApplicationSession mobicentsSipApplicationSession) {
		this.key = key;
		setSipApplicationSession(mobicentsSipApplicationSession);
		this.sipFactory = sipFactoryImpl;
		this.creationTime = this.lastAccessedTime = System.currentTimeMillis();		
		this.state = State.INITIAL;
		this.isValid = true;
		this.supervisedMode = true;
		this.sipSessionAttributeMap = new ConcurrentHashMap<String, Object>();
		this.derivedSipSessions = new ConcurrentHashMap<String, MobicentsSipSession>();
		this.ongoingTransactions = new CopyOnWriteArraySet<Transaction>();
		// the sip context can be null if the AR returned an application that was not deployed
		if(mobicentsSipApplicationSession.getSipContext() != null) {
			notifySipSessionListeners(SipSessionEventType.CREATION);
		}
	}
	/**
	 * Notifies the listeners that a lifecycle event occured on that sip session 
	 * @param sipSessionEventType the type of event that happened
	 */
	private void notifySipSessionListeners(SipSessionEventType sipSessionEventType) {
		SipContext sipContext = 
			getSipApplicationSession().getSipContext();		
		if(logger.isDebugEnabled()) {
			logger.debug("notifying sip session listeners of context " + sipContext.getApplicationName() + " of following event " +
					sipSessionEventType);
		}
		List<SipSessionListener> sipSessionListeners = 
			sipContext.getListeners().getSipSessionListeners();
		SipSessionEvent sipSessionEvent = new SipSessionEvent(this);
		for (SipSessionListener sipSessionListener : sipSessionListeners) {
			try {
				if(SipSessionEventType.CREATION.equals(sipSessionEventType)) {
					sipSessionListener.sessionCreated(sipSessionEvent);
				} else if (SipSessionEventType.DELETION.equals(sipSessionEventType)) {
					sipSessionListener.sessionDestroyed(sipSessionEvent);
				} else if (SipSessionEventType.READYTOINVALIDATE.equals(sipSessionEventType)) {
					sipSessionListener.sessionReadyToInvalidate(sipSessionEvent);
				}
			} catch (Throwable t) {
				logger.error("SipSessionListener threw exception", t);
			}
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#createRequest(java.lang.String)
	 */
	public SipServletRequest createRequest(final String method) {
		if(method.equalsIgnoreCase(Request.ACK)
				|| method.equalsIgnoreCase(Request.CANCEL)) {
			throw new IllegalArgumentException(
					"Can not create ACK or CANCEL requests with this method");
		}
		if(!isValid()) {
			throw new IllegalStateException("cannot create a request because the session is invalid");
		}
		if(State.TERMINATED.equals(state)) {
			throw new IllegalStateException("cannot create a request because the session is in TERMINATED state");
		}
		if((State.INITIAL.equals(state) && hasOngoingTransaction())) {
			throw new IllegalStateException("cannot create a request because the session is in INITIAL state with ongoing transactions");
		}
		if(logger.isDebugEnabled()) {
			logger.debug("dialog associated with this session to create the new request within that dialog "+
					sessionCreatingDialog);
		}
		SipServletRequestImpl sipServletRequest = null;
		if(this.sessionCreatingDialog != null && !DialogState.TERMINATED.equals(sessionCreatingDialog.getState())) {
			try {
				final Request methodRequest = this.sessionCreatingDialog.createRequest(method);
				
				//Issue 112 fix by folsson
				methodRequest.removeHeader(ViaHeader.NAME);
				
				//if a SUBSCRIBE or BYE is sent for exemple, it will reuse the prexisiting dialog
				sipServletRequest = new SipServletRequestImpl(
						methodRequest, this.sipFactory, this, null, sessionCreatingDialog,
						false);
			} catch (SipException e) {
				logger.error("Cannot create the " + method + " request from the dialog",e);
				throw new IllegalArgumentException("Cannot create the " + method + " request",e);
			} 	
		} else {
			if(logger.isDebugEnabled()) {
				logger.debug("The new request for the session is dialog creating (" + method + ")");
			}
			//case where other requests are sent with the same session like REGISTER or for challenge requests
			if(sessionCreatingTransaction != null && sessionCreatingTransaction.getRequest().getMethod().equalsIgnoreCase(method)) {
				Request request = (Request) sessionCreatingTransaction.getRequest().clone();
				sipServletRequest = new SipServletRequestImpl(
						request, this.sipFactory, this, null, null,
						false);
				
				CSeqHeader cSeq = (CSeqHeader) request.getHeader((CSeqHeader.NAME));
				try {
					cSeq.setSeqNumber(cSeq.getSeqNumber() + 1l);
				} catch (InvalidArgumentException e) {
					logger.error("Cannot increment the Cseq header to the new " + method + " request",e);
					throw new IllegalArgumentException("Cannot create the " + method + " request",e);				
				}
				request.setHeader(cSeq);
				request.removeHeader(ViaHeader.NAME);
				SipProvider sipProvider = sipFactory.getSipNetworkInterfaceManager().findMatchingListeningPoint(
						JainSipUtils.findTransport(request), false).getSipProvider();
				try {
					ClientTransaction retryTran = sipProvider
						.getNewClientTransaction(request);
					sessionCreatingTransaction = retryTran;				
					// SIP Request is ALWAYS pointed to by the client tx.
					// Notice that the tx appplication data is cached in the request
					// copied over to the tx so it can be quickly accessed when response
					// arrives.
					retryTran.setApplicationData(sipServletRequest.getTransactionApplicationData());
					
					Dialog dialog = retryTran.getDialog();
					if (dialog == null && JainSipUtils.dialogCreatingMethods.contains(sipServletRequest.getMethod())) {					
						dialog = sipProvider.getNewDialog(retryTran);
						this.setSessionCreatingDialog(dialog);
						dialog.setApplicationData(sipServletRequest.getTransactionApplicationData());
						if(logger.isDebugEnabled()) {
							logger.debug("new Dialog for request " + sipServletRequest + ", ref = " + dialog);
						}
					}													
															
					sipServletRequest.setTransaction(retryTran);					
				} catch (TransactionUnavailableException e) {
					logger.error("Cannot get a new transaction for the request " + sipServletRequest,e);
					throw new IllegalArgumentException("Cannot get a new transaction for the request " + sipServletRequest,e);
				} catch (SipException e) {
					logger.error("Cannot get a new dialog for the new request " + sipServletRequest,e);
					throw new IllegalArgumentException("Cannot get a new dialog for the request " + sipServletRequest,e);
				}
				
				return sipServletRequest;
			} else {
				sipServletRequest =(SipServletRequestImpl) sipFactory.createRequest(
					this.sipApplicationSession,
					method,
					this.getLocalParty(),
					this.getRemoteParty(),
					handlerServlet);			
			}
		}
		//Application Routing :
		//removing the route headers and adding them back again except the one
		//corresponding to the app that is creating the subsequent request
		//avoid going through the same app that created the subsequent request
		Request request = (Request) sipServletRequest.getMessage();
		final ListIterator<RouteHeader> routeHeaders = request.getHeaders(RouteHeader.NAME);
		request.removeHeader(RouteHeader.NAME);
		while (routeHeaders.hasNext()) {
			RouteHeader routeHeader = routeHeaders.next();
			String routeAppName = ((javax.sip.address.SipURI)routeHeader .getAddress().getURI()).
				getParameter(MessageDispatcher.RR_PARAM_APPLICATION_NAME);
			if(routeAppName == null || !routeAppName.equals(getKey().getApplicationName())) {
				request.addHeader(routeHeader);
			}
		}
		return sipServletRequest;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getApplicationSession()
	 */
	public SipApplicationSession getApplicationSession() {
		return this.sipApplicationSession;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		return sipSessionAttributeMap.get(name);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getAttributeNames()
	 */
	public Enumeration<String> getAttributeNames() {
		Vector<String> names = new Vector<String>(sipSessionAttributeMap.keySet());
		return names.elements();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getCallId()
	 */
	public String getCallId() {
		if(this.sessionCreatingDialog != null)
			return this.sessionCreatingDialog.getCallId().getCallId();
		else
			return ((CallIdHeader)this.sessionCreatingTransaction.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getCreationTime()
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getId()
	 */
	public String getId() {
		return key.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	private void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime= lastAccessedTime;
	}
	
	/**
     * Update the accessed time information for this session.  This method
     * should be called by the context when a request comes in for a particular
     * session, even if the application does not reference it.
     */
	public void access() {
		setLastAccessedTime(System.currentTimeMillis());
	}
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getLocalParty()
	 */
	public Address getLocalParty() {
		if(sessionCreatingDialog != null) {
			return new AddressImpl(sessionCreatingDialog.getLocalParty(), false);
		} else if (sessionCreatingTransaction != null){
			try {
				FromHeader fromHeader = (FromHeader)sessionCreatingTransaction.getRequest().getHeader(FromHeader.NAME);
				return new AddressImpl(fromHeader.getAddress(), false);
			} catch(Exception e) {
				throw new RuntimeException("Error creating Address", e);
			}
		} else {
			throw new RuntimeException("Error creating Address, no transaction or dialog have been found");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public SipApplicationRoutingRegion getRegion() {
		if(!isValid()) {
			throw new IllegalStateException("the session has been invalidated");
		}
		return routingRegion;
	}

	/**
	 * This method allows the application to set the region that the application 
	 * is in with respect to this SipSession
	 * @param routingRegion the region that the application is in  
	 */
	public void setRoutingRegion(SipApplicationRoutingRegion routingRegion) {
		this.routingRegion = routingRegion;
	}
	
	/**
	 * @return the stateInfo
	 */
	public Serializable getStateInfo() {
		return stateInfo;
	}

	/**
	 * @param stateInfo the stateInfo to set
	 */
	public void setStateInfo(Serializable stateInfo) {
		this.stateInfo = stateInfo;
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getRemoteParty()
	 */
	public Address getRemoteParty() {
		if(sessionCreatingDialog != null) {
			return new AddressImpl(sessionCreatingDialog.getRemoteParty(), false);
		} else if (sessionCreatingTransaction != null){
			try {
				ToHeader toHeader = (ToHeader)sessionCreatingTransaction.getRequest().getHeader(ToHeader.NAME);
				return new AddressImpl(toHeader.getAddress(), false);
			} catch(Exception e) {
				throw new RuntimeException("Error creating Address", e);
			}
		} else {
			throw new RuntimeException("Error creating Address, no transaction or dialog have been found");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#getState()
	 */
	public State getState() {
		return this.state;
	}

	/**
	 * {@inheritDoc}
	 */
	public URI getSubscriberURI() {
		if(!isValid()) {
			throw new IllegalStateException("the session has been invalidated");
		}
		if (this.subscriberURI == null)
			throw new IllegalStateException("Subscriber URI is only available for outbound sessions.");
		else 
			return this.subscriberURI;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#invalidate()
	 */
	public void invalidate() {		
		if(!isValid) {
			throw new IllegalStateException("SipSession already invalidated !");
		}		
		if(logger.isInfoEnabled()) {
			logger.info("Invalidating the sip session " + key);
		}
				
		// No need for checks after JSR 289 PFD spec
		//checkInvalidation();
		notifySipSessionListeners(SipSessionEventType.DELETION);
		for (String key : sipSessionAttributeMap.keySet()) {
			removeAttribute(key);
		}
		isValid = false;	
		
		for (MobicentsSipSession derivedMobicentsSipSession : derivedSipSessions.values()) {
			derivedMobicentsSipSession.invalidate();
		}
		derivedSipSessions.clear();
		derivedSipSessions = null;
		getSipApplicationSession().getSipContext().getSipManager().removeSipSession(key);		
		sipApplicationSession.onSipSessionReadyToInvalidate(this);
		sipSessionAttributeMap = null;
		proxyBranch = null;
		//key = null;
		sessionCreatingDialog = null;
		sessionCreatingTransaction = null;
		ongoingTransactions.clear();
		parentSession = null;
		userPrincipal = null;
	}
	
	/**
	 * Not needed anymore after PFD JSR 289 spec
	 */
	protected void checkInvalidation() {
		if(state.equals(State.CONFIRMED)
				|| state.equals(State.EARLY))
			throw new IllegalStateException("Can not invalidate sip session in " + 
					state.toString() + " state.");
		if(isSupervisedMode() && hasOngoingTransaction()) {
			dumpOngoingTransactions();
			throw new IllegalStateException("Can not invalidate sip session with " +
					ongoingTransactions.size() + " ongoing transactions in supervised mode.");
		}
	}

	private void dumpOngoingTransactions() {
		if(logger.isDebugEnabled()) {
			logger.debug("ongoing transactions in sip the session " + key);
		
			for (Transaction transaction : ongoingTransactions) {
				logger.debug("Transaction " + transaction + " : state = " + transaction.getState());
			}
		}
	}

	/**
	 * Removed from the interface in PFD stage
	 * so making it protected
	 */
	protected boolean hasOngoingTransaction() {
		if(!isSupervisedMode()) {
			return false;
		} else {
			for (Transaction transaction : ongoingTransactions) {
				if(TransactionState.CALLING.equals(transaction.getState()) ||
					TransactionState.TRYING.equals(transaction.getState()) ||
					TransactionState.PROCEEDING.equals(transaction.getState()) ||
					TransactionState.COMPLETED.equals(transaction.getState()) ||
					TransactionState.CONFIRMED.equals(transaction.getState())) {
						return true;
				}
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#isValid()
	 */
	public boolean isValid() {
		return this.isValid;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {

		if(!isValid())
			throw new IllegalStateException("Can not bind object to session that has been invalidated!!");
		
		if(name==null)
		//	throw new NullPointerException("Name of attribute to bind cant be null!!!");
			return;
		
		// Notifying Listeners of attribute removal
		SipSessionBindingEvent event = new SipSessionBindingEvent(this, name);
		SipListenersHolder sipListenersHolder = this.getSipApplicationSession().getSipContext().getListeners();		
		for (SipSessionBindingListener listener : sipListenersHolder.getSipSessionBindingListeners()) {
			try{
				listener.valueUnbound(event);
			} catch (Throwable t) {
				logger.error("SipSessionBindingListener threw exception", t);
			}
			
		}
		for (SipSessionAttributeListener listener : sipListenersHolder.getSipSessionAttributeListeners()) {
			try{
				listener.attributeRemoved(event);
			} catch (Throwable t) {
				logger.error("SipSessionAttributeListener threw exception", t);
			}
		}

		this.sipSessionAttributeMap.remove(name);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String key, Object attribute) {
		if(!isValid()) {
			throw new IllegalStateException("Can not bind object to session that has been invalidated!!");
		}
		if(key == null) {
			throw new NullPointerException("Name of attribute to bind cant be null!!!");
		}
		if(attribute == null) {
			throw new NullPointerException("Attribute that is to be bound cant be null!!!");
		}
		
		// Notifying Listeners of attribute addition or modification
		SipSessionBindingEvent event = new SipSessionBindingEvent(this, key);
		SipListenersHolder sipListenersHolder = this.getSipApplicationSession().getSipContext().getListeners();
		if (!this.sipSessionAttributeMap.containsKey(key)) {
			// This is initial, we need to send value bound event
			for (SipSessionBindingListener listener : sipListenersHolder
					.getSipSessionBindingListeners()) {
				try {
					listener.valueBound(event);
				} catch (Throwable t) {
					logger.error("SipSessionBindingListener threw exception", t);
				}
			}
			for (SipSessionAttributeListener listener : sipListenersHolder
					.getSipSessionAttributeListeners()) {
				try {
					listener.attributeAdded(event);
				} catch (Throwable t) {
					logger.error("SipSessionAttributeListener threw exception", t);
				}
			}
		} else {
			for (SipSessionAttributeListener listener : sipListenersHolder
					.getSipSessionAttributeListeners()) {
				try {
					listener.attributeReplaced(event);
				} catch (Throwable t) {
					logger.error("SipSessionAttributeListener threw exception", t);
				}
			}
		}

		this.sipSessionAttributeMap.put(key, attribute);

	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSession#setHandler(java.lang.String)
	 */
	public void setHandler(String name) throws ServletException {
		if(!isValid) {
			throw new IllegalStateException("the session has already been invalidated, no handler can be set on it anymore !");
		}
		if(name.equals(handlerServlet)) {
			return ;
		}
		SipContext sipContext = getSipApplicationSession().getSipContext();
		Map<String, Container> childrenMap = sipContext.getChildrenMap();
		Container container = childrenMap.get(name);
		
		if(container == null) {
			throw new ServletException("the sip servlet with the name "+ name + 
					" doesn't exist in the sip application " + sipContext.getApplicationName());
		}		
		this.handlerServlet = name;
		if(logger.isDebugEnabled()) {
			logger.debug("Session Handler for application " + getKey().getApplicationName() + " set to " + handlerServlet);
		}
	}
	
	/**
	 * Retrieve the handler associated with this sip session
	 * @return the handler associated with this sip session
	 */
	public String getHandler() {
		return handlerServlet;
	}

	/**
	 * @param dialog the dialog to set
	 */
	public void setSessionCreatingDialog(Dialog dialog) {
		this.sessionCreatingDialog = dialog;
	}

	/**
	 * @return the dialog
	 */
	public Dialog getSessionCreatingDialog() {
		return sessionCreatingDialog;
	}

	public MobicentsSipApplicationSession getSipApplicationSession() {
		return sipApplicationSession;
	}

	protected void setSipApplicationSession(
			MobicentsSipApplicationSession sipApplicationSession) {
		this.sipApplicationSession = sipApplicationSession;
		if ( sipApplicationSession != null) {
			if(sipApplicationSession.getSipSession(key.toString()) == null) {
				sipApplicationSession.addSipSession(this);
			}
		}
	}

	public Transaction getSessionCreatingTransaction() {
		return sessionCreatingTransaction;
	}	

	/**
	 * @param sessionCreatingTransaction the sessionCreatingTransaction to set
	 */
	public void setSessionCreatingTransaction(Transaction sessionCreatingTransaction) {
		this.sessionCreatingTransaction = sessionCreatingTransaction;
	}
	
	public boolean isSupervisedMode() {
		return this.supervisedMode;
	}

	public void setSupervisedMode(boolean supervisedMode) {
		this.supervisedMode = supervisedMode;
	}

	public void setSipSubscriberURI(URI subscriberURI) {
		this.subscriberURI = subscriberURI;
	}
	
	public URI getSipSubscriberURI() {
		return subscriberURI;
	}

	public SipURI getOutboundInterface() {
		return outboundInterface;
	}	
	
	public void onDialogTimeout(Dialog dialog) {
		if(hasOngoingTransaction()) {
			throw new IllegalStateException("Dialog timed out, but there are active transactions.");
		}
		this.state = State.TERMINATED;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public void onTerminatedState() {
		if(isValid) {
			onReadyToInvalidate();
			if(this.parentSession != null) {
				boolean allDerivedSessionsTerminated = true;
				Iterator<MobicentsSipSession> derivedSessionsIterator = parentSession.getDerivedSipSessions();
				while (derivedSessionsIterator.hasNext()) {
					MobicentsSipSession mobicentsSipSession = (MobicentsSipSession) derivedSessionsIterator
							.next();
					if(mobicentsSipSession.isReadyToInvalidate()) {
						allDerivedSessionsTerminated = false;
						break;
					}
				}					
				if(allDerivedSessionsTerminated) this.parentSession.onReadyToInvalidate();
			}
		}
	}

	/**
	 * Add an ongoing tx to the session.
	 */
	public void addOngoingTransaction(Transaction transaction) {
		readyToInvalidate = false;
		this.ongoingTransactions.add(transaction);
		if(logger.isDebugEnabled()) {
			logger.debug("transaction "+ transaction +" has been added to sip session's ongoingTransactions" );
		}
	}
	
	/**
	 * Remove an ongoing tx to the session.
	 */
	public void removeOngoingTransaction(Transaction transaction) {
		
		this.ongoingTransactions.remove(transaction);
		
		if(logger.isDebugEnabled()) {
			logger.debug("transaction "+ transaction +" has been removed from sip session's ongoingTransactions" );
		}		
	}
	
	public Set<Transaction> getOngoingTransactions() {
		return this.ongoingTransactions;
	}	
	
	/**
	 * Update the sip session state upon sending/receiving a response
	 * Covers JSR 289 Section 6.2.1 along with updateStateOnRequest method
	 * @param response the response received/to send
	 * @param receive true if the response has been received, false if it is to be sent.
	 */
	public void updateStateOnResponse(SipServletResponseImpl response, boolean receive) {
		CSeqHeader cSeqHeader = (CSeqHeader)response.getMessage().getHeader(CSeqHeader.NAME);
		// JSR 289 Section 6.2.1 Point 2 of rules governing the state of SipSession
		// In general, whenever a non-dialog creating request is sent or received, 
		// the SipSession state remains unchanged. Similarly, a response received 
		// for a non-dialog creating request also leaves the SipSession state unchanged. 
		// The exception to the general rule is that it does not apply to requests (e.g. BYE, CANCEL) 
		// that are dialog terminating according to the appropriate RFC rules relating to the kind of dialog.		
		if(!JainSipUtils.dialogCreatingMethods.contains(cSeqHeader.getMethod()) &&
				!JainSipUtils.dialogTerminatingMethods.contains(cSeqHeader.getMethod())) {
			return;
		}
		// Mapping to the sip session state machine (proxy is covered here too)
		if( (State.INITIAL.equals(state) || State.EARLY.equals(state)) && 
				response.getStatus() >= 200 && response.getStatus() < 300 && 
				!JainSipUtils.dialogTerminatingMethods.contains(cSeqHeader.getMethod())) {
			this.setState(State.CONFIRMED);
			if(this.proxyBranch != null && !this.proxyBranch.getRecordRoute()) {
				readyToInvalidate = true;
			}
			if(logger.isDebugEnabled()) {
				logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
			}
		}
		// Mapping to the sip session state machine
		if( State.INITIAL.equals(state) && response.getStatus() >= 100 && response.getStatus() < 200 ) {
			this.setState(State.EARLY);
			if(logger.isDebugEnabled()) {
				logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
			}
		}		
		if( (State.INITIAL.equals(state) || State.EARLY.equals(state)) && 
				response.getStatus() >= 300 && response.getStatus() < 700 &&
				JainSipUtils.dialogCreatingMethods.contains(cSeqHeader.getMethod()) && 
				!JainSipUtils.dialogTerminatingMethods.contains(cSeqHeader.getMethod())) {
			// If the servlet acts as a UAC and sends a dialog creating request, 
			// then the SipSession state tracks directly the SIP dialog state except 
			// that non-2XX final responses received in the EARLY or INITIAL states 
			// cause the SipSession state to return to the INITIAL state rather than going to TERMINATED.
			// +
			// If the servlet acts as a proxy for a dialog creating request then 
			// the SipSession state tracks the SIP dialog state except that non-2XX 
			// final responses received from downstream in the EARLY or INITIAL states 
			// cause the SipSession state to return to INITIAL rather than going to TERMINATED.
			if(receive) {
				setState(State.INITIAL);
//				readyToInvalidate = true; 
				if(logger.isDebugEnabled()) {
					logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
				}
			} 
			// If the servlet acts as a UAS and receives a dialog creating request, 
			// then the SipSession state directly tracks the SIP dialog state. 
			// Unlike a UAC, a non-2XX final response sent by the UAS in the EARLY or INITIAL 
			// states causes the SipSession state to go directly to the TERMINATED state.
			// +
			// This enables proxy servlets to proxy requests to additional destinations 
			// when called by the container in the doResponse() method for a tentative 
			// non-2XX best response. 
			// After all such additional proxy branches have been responded to and after 
			// considering any servlet created responses, the container eventually arrives at 
			// the overall best response and forwards this response upstream. 
			// If this best response is a non-2XX final response, then when the forwarding takes place, 
			// the state of the SipSession object becomes TERMINATED.
			else {
				setState(State.TERMINATED);
				readyToInvalidate =true;
				if(logger.isDebugEnabled()) {
					logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
				}
			}						
		}
		if(((State.CONFIRMED.equals(state) || State.TERMINATED.equals(state)) && response.getStatus() == 200 && Request.BYE.equals(cSeqHeader.getMethod())) || response.getStatus() == 487) {
			setState(State.TERMINATED);
			readyToInvalidate =true;
			
			if(logger.isDebugEnabled()) {
				logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
				logger.debug("the following sip session " + getKey() + " is ready to be invalidated ");
			}
		}
	}
	
	/**
	 * Update the sip session state upon sending/receiving a subsequent request
	 * Covers JSR 289 Section 6.2.1 along with updateStateOnResponse method
	 * @param request the subsequent request received/to send
	 * @param receive true if the subsequent request has been received, false if it is to be sent.
	 */
    public void updateStateOnSubsequentRequest(
			SipServletRequestImpl request, boolean receive) {
		if(Request.BYE.equalsIgnoreCase(request.getMethod())) {			
			this.setState(State.TERMINATED);
			if(logger.isDebugEnabled()) {
				logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
			}
		}
		//state updated to TERMINATED for CANCEL only if no final response had been received on the inviteTransaction
		if(((Request.CANCEL.equalsIgnoreCase(request.getMethod())))) {
			Transaction inviteTransaction = null;
			if(request.getTransaction() instanceof SIPServerTransaction) {
				inviteTransaction = ((SIPServerTransaction) request.getTransaction()).getCanceledInviteTransaction();
			} else {
				return ;
			}
			TransactionApplicationData inviteAppData = (TransactionApplicationData) 
				inviteTransaction.getApplicationData();
			ProxyImpl proxy = inviteAppData.getProxy();
			SipServletRequestImpl inviteRequest = (SipServletRequestImpl)inviteAppData.getSipServletMessage();
			if((inviteRequest != null && inviteRequest.getLastFinalResponse() == null) || 
						(proxy != null && proxy.getBestResponse() == null))  {
				this.setState(State.TERMINATED);
				if(logger.isDebugEnabled()) {
					logger.debug("the following sip session " + getKey() + " has its state updated to " + getState());
				}
			}
		}
		
	}
    
    /**
     * This method is called immediately when the conditions for read to invalidate
     * session are met
     */
    public void onReadyToInvalidate() {
    	this.readyToInvalidate = true;
    	
    	if(this.invalidateWhenReady) {
    		this.notifySipSessionListeners(SipSessionEventType.READYTOINVALIDATE);
    	}
    	
    	//If the application does not explicitly invalidate the session in the callback or has not defined a listener, 
    	//the container will invalidate the session. 
    	if(isValid) {
    		invalidate();
    	}
    }

	/**
	 * @return the key
	 */
	public SipSessionKey getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(SipSessionKey key) {
		this.key = key;
	}

	public ProxyBranchImpl getProxyBranch() {
		return proxyBranch;
	}

	public void setProxyBranch(ProxyBranchImpl proxyBranch) {
		this.proxyBranch = proxyBranch;
	}
	
	/**
     * Perform the internal processing required to passivate
     * this session.
     */
    public void passivate() {
        // Notify ActivationListeners
    	SipSessionEvent event = null;
        Set<String> keySet = sipSessionAttributeMap.keySet();
        for (String key : keySet) {
        	Object attribute = sipSessionAttributeMap.get(key);
            if (attribute instanceof SipSessionActivationListener) {
                if (event == null)
                    event = new SipSessionEvent(this);
                try {
                    ((SipSessionActivationListener)attribute)
                        .sessionWillPassivate(event);
                } catch (Throwable t) {
                    logger.error("SipSessionActivationListener threw exception", t);
                }
            }
		}
    }
    
    /**
     * Perform internal processing required to activate this
     * session.
     */
    public void activate() {        
        // Notify ActivationListeners
    	SipSessionEvent event = null;
        Set<String> keySet = sipSessionAttributeMap.keySet();
        for (String key : keySet) {
        	Object attribute = sipSessionAttributeMap.get(key);
            if (attribute instanceof SipSessionActivationListener) {
                if (event == null)
                    event = new SipSessionEvent(this);
                try {
                    ((SipSessionActivationListener)attribute)
                        .sessionDidActivate(event);
                } catch (Throwable t) {
                    logger.error("SipSessionActivationListener threw exception", t);
                }
            }
		}
    }
    
	public Principal getUserPrincipal() {
		return userPrincipal;
	}
	
	public void setUserPrincipal(Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}
	
	public boolean getInvalidateWhenReady() {
		return invalidateWhenReady;
	}
	
	public boolean isReadyToInvalidate() {
		return readyToInvalidate;
	}
	
	public void setInvalidateWhenReady(boolean arg0) {
		invalidateWhenReady = arg0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setOutboundInterface(InetAddress inetAddress) {
		//TODO check against our defined outbound interfaces
		String address = inetAddress.getHostAddress();
		try {
			outboundInterface = new SipURIImpl(SipFactories.addressFactory.createSipURI(null, address));
		} catch (ParseException e) {
			logger.error("couldn't parse the SipURI from USER[" + null
					+ "] HOST[" + address + "]", e);
			throw new IllegalArgumentException("Could not create SIP URI user = " + null + " host = " + address);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOutboundInterface(InetSocketAddress inetSocketAddress) {
		//TODO check against our defined outbound interfaces		
		String address = inetSocketAddress.getAddress().getHostAddress() + ":" + inetSocketAddress.getPort();
		try {
			outboundInterface = new SipURIImpl(SipFactories.addressFactory.createSipURI(null, address));
		} catch (ParseException e) {
			logger.error("couldn't parse the SipURI from USER[" + null
					+ "] HOST[" + address + "]", e);
			throw new IllegalArgumentException("Could not create SIP URI user = " + null + " host = " + address);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ServletContext getServletContext() {
		return sipApplicationSession.getSipContext().getServletContext();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public MobicentsSipSession removeDerivedSipSession(String toTag) {
		return derivedSipSessions.remove(toTag);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public MobicentsSipSession findDerivedSipSession(String toTag) {
		return derivedSipSessions.get(toTag);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Iterator<MobicentsSipSession> getDerivedSipSessions() {
		return derivedSipSessions.values().iterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setParentSession(MobicentsSipSession mobicentsSipSession) {
		parentSession = mobicentsSipSession;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setSipSessionAttributeMap(
			Map<String, Object> sipSessionAttributeMap) {
		this.sipSessionAttributeMap = sipSessionAttributeMap;
	}
	/**
	 * {@inheritDoc}
	 */
	public void addDerivedSipSessions(MobicentsSipSession derivedSession) {
		derivedSipSessions.putIfAbsent(derivedSession.getKey().getToTag(), derivedSession);
	}
	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getSipSessionAttributeMap() {
		return sipSessionAttributeMap;
	}
	/**
	 * {@inheritDoc}
	 */
	public boolean getSupervisedMode() {
		return supervisedMode;
	}
}
