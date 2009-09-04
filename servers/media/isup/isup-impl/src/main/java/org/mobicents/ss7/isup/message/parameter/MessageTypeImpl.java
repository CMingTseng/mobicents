/**
 * Start time:14:56:41 2009-04-20<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.message.parameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mobicents.ss7.isup.ParameterRangeInvalidException;

/**
 * Start time:14:56:41 2009-04-20<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
public class MessageTypeImpl implements ISUPParameter , MessageType{

	

	private int code = 0;

	public MessageTypeImpl(byte[] code) throws ParameterRangeInvalidException {
		super();
		this.decodeElement(code);
	}
	
	
	public MessageTypeImpl(int code) {
		super();
		this.code = code;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.parameters.ISUPParameter#getTag()
	 */
	public byte[] getTag() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws ParameterRangeInvalidException {
		if (b == null || b.length != 1)
			throw new ParameterRangeInvalidException();
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		return new byte[] { (byte) this.code };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.isup.ISUPComponent#encodeElement(java.io.ByteArrayOutputStream
	 * )
	 */
	public int encodeElement(ByteArrayOutputStream bos) throws IOException {
		bos.write(this.code);
		return 1;
	}

	public int getCode() {

		return code;
	}

}
