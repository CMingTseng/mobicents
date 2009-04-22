/**
 * Start time:12:00:59 2009-04-02<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:12:00:59 2009-04-02<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class OptionalForwardCallIndicator extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x08;
	private static final int _TURN_ON = 1;
	private static final int _TURN_OFF = 0;

	/**
	 * See Q.763 3.38 Simple segmentation indicator : no additional information
	 * will be sent
	 */
	private final static boolean _SSI_NO_ADDITIONAL_INFO = false;

	/**
	 * See Q.763 3.38 Simple segmentation indicator : additional information
	 * will be sent in a segmentation message
	 */
	private final static boolean _SSI_ADDITIONAL_INFO = true;

	/**
	 * See Q.763 3.38 Connected line identity request indicator :
	 */
	private final static boolean _CLIRI_NOT_REQUESTED = false;

	/**
	 * See Q.763 3.38 Connected line identity request indicator :
	 */
	private final static boolean _CLIRI_REQUESTED = true;
	/**
	 * See Q.763 3.38 Closed user group call indicator : non-CUG call
	 */
	private final static int _CIGCI_NON_CUG_CALL = 0;

	/**
	 * See Q.763 3.38 Closed user group call indicator : closed user group call,
	 * outgoing access allowed
	 */
	private final static int _CIGCI_CUG_CALL_OAL = 2;

	/**
	 * See Q.763 3.38 Closed user group call indicator : closed user group call,
	 * outgoing access not allowed
	 */
	private final static int _CIGCI_CUG_CALL_OANL = 3;

	private byte closedUserGroupCallIndicator = 0;
	private boolean simpleSegmentationIndicator = false;
	private boolean connectedLineIdentityRequestIndicator = false;

	public OptionalForwardCallIndicator(byte[] b) {
		super();
		decodeElement(b);
	}

	public OptionalForwardCallIndicator(byte closedUserGroupCallIndicator, boolean simpleSegmentationIndicator, boolean connectedLineIdentityRequestIndicator) {
		super();
		this.closedUserGroupCallIndicator = closedUserGroupCallIndicator;
		this.simpleSegmentationIndicator = simpleSegmentationIndicator;
		this.connectedLineIdentityRequestIndicator = connectedLineIdentityRequestIndicator;
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
		this.closedUserGroupCallIndicator = (byte) (b[0] & 0x03);
		this.simpleSegmentationIndicator = ((b[0] >> 2) & 0x01) == _TURN_ON;
		this.simpleSegmentationIndicator = ((b[0] >> 7) & 0x01) == _TURN_ON;
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		int b0 = 0;

		b0 = this.closedUserGroupCallIndicator & 0x03;
		b0 |= (this.connectedLineIdentityRequestIndicator ? _TURN_ON : _TURN_OFF) << 2;
		b0 |= (this.connectedLineIdentityRequestIndicator ? _TURN_ON : _TURN_OFF) << 7;

		return new byte[] { (byte) b0 };
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
