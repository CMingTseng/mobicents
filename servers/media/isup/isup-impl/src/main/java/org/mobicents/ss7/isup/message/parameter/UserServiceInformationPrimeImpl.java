/**
 * Start time:12:36:18 2009-04-04<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.message.parameter;

import java.io.IOException;

import org.mobicents.ss7.isup.ParameterRangeInvalidException;

/**
 * Start time:12:36:18 2009-04-04<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class UserServiceInformationPrimeImpl extends AbstractParameter implements UserServiceInformationPrime{
	
//FIXME Q.931
	
	/* (non-Javadoc)
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws ParameterRangeInvalidException {
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
	public int getCode() {

		return _PARAMETER_CODE;
	}

	public UserServiceInformationPrimeImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UserServiceInformationPrimeImpl(byte[] b) throws ParameterRangeInvalidException {
		super();
		decodeElement(b);
	}
	
}
