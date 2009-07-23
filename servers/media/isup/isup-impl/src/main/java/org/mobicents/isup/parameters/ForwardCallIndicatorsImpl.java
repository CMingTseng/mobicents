/**
 * Start time:11:58:42 2009-03-31<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mobicents.isup.ParameterRangeInvalidException;

/**
 * Start time:11:58:42 2009-03-31<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class ForwardCallIndicatorsImpl extends AbstractParameter implements ForwardCallIndicators{

	
	private final static int _TURN_ON = 1;
	private final static int _TURN_OFF = 0;

	private boolean nationalCallIdentificator = false;
	private int endToEndMethodIndicator = 0;
	private boolean interworkingIndicator = false;
	private boolean endToEndInformationIndicator = false;
	private boolean isdnUserPartIndicator = false;
	private int isdnUserPartReferenceIndicator = 0;
	private int sccpMethodIndicator = 0;
	private boolean isdnAccessIndicator = false;

	

	public ForwardCallIndicatorsImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ForwardCallIndicatorsImpl(byte[] b) throws ParameterRangeInvalidException {
		super();
		decodeElement(b);
	}

	public ForwardCallIndicatorsImpl(boolean nationalCallIdentificator, int endToEndMethodIndicator, boolean interworkingIndicator, boolean endToEndInformationIndicator, boolean isdnUserPartIndicator,
			int isdnUserPartReferenceIndicator, int sccpMethodIndicator, boolean isdnAccessIndicator) {
		super();
		this.nationalCallIdentificator = nationalCallIdentificator;
		this.endToEndMethodIndicator = endToEndMethodIndicator;
		this.interworkingIndicator = interworkingIndicator;
		this.endToEndInformationIndicator = endToEndInformationIndicator;
		this.isdnUserPartIndicator = isdnUserPartIndicator;
		this.isdnUserPartReferenceIndicator = isdnUserPartReferenceIndicator;
		this.sccpMethodIndicator = sccpMethodIndicator;
		this.isdnAccessIndicator = isdnAccessIndicator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws org.mobicents.isup.ParameterRangeInvalidException {
		if (b == null || b.length != 2) {
			throw new IllegalArgumentException("byte[] must not be null or have different size than 2");
		}
		int v = 0;

		v = b[0];

		this.nationalCallIdentificator = (v & 0x01) == _TURN_ON;
		this.endToEndMethodIndicator = (v >> 1) & 0x03;
		this.interworkingIndicator = ((v >> 3) & 0x01) == _TURN_ON;
		this.endToEndInformationIndicator = ((v >> 4) & 0x01) == _TURN_ON;
		this.isdnUserPartIndicator = ((v >> 5) & 0x01) == _TURN_ON;
		this.isdnUserPartReferenceIndicator = (v >> 6) & 0x03;

		v = b[1];

		this.isdnAccessIndicator = (v & 0x01) == _TURN_ON;
		// FIXME: should we allow older bytes to pass ?
		this.sccpMethodIndicator = (v >> 1) & 0x03;

		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		byte[] b = new byte[2];

		b[0] |= this.nationalCallIdentificator ? _TURN_ON : _TURN_OFF;
		b[0] |= (this.endToEndMethodIndicator & 0x03) << 1;
		b[0] |= (this.interworkingIndicator ? _TURN_ON : _TURN_OFF) << 3;
		b[0] |= (this.endToEndInformationIndicator ? _TURN_ON : _TURN_OFF) << 4;
		b[0] |= (this.isdnUserPartIndicator ? _TURN_ON : _TURN_OFF) << 5;
		b[0] |= (this.isdnUserPartReferenceIndicator & 0x03) << 6;

		b[1] = (byte) (this.isdnAccessIndicator ? _TURN_ON : _TURN_OFF);
		// FIXME should we allow here older bytes to pass
		b[1] |= (this.sccpMethodIndicator & 0x03) << 1;
		return b;
	}

	@Override
	public int encodeElement(ByteArrayOutputStream bos) throws IOException {
		byte[] b = this.encodeElement();
		bos.write(b);
		return b.length;
	}

	public boolean isNationalCallIdentificator() {
		return nationalCallIdentificator;
	}

	public void setNationalCallIdentificator(boolean nationalCallIdentificator) {
		this.nationalCallIdentificator = nationalCallIdentificator;
	}

	public int getEndToEndMethodIndicator() {
		return endToEndMethodIndicator;
	}

	public void setEndToEndMethodIndicator(int endToEndMethodIndicator) {
		this.endToEndMethodIndicator = endToEndMethodIndicator;
	}

	public boolean isInterworkingIndicator() {
		return interworkingIndicator;
	}

	public void setInterworkingIndicator(boolean interworkingIndicator) {
		this.interworkingIndicator = interworkingIndicator;
	}

	public boolean isEndToEndInformationIndicator() {
		return endToEndInformationIndicator;
	}

	public void setEndToEndInformationIndicator(boolean endToEndInformationIndicator) {
		this.endToEndInformationIndicator = endToEndInformationIndicator;
	}

	public boolean isIsdnUserPartIndicator() {
		return isdnUserPartIndicator;
	}

	public void setIsdnUserPartIndicator(boolean isdnUserPartIndicator) {
		this.isdnUserPartIndicator = isdnUserPartIndicator;
	}

	public int getIsdnUserPartReferenceIndicator() {
		return isdnUserPartReferenceIndicator;
	}

	public void setIsdnUserPartReferenceIndicator(int isdnUserPartReferenceIndicator) {
		this.isdnUserPartReferenceIndicator = isdnUserPartReferenceIndicator;
	}

	public int getSccpMethodIndicator() {
		return sccpMethodIndicator;
	}

	public void setSccpMethodIndicator(int sccpMethodIndicator) {
		this.sccpMethodIndicator = sccpMethodIndicator;
	}

	public boolean isIsdnAccessIndicator() {
		return isdnAccessIndicator;
	}

	public void setIsdnAccessIndicator(boolean isdnAccessIndicator) {
		this.isdnAccessIndicator = isdnAccessIndicator;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
