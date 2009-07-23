/**
 * Start time:15:10:58 2009-04-05<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

import org.mobicents.isup.ParameterRangeInvalidException;

/**
 * Start time:15:10:58 2009-04-05<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class ApplicationTransportParameterImpl extends AbstractParameter implements ApplicationTransportParameter{

	//FIXME: Oleg ? how can we do that ? Q.763 3.82
	
	/* (non-Javadoc)
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	

	
	public ApplicationTransportParameterImpl() throws ParameterRangeInvalidException{
		super();

	}
	
	public ApplicationTransportParameterImpl(byte[] b) throws ParameterRangeInvalidException{
		super();
		decodeElement(b);
	}

	public int decodeElement(byte[] b) throws org.mobicents.isup.ParameterRangeInvalidException {
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
}
