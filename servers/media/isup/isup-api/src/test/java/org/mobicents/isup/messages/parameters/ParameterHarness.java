/**
 * Start time:09:16:42 2009-04-22<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.messages.parameters;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mobicents.isup.ISUPComponent;
import org.mobicents.isup.parameters.CalledDirectoryNumber;

import junit.framework.TestCase;

/**
 * Start time:09:16:42 2009-04-22<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public abstract class ParameterHarness extends TestCase {

	// 21 10000011 Address....................... 83
	// 22 01100000 Address....................... 60
	// 23 00111000 Address....................... 38
	// NOTE: now see how nice digits swap can come out with conversion, lol
	private final static byte[] sixDigits = new byte[] { (byte) 0x83, 0x60, 0x38 };
	private final static byte[] fiveDigits = new byte[] { (byte) 0x83, 0x60, 0x08 };
	private final static byte[] threeDigits = new byte[] { (byte) 0x83, 0x0 };;
	private final static String sixDigitsString = "380683";
	private final static String fiveDigitsString = "38068";
	private final static String threeDigitsString = "380";

	// FIXME: add code to check values :)

	protected List<byte[]> goodBodies = new ArrayList<byte[]>();

	protected List<byte[]> badBodies = new ArrayList<byte[]>();

	protected String makeCompare(byte[] hardcodedBody, byte[] elementEncoded) {
		int totalLength = 0;
		if (hardcodedBody == null || elementEncoded == null) {
			return "One arg is null";
		}
		if (hardcodedBody.length >= elementEncoded.length) {
			totalLength = hardcodedBody.length;
		} else {
			totalLength = elementEncoded.length;
		}

		String out = "";

		for (int index = 0; index < totalLength; index++) {
			if (hardcodedBody.length > index) {
				out += "hardcodedBody[" + Integer.toHexString(hardcodedBody[index]) + "]";
			} else {
				out += "hardcodedBody[NOP]";
			}

			if (elementEncoded.length > index) {
				out += "elementEncoded[" + Integer.toHexString(elementEncoded[index]) + "]";
			} else {
				out += "elementEncoded[NOP]";
			}
			out += "\n";
		}

		return out;
	}

	public void testDecodeEncode() throws IOException {

		for (int index = 0; index < this.goodBodies.size(); index++) {
			byte[] goodBody = this.goodBodies.get(index);
			ISUPComponent component = this.getTestedComponent();
			doTestDecode(goodBody, true, component, index);
			byte[] encodedBody = component.encodeElement();
			boolean equal = Arrays.equals(goodBody, encodedBody);
			assertTrue("Body index: " + index + "\n" + makeCompare(goodBody, encodedBody), equal);

		}
		for (int index = 0; index < this.badBodies.size(); index++) {

			byte[] badBody = this.badBodies.get(index);
			ISUPComponent component = this.getTestedComponent();
			doTestDecode(badBody, false, component, index);
			byte[] encodedBody = component.encodeElement();

		}

	}

	public abstract ISUPComponent getTestedComponent();

	protected void doTestDecode(byte[] presumableBody, boolean shouldPass, ISUPComponent component, int index) {
		try {
			component.decodeElement(presumableBody);
			if (!shouldPass) {
				fail("Decoded[" + index + "] parameter[" + component.getClass() + "], should not happen. Passed data: " + dumpData(presumableBody));
			}

		} catch (IllegalArgumentException iae) {
			if (shouldPass) {
				fail("Failed to decode[" + index + "] parameter[" + component.getClass() + "], should not happen. " + iae + ".Passed data: " + dumpData(presumableBody));
				iae.printStackTrace();
			}
		} catch (Exception e) {
			fail("Failed to decode[" + index + "] parameter[" + component.getClass() + "]." + e + ". Passed data: " + dumpData(presumableBody));
			e.printStackTrace();
		}
	}

	protected String dumpData(byte[] b) {
		String s = "\n";
		for (byte bb : b) {
			s += Integer.toHexString(bb);
		}

		return s;
	}

	public void testValues(ISUPComponent component, String[] getterMethodNames, Object[] expectedValues) {
		try {
			Class cClass = component.getClass();
			for (int index = 0; index < getterMethodNames.length; index++) {
				Method m = cClass.getMethod(getterMethodNames[index], null);
				// Should not be null by now
				Object v = m.invoke(component, null);
				if (v == null && expectedValues != null) {
					fail("Failed to validate values in component: " + component.getClass().getName() + ". Value of: " + getterMethodNames[index] + " is null, but test values is not.");
				}
				if(component instanceof CalledDirectoryNumber)
				{
					((CalledDirectoryNumber)component).getNumberingPlanIndicator();
				}
				assertEquals("Failed to validate values in component: " + component.getClass().getName() + ". Value of: " + getterMethodNames[index], expectedValues[index], v);
				
			}

		} catch (Exception e) {
			fail("Failed to check values on component: " + component.getClass().getName() + ", due to: " + e);
			e.printStackTrace();
		}
	}

	public static byte[] getSixDigits() {
		return sixDigits;
	}

	public static byte[] getFiveDigits() {
		return fiveDigits;
	}

	public static byte[] getThreeDigits() {
		return threeDigits;
	}

	public static String getSixDigitsString() {
		return sixDigitsString;
	}

	public static String getFiveDigitsString() {
		return fiveDigitsString;
	}

	public static String getThreeDigitsString() {
		return threeDigitsString;
	}

}
