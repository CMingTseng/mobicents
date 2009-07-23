/**
 * Start time:17:14:12 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.isup.messages.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mobicents.isup.ISUPComponent;
import org.mobicents.isup.ParameterRangeInvalidException;
import org.mobicents.isup.parameters.MCIDResponseIndicatorsImpl;
import org.mobicents.isup.parameters.MLPPPrecedenceImpl;

/**
 * Start time:17:14:12 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
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
		MLPPPrecedenceImpl eci = new MLPPPrecedenceImpl(getBody(MLPPPrecedenceImpl._LFB_INDICATOR_ALLOWED,MLPPPrecedenceImpl._PLI_PRIORITY,new byte[]{3,4}, serDomain));
		
		String[] methodNames = { "getLfb", "getPrecedenceLevel","getMllpServiceDomain" };
		Object[] expectedValues = { (byte)MLPPPrecedenceImpl._LFB_INDICATOR_ALLOWED,(byte)MLPPPrecedenceImpl._PLI_PRIORITY, serDomain };
	
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
		MLPPPrecedenceImpl component = new MLPPPrecedenceImpl(new byte[6]);
		return component;
	}

}
