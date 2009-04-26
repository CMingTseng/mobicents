/**
 * Start time:14:11:03 2009-04-23<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.messages.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.mobicents.isup.ISUPComponent;
import org.mobicents.isup.parameters.RedirectionNumber;

/**
 * Start time:14:11:03 2009-04-23<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class RedirectionNumberTest extends ParameterHarness {

	/**
	 * @throws IOException
	 */
	public RedirectionNumberTest() throws IOException {
		super.badBodies.add(new byte[1]);

	}


	public void testBody1EncodedValues() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		RedirectionNumber bci = new RedirectionNumber(getBody(false, RedirectionNumber._NAI_NETWORK_SPECIFIC, RedirectionNumber._INN_ROUTING_ALLOWED, RedirectionNumber._NPI_TELEX, getSixDigits()));

		String[] methodNames = { "isOddFlag", 
								 "getNatureOfAddressIndicator", 
								 "getInternalNetworkNumberIndicator",
								 "getNumberingPlanIndicator" ,
								 "getAddress" };
		Object[] expectedValues = {false, 
								   RedirectionNumber._NAI_NETWORK_SPECIFIC, 
								   RedirectionNumber._INN_ROUTING_ALLOWED, 
								   RedirectionNumber._NPI_TELEX, 
								   getSixDigitsString()};
		super.testValues(bci, methodNames, expectedValues);
	}

	private byte[] getBody(boolean isODD, int naiNetworkSpecific, int innRoutingAllowed, int npiTelex, byte[] dgits) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// we will use odd number of digits, so we leave zero as MSB

		if (isODD) {
			bos.write(0x80 | naiNetworkSpecific);
		} else {
			bos.write(naiNetworkSpecific);
		}

		bos.write((innRoutingAllowed << 7) | (npiTelex << 4));

		bos.write(dgits);
		return bos.toByteArray();
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
		return new RedirectionNumber(0, "1", 1, 1);
	}

}
