/**
 * Start time:17:05:31 2009-04-03<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.isup.ParameterRangeInvalidException;

/**
 * Start time:17:05:31 2009-04-03<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski
 *         </a>
 */
public class TransitNetworkSelection extends AbstractParameter {

	protected static final Logger logger = Logger.getLogger(TransitNetworkSelection.class);

	public static final int _PARAMETER_CODE = 0x23;
	/**
	 * See Q.763 3.53 Type of network identification : CCITT/ITU-T-standardized
	 * identification
	 */
	public static final int _TONI_ITU_T = 0;
	/**
	 * See Q.763 3.53 Type of network identification : national network
	 * identification
	 */
	public static final int _TONI_NNI = 2;

	/**
	 * See Q.763 3.53 Network identification plan : For CCITT/ITU-T-standardized
	 * identification : unknown
	 */
	public static final int _NIP_UNKNOWN = 0;

	/**
	 * See Q.763 3.53 Network identification plan : For CCITT/ITU-T-standardized
	 * identification : public data network identification code (DNIC), ITU-T
	 * Recommendation X.121
	 */
	public static final int _NIP_PDNIC = 3;

	/**
	 * See Q.763 3.53 Network identification plan : For CCITT/ITU-T-standardized
	 * identification : public land Mobile Network Identification Code (MNIC),
	 * ITU-T Recommendation E.212
	 */
	public static final int _NIP_PLMNIC = 6;

	// FIXME: Oleg is this correct?
	private String address;
	private int typeOfNetworkIdentification;
	private int networkIdentificationPlan;
	/**
	 * Holds odd flag, it can have either value: 10000000(x80) or 00000000. For
	 * each it takes value 1 and 0;
	 */
	protected int oddFlag;

	/**
	 * indicates odd flag value (0x80) as integer (1). This is achieved when odd
	 * flag in parameter is moved to the right by sever possitions ( >> 7)
	 */
	public static final int _FLAG_ODD = 1;

	public TransitNetworkSelection(String address, int typeOfNetworkIdentification, int networkIdentificationPlan) {
		super();
		this.setAddress(address);
		this.typeOfNetworkIdentification = typeOfNetworkIdentification;
		this.networkIdentificationPlan = networkIdentificationPlan;
	}

	public TransitNetworkSelection(byte[] b) throws ParameterRangeInvalidException {
		super();
		decodeElement(b);
	}

	public byte[] encodeElement() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding header");
		}
		int count = encodeHeader(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding header, write count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding body");
		}

		count += encodeDigits(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding digits, write count: " + count);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// out.write(tag);
		// Util.encodeLength(count, out);
		out.write(bos.toByteArray());
		return out.toByteArray();
	}

	public int encodeElement(ByteArrayOutputStream out) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding header");
		}
		int count = encodeHeader(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding header, write count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding body");
		}

		count += encodeDigits(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding digits, write count: " + count);
		}

		// count += tag.length;
		// out.write(tag);
		// count += Util.encodeLength(count, out);
		out.write(bos.toByteArray());
		return count;
	}

	public int decodeElement(byte[] b) throws org.mobicents.isup.ParameterRangeInvalidException {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);

		return this.decodeElement(bis);
	}

	protected int decodeElement(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding header");
		}

		int count = decodeHeader(bis);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding header, read count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding body");
		}

		count += decodeDigits(bis);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding digits, read count: " + count);
		}
		return count;
	}

	/**
	 * This method is used in encodeElement. Encodes digits part. This is
	 * because
	 * 
	 * @param bos
	 *            - where digits will be encoded
	 * @return - number of bytes encoded
	 * 
	 */
	public int encodeDigits(ByteArrayOutputStream bos) {
		boolean isOdd = this.oddFlag == _FLAG_ODD;

		byte b = 0;
		int count = (!isOdd) ? address.length() : address.length() - 1;
		int bytesCount = 0;
		for (int i = 0; i < count - 1; i += 2) {
			String ds1 = address.substring(i, i + 1);
			String ds2 = address.substring(i + 1, i + 2);

			int d1 = Integer.parseInt(ds1, 16);
			int d2 = Integer.parseInt(ds2, 16);

			b = (byte) (d2 << 4 | d1);
			bos.write(b);
			bytesCount++;
		}

		if (isOdd) {
			String ds1 = address.substring(count, count + 1);
			int d = Integer.parseInt(ds1);

			b = (byte) (d & 0x0f);
			bos.write(b);
			bytesCount++;
		}

		return bytesCount;
	}

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes digits part. Stores result in
	 * digits field, where digits[0] holds most significant digit. This is
	 * because
	 * 
	 * @param bis
	 * @return - number of bytes reads throws IllegalArgumentException - thrown
	 *         if read error is encountered.
	 * @throws ParameterRangeInvalidException
	 *             - thrown if read error is encountered.
	 */
	public int decodeDigits(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		if (bis.available() == 0) {
			throw new ParameterRangeInvalidException("No more data to read.");
		}
		// FIXME: we could spare time by passing length arg - or getting it from
		// bis??
		int count = 0;
		address = "";
		int b = 0;
		while (bis.available() - 1 > 0) {
			b = (byte) bis.read();

			int d1 = b & 0x0f;
			int d2 = (b & 0xf0) >> 4;

			address += Integer.toHexString(d1) + Integer.toHexString(d2);

		}

		b = bis.read() & 0xff;
		address += Integer.toHexString((b & 0x0f));

		if (oddFlag != 1) {
			address += Integer.toHexString((b & 0xf0) >> 4);
		}
		return count;
	}

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes header part (its 1 or 2 bytes
	 * usually.) Default implemetnation decodes header of one byte - where most
	 * significant bit is O/E indicator and bits 7-1 are NAI. This method should
	 * be over
	 * 
	 * @param bis
	 * @return - number of bytes reads
	 * @throws ParameterRangeInvalidException
	 *             - thrown if read error is encountered.
	 */
	public int decodeHeader(ByteArrayInputStream bis) throws ParameterRangeInvalidException {
		if (bis.available() == 0) {
			throw new ParameterRangeInvalidException("No more data to read.");
		}
		int b = bis.read() & 0xff;

		this.oddFlag = (b & 0x80) >> 7;
		this.setTypeOfNetworkIdentification((b >> 4));
		this.setNetworkIdentificationPlan(b);
		return 1;
	}

	/**
	 * This method is used in encodeElement method. It encodes header part (1 or
	 * 2 bytes usually.)
	 * 
	 * @param bis
	 * @return - number of bytes encoded.
	 */
	public int encodeHeader(ByteArrayOutputStream bos) {
		int b = this.networkIdentificationPlan & 0x0F;
		b |= (this.typeOfNetworkIdentification & 0x07) << 4;
		// Even is 000000000 == 0
		boolean isOdd = this.oddFlag == _FLAG_ODD;
		if (isOdd)
			b |= 0x80;
		bos.write(b);

		return 1;
	}

	public int getTypeOfNetworkIdentification() {
		return typeOfNetworkIdentification;
	}

	public void setTypeOfNetworkIdentification(int typeOfNetworkIdentification) {
		this.typeOfNetworkIdentification = typeOfNetworkIdentification & 0x07;
	}

	public int getNetworkIdentificationPlan() {
		return networkIdentificationPlan;
	}

	public void setNetworkIdentificationPlan(int networkIdentificationPlan) {
		this.networkIdentificationPlan = networkIdentificationPlan & 0x0F;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
		oddFlag = this.address.length() % 2;
	}

	public boolean isOddFlag() {
		return oddFlag == _FLAG_ODD;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
