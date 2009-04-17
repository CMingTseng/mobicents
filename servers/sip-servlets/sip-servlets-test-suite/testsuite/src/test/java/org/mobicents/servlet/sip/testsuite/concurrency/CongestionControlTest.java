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
package org.mobicents.servlet.sip.testsuite.concurrency;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.core.CongestionControlPolicy;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class CongestionControlTest extends SipServletTestCase {
	
	private static transient Logger logger = Logger.getLogger(CongestionControlTest.class);

	private static final String TRANSPORT = "udp";
	private static final boolean AUTODIALOG = true;
	private static final int TIMEOUT = 5000;	
//	private static final int TIMEOUT = 100000000;
	
	TestSipListener sender;
	
	ProtocolObjects senderProtocolObjects;	

	
	public CongestionControlTest(String name) {
		super(name);
	}

	@Override
	public void deployApplication() {
		assertTrue(tomcat.deployContext(
				projectHome + "/sip-servlets-test-suite/applications/simple-sip-servlet/src/main/sipapp",
				"sip-test-context", "sip-test"));
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/" +
				"org/mobicents/servlet/sip/testsuite/simple/simple-sip-servlet-dar.properties";
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();						
		
		senderProtocolObjects =new ProtocolObjects(
				"sender", "gov.nist", TRANSPORT, AUTODIALOG);
					
		sender = new TestSipListener(5080, 5070, senderProtocolObjects, true);
		SipProvider senderProvider = sender.createProvider();			
		
		senderProvider.addSipListener(sender);
		
		senderProtocolObjects.start();	

	}
	
	public void testCongestedQueueErrorResponse() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlCheckingInterval(2000);
		tomcat.getSipService().getSipApplicationDispatcher().setQueueSize(5);
		String fromName = "sender";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver";
		String toSipAddress = "sip-servlets.com";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.setSendBye(false);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
		Thread.sleep(3000);
		// For this test the queue size is 3, so we feed 40 messages asap and watch for error response.
		// Since we dont want to wait 40*5 secs, we kill everything with no clean up, that's fine for this test.
		for(int q=0; q<40; q++) {
			sender.sendInDialogSipRequest("INFO", new Integer(q).toString(), "text", "plain", null);
			Thread.sleep(500);
		}
		sender.sendBye();
		Thread.sleep(10000);
		assertTrue(sender.isServerErrorReceived());
		assertTrue(sender.isAckSent());		
	}
	
	public void testCongestedQueueDropMessage() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlCheckingInterval(2000);
		tomcat.getSipService().getSipApplicationDispatcher().setQueueSize(5);
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlPolicy(CongestionControlPolicy.DropMessage);
		String fromName = "sender";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver";
		String toSipAddress = "sip-servlets.com";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.setSendBye(false);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
		Thread.sleep(3000);
		// For this test the queue size is 3, so we feed 40 messages asap and watch for error response.
		// Since we dont want to wait 40*5 secs, we kill everything with no clean up, that's fine for this test.
		for(int q=0; q<40; q++) {
			sender.sendInDialogSipRequest("INFO", new Integer(q).toString(), "text", "plain", null);
			Thread.sleep(500);
		}
		sender.sendBye();
		Thread.sleep(10000);
		assertFalse(sender.isServerErrorReceived());
		assertTrue(sender.isAckSent());		
	}
	
	public void testMemoryCongestedErrorResponse() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlCheckingInterval(2000);
		tomcat.getSipService().getSipApplicationDispatcher().setMemoryThreshold(2);
		String fromName = "sender";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver";
		String toSipAddress = "sip-servlets.com";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.setSendBye(false);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
		Thread.sleep(3000);
		// For this test the queue size is 3, so we feed 40 messages asap and watch for error response.
		// Since we dont want to wait 40*5 secs, we kill everything with no clean up, that's fine for this test.
		for(int q=0; q<40; q++) {
			sender.sendInDialogSipRequest("INFO", new Integer(q).toString(), "text", "plain", null);
			Thread.sleep(500);
		}
		sender.sendBye();
		Thread.sleep(10000);
		assertTrue(sender.isServerErrorReceived());
		assertTrue(sender.isAckSent());		
	}
	
	public void testMemoryCongestedDropMessage() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlCheckingInterval(2000);
		tomcat.getSipService().getSipApplicationDispatcher().setMemoryThreshold(2);
		tomcat.getSipService().getSipApplicationDispatcher().setCongestionControlPolicy(CongestionControlPolicy.DropMessage);
		String fromName = "sender";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver";
		String toSipAddress = "sip-servlets.com";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.setSendBye(false);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
		Thread.sleep(3000);
		// For this test the queue size is 3, so we feed 40 messages asap and watch for error response.
		// Since we dont want to wait 40*5 secs, we kill everything with no clean up, that's fine for this test.
		for(int q=0; q<40; q++) {			
			sender.sendInDialogSipRequest("INFO", new Integer(q).toString(), "text", "plain", null);
			Thread.sleep(500);
		}
		sender.sendBye();
		Thread.sleep(10000);
		assertFalse(sender.isServerErrorReceived());		
		assertTrue(sender.isAckSent());		
	}
	
	@Override
	protected void tearDown() throws Exception {					
		senderProtocolObjects.destroy();			
		logger.info("Test completed");
		super.tearDown();
	}


}
