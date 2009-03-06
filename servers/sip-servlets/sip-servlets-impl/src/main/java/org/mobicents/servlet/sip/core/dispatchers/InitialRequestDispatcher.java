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
package org.mobicents.servlet.sip.core.dispatchers;

import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.header.extensions.JoinHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Iterator;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;
import javax.servlet.sip.ar.SipTargetedRequestInfo;
import javax.servlet.sip.ar.SipTargetedRequestType;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.Header;
import javax.sip.header.RouteHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.servlet.sip.JainSipUtils;
import org.mobicents.servlet.sip.SipFactories;
import org.mobicents.servlet.sip.address.GenericURIImpl;
import org.mobicents.servlet.sip.address.RFC2396UrlDecoder;
import org.mobicents.servlet.sip.address.SipURIImpl;
import org.mobicents.servlet.sip.address.TelURLImpl;
import org.mobicents.servlet.sip.core.RoutingState;
import org.mobicents.servlet.sip.core.SipApplicationDispatcher;
import org.mobicents.servlet.sip.core.SipSessionRoutingType;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
import org.mobicents.servlet.sip.core.session.MobicentsSipSession;
import org.mobicents.servlet.sip.core.session.SessionManagerUtil;
import org.mobicents.servlet.sip.core.session.SipApplicationSessionKey;
import org.mobicents.servlet.sip.core.session.SipManager;
import org.mobicents.servlet.sip.core.session.SipSessionKey;
import org.mobicents.servlet.sip.message.SipFactoryImpl;
import org.mobicents.servlet.sip.message.SipServletMessageImpl;
import org.mobicents.servlet.sip.message.SipServletRequestImpl;
import org.mobicents.servlet.sip.message.SipServletRequestReadOnly;
import org.mobicents.servlet.sip.message.TransactionApplicationData;
import org.mobicents.servlet.sip.startup.SipContext;
import org.mobicents.servlet.sip.startup.loading.SipServletMapping;

