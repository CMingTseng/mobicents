package org.mobicents.servlet.sip.testsuite.proxy;

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

import java.util.EventObject;
import java.util.Hashtable;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;

/*
 * This test sends two PUBLISH requests as if they belong to the same dialog
 * See http://code.google.com/p/mobicents/issues/detail?id=568
 */
public class ProxySubsequentPublishRequestTest extends SipServletTestCase implements SipListener {

	private static transient Logger logger = Logger.getLogger(ProxySubsequentPublishRequestTest.class);

	protected Shootist shootist;

	protected Shootme shootme;
	
	protected Cutme cutme;

	protected Hashtable providerTable = new Hashtable();

	private static final int timeout = 5000;

	private static final int receiversCount = 1;

	public ProxySubsequentPublishRequestTest(String name) {
		super(name);

		this.sipIpAddress="0.0.0.0";
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.shootist = new Shootist(false);
		this.shootme = new Shootme(5057);
		this.cutme = new Cutme();
		this.shootist.requestMethod = Request.PUBLISH;
	}

	public void testProxyPublish() {
		this.shootme.init("stackName");
		this.cutme.init();
		this.shootist.init();
		for (int q = 0; q < 20; q++) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(shootist.lastResponse.getStatusCode() == 200) {
				String lastcseq = shootist.lastResponse.getHeader(CSeqHeader.NAME).toString();
				if(lastcseq.contains("2 PUBLISH")) {
					return;
				}
			}
		}
		fail("We expect two responses to the PUBLISH request. We didn't receive them.");
	}
	


	@Override
	public void tearDown() throws Exception {
		shootist.destroy();
		shootme.destroy();
		cutme.destroy();
		super.tearDown();
	}

	@Override
	public void deployApplication() {
		assertTrue(tomcat
				.deployContext(
						projectHome
								+ "/sip-servlets-test-suite/applications/proxy-sip-servlet/src/main/sipapp",
						"sip-test-context", "sip-test"));
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///"
				+ projectHome
				+ "/sip-servlets-test-suite/testsuite/src/test/resources/"
				+ "org/mobicents/servlet/sip/testsuite/proxy/simple-sip-servlet-dar.properties";
	}

	public void init() {
		// setupPhones();
	}

	private SipListener getSipListener(EventObject sipEvent) {
		SipProvider source = (SipProvider) sipEvent.getSource();
		SipListener listener = (SipListener) providerTable.get(source);
		if (listener == null)
			throw new RuntimeException("Unexpected null listener");
		return listener;
	}

	public void processRequest(RequestEvent requestEvent) {
		getSipListener(requestEvent).processRequest(requestEvent);

	}

	public void processResponse(ResponseEvent responseEvent) {
		getSipListener(responseEvent).processResponse(responseEvent);

	}

	public void processTimeout(TimeoutEvent timeoutEvent) {
		getSipListener(timeoutEvent).processTimeout(timeoutEvent);
	}

	public void processIOException(IOExceptionEvent exceptionEvent) {
		fail("unexpected exception");

	}

	public void processTransactionTerminated(
			TransactionTerminatedEvent transactionTerminatedEvent) {
		getSipListener(transactionTerminatedEvent)
				.processTransactionTerminated(transactionTerminatedEvent);

	}

	public void processDialogTerminated(
			DialogTerminatedEvent dialogTerminatedEvent) {
		getSipListener(dialogTerminatedEvent).processDialogTerminated(
				dialogTerminatedEvent);

	}
}
