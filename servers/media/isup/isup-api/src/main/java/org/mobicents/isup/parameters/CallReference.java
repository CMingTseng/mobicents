/**
 * Start time:12:03:25 2009-07-23<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.isup.parameters;

/**
 * Start time:12:03:25 2009-07-23<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public interface CallReference extends ISUPParameter {
	public static final int _PARAMETER_CODE = 0x01;

	public int getCallIdentity();

	public void setCallIdentity(int callIdentity);

	public int getSignalingPointCode();

	public void setSignalingPointCode(int signalingPointCode);
}
