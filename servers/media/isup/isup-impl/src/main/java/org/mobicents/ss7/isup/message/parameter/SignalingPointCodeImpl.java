/**
 * Start time:12:23:47 2009-04-02<br>
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
 * Start time:12:23:47 2009-04-02<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class SignalingPointCodeImpl extends AbstractPointCode implements SignalingPointCode {

	public int getCode() {

		return _PARAMETER_CODE;
	}

	public SignalingPointCodeImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SignalingPointCodeImpl(byte[] b) throws ParameterRangeInvalidException {
		super(b);
		// TODO Auto-generated constructor stub
	}

}
