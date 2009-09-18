/**
 * Start time:09:26:46 2009-04-22<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.impl;

import java.util.Arrays;


import org.mobicents.ss7.isup.message.CallProgressMessage;
import org.mobicents.ss7.isup.message.CircuitGroupBlockingMessage;
import org.mobicents.ss7.isup.message.CircuitGroupQueryMessage;
import org.mobicents.ss7.isup.message.CircuitGroupResetMessage;
import org.mobicents.ss7.isup.message.ISUPMessage;
import org.mobicents.ss7.isup.message.parameter.*;


/**
 * Start time:09:26:46 2009-04-22<br>
 * Project: mobicents-isup-stack<br>
 * Test for CQM
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class CQMTest extends MessageHarness {
	
	
	
	public void testTwo_Params() throws Exception
	{
		byte[] message = getDefaultBody();

		//CircuitGroupQueryMessage grs=new CircuitGroupQueryMessageImpl(this,message);
		CircuitGroupQueryMessage grs=super.messageFactory.createCQM();
		grs.decodeElement(message);

		
		try{
			RangeAndStatus RS = (RangeAndStatus) grs.getParameter(RangeAndStatus._PARAMETER_CODE);
			assertNotNull("Range And Status return is null, it should not be",RS);
			if(RS == null)
				return;
			byte range = RS.getRange();
			assertEquals("Range is wrong,",0x01, range);
			byte[] b=RS.getStatus();
			assertNull("RangeAndStatus.getRange() is not null",b);
		
			
		}catch(Exception e)
		{
			e.printStackTrace();
			super.fail("Failed on get parameter["+CallReference._PARAMETER_CODE+"]:"+e);
		}
	
	}
	@Override
	protected byte[] getDefaultBody() {
		//FIXME: for now we strip MTP part
		byte[] message={

				0x0C
				,(byte) 0x0B
				,CircuitGroupQueryMessage._MESSAGE_CODE_CQM

				,0x01 // ptr to variable part
				//no optional, so no pointer
				//RangeAndStatus._PARAMETER_CODE
				,0x01
				,0x01
				
		};

		return message;
	}
	@Override
	protected ISUPMessage getDefaultMessage() {
		return super.messageFactory.createCQM();
	}
}
