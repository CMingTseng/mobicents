/**
 * Start time:16:36:21 2009-03-29<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * 
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.mobicents.isup.ParameterRangeInvalidException;

/**
 * Start time:16:36:21 2009-03-29<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class ConnectedNumberImpl extends AbstractNAINumber implements ConnectedNumber {

	protected int numberingPlanIndicator;

	protected int addressRepresentationREstrictedIndicator;

	protected int screeningIndicator;

	/**
	 * 
	 * @param representation
	 * @throws ParameterRangeInvalidException
	 */
	public ConnectedNumberImpl(byte[] representation) throws ParameterRangeInvalidException {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public ConnectedNumberImpl() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * tttttt
	 * 
	 * @param bis
	 * @throws ParameterRangeInvalidException
	 */
	public ConnectedNumberImpl(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		super(bis);
		// TODO Auto-generated constructor stub
	}

	public ConnectedNumberImpl(int natureOfAddresIndicator, String address, int numberingPlanIndicator, int addressRepresentationREstrictedIndicator, int screeningIndicator) {
		super(natureOfAddresIndicator, address);
		this.numberingPlanIndicator = numberingPlanIndicator;
		this.addressRepresentationREstrictedIndicator = addressRepresentationREstrictedIndicator;
		this.screeningIndicator = screeningIndicator;
	}

	@Override
	public int encodeHeader(ByteArrayOutputStream bos) {
		doAddressPresentationRestricted();
		return super.encodeHeader(bos);
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
		this.addressRepresentationREstrictedIndicator = (b & 0x0c) >> 2;
		this.screeningIndicator = (b & 0x03);
		return 1;
	}

	/**
	 * makes checks on APRI - see NOTE to APRI in Q.763, p 23
	 */
	protected void doAddressPresentationRestricted() {

		if (this.addressRepresentationREstrictedIndicator == _APRI_NOT_AVAILABLE)
			return;

		// NOTE 1 � If the parameter is included and the address presentation
		// restricted indicator indicates
		// address not available, octets 3 to n( this are digits.) are omitted,
		// the subfields in items a - odd/evem, b -nai , c - ni and d -npi, are
		// coded with
		// 0's, and the subfield f - filler, is coded with 11.
		this.oddFlag = 0;
		this.natureOfAddresIndicator = 0;
		this.numberingPlanIndicator = 0;
		// 11
		this.screeningIndicator = 3;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.isup.parameters.AbstractNumber#encodeBody(java.io.
	 * ByteArrayOutputStream)
	 */
	@Override
	public int encodeBody(ByteArrayOutputStream bos) {
		int c = this.numberingPlanIndicator << 4;

		c |= (this.addressRepresentationREstrictedIndicator << 2);
		c |= (this.screeningIndicator);
		bos.write(c & 0x7F);
		return 1;
	}

	public int getNumberingPlanIndicator() {
		return numberingPlanIndicator;
	}

	public void setNumberingPlanIndicator(int numberingPlanIndicator) {
		this.numberingPlanIndicator = numberingPlanIndicator;
	}

	public int getAddressRepresentationRestrictedIndicator() {
		return addressRepresentationREstrictedIndicator;
	}

	public void setAddressRepresentationRestrictedIndicator(int addressRepresentationREstrictedIndicator) {
		this.addressRepresentationREstrictedIndicator = addressRepresentationREstrictedIndicator;
	}

	public int getScreeningIndicator() {
		return screeningIndicator;
	}

	public void setScreeningIndicator(int screeningIndicator) {
		this.screeningIndicator = screeningIndicator;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
