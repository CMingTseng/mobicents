/**
 * Start time:13:15:04 2009-04-04<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:13:15:04 2009-04-04<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class BackwardGVNS extends AbstractParameter {
	public static final int _PARAMETER_CODE = 0x4D;
	private byte[] backwardGVNS = null;

	public BackwardGVNS(byte[] backwardGVNS) {
		super();
		decodeElement(backwardGVNS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length == 0) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be greater than 0");
		}
		this.backwardGVNS = b;
		return b.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		for (int index = 0; index < this.backwardGVNS.length; index++) {
			this.backwardGVNS[index] = (byte) (this.backwardGVNS[index] & 0x7F);
		}

		this.backwardGVNS[this.backwardGVNS.length - 1] = (byte) (this.backwardGVNS[this.backwardGVNS.length - 1] & (1 << 7));
		return this.backwardGVNS;
	}

	public byte[] getBackwardGVNS() {
		return backwardGVNS;
	}

	public void setBackwardGVNS(byte[] backwardGVNS) {
		if (backwardGVNS == null || backwardGVNS.length == 0) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be greater than 0");
		}
		this.backwardGVNS = backwardGVNS;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
