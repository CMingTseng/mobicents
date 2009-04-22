/**
 * Start time:12:20:07 2009-04-05<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:12:20:07 2009-04-05<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class CircuitAssigmentMap extends AbstractParameter {

	
	public static final int _PARAMETER_CODE = 0x25;
	/**
	 * See Q.763 3.69 Map type : 1544 kbit/s digital path map format (64 kbit/s
	 * base rate)
	 */
	public static final int _MAP_TYPE_1544 = 1;

	/**
	 * See Q.763 3.69 Map type : 2048 kbit/s digital path map format (64 kbit/s
	 * base rate)
	 */
	public static final int _MAP_TYPE_2048 = 2;

	private int mapType = 0;

	private int mapFormat = 0;

	public CircuitAssigmentMap(byte[] b) {
		super();
		decodeElement(b);
	}

	public CircuitAssigmentMap(int mapType, int mapFormat) {
		super();
		this.mapType = mapType;
		this.mapFormat = mapFormat;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null || b.length != 5) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  be 5");
		}

		this.mapType = b[0] & 0x3F;
		this.mapFormat = b[1];
		this.mapFormat |= b[2] << 8;
		this.mapFormat |= b[3] << 16;
		this.mapFormat |= (b[4] & 0x7F) << 24;
		return 5;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		byte[] b = new byte[5];
		b[0] = (byte) (this.mapType & 0x3F);
		b[1] = (byte) this.mapFormat;
		b[2] = (byte) (this.mapFormat >> 8);
		b[3] = (byte) (this.mapFormat >> 16);
		b[4] = (byte) ((this.mapFormat >> 24) & 0x7F);
		return null;
	}

	public int getMapType() {
		return mapType;
	}

	public void setMapType(int mapType) {
		this.mapType = mapType;
	}

	public int getMapFormat() {
		return mapFormat;
	}

	public void setMapFormat(int mapFormat) {
		this.mapFormat = mapFormat;
	}

	/**
	 * Enables circuit
	 * 
	 * @param circuitNumber
	 *            - index of circuit - must be number <1,31>
	 * @throws IllegalArgumentException
	 *             - when number is not in range
	 */
	public void enableCircuit(int circuitNumber) throws IllegalArgumentException {
		if (circuitNumber < 1 || circuitNumber > 31) {
			throw new IllegalArgumentException("Cicruit number is out of range[" + circuitNumber + "] <1,31>");
		}

		this.mapFormat |= 0x01 << circuitNumber;
	}

	/**
	 * Disables circuit
	 * 
	 * @param circuitNumber
	 *            - index of circuit - must be number <1,31>
	 * @throws IllegalArgumentException
	 *             - when number is not in range
	 */
	public void disableCircuit(int circuitNumber) throws IllegalArgumentException {
		if (circuitNumber < 1 || circuitNumber > 31) {
			throw new IllegalArgumentException("Cicruit number is out of range[" + circuitNumber + "] <1,31>");
		}
		this.mapFormat &= 0xFFFFFFFE << circuitNumber;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
