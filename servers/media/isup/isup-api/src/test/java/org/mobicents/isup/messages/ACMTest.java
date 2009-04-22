/**
 * Start time:09:26:46 2009-04-22<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.messages;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

/**
 * Start time:09:26:46 2009-04-22<br>
 * Project: mobicents-jain-isup-stack<br>
 * Test for ACM 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class ACMTest extends MessageHarness {

	
	
	public void testOne() throws IOException
	{
		//FIXME: for now we strip MTP part
		byte[] message={
//				(byte) 0x9B 
//				,(byte) 0xE6
//				,0x13
//				,(byte) 0xC5
//				,0x00
//				,(byte) 0xB7 
//				,(byte) 0xD1 
//				,(byte) 0x8D 
//				,0x08 
//				,0x00, 
				 0x06 
				,0x01 
				,0x20 
				,0x01 
				,0x29 
				,0x01 
				,0x01 
				,0x12 
				,0x02 
				,(byte) 0x82 
				,(byte) 0x9C 
				,0x00

		};

		AddressCompleteMessage acm=new AddressCompleteMessage(this,message);
		byte[] encodedBody = acm.encodeElement();
		boolean equal = Arrays.equals(message, encodedBody);
		assertTrue(super.makeCompare(message, encodedBody),equal);
	}
	
}
