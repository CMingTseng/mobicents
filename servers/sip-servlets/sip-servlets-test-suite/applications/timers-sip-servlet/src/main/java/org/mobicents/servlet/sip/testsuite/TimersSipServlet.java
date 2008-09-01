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
package org.mobicents.servlet.sip.testsuite;

import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public class TimersSipServlet
		extends SipServlet 
		implements SipApplicationSessionListener, TimerListener {

	private static final String ALREADY_EXTENDED = "alreadyExtended";
	private static final String ALREADY_INVALIDATED = "alreadyInvalidated";
	

	private static Log logger = LogFactory.getLog(TimersSipServlet.class);
	
	private static final String CONTENT_TYPE = "text/plain;charset=UTF-8";
	private static final String SIP_APP_SESSION_EXPIRED = "sipAppSessionExpired";
	private static final String SIP_APP_SESSION_READY_TO_BE_INVALIDATED = "sipAppSessionReadyToBeInvalidated";
	private static final String TIMER_EXPIRED = "timerExpired";
	
	private SipFactory sipFactory;	
	
	
	/** Creates a new instance of TimersSipServlet */
	public TimersSipServlet() {
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {		
		super.init(servletConfig);
		logger.info("the timers test sip servlet has been started");
		try { 			
			// Getting the Sip factory from the JNDI Context
			Properties jndiProps = new Properties();			
			Context initCtx = new InitialContext(jndiProps);
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			sipFactory = (SipFactory) envCtx.lookup("sip/org.mobicents.servlet.sip.testsuite.TimersSipServlet/SipFactory");
			logger.info("Sip Factory ref from JNDI : " + sipFactory);
		} catch (NamingException e) {
			throw new ServletException("Uh oh -- JNDI problem !", e);
		}
	}		
	
	/**
	 * {@inheritDoc}
	 */
	protected void doInvite(SipServletRequest request) throws ServletException,
			IOException {

		logger.info("Got request: "
				+ request.getMethod());
//		SipServletResponse ringingResponse = request.createResponse(SipServletResponse.SC_RINGING);
//		ringingResponse.send();
		SipServletResponse okResponse = request.createResponse(SipServletResponse.SC_OK);
		okResponse.send();
		request.getApplicationSession().setAttribute("sipFactory", sipFactory);
		request.getApplicationSession().setInvalidateWhenReady(true);
		//create a timer to test the feature				
		TimerService timerService = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
		timerService.createTimer(request.getApplicationSession(), 1000, false, null);
	}
	
	@Override
	protected void doSuccessResponse(SipServletResponse resp)
			throws ServletException, IOException {

		if("MESSAGE".equals(resp.getMethod())) {
			resp.getSession().invalidate();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doBye(SipServletRequest request) throws ServletException,
			IOException {

		logger.info("Got BYE request: " + request);
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();				
	}	
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSessionListener#sessionExpired(javax.servlet.sip.SipApplicationSessionEvent)
	 */
	public void sessionExpired(SipApplicationSessionEvent ev) {
		logger.info("sip application session expired " +  ev.getApplicationSession());
		if(!ev.getApplicationSession().isReadyToInvalidate() && ev.getApplicationSession().getAttribute(ALREADY_EXTENDED) == null) {
			ev.getApplicationSession().setExpires(1);
			ev.getApplicationSession().setAttribute(ALREADY_EXTENDED, Boolean.TRUE);
		}
		
		SipFactory storedFactory = (SipFactory)ev.getApplicationSession().getAttribute("sipFactory");		
		try {
			SipServletRequest sipServletRequest = storedFactory.createRequest(
					ev.getApplicationSession(), 
					"MESSAGE", 
					"sip:sender@sip-servlets.com", 
					"sip:receiver@sip-servlets.com");
			SipURI sipUri=storedFactory.createSipURI("receiver", "127.0.0.1:5080");
			sipServletRequest.setRequestURI(sipUri);
			sipServletRequest.setContentLength(SIP_APP_SESSION_EXPIRED.length());
			sipServletRequest.setContent(SIP_APP_SESSION_EXPIRED, CONTENT_TYPE);
			sipServletRequest.send();
		} catch (ServletParseException e) {
			logger.error("Exception occured while parsing the addresses",e);
		} catch (IOException e) {
			logger.error("Exception occured while sending the request",e);			
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSessionListener#sessionCreated(javax.servlet.sip.SipApplicationSessionEvent)
	 */
	public void sessionCreated(SipApplicationSessionEvent ev) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSessionListener#sessionDestroyed(javax.servlet.sip.SipApplicationSessionEvent)
	 */
	public void sessionDestroyed(SipApplicationSessionEvent ev) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.TimerListener#timeout(javax.servlet.sip.ServletTimer)
	 */
	public void timeout(ServletTimer timer) {
		SipApplicationSession sipApplicationSession = timer.getApplicationSession();
		logger.info("timer expired " +  timer.getId());		
		SipFactory storedFactory = (SipFactory)sipApplicationSession.getAttribute("sipFactory");		
		try {
			SipServletRequest sipServletRequest = storedFactory.createRequest(
					sipApplicationSession, 
					"MESSAGE", 
					"sip:sender@sip-servlets.com", 
					"sip:receiver@sip-servlets.com");
			SipURI sipUri=storedFactory.createSipURI("receiver", "127.0.0.1:5080");
			sipServletRequest.setRequestURI(sipUri);
			sipServletRequest.setContentLength(TIMER_EXPIRED.length());
			sipServletRequest.setContent(TIMER_EXPIRED, CONTENT_TYPE);
			sipServletRequest.send();
		} catch (ServletParseException e) {
			logger.error("Exception occured while parsing the addresses",e);
		} catch (IOException e) {
			logger.error("Exception occured while sending the request",e);			
		}
	}

	public void sessionReadyToInvalidate(SipApplicationSessionEvent ev) {
		logger.info("sessionReadyToInvalidate called");
		if(ev.getApplicationSession().getAttribute(ALREADY_INVALIDATED) == null) {
			ev.getApplicationSession().setAttribute(ALREADY_INVALIDATED, Boolean.TRUE);
			SipApplicationSession sipApplicationSession = ev.getApplicationSession();		
			SipFactory storedFactory = (SipFactory)sipApplicationSession.getAttribute("sipFactory");		
			try {
				SipServletRequest sipServletRequest = storedFactory.createRequest(
						sipApplicationSession, 
						"MESSAGE", 
						"sip:sender@sip-servlets.com", 
						"sip:receiver@sip-servlets.com");
				SipURI sipUri=storedFactory.createSipURI("receiver", "127.0.0.1:5080");
				sipServletRequest.setRequestURI(sipUri);
				sipServletRequest.setContentLength(SIP_APP_SESSION_READY_TO_BE_INVALIDATED.length());
				sipServletRequest.setContent(SIP_APP_SESSION_READY_TO_BE_INVALIDATED, CONTENT_TYPE);
				sipServletRequest.send();
			} catch (ServletParseException e) {
				logger.error("Exception occured while parsing the addresses",e);
			} catch (IOException e) {
				logger.error("Exception occured while sending the request",e);			
			}
		}
	}

}
