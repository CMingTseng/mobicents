/**
 * Start time:08:29:07 2009-04-02<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:08:29:07 2009-04-02<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class MCIDResponseIndicators extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x3C;
	private static final int _TURN_ON = 1;
	private static final int _TURN_OFF = 0;
	// public static boolean HOLDING_NOT_RROVIDED = false;
	//
	// public static boolean HOLDING_PROVIDED = true;
	//
	// public static boolean MCID_INCLUDED = true;
	//
	// public static boolean MCID_NOT_INCLUDED = false;

	/**
	 * Flag that indicates that information is requested
	 */
	private static final boolean _INDICATOR_PROVIDED = true;

	/**
	 * Flag that indicates that information is not requested
	 */
	private static final boolean _INDICATOR_NOT_PROVIDED = false;

	private boolean mcidIncludedIndicator = false;
	private boolean holdingProvidedIndicator = false;

	public MCIDResponseIndicators(byte[] b) {
		super();
		decodeElement(b);
	}

	public MCIDResponseIndicators(boolean mcidIncludedIndicator, boolean holdingProvidedIndicator) {
		super();
		this.mcidIncludedIndicator = mcidIncludedIndicator;
		this.holdingProvidedIndicator = holdingProvidedIndicator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length != 1) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be 1");
		}

		this.mcidIncludedIndicator = (b[0] & 0x01) == _TURN_ON;
		this.holdingProvidedIndicator = ((b[0] >> 1) & 0x01) == _TURN_ON;
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		int b0 = 0;

		b0 |= (this.mcidIncludedIndicator ? _TURN_ON : _TURN_OFF);
		b0 |= ((this.holdingProvidedIndicator ? _TURN_ON : _TURN_OFF)) << 1;

		return new byte[] { (byte) b0 };
	}

	public boolean isMcidIncludedIndicator() {
		return mcidIncludedIndicator;
	}

	public void setMcidIncludedIndicator(boolean mcidIncludedIndicator) {
		this.mcidIncludedIndicator = mcidIncludedIndicator;
	}

	public boolean isHoldingProvidedIndicator() {
		return holdingProvidedIndicator;
	}

	public void setHoldingProvidedIndicator(boolean holdingProvidedIndicator) {
		this.holdingProvidedIndicator = holdingProvidedIndicator;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
