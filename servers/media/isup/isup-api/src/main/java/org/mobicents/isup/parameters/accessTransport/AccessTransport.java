/**
 * Start time:13:39:50 2009-03-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters.accessTransport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mobicents.isup.parameters.AbstractParameter;

/**
 * Start time:13:39:50 2009-03-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class AccessTransport extends AbstractParameter {
a
//FIXME: Q763 3.3
	/* (non-Javadoc)
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public static final int _PARAMETER_CODE = 0x03;


public AccessTransport() {
	super();

}

	public AccessTransport(byte[] b) {
		super();
		decodeElement(b);
	}

	public int decodeElement(byte[] b) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.isup.ISUPComponent#encodeElement(java.io.ByteArrayOutputStream)
	 */
	public int encodeElement(ByteArrayOutputStream bos) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
