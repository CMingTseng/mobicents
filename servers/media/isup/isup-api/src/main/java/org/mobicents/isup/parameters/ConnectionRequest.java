/**
 * Start time:17:59:38 2009-03-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.IOException;

/**
 * Start time:17:59:38 2009-03-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class ConnectionRequest extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x0D;
	private int localReference;
	private int signalingPointCode;
	private boolean protocolClassSet = false;
	private int protocolClass;
	private boolean creditSet = false;
	private int credit;

	public ConnectionRequest(byte[] b) {
		decodeElement(b);
	}

	public ConnectionRequest(int localReference, int signalingPointCode, int protocolClass, int credit) {
		super();
		this.localReference = localReference;
		this.signalingPointCode = signalingPointCode;
		this.protocolClass = protocolClass;
		this.credit = credit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		if (b == null) {
			throw new IllegalArgumentException("byte[] must not be null");
		}

		if (_PROTOCOL_VERSION == 1 && b.length != 7) {
			throw new IllegalArgumentException("For protocol version 1 length of this parameter must be 7 octets");
		}

		if (b.length < 5 || b.length > 7) {
			throw new IllegalArgumentException("byte[] length must be <5,7>");
		}

		// FIXME: This is not mentioned, is it inverted as usually or not ?
		this.localReference |= b[0];
		this.localReference |= b[1] << 8;
		this.localReference |= b[2] << 16;

		this.signalingPointCode = b[3];
		this.signalingPointCode |= (b[4] & 0x3F) << 8;

		if (b.length == 7) {
			this.creditSet = true;
			this.protocolClassSet = true;
			this.protocolClass = b[5];
			this.credit = b[6];
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		byte[] b = null;
		if (this.creditSet || this.protocolClassSet) {
			b = new byte[7];

			b[5] = (byte) this.protocolClass;
			b[6] = (byte) this.credit;
		} else {
			b = new byte[5];
		}

		b[0] = (byte) this.localReference;
		b[1] = (byte) (this.localReference >> 8);
		b[2] = (byte) (this.localReference >> 16);

		b[3] = (byte) this.signalingPointCode;
		b[4] = (byte) ((this.signalingPointCode >> 8) & 0x3F);
		return b;
	}

	public int getLocalReference() {
		return localReference;
	}

	public void setLocalReference(int localReference) {
		this.localReference = localReference;
	}

	public int getSignalingPointCode() {
		return signalingPointCode;
	}

	public void setSignalingPointCode(int signalingPointCode) {
		this.signalingPointCode = signalingPointCode;
	}

	public int getProtocolClass() {

		return protocolClass;
	}

	public void setProtocolClass(int protocolClass) {
		this.protocolClassSet = true;
		this.protocolClass = protocolClass;
	}

	public int getCredit() {
		return credit;
	}

	public void setCredit(int credit) {
		this.creditSet = true;
		this.credit = credit;
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
