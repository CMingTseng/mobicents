/**
 * Start time:14:58:14 2009-04-05<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:14:58:14 2009-04-05<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class HopCounter extends AbstractParameter {

	private int hopCounter = 0;

	public HopCounter(byte[] b) {
		super();
		decodeElement(b);
	}

	public HopCounter(int hopCounter) {
		super();
		this.hopCounter = hopCounter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length != 1) {
			throw new IllegalArgumentException("byte[] must not be null and length must be 1");
		}
		this.hopCounter = b[0] & 0x1F;
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		return new byte[] { (byte) (this.hopCounter & 0x1F) };
	}

	public int getHopCounter() {
		return hopCounter;
	}

	public void setHopCounter(int hopCounter) {
		this.hopCounter = hopCounter;
	}

}
