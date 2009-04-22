/**
 * Start time:17:02:12 2009-03-29<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Start time:17:02:12 2009-03-29<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class LocationNumber extends AbstractNAINumber {

	
	public static final int _PARAMETER_CODE = 0x3F;
	/**
	 * numbering plan indicator indicator value. See Q.763 - 3.9d
	 */
	public final static int _NPI_ISDN = 1;
	/**
	 * numbering plan indicator indicator value. See Q.763 - 3.9d
	 */
	public final static int _NPI_DATA = 3;
	/**
	 * numbering plan indicator indicator value. See Q.763 - 3.9d
	 */
	public final static int _NPI_TELEX = 4;

	/**
	 * internal network number indicator indicator value. See Q.763 - 3.9c
	 */
	public final static int _INN_ROUTING_ALLOWED = 0;
	/**
	 * internal network number indicator indicator value. See Q.763 - 3.9c
	 */
	public final static int _INN_ROUTING_NOT_ALLOWED = 1;

	/**
	 * address presentation restricted indicator indicator value. See Q.763 -
	 * 3.10e
	 */
	public final static int _APRI_ALLOWED = 0;

	/**
	 * address presentation restricted indicator indicator value. See Q.763 -
	 * 3.10e
	 */
	public final static int _APRI_RESTRICTED = 1;

	/**
	 * address presentation restricted indicator indicator value. See Q.763 -
	 * 3.10e
	 */
	public final static int _APRI_NOT_AVAILABLE = 2;

	/**
	 * address presentation restricted indicator indicator value. See Q.763 -
	 * 3.16d
	 */
	public final static int _APRI_SPARE = 3;

	/**
	 * screening indicator indicator value. See Q.763 - 3.30f
	 */
	public final static int _SI_USER_PROVIDED_VERIFIED_PASSED = 1;

	/**
	 * screening indicator indicator value. See Q.763 - 3.30f
	 */
	public final static int _SI_NETWORK_PROVIDED = 3;

	protected int numberingPlanIndicator = 0;

	protected int internalNetworkNumberIndicator = 0;

	protected int addressRepresentationREstrictedIndicator = 0;

	protected int screeningIndicator = 0;

	public LocationNumber(int natureOfAddresIndicator, String address, int numberingPlanIndicator, int internalNetworkNumberIndicator, int addressRepresentationREstrictedIndicator,
			int screeningIndicator) {
		super(natureOfAddresIndicator, address);
		this.numberingPlanIndicator = numberingPlanIndicator;
		this.internalNetworkNumberIndicator = internalNetworkNumberIndicator;
		this.addressRepresentationREstrictedIndicator = addressRepresentationREstrictedIndicator;
		this.screeningIndicator = screeningIndicator;
	}

	public LocationNumber(byte[] representation) {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public LocationNumber(ByteArrayInputStream bis) {
		super(bis);
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

		this.internalNetworkNumberIndicator = (b & 0x80) >> 7;
		this.numberingPlanIndicator = (b & 0x70) >> 4;
		this.addressRepresentationREstrictedIndicator = (b & 0x0c) >> 2;
		this.screeningIndicator = (b & 0x03);
		return 1;
	}

	@Override
	public int encodeHeader(ByteArrayOutputStream bos) {
		doAddressPresentationRestricted();

		return super.encodeHeader(bos);
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
		this.internalNetworkNumberIndicator = 0;

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
		int c = this.natureOfAddresIndicator << 4;
		c |= (this.internalNetworkNumberIndicator << 7);
		c |= (this.addressRepresentationREstrictedIndicator << 2);
		c |= (this.screeningIndicator);
		bos.write(c);
		return 1;

	}

	public int getNumberingPlanIndicator() {
		return numberingPlanIndicator;
	}

	public void setNumberingPlanIndicator(int numberingPlanIndicator) {
		this.numberingPlanIndicator = numberingPlanIndicator;
	}

	public int getInternalNetworkNumberIndicator() {
		return internalNetworkNumberIndicator;
	}

	public void setInternalNetworkNumberIndicator(int internalNetworkNumberIndicator) {
		this.internalNetworkNumberIndicator = internalNetworkNumberIndicator;
	}

	public int getAddressRepresentationREstrictedIndicator() {
		return addressRepresentationREstrictedIndicator;
	}

	public void setAddressRepresentationREstrictedIndicator(int addressRepresentationREstrictedIndicator) {
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
