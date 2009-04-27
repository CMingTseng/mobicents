/**
 * Start time:14:36:25 2009-03-31<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:14:36:25 2009-03-31<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class InformationIndicators extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x0F;
	private final static int _TURN_ON = 1;
	private final static int _TURN_OFF = 0;

	/**
	 * See Q.763 3.28 Calling party address response indicator : calling party
	 * address not included
	 */
	public static final int _CPARI_ADDRESS_NOT_INCLUDED = 0;

	/**
	 * See Q.763 3.28 Calling party address response indicator : calling party
	 * address included
	 */
	public static final int _CPARI_ADDRESS_INCLUDED = 3;
	/**
	 * See Q.763 3.28 Calling party address response indicator : calling party
	 * address not available
	 */
	public static final int _CPARI_ADDRESS_NOT_AVAILABLE = 1;

	/**
	 * See Q.763 3.28 Hold provided indicator : hold not provided
	 */
	public static final boolean _HPI_NOT_PROVIDED = false;
	/**
	 * See Q.763 3.28 Hold provided indicator : hold provided
	 */
	public static final boolean _HPI_PROVIDED = true;

	/**
	 * See Q.763 3.28 Charge information response indicator : charge information
	 * not included
	 */
	public static final boolean _CIRI_NOT_INCLUDED = false;
	/**
	 * See Q.763 3.28 Charge information response indicator : charge information
	 * included
	 */
	public static final boolean _CIRI_INCLUDED = true;

	/**
	 * See Q.763 3.28 Solicited information indicator : solicited
	 */
	public static final boolean _SII_SOLICITED = false;
	/**
	 * See Q.763 3.28 Solicited information indicator : unsolicited
	 */
	public static final boolean _SII_UNSOLICITED = true;
	/**
	 * See Q.763 3.28 Calling party's category response indicator : calling
	 * party's category not included
	 */
	public static final boolean _CPCRI_CATEOGRY_NOT_INCLUDED = false;

	/**
	 * See Q.763 3.28 Calling party's category response indicator : calling
	 * party's category not included
	 */
	public static final boolean _CPCRI_CATEOGRY_INCLUDED = true;

	private int callingPartyAddressResponseIndicator;
	private boolean holdProvidedIndicator;
	private boolean callingPartysCategoryResponseIndicator;
	private boolean chargeInformationResponseIndicator;
	private boolean solicitedInformationIndicator;
	// FIXME: should we care about it.
	private int reserved;

	public InformationIndicators(byte[] b) {
		super();
		decodeElement(b);
	}

	public InformationIndicators(int callingPartyAddressResponseIndicator, boolean holdProvidedIndicator, boolean callingPartysCategoryResponseIndicator, boolean chargeInformationResponseIndicator,
			boolean solicitedInformationIndicator, int reserved) {
		super();
		this.callingPartyAddressResponseIndicator = callingPartyAddressResponseIndicator;
		this.holdProvidedIndicator = holdProvidedIndicator;
		this.callingPartysCategoryResponseIndicator = callingPartysCategoryResponseIndicator;
		this.chargeInformationResponseIndicator = chargeInformationResponseIndicator;
		this.solicitedInformationIndicator = solicitedInformationIndicator;
		this.reserved = reserved;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length != 2) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be 2");
		}

		this.reserved = (b[1] >> 4) & 0x0F;
		this.callingPartyAddressResponseIndicator = b[0] & 0x03;
		this.holdProvidedIndicator = ((b[0] >> 2) & 0x01) == _TURN_ON;
		this.callingPartysCategoryResponseIndicator = ((b[0] >> 5) & 0x01) == _TURN_ON;
		this.chargeInformationResponseIndicator = ((b[0] >> 6) & 0x01) == _TURN_ON;
		this.solicitedInformationIndicator = ((b[0] >> 7) & 0x01) == _TURN_ON;
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		int b1 = this.callingPartyAddressResponseIndicator & 0x03;
		b1 |= (this.holdProvidedIndicator ? _TURN_ON : _TURN_OFF) << 2;
		b1 |= (this.callingPartysCategoryResponseIndicator ? _TURN_ON : _TURN_OFF) << 5;
		b1 |= (this.chargeInformationResponseIndicator ? _TURN_ON : _TURN_OFF) << 6;
		b1 |= (this.solicitedInformationIndicator ? _TURN_ON : _TURN_OFF) << 7;

		int b2 = this.reserved & 0x0F;
		byte[] b = new byte[] { (byte) b1, (byte) b2 };
		return b;
	}

	public int getCallingPartyAddressResponseIndicator() {
		return callingPartyAddressResponseIndicator;
	}

	public void setCallingPartyAddressResponseIndicator(int callingPartyAddressResponseIndicator) {
		this.callingPartyAddressResponseIndicator = callingPartyAddressResponseIndicator;
	}

	public boolean isHoldProvidedIndicator() {
		return holdProvidedIndicator;
	}

	public void setHoldProvidedIndicator(boolean holdProvidedIndicator) {
		this.holdProvidedIndicator = holdProvidedIndicator;
	}

	public boolean isCallingPartysCategoryResponseIndicator() {
		return callingPartysCategoryResponseIndicator;
	}

	public void setCallingPartysCategoryResponseIndicator(boolean callingPartysCategoryResponseIndicator) {
		this.callingPartysCategoryResponseIndicator = callingPartysCategoryResponseIndicator;
	}

	public boolean isChargeInformationResponseIndicator() {
		return chargeInformationResponseIndicator;
	}

	public void setChargeInformationResponseIndicator(boolean chargeInformationResponseIndicator) {
		this.chargeInformationResponseIndicator = chargeInformationResponseIndicator;
	}

	public boolean isSolicitedInformationIndicator() {
		return solicitedInformationIndicator;
	}

	public void setSolicitedInformationIndicator(boolean solicitedInformationIndicator) {
		this.solicitedInformationIndicator = solicitedInformationIndicator;
	}

	public int getReserved() {
		return reserved;
	}

	public void setReserved(int reserved) {
		this.reserved = reserved;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
