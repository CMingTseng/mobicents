/**
 * Start time:15:14:32 2009-03-30<br>
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
 * Start time:15:14:32 2009-03-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class CauseIndicatorsImpl extends AbstractParameter implements CauseIndicators {

	// FIXME: we ignore EXT fields , is this ok ?
	
	private int location = 0;
	private int causeValue = 0;
	private int codingStandard = 0;
	private byte[] diagnostics = null;

	public CauseIndicatorsImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CauseIndicatorsImpl(byte[] b) throws ParameterRangeInvalidException {
		super();
		decodeElement(b);
	}

	public CauseIndicatorsImpl(int codingStandard, int location, int causeValue, byte[] diagnostics) {
		super();
		this.setCodingStandard(codingStandard);
		this.setLocation(location);
		this.setCauseValue(causeValue);
		this.diagnostics = diagnostics;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws org.mobicents.isup.ParameterRangeInvalidException {

		// FIXME: there are ext bits, does this mean this param can be from 1 to
		// 3+ bytes?
		// but trace shows that extension bit is always on... does this mean
		// that we can have mutliptle indicators?
		if (b == null || b.length < 2) {
			throw new ParameterRangeInvalidException("byte[] must not be null or has size less than 2");
		}
		// Used because of Q.850 - we must ignore recomendation
		int index = 0;
		// first two bytes are mandatory
		int v = 0;
		// remove ext
		v = b[index] & 0x7F;
		this.location = v & 0x0F;
		this.codingStandard = v >> 5;
		if (((b[index] & 0x7F) >> 7) == 0) {
			index += 2;
		} else {
			index++;
		}
		v = 0;
		v = b[1] & 0x7F;
		this.causeValue = v;
		if (b.length == 2) {
			return 2;
		} else {
			if ((b.length - 2) % 3 != 0) {
				throw new ParameterRangeInvalidException("Diagnostics part  must have 3xN bytes, it has: " + (b.length - 2));
			}

			int byteCounter = 2;

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			for (int i = 2; i < b.length; i++) {
				bos.write(b[i]);
				byteCounter++;
			}

			this.diagnostics = bos.toByteArray();

			return byteCounter;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		int v = this.location & 0x0F;
		v |= (byte) ((this.codingStandard & 0x03) << 5) | (0x01 << 7);
		bos.write(v);
		bos.write(this.causeValue | (0x01 << 7));
		if (this.diagnostics != null)
			bos.write(this.diagnostics);
		byte[] b = bos.toByteArray();

		return b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.isup.ISUPComponent#encodeElement(java.io.ByteArrayOutputStream
	 * )
	 */
	public int encodeElement(ByteArrayOutputStream bos) throws IOException {
		byte[] b = this.encodeElement();
		bos.write(b);
		return b.length;
	}

	public int getCodingStandard() {
		return codingStandard;
	}

	public void setCodingStandard(int codingStandard) {
		this.codingStandard = codingStandard & 0x03;
	}

	public int getLocation() {
		return location;
	}

	public void setLocation(int location) {
		this.location = location & 0x0F;
	}

	public int getCauseValue() {
		return causeValue & 0x7F;
	}

	public void setCauseValue(int causeValue) {
		this.causeValue = causeValue;
	}

	public byte[] getDiagnostics() {
		return diagnostics;
	}

	public void setDiagnostics(byte[] diagnostics) {
		this.diagnostics = diagnostics;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
