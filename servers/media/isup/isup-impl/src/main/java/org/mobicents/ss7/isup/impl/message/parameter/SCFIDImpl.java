/**
 * Start time:12:48:19 2009-04-05<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.impl.message.parameter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.mobicents.ss7.isup.ParameterRangeInvalidException;
import org.mobicents.ss7.isup.message.parameter.SCFID;

/**
 * Start time:12:48:19 2009-04-05<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
public class SCFIDImpl extends NetworkRoutingNumberImpl implements SCFID{
	
	// FIXME: Q.1218 - oleg is this correct? :
	// http://www.itu.int/ITU-T/asn1/database/itu-t/q/q1238.2/2000/IN-CS3-SSF-SCF-datatypes.html
	public SCFIDImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SCFIDImpl(byte[] representation) throws ParameterRangeInvalidException {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public SCFIDImpl(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		super(bis);
		// TODO Auto-generated constructor stub
	}

	public SCFIDImpl(String address, int numberingPlanIndicator, int natureOfAddressIndicator) {
		super(address, numberingPlanIndicator, natureOfAddressIndicator);
		// TODO Auto-generated constructor stub
	}

	public int getCode() {

		return SCFID._PARAMETER_CODE;
	}

}
