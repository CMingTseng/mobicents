/**
 * Start time:11:34:01 2009-04-24<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.messages.parameters;

import org.mobicents.isup.ISUPComponent;
import org.mobicents.isup.parameters.EchoControlInformation;

/**
 * Start time:11:34:01 2009-04-24<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class EchoControlInformationTest extends ParameterHarness {

	public EchoControlInformationTest() {
		super();
		super.goodBodies.add(new byte[] { 67 });
		super.badBodies.add(new byte[2]);
	}

	private byte[] getBody(int _OUT_E_CDII, int _IN_E_CDII, int _IN_E_CDRI, int _OUT_E_CDRI) {
		byte[] b = new byte[1];
		int v = _OUT_E_CDII;
		v |= _IN_E_CDII << 2;
		v |= _OUT_E_CDRI << 4;
		v |= _IN_E_CDRI << 6;
		b[0] = (byte) v;

		return b;
	}

	public void testBody1EncodedValues() {
		EchoControlInformation eci = new EchoControlInformation(getBody(EchoControlInformation._OUTGOING_ECHO_CDII_NINA, EchoControlInformation._INCOMING_ECHO_CDII_INCLUDED,
				EchoControlInformation._INCOMING_ECHO_CDRI_AR, EchoControlInformation._OUTGOING_ECHO_CDRI_NOINFO));

		String[] methodNames = { "getOutgoingEchoControlDeviceInformationIndicator", "getIncomingEchoControlDeviceInformationIndicator", "getIncomingEchoControlDeviceInformationRequestIndicator",
				"getOutgoingEchoControlDeviceInformationRequestIndicator" };
		Object[] expectedValues = { EchoControlInformation._OUTGOING_ECHO_CDII_NINA, EchoControlInformation._INCOMING_ECHO_CDII_INCLUDED, EchoControlInformation._INCOMING_ECHO_CDRI_AR,
				EchoControlInformation._OUTGOING_ECHO_CDRI_NOINFO };
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
	public ISUPComponent getTestedComponent() {
		return new EchoControlInformation(new byte[1]);
	}

}
