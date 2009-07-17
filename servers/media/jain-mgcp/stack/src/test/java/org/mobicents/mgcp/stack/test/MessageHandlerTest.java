package org.mobicents.mgcp.stack.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.mgcp.stack.MessageHandler;
import org.mobicents.mgcp.stack.parser.UtilsFactory;

public class MessageHandlerTest extends TestCase {
	private MessageHandler handler = null;
	private JainMgcpStackImpl stack = null;
	private UtilsFactory factory = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		factory = new UtilsFactory(1);
		stack = new JainMgcpStackImpl();
		stack.setUtilsFactory(factory);
		handler = new MessageHandler(stack);
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testPiggyDismount() throws Exception {
		String message = "200 2005 OK\n.\nDLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\naakkkxxcd";
		byte[] rawByte = message.getBytes();

		String[] messages = handler.piggyDismount(rawByte, rawByte.length-9);
		assertEquals("200 2005 OK\n", messages[0]);

		boolean flag = messages[1].startsWith("DLCX") && messages[1].endsWith("FDE234C8\n");
		assertTrue(flag);
		
		assertEquals("DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n",messages[1] );
 
	}

	@Test
	public void testNoPiggyDismount() throws Exception {
		String message = "DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n";
		byte[] rawByte = message.getBytes();

		String[] messages = handler.piggyDismount(rawByte, rawByte.length);

		assertEquals(1, messages.length);
		boolean flag = messages[0].startsWith("DLCX") && messages[0].endsWith("FDE234C8\n");
		assertTrue(flag);

	}

}
