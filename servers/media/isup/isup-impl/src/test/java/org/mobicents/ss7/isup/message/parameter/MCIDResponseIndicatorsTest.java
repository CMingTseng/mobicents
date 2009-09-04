/**
 * Start time:11:34:01 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.message.parameter;

import org.mobicents.ss7.isup.ISUPComponent;
import org.mobicents.ss7.isup.ParameterRangeInvalidException;
import org.mobicents.ss7.isup.message.parameter.MCIDResponseIndicatorsImpl;


/**
 * Start time:11:34:01 2009-04-24<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 */
public class MCIDResponseIndicatorsTest extends ParameterHarness {
	private static final int _TURN_ON = 1;
	private static final int _TURN_OFF = 0;

	public MCIDResponseIndicatorsTest() {
		super();
		super.goodBodies.add(new byte[] { 3 });
		super.badBodies.add(new byte[2]);
	}

	private byte[] getBody(boolean mcidRequest, boolean holdingRequested) {
		int b0 = 0;

		b0 |= (mcidRequest ? _TURN_ON : _TURN_OFF);
		b0 |= ((holdingRequested ? _TURN_ON : _TURN_OFF)) << 1;

		return new byte[] { (byte) b0 };
	}

	public void testBody1EncodedValues() throws ParameterRangeInvalidException {
		MCIDResponseIndicatorsImpl eci = new MCIDResponseIndicatorsImpl(getBody(MCIDResponseIndicatorsImpl._INDICATOR_PROVIDED, MCIDResponseIndicatorsImpl._INDICATOR_NOT_PROVIDED));

		String[] methodNames = { "isMcidIncludedIndicator", "isHoldingProvidedIndicator" };
		Object[] expectedValues = { MCIDResponseIndicatorsImpl._INDICATOR_PROVIDED, MCIDResponseIndicatorsImpl._INDICATOR_NOT_PROVIDED };
		super.testValues(eci, methodNames, expectedValues);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.isup.messages.parameters.ParameterHarness#getTestedComponent
	 * ()
	 */
	@Override
	public ISUPComponent getTestedComponent() throws ParameterRangeInvalidException {
		return new MCIDResponseIndicatorsImpl(new byte[1]);
	}

}