/**
 * This class is responsible for implementing the logic for routing an initial request
 * according to the following sections of the JSR 289 specification Section 
 * 15.4.1 Procedure for Routing an Initial Request.
 * 15.11 Session Targeting 
 * 16 Mapping Requests To Servlets 
 * 
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public class InitialRequestDispatcher extends RequestDispatcher {

	private static Log logger = LogFactory.getLog(InitialRequestDispatcher.class);
	private SipApplicationRouter sipApplicationRouter;
	
	public InitialRequestDispatcher(
			SipApplicationDispatcher sipApplicationDispatcher) {		
		super(sipApplicationDispatcher);
		this.sipApplicationRouter = sipApplicationDispatcher.getSipApplicationRouter();
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispatchMessage(SipProvider sipProvider, SipServletMessageImpl sipServletMessage) throws DispatcherException {
		final SipFactoryImpl sipFactoryImpl = sipApplicationDispatcher.getSipFactory();
		SipServletRequestImpl sipServletRequest = (SipServletRequestImpl) sipServletMessage;
		if(logger.isInfoEnabled()) {
			logger.info("Routing of Initial Request " + sipServletRequest);
		}		
		final Request request = (Request) sipServletRequest.getMessage();
		
		javax.servlet.sip.Address poppedAddress = sipServletRequest.getPoppedRoute();
		if(logger.isInfoEnabled()) {
			logger.info("popped route : " + poppedAddress);
		}
		MobicentsSipSession sipSessionImpl = sipServletRequest.getSipSession();
		//set directive from popped route header if it is present			
		Serializable stateInfo = null;
		//If request is received from an external SIP entity, directive is set to NEW. 
		SipApplicationRoutingDirective sipApplicationRoutingDirective = SipApplicationRoutingDirective.NEW;
		if(poppedAddress != null) {
			// get the state info associated with the request because it means 
			// that is has been routed back to the container			
			String directive = poppedAddress.getParameter(ROUTE_PARAM_DIRECTIVE);
			if(directive != null && directive.length() > 0) {
				//If request is received from an application, directive is set either implicitly 
				//or explicitly by the application. 
				if(logger.isInfoEnabled()) {
					logger.info("directive before the request has been routed back to container : " + directive);
				}
				sipApplicationRoutingDirective = SipApplicationRoutingDirective.valueOf(
						SipApplicationRoutingDirective.class, directive);
				String previousAppName = poppedAddress.getParameter(ROUTE_PARAM_PREV_APPLICATION_NAME);
				if(logger.isInfoEnabled()) {
					logger.info("application name before the request has been routed back to container : " + previousAppName);
				}
				SipContext sipContext = sipApplicationDispatcher.findSipApplication(previousAppName);				
				SipSessionKey sipSessionKey = SessionManagerUtil.getSipSessionKey(previousAppName, request, false);
				sipSessionImpl = sipContext.getSipManager().getSipSession(sipSessionKey, false, sipFactoryImpl, null);
				//If request is received from an application, and directive is CONTINUE or REVERSE, 
				//stateInfo is set to that of the original request that this request is associated with. 
				stateInfo = sipSessionImpl.getStateInfo();
				sipServletRequest.setSipSession(sipSessionImpl);
				if(logger.isInfoEnabled()) {
					logger.info("state info before the request has been routed back to container : " + stateInfo);
				}
			}
		} else if(sipSessionImpl != null) {
			stateInfo = sipSessionImpl.getStateInfo();
			sipApplicationRoutingDirective = sipServletRequest.getRoutingDirective();
			if(logger.isInfoEnabled()) {
				logger.info("previous state info : " + stateInfo);
			}
		}
		SipApplicationRoutingRegion routingRegion = null;
		if(sipSessionImpl != null) {
			routingRegion = sipSessionImpl.getRegion();
		}
		
		// 15.11.3 Target Session : Encode URI mechanism
		// Upon receiving an initial request for processing, a container MUST check the topmost Route header and 
		// Request-URI (in that order) to see if it contains an encoded URI. 
		// If it does, the container MUST use the encoded URI to locate the targeted SipApplicationSession object
		String targetedApplicationKey = sipServletRequest.getRequestURI().getParameter(MobicentsSipApplicationSession.SIP_APPLICATION_KEY_PARAM_NAME);
		if(targetedApplicationKey != null) {
			targetedApplicationKey = RFC2396UrlDecoder.decode(targetedApplicationKey);
		}
		SipTargetedRequestInfo targetedRequestInfo = retrieveTargetedApplication(targetedApplicationKey);
		if(targetedRequestInfo == null && poppedAddress != null) {
			targetedApplicationKey = poppedAddress.getURI().getParameter(MobicentsSipApplicationSession.SIP_APPLICATION_KEY_PARAM_NAME);
			targetedRequestInfo = retrieveTargetedApplication(targetedApplicationKey);
		}	
		
		// 15.11.4 Join and Replaces Targeting Mechanism		
		SipTargetedRequestInfo joinReplacesTargetedRequestInfo = null;
		MobicentsSipSession joinReplacesCorrespondingSession = null;
		JoinHeader joinHeader = (JoinHeader)request.getHeader(JoinHeader.NAME);
		ReplacesHeader replacesHeader = (ReplacesHeader)request.getHeader(ReplacesHeader.NAME);
		if(joinHeader != null || replacesHeader != null) {
			if(logger.isDebugEnabled()) {
				logger.debug("One Join or Replaces Header has been found : JoinHeader = " + joinHeader + ", ReplacesHeader = " + replacesHeader);
			}
			// 15.11.4.1 If a container supporting [RFC 3911] or [RFC 3891] receives an initial INVITE request with Join or Replaces header, 
			// the container must first ensure that the request passes the RFC-defined validation rules.
			if(!Request.INVITE.equals(request.getMethod())) {
				throw new DispatcherException(Response.BAD_REQUEST, "A Join or Replaces Header cannot be present in a request other than INVITE as per RFC 3911, Section 4 or RFC 3891, Section 3. Check your request " + request);
			}
			if(joinHeader != null && replacesHeader != null) {
				throw new DispatcherException(Response.BAD_REQUEST, "A Join Header and a Replaces Header cannot be present as per RFC 3911, Section 4 in the same request " + request);
			}
			Dialog joinReplacesDialog = null;
			if(joinHeader != null) {
				joinReplacesDialog = ((SipStackExt)sipProvider.getSipStack()).getJoinDialog(joinHeader);
			}
			if(replacesHeader != null) {
				joinReplacesDialog = ((SipStackExt)sipProvider.getSipStack()).getReplacesDialog(replacesHeader);
			}
			if(targetedRequestInfo != null) {
				// If no match is found, but the Request-URI in the INVITE corresponds to a conference URI, the UAS MUST ignore the Join header and continue
				// processing the INVITE as if the Join header did not exist
			} else 
			//Otherwise if no match is found, the UAS rejects the INVITE and returns a 481 Call/Transaction Does Not Exist response.  
			if(joinReplacesDialog == null && targetedRequestInfo == null) {
				throw new DispatcherException(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, "Join/Replaces Header : no match is found as per RFC 3911, Section 4 or RFC 3891, Section 3 in the request " + request);
			} else 
			//Likewise, if the Join/Replaces header field matches a dialog which was not created with an INVITE, the UAS MUST reject the request with a 481 response.	
			if(joinReplacesDialog != null && ((TransactionApplicationData)joinReplacesDialog.getApplicationData()) != null && !Request.INVITE.equals(((TransactionApplicationData)joinReplacesDialog.getApplicationData()).getSipServletMessage().getMethod())) {					 
				throw new DispatcherException(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, "Join/Replaces header field matches a dialog which was not created with an INVITE as per RFC 3911, Section 4 or RFC 3891, Section 3 in the request " + request);
			} else
			// If the Join/Replaces header field matches a dialog which has already terminated, the UA SHOULD decline the request with a 603 Declined response.
			if(joinReplacesDialog != null && DialogState.TERMINATED.equals(joinReplacesDialog.getState())) {
				throw new DispatcherException(Response.DECLINE, "Join/Replaces header field matches a dialog which has already terminated as per RFC 3911, Section 4 or RFC 3891, Section 3 in the request " + request);
			}
			//TODO If the initiator of the new INVITE has authenticated successfully as equivalent to the user who is being joined, then the join is authorized
			//TODO Alternatively, the Referred-By mechanism [9] defines a mechanism that the UAS can use to verify that a join request was sent on behalf of the other participant in the matched dialog
			
			// 15.11.4.2. For a request that passes the validation step, the container MUST attempt to locate the SipSession object matching 
			// the tuple from the Join or Replaces header. In locating this SipSession, the container MUST not match a SipSession 
			// owned by an application that has acted as a proxy with respect to the candidate SipSession. 
			// It must only match a SipSession for an application that has acted as a UA. 
			// If the matching SipSession is found, the container Session Targeting must locate the corresponding SipApplicationSession 
			// object along with the application name of the application that owns the SipApplicationSession.
			joinReplacesCorrespondingSession = retrieveSipSession(joinReplacesDialog);			
			if(joinReplacesCorrespondingSession != null) {
				if(logger.isDebugEnabled()) {
					logger.debug("the following matching sip session has been found for the Join or Replaces Header " + joinReplacesCorrespondingSession.getKey());
				}
				// 15.11.4.3. The container MUST populate a SipTargetedRequestInfo object with the type corresponding to whichever header 
				// appears in the request (e.g. JOIN or REPLACES) and with the application name that owns the identified SipApplicationSession 
				// and SipSession. 
				// The container MUST then pass the request to the Application Router’s getNextApplication() method as a targeted request i.e., 
				// along with the populated SipTargetedRequestInfo object.
				SipTargetedRequestType sipTargetedRequestType = null;
				if(joinHeader != null) {
					sipTargetedRequestType = SipTargetedRequestType.JOIN;
				} 
				if(replacesHeader != null) {
					sipTargetedRequestType = SipTargetedRequestType.REPLACES;
				}
				joinReplacesTargetedRequestInfo = new SipTargetedRequestInfo(sipTargetedRequestType, joinReplacesCorrespondingSession.getKey().getApplicationName());
			} else {
				// 15.11.4.4.If no SipSession matching the tuple is found, the container MUST pass the request to the Application Router as an untargeted request, i.e., where the SipTargetedRequestInfo argument to getNextApplication() is null.
				if(logger.isDebugEnabled()) {
					logger.debug("no matching sip session found for the Join or Replaces Header");
				}
			}
		}		
		if(joinReplacesTargetedRequestInfo != null) {
			targetedRequestInfo = joinReplacesTargetedRequestInfo;
		}
		
		// 15.4.1 Routing an Initial request Algorithm
		// 15.4.1 Procedure : point 1			
		
		//the application router shouldn't modify the request
		SipServletRequestReadOnly sipServletRequestReadOnly = new SipServletRequestReadOnly(sipServletRequest);
		
		SipApplicationRouterInfo applicationRouterInfo = 
			sipApplicationRouter.getNextApplication(
					sipServletRequestReadOnly, 
					routingRegion, 
					sipApplicationRoutingDirective,
					targetedRequestInfo,
					stateInfo);
		
		sipServletRequestReadOnly = null;
		
		// 15.4.1 Procedure : point 2
		if(checkRouteModifier(applicationRouterInfo, sipServletRequest)) {
			return;
		}
		// 15.4.1 Procedure : point 3
		if(applicationRouterInfo.getNextApplicationName() == null) {
			dispatchOutsideContainer(sipServletRequest);
		} else {			
			dispatchInsideContainer(sipProvider, applicationRouterInfo, sipServletRequest, sipFactoryImpl, joinReplacesCorrespondingSession);
		}		
	}

	/**
	 * Dispatch a request inside the container based on the information returned by the AR
	 * @param sipProvider the sip Provider on which the request has been received 
	 * @param applicationRouterInfo information returned by the AR
	 * @param sipServletRequest the request to dispatch
	 * @param sipFactoryImpl the sip factory
	 * @param joinReplacesSipSession the potential corresponding Join/Replaces SipSession previously found, can be null
	 * @throws DispatcherException a problem occured while dispatching the request
	 */
	private void dispatchInsideContainer(final SipProvider sipProvider, final SipApplicationRouterInfo applicationRouterInfo, final SipServletRequestImpl sipServletRequest, final SipFactoryImpl sipFactoryImpl, MobicentsSipSession joinReplacesSipSession) throws DispatcherException {
		if(logger.isInfoEnabled()) {
			logger.info("Dispatching the request event to " + applicationRouterInfo.getNextApplicationName());
		}
		final Request request = (Request) sipServletRequest.getMessage();
		sipServletRequest.setCurrentApplicationName(applicationRouterInfo.getNextApplicationName());
		final SipContext sipContext = sipApplicationDispatcher.findSipApplication(applicationRouterInfo.getNextApplicationName());
		// follow the procedures of Chapter 16 to select a servlet from the application.									
		//no matching deployed apps
		if(sipContext == null) {
			// the app returned by the Application Router returned an app
			// that is not currently deployed, sends a 500 error response as was discussed 
			// in the JSR 289 EG, (for reference thread "Questions regarding AR" initiated by Uri Segev)
			// and stops processing.
			throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "No matching deployed application has been found !");
		}			
		final SipManager sipManager = (SipManager)sipContext.getManager();
		
		JoinHeader joinHeader = (JoinHeader)request.getHeader(JoinHeader.NAME);
		ReplacesHeader replacesHeader = (ReplacesHeader)request.getHeader(ReplacesHeader.NAME);
		String headerName = null;
		if(joinHeader != null) {
			headerName = JoinHeader.NAME;
		} else if (replacesHeader != null) {
			headerName = ReplacesHeader.NAME;	
		}
		
		MobicentsSipApplicationSession sipApplicationSession = null;
		if(joinReplacesSipSession != null && applicationRouterInfo.getNextApplicationName().equals(joinReplacesSipSession.getKey().getApplicationName())) {
			// 15.11.4.5. If the Application Router returns an application name that matches the application name found in step 15.11.4.2, 
			// then the container must create a SipSession object and associate it with the SipApplicationSession identified in step 2. 
			
			sipApplicationSession = joinReplacesSipSession.getSipApplicationSession();
			if(logger.isDebugEnabled()) {
				logger.debug("Reusing the application session from the Join/Replaces " + sipApplicationSession.getId());
			}
			SipApplicationSessionKey sipApplicationSessionKey = makeAppSessionKey(
					sipContext, sipServletRequest, applicationRouterInfo.getNextApplicationName());
			sipContext.getSipSessionsUtil().addCorrespondingSipApplicationSession(sipApplicationSessionKey, sipApplicationSession.getKey(), headerName);
		} else {
			// 15.11.4.6. If the Application Router returns an application name that does not match the application name found in step 15.11.4.2,
			// then the container invokes the application using its normal procedure, creating a new SipSession and SipApplicationSession.

			SipApplicationSessionKey sipApplicationSessionKey = makeAppSessionKey(
					sipContext, sipServletRequest, applicationRouterInfo.getNextApplicationName());
			sipApplicationSession = sipManager.getSipApplicationSession(
					sipApplicationSessionKey, true);
		}	
		
		//sip application session association
		final MobicentsSipApplicationSession appSession = sipApplicationSession;
		//sip session association
		SipSessionKey sessionKey = SessionManagerUtil.getSipSessionKey(applicationRouterInfo.getNextApplicationName(), request, false);
		final MobicentsSipSession sipSessionImpl = sipManager.getSipSession(sessionKey, true, sipFactoryImpl, appSession);
		sipServletRequest.setSipSession(sipSessionImpl);

		if(joinReplacesSipSession != null && applicationRouterInfo.getNextApplicationName().equals(joinReplacesSipSession.getKey().getApplicationName())) {
			// 15.11.4.5. If the Application Router returns an application name that matches the application name found in step 15.11.4.2, 
			// then the container must create a SipSession object and associate it with the SipApplicationSession identified in step 2. 
			// The association of this newly created SipSession with the one found in step 15.11.4.2 is made available to the application 
			// through the SipSessionsUtil.getCorrespondingSipSession(SipSession session, String headerName) method.			
			sipContext.getSipSessionsUtil().addCorrespondingSipSession(sipSessionImpl, joinReplacesSipSession, headerName);		
		}
		
		DispatchTask dispatchTask = new DispatchTask(sipServletRequest, sipProvider) {

			public void dispatch() throws DispatcherException {
				sipContext.enterSipApp(sipServletRequest, null, sipManager, true, true);
				try {
					sipSessionImpl.setSessionCreatingTransaction(sipServletRequest.getTransaction());
					
					// set the request's stateInfo to result.getStateInfo(), region to result.getRegion(), and URI to result.getSubscriberURI().			
					sipSessionImpl.setStateInfo(applicationRouterInfo.getStateInfo());
					sipSessionImpl.setRoutingRegion(applicationRouterInfo.getRoutingRegion());
					sipServletRequest.setRoutingRegion(applicationRouterInfo.getRoutingRegion());		
					try {
						URI subscriberUri = SipFactories.addressFactory.createURI(applicationRouterInfo.getSubscriberURI());				
						if(subscriberUri instanceof javax.sip.address.SipURI) {
							javax.servlet.sip.URI uri = new SipURIImpl((javax.sip.address.SipURI)subscriberUri);
							sipServletRequest.setSubscriberURI(uri);
							sipSessionImpl.setSipSubscriberURI(uri);
						} else if (subscriberUri instanceof javax.sip.address.TelURL) {
							javax.servlet.sip.URI uri = new TelURLImpl((javax.sip.address.TelURL)subscriberUri);
							sipServletRequest.setSubscriberURI(uri);
							sipSessionImpl.setSipSubscriberURI(uri);
						} else {
							javax.servlet.sip.URI uri = new GenericURIImpl(subscriberUri);
							sipServletRequest.setSubscriberURI(uri);
							sipSessionImpl.setSipSubscriberURI(uri);
						}
					} catch (ParseException pe) {
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Impossible to parse the subscriber URI returned by the Application Router " 
								+ applicationRouterInfo.getSubscriberURI() +
								", please put one of DAR:<HeaderName> with Header containing a valid URI or an exlicit valid URI ", pe);
					} 									
					String sipSessionHandlerName = sipSessionImpl.getHandler();						
					if(sipSessionHandlerName == null || sipSessionHandlerName.length() < 1) {
						String mainServlet = appSession.getCurrentRequestHandler();
						if(mainServlet != null && mainServlet.length() > 0) {
							sipSessionHandlerName = mainServlet;				
						} else {
							SipServletMapping sipServletMapping = sipContext.findSipServletMappings(sipServletRequest);
							if(sipServletMapping == null) {
								logger.error("Sending 404 because no matching servlet found for this request ");
								sendErrorResponse(Response.NOT_FOUND, (ServerTransaction) sipServletRequest.getTransaction(), request, sipProvider);
								return;
							} else {
								sipSessionHandlerName = sipServletMapping.getServletName();
							}
						}
						try {
							sipSessionImpl.setHandler(sipSessionHandlerName);
						} catch (ServletException e) {
							// this should never happen
							throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "An unexpected servlet exception occured while routing an initial request",e);
						} 
					}
					try {
						callServlet(sipServletRequest);
						if(logger.isInfoEnabled()) {
							logger.info("Request event dispatched to " + sipContext.getApplicationName());
						}
					} catch (ServletException e) {
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "An unexpected servlet exception occured while routing the following initial request " + request, e);
					} catch (IOException e) {				
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "An unexpected IO exception occured while routing the following initial request " + request, e);
					}
				} finally {
					sipContext.exitSipApp();
				}
				//if a final response has been sent, or if the request has 
				//been proxied or relayed we stop routing the request
				RoutingState routingState = sipServletRequest.getRoutingState();			
				if(RoutingState.INFORMATIONAL_RESPONSE_SENT.equals(routingState) ||
						RoutingState.FINAL_RESPONSE_SENT.equals(routingState) ||
						RoutingState.PROXIED.equals(routingState) ||
						RoutingState.RELAYED.equals(routingState) ||
						RoutingState.CANCELLED.equals(routingState)) {
					if(logger.isDebugEnabled()) {
						logger.debug("Routing State : " + sipServletRequest.getRoutingState() +
								"The Container hence stops routing the initial request.");
						sipContext.getSipManager().dumpSipSessions();
					}
				} else {
					if(logger.isDebugEnabled()) {
						logger.debug("Routing State : " + sipServletRequest.getRoutingState() +
								"The Container hence continue routing the initial request.");
					}
					try {
						// the app that was called didn't do anything with the request
						// in any case we should route back to container statefully 
//						sipServletRequest.addAppCompositionRRHeader();
						SipApplicationRouterInfo routerInfo = sipApplicationDispatcher.getNextInterestedApplication(sipServletRequest);
						if(routerInfo.getNextApplicationName() != null) {
							if(logger.isDebugEnabled()) {
								logger.debug("routing back to the container " +
										"since the following app is interested " + routerInfo.getNextApplicationName());
							}
							//add a route header to direct the request back to the container 
							//to check if there is any other apps interested in it
							sipServletRequest.addInfoForRoutingBackToContainer(applicationRouterInfo.getNextApplicationName());
						} else {							
							// If a servlet does not generate final response the routing process
							// will continue (non-terminating routing state). This code stops
							// routing these requests.
							javax.sip.address.SipURI sipRequestUri = (javax.sip.address.SipURI)request.getRequestURI();
							String host = sipRequestUri.getHost();
							int port = sipRequestUri.getPort();
							String transport = JainSipUtils.findTransport(request);
							boolean isAnotherDomain = sipApplicationDispatcher.isExternal(host, port, transport);
							if(!isAnotherDomain) {
								if(logger.isDebugEnabled()) {
									logger.debug("stop routing the request " +
											"since no more apps are is interested and the request uri points to the container.");
								}
								return ;
							} else {
								if(logger.isDebugEnabled()) {
									logger.debug("routing outside the container " +
											"since no more apps are is interested.");
								}
							}
						}
						try {
							forwardRequestStatefully(sipServletRequest, SipSessionRoutingType.CURRENT_SESSION, SipRouteModifier.NO_ROUTE);
						} catch (Exception e) {
							throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to forward statefully the following subsequent request " + request, e);
						}
					} 
//					catch (SipException e) {
//						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "an exception has occured when trying to forward statefully", e);
//					} 
					catch (ParseException e) {
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, e);					
					}	
				}
				
			}
			
		};
		getConcurrencyModelExecutorService(sipContext, sipServletRequest).execute(dispatchTask);

	}

	/**
	 * Dispatch a request outside the container
	 * @param sipServletRequest request
	 * @throws DispatcherException problem occured when dispatching the request outside the container
	 */
	private void dispatchOutsideContainer(SipServletRequestImpl sipServletRequest) throws DispatcherException {
		final Request request = (Request) sipServletRequest.getMessage();
		if(logger.isInfoEnabled()) {
			logger.info("Dispatching the request event outside the container");
		}
		//check if the request point to another domain
		if(request.getRequestURI() instanceof TelURL) {
			throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "cannot dispatch a request with a tel url request uri outside the container ");
		}
		javax.sip.address.SipURI sipRequestUri = (javax.sip.address.SipURI)request.getRequestURI();
		
		String host = sipRequestUri.getHost();
		int port = sipRequestUri.getPort();
		String transport = JainSipUtils.findTransport(request);
		boolean isAnotherDomain = sipApplicationDispatcher.isExternal(host, port, transport);			
		ListIterator<String> routeHeaders = sipServletRequest.getHeaders(RouteHeader.NAME);				
		if(isAnotherDomain || routeHeaders.hasNext()) {
			try {
				forwardRequestStatefully(sipServletRequest, SipSessionRoutingType.PREVIOUS_SESSION, SipRouteModifier.NO_ROUTE);
			} catch (Exception e) {
				throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to forward statefully the following subsequent request " + request, e);
			}
		} else {
			// the Request-URI does not point to another domain, and there is no Route header, 
			// the container should not send the request as it will cause a loop. 
			// Instead, the container must reject the request with 404 Not Found final response with no Retry-After header.
			throw new DispatcherException(Response.NOT_FOUND, "the Request-URI does not point to another domain, and there is no Route header," + 
					"the container should not send the request as it will cause a loop. " +
					"Instead, the container must reject the request with 404 Not Found final response with no Retry-After header. You may want to check your dar configuration file to see if the request can be handled or make sure you use the correct Application Router jar.");				
		}
	}

	/**
	 * JSR 289 Section 15.4.1 Procedure : point 2
	 * Check the result.getRouteModifier() :
	 * 	If result.getRouteModifier() is ROUTE, then get the routes using result.getRoutes()
	 * 		If the first returned route is external (does not belong to this container) then 
	 * 		push all of the routes on the Route header stack of the request and send the request externally. 
	 * 		Note that the first returned route becomes the top route header of the request.
	 * 	
	 * 		If the first returned route is internal then the container MUST make it available 
	 * 		to the applications via the SipServletRequest.getPoppedRoute() method and ignore the remaining ones, if any. 
	 * 		This allows the AR modify the popped route before passing it to the application.
	 * 
	 * If result.getRouteModifier() is ROUTE_BACK then push a route back to the container followed 
	 * by the external routes obtained from result.getRoutes() and send the request externally. 
	 * Note that the container route SHOULD include AR state encoded as a route parameter in order for the AR to continue processing the application chain once the request returns back to the container.
	 * 
	 * If result.getRouteModifier() is NO_ROUTE then disregard the result.getRoutes() and proceed.
	 * 
	 * @param applicationRouterInfo the applicationRouterInfo returned by the AR
	 * @param sipServletRequest the request
	 * @return true if the request has been routed outside, false otherwise
	 * @throws DispatcherException a proble occured while checking the information returned by the AR
	 */
	private boolean checkRouteModifier(SipApplicationRouterInfo applicationRouterInfo, SipServletRequestImpl sipServletRequest) throws DispatcherException {
		Request request = (Request) sipServletRequest.getMessage();
		SipRouteModifier sipRouteModifier = applicationRouterInfo.getRouteModifier();
		if(logger.isDebugEnabled()) {
			logger.debug("the AR returned the following sip route modifier" + sipRouteModifier);
		}
		if(sipRouteModifier != null) {
			String[] routes = applicationRouterInfo.getRoutes();
			switch(sipRouteModifier) {
				// ROUTE modifier indicates that SipApplicationRouterInfo.getRoute() returns a valid route,
				// it is up to container to decide whether it is external or internal.
				case ROUTE :					
					Address routeAddress = null; 
					RouteHeader applicationRouterInfoRouteHeader = null;
					try {
						routeAddress = SipFactories.addressFactory.createAddress(routes[0]);
						applicationRouterInfoRouteHeader = SipFactories.headerFactory.createRouteHeader(routeAddress);										
						if(sipApplicationDispatcher.isRouteExternal(applicationRouterInfoRouteHeader)) {						
							// push all of the routes on the Route header stack of the request and 
							// send the request externally
							for (int i = routes.length-1 ; i >= 0; i--) {
								routeAddress = SipFactories.addressFactory.createAddress(routes[i]);
								Header routeHeader = SipFactories.headerFactory.createRouteHeader(routeAddress);
								// Fix for Issue 280 provided by eelcoc
								try {
									request.addFirst(routeHeader);
								} catch (SipException e) {
									throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to add a route header to route externally the following initial request " + request, e);
								}
							}
							if(logger.isDebugEnabled()) {
								logger.debug("Routing the request externally " + sipServletRequest );
							}
							request.setRequestURI(SipFactories.addressFactory.createURI(routes[0]));
							try {
								forwardRequestStatefully(sipServletRequest, null, sipRouteModifier);
								return true;
							} catch (Exception e) {
								throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to forward statefully the following initial request " + request, e);
							}							
						} else {
							// the container MUST make the route available to the applications 
							// via the SipServletRequest.getPoppedRoute() method.							
							sipServletRequest.setPoppedRoute(applicationRouterInfoRouteHeader);
							break;
						}
					} catch (ParseException e) {
						// the AR returned an empty string route or a bad route
						// this shouldn't happen if the route modifier is ROUTE, processing is stopped
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Impossible to parse the route returned by the application router " +
								"into a compliant address" ,e);																						
					}	
				// NO_ROUTE indicates that application router is not returning any route 
				// and the SipApplicationRouterInfo.getRoute() value if any should be disregarded.	
				case NO_ROUTE :
					//nothing to do here
					//disregard the result.getRoute() and proceed. Specifically the SipServletRequest.getPoppedRoute() 
					//returns the same value as before the invocation of application router.
					break;
				// ROUTE_BACK directs the container to push its own route before 
				// pushing the external routes obtained from SipApplicationRouterInfo.getRoutes(). 
				case ROUTE_BACK :
					try {
						// Push container Route, pick up the first outbound interface
						SipURI sipURI = sipApplicationDispatcher.getOutboundInterfaces().get(0);
						sipURI.setParameter("modifier", "route_back");
						Header routeHeader = SipFactories.headerFactory.createHeader(RouteHeader.NAME, sipURI.toString());
						// Fix for Issue 280 provided by eelcoc
						try {
							request.addFirst(routeHeader);
							// push all of the routes on the Route header stack of the request and 
							// send the request externally
							for (int i = routes.length-1 ; i >= 0; i--) {
								routeHeader = SipFactories.headerFactory.createHeader(RouteHeader.NAME, routes[i]);
								request.addFirst(routeHeader);
							}
						} catch (SipException e) {
							throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to add a route header to route externally the following initial request " + request, e);
						}
						if(logger.isDebugEnabled()) {
							logger.debug("Routing the request externally " + sipServletRequest );
						}					
						try {
							forwardRequestStatefully(sipServletRequest, null, sipRouteModifier);
							return true;
						} catch (Exception e) {
							throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Unexpected Exception while trying to forward statefully the following subsequent request " + request, e);
						}										
					} catch (ParseException e) {
						// the AR returned an empty string route or a bad route
						// this shouldn't happen if the route modifier is ROUTE, processing is stopped
						throw new DispatcherException(Response.SERVER_INTERNAL_ERROR, "Impossible to parse the route returned by the application router " +
								"into a compliant address" ,e);							
					}	
			}			
		}
		return false;
	}

	/**
	 * Section 15.11.3 Encode URI Mechanism
	 * The container MUST use the encoded URI to locate the targeted SipApplicationSession object. 
	 * If a valid SipApplicationSession is found, the container must determine 
	 * the name of the application that owns the SipApplicationSession object.
	 * 
	 * @param targetedApplicationKey the encoded URI parameter
	 * @return null if no corresponding SipApplicationSession could be found or 
	 * if the param is null, the SipTargetedRequestInfo completed otherwise
	 */
	private final SipTargetedRequestInfo retrieveTargetedApplication(
			String targetedApplicationKey) {
		if( targetedApplicationKey != null && 
				targetedApplicationKey.length() > 0) {
			SipApplicationSessionKey targetedApplicationSessionKey;
			try {
				targetedApplicationSessionKey = SessionManagerUtil.parseSipApplicationSessionKey(targetedApplicationKey);
			} catch (ParseException e) {
				logger.error("Couldn't parse the targeted application key " + targetedApplicationKey, e);
				return null;
			}
			SipContext sipContext = sipApplicationDispatcher.findSipApplication(targetedApplicationSessionKey.getApplicationName());
			if(sipContext != null && ((SipManager)sipContext.getManager()).getSipApplicationSession(targetedApplicationSessionKey, false) != null) {
				return new SipTargetedRequestInfo(SipTargetedRequestType.ENCODED_URI, targetedApplicationSessionKey.getApplicationName());
			}				
		}
		return null;
	}
	
	/**
	 * Try to find a matching Sip Session to a given dialog
	 * @param dialog the dialog to find the session
	 * @return the matching session, null if not session have been found
	 */
	private MobicentsSipSession retrieveSipSession(Dialog dialog) {
		
		Iterator<SipContext> iterator = sipApplicationDispatcher.findSipApplications();
		while(iterator.hasNext()) {
			SipContext sipContext = iterator.next();
			SipManager sipManager = sipContext.getSipManager();
			
			Iterator<MobicentsSipSession> sipSessionsIt= sipManager.getAllSipSessions();
			while (sipSessionsIt.hasNext()) {
				MobicentsSipSession mobicentsSipSession = (MobicentsSipSession) sipSessionsIt
						.next();
				SipSessionKey sessionKey = mobicentsSipSession.getKey();				
				if(sessionKey.getCallId().trim().equals(dialog.getCallId().getCallId())) {
					if(logger.isDebugEnabled()) {
						logger.debug("found session with the same Call Id " + sessionKey + ", to Tag " + sessionKey.getToTag());
						logger.debug("dialog localParty = " + dialog.getLocalParty().getURI() + ", localTag " + dialog.getLocalTag());
						logger.debug("dialog remoteParty = " + dialog.getRemoteParty().getURI() + ", remoteTag " + dialog.getRemoteTag());
					}
					if(sessionKey.getFromAddress().equals(dialog.getLocalParty().getURI().toString()) && sessionKey.getToAddress().equals(dialog.getRemoteParty().getURI().toString()) &&
							sessionKey.getFromTag().equals(dialog.getLocalTag()) && sessionKey.getToTag().equals(dialog.getRemoteTag())) {
						if(mobicentsSipSession.getProxy() == null) {
							return mobicentsSipSession;	
						}
					} else if (sessionKey.getFromAddress().equals(dialog.getRemoteParty().getURI().toString()) && sessionKey.getToAddress().equals(dialog.getLocalParty().getURI().toString()) &&
							sessionKey.getFromTag().equals(dialog.getRemoteTag()) && sessionKey.getToTag().equals(dialog.getLocalTag())){
						if(mobicentsSipSession.getProxy() == null) {
							return mobicentsSipSession;	
						}
					}
				}
			}
		}
		
		return null;
	}
}
