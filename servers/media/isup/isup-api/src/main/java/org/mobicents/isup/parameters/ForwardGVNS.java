/**
 * Start time:13:39:30 2009-04-04<br>
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

/**
 * Start time:13:39:30 2009-04-04<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class ForwardGVNS extends AbstractParameter {

	private OriginatingParticipatingServiceProvider opServiceProvider = null;
	private GVNSUserGroup gvnsUserGroup = null;
	private TerminatingNetworkRoutingNumber tnRoutingNumber = null;

	public ForwardGVNS(OriginatingParticipatingServiceProvider opServiceProvider, GVNSUserGroup gvnsUserGroup, TerminatingNetworkRoutingNumber tnRoutingNumber) {
		super();
		this.opServiceProvider = opServiceProvider;
		this.gvnsUserGroup = gvnsUserGroup;
		this.tnRoutingNumber = tnRoutingNumber;
	}

	public ForwardGVNS(byte[] b) {
		super();
		decodeElement(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {
		// Add kength ? || b.length != xxx
		if (b == null) {
			throw new IllegalArgumentException("byte[] must  not be null");
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		this.opServiceProvider = new OriginatingParticipatingServiceProvider();
		this.gvnsUserGroup = new GVNSUserGroup();
		this.tnRoutingNumber = new TerminatingNetworkRoutingNumber();

		int count = 0;
		count += this.opServiceProvider.decodeElement(bis);
		count += this.gvnsUserGroup.decodeElement(bis);
		count += this.tnRoutingNumber.decodeElement(bis);

		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {

		if (this.opServiceProvider == null) {
			throw new IllegalArgumentException("OriginatingParticipatingServiceProvider must not be null.");
		}
		if (this.gvnsUserGroup == null) {
			throw new IllegalArgumentException("GVNSUserGruop must not be null.");
		}
		if (this.tnRoutingNumber == null) {
			throw new IllegalArgumentException("TerminatingNetworkRoutingNumber must not be null.");
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(this.opServiceProvider.encodeElement());
		bos.write(this.gvnsUserGroup.encodeElement());
		bos.write(this.tnRoutingNumber.encodeElement());
		return bos.toByteArray();
	}

	public OriginatingParticipatingServiceProvider getOpServiceProvider() {
		return opServiceProvider;
	}

	public void setOpServiceProvider(OriginatingParticipatingServiceProvider opServiceProvider) {
		this.opServiceProvider = opServiceProvider;
	}

	public GVNSUserGroup getGvnsUserGroup() {
		return gvnsUserGroup;
	}

	public void setGvnsUserGroup(GVNSUserGroup gvnsUserGroup) {
		this.gvnsUserGroup = gvnsUserGroup;
	}

	public TerminatingNetworkRoutingNumber getTnRoutingNumber() {
		return tnRoutingNumber;
	}

	public void setTnRoutingNumber(TerminatingNetworkRoutingNumber tnRoutingNumber) {
		this.tnRoutingNumber = tnRoutingNumber;
	}
}
