/**
 * Start time:13:58:48 2009-04-04<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Start time:13:58:48 2009-04-04<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class GVNSUserGroup extends AbstractNumber {

	public static final int _PARAMETER_CODE = 0;
	// FIXME: shoudl we add max octets ?
	private int gugLengthIndicator;

	public GVNSUserGroup() {

	}

	public GVNSUserGroup(byte[] representation) {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public GVNSUserGroup(ByteArrayInputStream bis) {
		super(bis);
		// TODO Auto-generated constructor stub
	}

	public GVNSUserGroup(String address) {
		super(address);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		return super.decodeElement(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		return super.encodeElement();
	}

	@Override
	public int decodeHeader(ByteArrayInputStream bis) throws IllegalArgumentException {
		int b = bis.read() & 0xff;

		this.oddFlag = (b & 0x80) >> 7;
		this.gugLengthIndicator = b & 0x0F;
		return 1;
	}

	@Override
	public int encodeHeader(ByteArrayOutputStream bos) {
		int b = 0;
		// Even is 000000000 == 0
		boolean isOdd = this.oddFlag == _FLAG_ODD;
		if (isOdd)
			b |= 0x80;
		b |= this.gugLengthIndicator & 0x0F;
		bos.write(b);
		return 1;
	}

	@Override
	public int decodeBody(ByteArrayInputStream bis) throws IllegalArgumentException {

		return 0;
	}

	@Override
	public int encodeBody(ByteArrayOutputStream bos) {

		return 0;
	}

	public int getGugLengthIndicator() {
		return gugLengthIndicator;
	}

	@Override
	public int decodeDigits(ByteArrayInputStream bis) throws IllegalArgumentException {
		return super.decodeDigits(bis, this.gugLengthIndicator);
	}

	@Override
	public void setAddress(String address) {
		// TODO Auto-generated method stub
		super.setAddress(address);
		int l = super.address.length();
		this.gugLengthIndicator = l / 2 + l % 2;
		if (gugLengthIndicator > 8) {
			throw new IllegalArgumentException("Maximum octets for this parameter in digits part is 8.");
			// FIXME: add check for digit (max 7 ?)
		}
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
