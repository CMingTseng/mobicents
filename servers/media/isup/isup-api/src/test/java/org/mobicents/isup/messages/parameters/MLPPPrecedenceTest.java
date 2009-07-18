/**
 * Start time:17:14:12 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.messages.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mobicents.isup.ISUPComponent;
import org.mobicents.isup.ParameterRangeInvalidException;
import org.mobicents.isup.parameters.MCIDResponseIndicators;
import org.mobicents.isup.parameters.MLPPPrecedence;

/**
 * Start time:17:14:12 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class MLPPPrecedenceTest extends ParameterHarness {


	
	
	public MLPPPrecedenceTest() {
		super();
		super.goodBodies.add(new byte[6]);
		
		super.badBodies.add(new byte[5]);
		super.badBodies.add(new byte[7]);
		
	}

	
	
	public void testBody1EncodedValues() throws IOException, ParameterRangeInvalidException {
		//FIXME: This one fails....
		int serDomain = 15;
		MLPPPrecedence eci = new MLPPPrecedence(getBody(MLPPPrecedence._LFB_INDICATOR_ALLOWED,MLPPPrecedence._PLI_PRIORITY,new byte[]{3,4}, serDomain));
		
		String[] methodNames = { "getLfb", "getPrecedenceLevel","getMllpServiceDomain" };
		Object[] expectedValues = { (byte)MLPPPrecedence._LFB_INDICATOR_ALLOWED,(byte)MLPPPrecedence._PLI_PRIORITY, serDomain };
	
		super.testValues(eci, methodNames, expectedValues);
	}
	
	
	
	
	private byte[] getBody(int lfbIndicatorAllowed, int precedenceLevel, byte[] bs, int mllpServiceDomain) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte b = (byte) ((lfbIndicatorAllowed & 0x03) << 5);
		b |= precedenceLevel & 0x0F;
		bos.write(b);
		bos.write(bs);
		
		
		bos.write(mllpServiceDomain >> 16);
		bos.write(mllpServiceDomain >> 8);
		bos.write( mllpServiceDomain);
		byte[] bb =bos.toByteArray();
		return bb;
		
	}



	/* (non-Javadoc)
	 * @see org.mobicents.isup.messages.parameters.ParameterHarness#getTestedComponent()
	 */
	@Override
	public ISUPComponent getTestedComponent() throws ParameterRangeInvalidException {
		MLPPPrecedence component = new MLPPPrecedence(new byte[6]);
		return component;
	}

}
