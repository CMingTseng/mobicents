/**
 * Start time:16:41:45 2009-04-02<br>
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
 * Start time:16:41:45 2009-04-02<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class RedirectionNumber extends AbstractNAINumber {

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
	public static final int _PARAMETER_CODE = 0x0C;

	protected int numberingPlanIndicator = 0;

	protected int internalNetworkNumberIndicator = 0;

	public RedirectionNumber(byte[] representation) {
		super(representation);
		// TODO Auto-generated constructor stub
	}

	public RedirectionNumber(ByteArrayInputStream bis) {
		super(bis);
		// TODO Auto-generated constructor stub
	}

	public RedirectionNumber(int natureOfAddresIndicator, String address, int numberingPlanIndicator, int internalNetworkNumberIndicator) {
		super(natureOfAddresIndicator, address);
		this.numberingPlanIndicator = numberingPlanIndicator;
		this.internalNetworkNumberIndicator = internalNetworkNumberIndicator;
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
		int c = this.natureOfAddresIndicator << 4;
		c |= (this.internalNetworkNumberIndicator << 7);
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
	public int getCode() {

		return _PARAMETER_CODE;
	}
}
