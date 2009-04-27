/**
 * Start time:18:44:10 2009-03-27<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Start time:18:44:10 2009-03-27<br>
 * Project: mobicents-jain-isup-stack<br>
 * This class is super classes for all message parameters with form of:
 * 
 * <pre>
 *    ________
 *   | Header |
 *   |________|
 *   |  Body  |
 *   |________|
 *   | Digits |
 *   |________|
 * 
 * 
 * </pre>
 * 
 * Where Header has 1+ bytes, body 1+ byte, and digits part contains pairs of
 * digits encoded from righ t to left from most significant digits in number.
 * Examples parameters are(from Q763): 3.16 Connected Number,3.9 Called party
 * number, 3.10 Calling party number, 3.26 Generic number, 3.30 Location number.
 * Also (3.39,) Implemetnation class must fill tag variable with proper
 * information. Length part of information component is computed from length of
 * this element. See section (B1, B2 and B3 of Q.763)
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public abstract class AbstractNumber extends AbstractParameter {

	protected Logger logger = Logger.getLogger(this.getClass());
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

	/**
	 * Holds digits(in specs: "signal"). digits[0] holds most siginificant
	 * digit. Also length of this table contains information about Odd/even
	 * flag. However there is distinct flag used in process of decoding from
	 * byte[]. This is becuse in case of decoding we dont know if last digit is
	 * filler or digit.
	 */
	protected String address;

	public AbstractNumber() {
		super();

	}

	public AbstractNumber(byte[] representation) {
		super();

		decodeElement(representation);

	}

	public AbstractNumber(ByteArrayInputStream bis) {
		super();

		this.decodeElement(bis);
	}

	public AbstractNumber(String address) {
		super();
		if (address == null) {
			throw new IllegalArgumentException("Address is null.");
		}

		this.setAddress(address);
	}

	public int decodeElement(byte[] b) throws IllegalArgumentException {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);

		return this.decodeElement(bis);
	}

	protected int decodeElement(ByteArrayInputStream bis) throws IllegalArgumentException {
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding header");
		}

		int count = decodeHeader(bis);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding header, read count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding body");
		}
		count += decodeBody(bis);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding body, read count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding digits");
		}
		count += decodeDigits(bis);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Decoding digits, read count: " + count);
		}
		return count;
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
		count += encodeBody(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding body, write count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding digits");
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
		count += encodeBody(bos);
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding body, write count: " + count);
			logger.debug("[" + this.getClass().getSimpleName() + "]Encoding digits");
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

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes header part (its 1 or 2 bytes
	 * usually.) Default implemetnation decodes header of one byte - where most
	 * significant bit is O/E indicator and bits 7-1 are NAI. This method should
	 * be over
	 * 
	 * @param bis
	 * @return - number of bytes reads
	 * @throws IllegalArgumentException
	 *             - thrown if read error is encountered.
	 */
	public int decodeHeader(ByteArrayInputStream bis) throws IllegalArgumentException {
		if(bis.available()==0)
		{
			throw new IllegalArgumentException("No more data to read.");
		}
		int b = bis.read() & 0xff;

		this.oddFlag = (b & 0x80) >> 7;

		return 1;
	}

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes body part (its 1 byte
	 * usually.) However in different "numbers" it has different meaning. Each
	 * subclass should provide implementation
	 * 
	 * @param bis
	 * @return - number of bytes reads throws IllegalArgumentException - thrown
	 *         if read error is encountered.
	 * @throws IllegalArgumentException
	 *             - thrown if read error is encountered.
	 */
	public abstract int decodeBody(ByteArrayInputStream bis) throws IllegalArgumentException;

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes digits part.
	 * 
	 * @param bis
	 * @return - number of bytes reads throws IllegalArgumentException - thrown
	 *         if read error is encountered.
	 * @throws IllegalArgumentException
	 *             - thrown if read error is encountered.
	 */
	public int decodeDigits(ByteArrayInputStream bis) throws IllegalArgumentException {

		if(bis.available()==0)
		{
			throw new IllegalArgumentException("No more data to read.");
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
		
		if (oddFlag != _FLAG_ODD) {
			address += Integer.toHexString((b & 0xf0) >> 4);
		}
		this.setAddress(address);
		
		return count;
	}

	/**
	 * This method is used in constructor that takes byte[] or
	 * ByteArrayInputStream as parameter. Decodes digits part. It reads exactly
	 * octetsCount number of octets.
	 * 
	 * @param bis
	 * @param octetsCount
	 *            - number iof octets to read from stream
	 * @return
	 * @throws IllegalArgumentException
	 */
	public int decodeDigits(ByteArrayInputStream bis, int octetsCount) throws IllegalArgumentException {
		if(bis.available()==0)
		{
			throw new IllegalArgumentException("No more data to read.");
		}
		int count = 0;
		address = "";
		int b = 0;
		while (octetsCount != count - 1 && bis.available() - 1 > 0) {
			b = (byte) bis.read();
			count++;
			
			int d1 = b & 0x0f;
			int d2 = (b & 0xf0) >> 4;

			address += Integer.toHexString(d1) + Integer.toHexString(d2);
			
		}

		b = bis.read() & 0xff;
		count++;
		address += Integer.toHexString((b & 0x0f));
		
		if (oddFlag != _FLAG_ODD) {
			address += Integer.toHexString((b & 0xf0) >> 4);
		}
		this.setAddress(address);
		if (octetsCount != count) {
			throw new IllegalArgumentException("Failed to read [" + octetsCount + "], encountered only [" + count + "]");
		}
		return count;
	}

	/**
	 * This method is used in encodeElement method. It encodes header part (1 or
	 * 2 bytes usually.)
	 * 
	 * @param bis
	 * @return - number of bytes encoded.
	 */
	public int encodeHeader(ByteArrayOutputStream bos) {
		int b = 0;
		// Even is 000000000 == 0
		boolean isOdd = this.oddFlag == _FLAG_ODD;
		if (isOdd)
			b |= 0x80;
		bos.write(b);

		return 1;
	}

	/**
	 * This methods is used in encodeElement method. It encodes body. Each
	 * subclass shoudl provide implementetaion.
	 * 
	 * @param bis
	 * @return - number of bytes reads
	 * 
	 */
	public abstract int encodeBody(ByteArrayOutputStream bos);

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

	public boolean isOddFlag() {
		return oddFlag == _FLAG_ODD;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;

		// lets compute odd flag
		// FIXME: Oleg is this correct?
		oddFlag = this.address.length() % 2;
	}

}
