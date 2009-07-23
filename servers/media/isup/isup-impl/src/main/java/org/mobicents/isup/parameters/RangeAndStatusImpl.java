/**
 * Start time:14:44:16 2009-04-02<br>
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
 * Start time:14:44:16 2009-04-02<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class RangeAndStatusImpl extends AbstractParameter implements RangeAndStatus {

	private byte range;
	private byte[] status;

	// FIXME:
	// private Status[] status = null;

	public RangeAndStatusImpl(byte[] b) throws ParameterRangeInvalidException {
		super();
		decodeElement(b);
	}

	public RangeAndStatusImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RangeAndStatusImpl(byte range, byte[] status) {
		super();
		this.range = range;
		setStatus(status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws org.mobicents.isup.ParameterRangeInvalidException {

		this.range = b[0];
		this.status = new byte[b.length - 1];
		System.arraycopy(b, 1, this.status, 0, this.status.length);

		return b.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(this.range);
		bos.write(this.status);
		return bos.toByteArray();
	}

	public byte getRange() {
		return range;
	}

	public void setRange(byte range) {
		this.range = range;
	}

	public byte[] getStatus() {
		return status;
	}

	public void setStatus(byte[] status) {
		this.status = status;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
