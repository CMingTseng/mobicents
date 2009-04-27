/**
 * Start time:18:44:56 2009-04-03<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:18:44:56 2009-04-03<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class TransmissionMediumUsed extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x35;

	/**
	 * 0 0 0 0 0 0 0 0 speech
	 */
	public static int _MEDIUM_SPEECH = 0;

	/**
	 * 0 0 0 0 0 0 1 0 - 64 kbit/s unrestricted
	 */
	public static int _MEDIUM_64_KBIT_UNRESTRICTED = 2;
	/**
	 * 0 0 0 0 0 0 1 1 - 3.1 kHz audio
	 */
	public static int _MEDIUM_3_1_KHz_AUDIO = 3;
	/**
	 * 0 0 0 0 0 1 0 0 - reserved for alternate speech (service 2)/64 kbit/s
	 * unrestricted (service 1)
	 */
	public static int _MEDIUM_RESERVED_ASS2 = 4;
	/**
	 * 0 0 0 0 0 1 0 1 - reserved for alternate 64 kbit/s unrestricted (service
	 * 1)/speech (service 2)
	 */
	public static int _MEDIUM_RESERVED_ASS1 = 5;
	/**
	 * 0 0 0 0 0 1 1 0 - 64 kbit/s preferred
	 */
	public static int _MEDIUM_64_KBIT_PREFERED = 6;
	/**
	 * 0 0 0 0 0 1 1 1 - 2 � 64 kbit/s unrestricted
	 */
	public static int _MEDIUM_2x64_KBIT_UNRESTRICTED = 7;
	/**
	 * 0 0 0 0 1 0 0 0 - 384 kbit/s unrestricted
	 */
	public static int _MEDIUM_384_KBIT_UNRESTRICTED = 8;
	/**
	 * 0 0 0 0 1 0 0 1 - 1536 kbit/s unrestricted
	 */
	public static int _MEDIUM_1536_KBIT_UNRESTRICTED = 9;
	/**
	 * 0 0 0 0 1 0 1 0 - 1920 kbit/s unrestricted
	 */
	public static int _MEDIUM_1920_KBIT_UNRESTRICTED = 10;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */

	public TransmissionMediumUsed(int transimissionMediumUsed) {
		super();
		this.transimissionMediumUsed = transimissionMediumUsed;
	}

	public TransmissionMediumUsed(byte[] b) {
		super();
		decodeElement(b);
	}

	// Defualt indicate speech
	private int transimissionMediumUsed;

	// FIXME: again wrapper class but hell there is a lot of statics....
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length != 1) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be 1");
		}

		this.transimissionMediumUsed = b[0];

		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		return new byte[] { (byte) this.transimissionMediumUsed };
	}

	public int getTransimissionMediumUsed() {
		return transimissionMediumUsed;
	}

	public void setTransimissionMediumUsed(int transimissionMediumUsed) {
		this.transimissionMediumUsed = transimissionMediumUsed;
	}
	public int getCode() {

		return _PARAMETER_CODE;
	}
}
