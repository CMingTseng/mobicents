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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.servlet.sip.SipFactories;
import org.mobicents.servlet.sip.core.SipApplicationDispatcher;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
import org.mobicents.servlet.sip.core.session.MobicentsSipSession;
import org.mobicents.servlet.sip.core.session.SessionManagerUtil;
import org.mobicents.servlet.sip.core.session.SipApplicationSessionKey;
import org.mobicents.servlet.sip.message.SipServletMessageImpl;
import org.mobicents.servlet.sip.message.SipServletRequestImpl;
import org.mobicents.servlet.sip.message.SipServletRequestReadOnly;
import org.mobicents.servlet.sip.message.SipServletResponseImpl;
import org.mobicents.servlet.sip.security.SipSecurityUtils;
import org.mobicents.servlet.sip.startup.SipContext;

/**
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public abstract class MessageDispatcher {

	private static Log logger = LogFactory.getLog(MessageDispatcher.class);
	
	public static final String ROUTE_PARAM_DIRECTIVE = "directive";
	
	public static final String ROUTE_PARAM_PREV_APPLICATION_NAME = "previousappname";
	/* 
	 * This parameter is to know which app handled the request 
	 */
	public static final String RR_PARAM_APPLICATION_NAME = "appname";
	/* 
	 * This parameter is to know which servlet handled the request 
	 */
	public static final String RR_PARAM_HANDLER_NAME = "handler";
	/* 
	 * This parameter is to know if a servlet application sent a final response
	 */
	public static final String FINAL_RESPONSE = "final_response";
	/* 
	 * This parameter is to know if a servlet application has generated its own application key
	 */
	public static final String GENERATED_APP_KEY = "gen_app_key";
	/* 
	 * This parameter is to know when an app was not deployed and couldn't handle the request
	 * used so that the response doesn't try to call the app not deployed
	 */
	public static final String APP_NOT_DEPLOYED = "appnotdeployed";
	/* 
	 * This parameter is to know when no app was returned by the AR when doing the initial selection process
	 * used so that the response is forwarded externally directly
	 */
	public static final String NO_APP_RETURNED = "noappreturned";
	/*
	 * This parameter is to know when the AR returned an external route instead of an app
	 */
	public static final String MODIFIER = "modifier";
	
	/*
	 * This is URI parameter to contain application route in from/to header addresses. Used when in UAC mode (workaround IMS)
	 */
	public static final String MOBICENTS_URI_ROUTE_PARAM = "mobicents-app-route";
	
	protected SipApplicationDispatcher sipApplicationDispatcher = null;
		
	public MessageDispatcher(SipApplicationDispatcher sipApplicationDispatcher) {
		this.sipApplicationDispatcher = sipApplicationDispatcher;
	}
	
	/**
	 * 
	 * @param errorCode
	 * @param transaction
	 * @param request
	 * @param sipProvider
	 */
	public static void sendErrorResponse(int errorCode,
			ServerTransaction transaction, Request request,
			SipProvider sipProvider) {
		try{
			Response response=SipFactories.messageFactory.createResponse
	        	(errorCode,request);			
	        if (transaction!=null) {
	        	transaction.sendResponse(response);
	        } else { 
	        	sipProvider.sendResponse(response);
	        }
		} catch (Exception e) {
			logger.error("Problem while sending the error response to the following request "
					+ request.toString(), e);
		}
	}
	
	protected static SipApplicationSessionKey makeAppSessionKey(SipContext sipContext, SipServletRequestImpl sipServletRequestImpl, String applicationName) {
		String id = null;
		boolean isAppGeneratedKey = false;
		Request request = (Request) sipServletRequestImpl.getMessage();
		Method appKeyMethod = null;
		// Session Key Based Targeted Mechanism is oly for Initial Requests 
		// see JSR 289 Section 15.11.2 Session Key Based Targeting Mechanism 
		if(sipServletRequestImpl.isInitial() && sipContext != null) {
			appKeyMethod = sipContext.getSipApplicationKeyMethod();
		}
		if(appKeyMethod != null) {
			if(logger.isDebugEnabled()) {
				logger.debug("For request target to application " + sipContext.getApplicationName() + 
						", using the following annotated method to generate the application key " + appKeyMethod);
			}
			SipServletRequestReadOnly readOnlyRequest = new SipServletRequestReadOnly(sipServletRequestImpl);
			try {
				id = (String) appKeyMethod.invoke(null, new Object[] {readOnlyRequest});
				isAppGeneratedKey = true;
			} catch (IllegalArgumentException e) {
				logger.error("Couldn't invoke the app session key annotated method !", e);
			} catch (IllegalAccessException e) {
				logger.error("Couldn't invoke the app session key annotated method !", e);
			} catch (InvocationTargetException e) {
				logger.error("Couldn't invoke the app session key annotated method !", e);
			} finally {
				readOnlyRequest = null;
			}
			if(id == null) {
				throw new IllegalStateException("SipApplicationKey annotated method shoud not return null");
			}
			if(logger.isDebugEnabled()) {
				logger.debug("For request target to application " + sipContext.getApplicationName() + 
						", following annotated method " + appKeyMethod + " generated the application key : " + id);
			}
		} else {
			id = ((CallIdHeader)request.getHeader((CallIdHeader.NAME))).getCallId();
		}
		SipApplicationSessionKey sipApplicationSessionKey = SessionManagerUtil.getSipApplicationSessionKey(
				applicationName, 
				id,
				isAppGeneratedKey);
		return sipApplicationSessionKey;
	}
	
	public static void callServlet(SipServletRequestImpl request) throws ServletException, IOException {
		MobicentsSipSession session = request.getSipSession();
		if(logger.isInfoEnabled()) {
			logger.info("Dispatching request " + request.toString() + 
				" to following App/servlet => " + session.getKey().getApplicationName()+ 
				"/" + session.getHandler());
		}
		String sessionHandler = session.getHandler();
		MobicentsSipApplicationSession sipApplicationSessionImpl = session.getSipApplicationSession();
		SipContext sipContext = sipApplicationSessionImpl.getSipContext();
		Wrapper sipServletImpl = (Wrapper) sipContext.findChild(sessionHandler);
		Servlet servlet = sipServletImpl.allocate();		
		
		// JBoss-specific CL issue:
		// This is needed because usually the classloader here is some UnifiedClassLoader3,
		// which has no idea where the servlet ENC is. We will use the classloader of the
		// servlet class, which is the WebAppClassLoader, and has ENC fully loaded with
		// with java:comp/env/security (manager, context etc)
		ClassLoader cl = servlet.getClass().getClassLoader();
		Thread.currentThread().setContextClassLoader(cl);
		
		if(!securityCheck(request)) return;
	    
		try {
			servlet.service(request, null);
		} finally {		
			sipServletImpl.deallocate(servlet);
		}
	}
	
	public static void callServlet(SipServletResponseImpl response) throws ServletException, IOException {		
		MobicentsSipSession session = response.getSipSession();
		if(logger.isInfoEnabled()) {
			logger.info("Dispatching response " + response.toString() + 
				" to following App/servlet => " + session.getKey().getApplicationName()+ 
				"/" + session.getHandler());
		}
		
		Container container = ((MobicentsSipApplicationSession)session.getApplicationSession()).getSipContext().findChild(session.getHandler());
		Wrapper sipServletImpl = (Wrapper) container;
		
		if(sipServletImpl == null || sipServletImpl.isUnavailable()) {
			logger.warn(sipServletImpl.getName()+ " is unavailable, dropping response " + response);
		} else {
			Servlet servlet = sipServletImpl.allocate();
			try {
				servlet.service(null, response);
			} finally {
				sipServletImpl.deallocate(servlet);
			}
		}
				
	}
	
	public static boolean securityCheck(SipServletRequestImpl request)
	{
		MobicentsSipApplicationSession appSession = (MobicentsSipApplicationSession) request.getApplicationSession();
		SipContext sipStandardContext = appSession.getSipContext();
		boolean authorized = SipSecurityUtils.authorize(sipStandardContext, request);
		
		// This will propagate the identity for the thread and all called components
		// TODO: FIXME: This would introduce a dependency on JBoss
		// SecurityAssociation.setPrincipal(request.getUserPrincipal());
		
		return authorized;
	}
	
	/**
	 * Responsible for routing and dispatching a SIP message to the correct application
	 * 
	 * @param sipProvider use the sipProvider to route the message if needed, can be null
	 * @param sipServletMessage the SIP message to route and dispatch
	 * @return true if we should continue routing to other applications, false otherwise
	 * @throws Exception if anything wrong happens
	 */
	public abstract void dispatchMessage(SipProvider sipProvider, SipServletMessageImpl sipServletMessage) throws DispatcherException;
}
