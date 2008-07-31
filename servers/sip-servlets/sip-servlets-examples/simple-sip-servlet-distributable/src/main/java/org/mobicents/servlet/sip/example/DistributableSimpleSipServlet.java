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
package org.mobicents.servlet.sip.example;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.SipSession.State;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This example shows a simple User agent that can any accept call and reply to BYE or initiate BYE 
 * depending on the sender.
 * 
 * @author Jean Deruelle
 *
 */
public class DistributableSimpleSipServlet 
		extends SipServlet 
		implements TimerListener, SipApplicationSessionActivationListener, SipSessionActivationListener {
	
	private static final String RECEIVED = "Received";

	private static Log logger = LogFactory.getLog(DistributableSimpleSipServlet.class);
	
	private static final String CALLEE_SEND_BYE = "YouSendBye";
	//60 sec
	private static final int DEFAULT_BYE_DELAY = 180000;
	
	/** Creates a new instance of SimpleProxyServlet */
	public DistributableSimpleSipServlet() {
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		logger.info("the distributable simple sip servlet has been started");
		super.init(servletConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doInvite(SipServletRequest request) throws ServletException,
			IOException {
		
		if(logger.isInfoEnabled()) {
			logger.info("Distributable Simple Servlet: Got request:\n"
				+ request.getMethod());
		}
		request.getSession().setAttribute("INVITE", RECEIVED);
		request.getApplicationSession().setAttribute("INVITE", RECEIVED);
//		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_RINGING);
//		sipServletResponse.send();
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
		if(CALLEE_SEND_BYE.equalsIgnoreCase(((SipURI)request.getTo().getURI()).getUser())) {
			TimerService timer = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
			String byeDelayStr = getServletContext().getInitParameter("bye.delay");
			int byeDelay = DEFAULT_BYE_DELAY;
			try{
				Integer.parseInt(byeDelayStr);
			} catch (NumberFormatException e) {
				logger.error("Impossible to parse the bye delay : " + byeDelayStr, e);
			}
			timer.createTimer(request.getApplicationSession(), byeDelay, false, request.getSession().getId());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doBye(SipServletRequest request) throws ServletException,
			IOException {
		if(logger.isInfoEnabled()) {
			logger.info("Distributable Simple Servlet: Got BYE request:\n" + request);
		}
		String sipSessionInviteAttribute  = (String) request.getSession().getAttribute("INVITE");
		String sipApplicationSessionInviteAttribute  = (String) request.getApplicationSession().getAttribute("INVITE");
		if(logger.isInfoEnabled()) {			
			logger.info("Distributable Simple Servlet: attributes previously set in sip session INVITE : "+ sipSessionInviteAttribute);
			logger.info("Distributable Simple Servlet: attributes previously set in sip application session INVITE : "+ sipApplicationSessionInviteAttribute);
		}
		if(sipSessionInviteAttribute != null  && sipApplicationSessionInviteAttribute != null 
				&& RECEIVED.equalsIgnoreCase(sipSessionInviteAttribute) 
				&& RECEIVED.equalsIgnoreCase(sipApplicationSessionInviteAttribute)) {
			request.getSession().setAttribute("BYE", RECEIVED);
			request.getApplicationSession().setAttribute(" BYE", RECEIVED);
			SipServletResponse sipServletResponse = request.createResponse(200);
			sipServletResponse.send();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doResponse(SipServletResponse response)
			throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
			logger.info("Distributable Simple Servlet: Got response:\n" + response);
		}
		if(SipServletResponse.SC_OK == response.getStatus() && "BYE".equalsIgnoreCase(response.getMethod())) {
			SipSession sipSession = response.getSession(false);
			if(sipSession != null) {
				SipApplicationSession sipApplicationSession = sipSession.getApplicationSession();
				if(sipSession.isValid()) {
					sipSession.invalidate();
				}
				if(sipApplicationSession.isValid()) {
					sipApplicationSession.invalidate();
				}
			}			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.TimerListener#timeout(javax.servlet.sip.ServletTimer)
	 */
	public void timeout(ServletTimer servletTimer) {
		if(logger.isInfoEnabled()) {
			logger.info("Distributable Simple Servlet: timer expired\n");
		}
		SipSession sipSession = servletTimer.getApplicationSession().getSipSession((String)servletTimer.getInfo());
		if(!State.TERMINATED.equals(sipSession.getState())) {
			try {
				sipSession.createRequest("BYE").send();
			} catch (IOException e) {
				logger.error("An unexpected exception occured while sending the BYE", e);
			}				
		}
	}
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSessionActivationListener#sessionDidActivate(javax.servlet.sip.SipApplicationSessionEvent)
	 */
	public void sessionDidActivate(SipApplicationSessionEvent event) {
		logger.info("Following sip application session just activated " + event.getApplicationSession().getId());
	}
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSessionActivationListener#sessionWillPassivate(javax.servlet.sip.SipApplicationSessionEvent)
	 */
	public void sessionWillPassivate(SipApplicationSessionEvent event) {
		logger.info("Following sip application session just passivated " + event.getApplicationSession().getId());		
	}
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSessionActivationListener#sessionDidActivate(javax.servlet.sip.SipSessionEvent)
	 */
	public void sessionDidActivate(SipSessionEvent event) {
		logger.info("Following sip session just activated " + event.getSession().getId());		
	}
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipSessionActivationListener#sessionWillPassivate(javax.servlet.sip.SipSessionEvent)
	 */
	public void sessionWillPassivate(SipSessionEvent event) {
		logger.info("Following sip session just passivated " + event.getSession().getId());		
	}
}