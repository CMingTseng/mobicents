package org.mobicents.mgcp.stack.test.transactionretransmisson;

import org.apache.log4j.Logger;
import org.mobicents.mgcp.stack.test.MessageFlowHarness;

public class TxRetransmissionTest extends MessageFlowHarness {

	private static Logger logger = Logger.getLogger("mgcp.test");

	private CA ca;
	private MGW mgw;

	public TxRetransmissionTest() {
		super("TxRetransmissionTest");
	}

	public void setUp() {
		try {
			super.setUp();

			ca = new CA(caProvider, mgProvider);
			mgw = new MGW(mgProvider);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected Exception");
		}
	}

	public void testCreateConnection() {
		this.ca.sendCreateConnection();
		waitForTimeout();
	}

	public void tearDown() {
		try {
			super.tearDown();
		} catch (Exception ex) {

		}

		this.ca.checkState();
		this.mgw.checkState();
		logTestCompleted();

	}

	public static void assertTrue(String diagnostic, boolean cond) {
		if (cond) {
			assertTrue(cond);
		} else {
			fail(diagnostic);
		}

	}
}
