/**
 * Start time:08:17:06 2009-04-06<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.ss7.isup.message.parameter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.mobicents.ss7.isup.ParameterRangeInvalidException;

/**
 * Start time:08:17:06 2009-04-06<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class HTRInformationImpl extends AbstractNAINumber implements HTRInformation{
	
	
	

	private int numberingPlanIndicator;

	public HTRInformationImpl(byte[] representation) throws ParameterRangeInvalidException {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public HTRInformationImpl(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		super(bis);
		// TODO Auto-generated constructor stub
	}

	public HTRInformationImpl(int natureOfAddresIndicator, String address, int numberingPlanIndicator) {
		super(natureOfAddresIndicator, address);
		this.numberingPlanIndicator = numberingPlanIndicator;
	}

	public HTRInformationImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.isup.parameters.AbstractNumber#decodeBody(java.io.
	 * ByteArrayInputStream)
	 */
	@Override
	public int decodeBody(ByteArrayInputStream bis) throws IllegalArgumentException {
		int b = bis.read() & 0xff;

		this.numberingPlanIndicator = (b & 0x70) >> 4;
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.isup.parameters.AbstractNumber#encodeBody(java.io.
	 * ByteArrayOutputStream)
	 */
	@Override
	public int encodeBody(ByteArrayOutputStream bos) {
		int c = (this.numberingPlanIndicator & 0x07) << 4;
		bos.write(c);
		return 1;
	}

	public int getNumberingPlanIndicator() {
		return numberingPlanIndicator;
	}

	public void setNumberingPlanIndicator(int numberingPlanIndicator) {
		this.numberingPlanIndicator = numberingPlanIndicator;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
