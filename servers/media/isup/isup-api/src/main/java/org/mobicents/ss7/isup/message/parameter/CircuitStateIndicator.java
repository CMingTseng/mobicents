/**
 * Start time:12:22:51 2009-07-23<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.ss7.isup.message.parameter;

/**
 * Start time:12:22:51 2009-07-23<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public interface CircuitStateIndicator extends ISUPParameter {
	public static final int _PARAMETER_CODE = 0x26;

	// FIXME: Q.763 3.14 - Oleg is this correct?

	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state "0"
	 * : transient
	 */
	public static int _MBS_NPS_TRANSIENT = 0;

	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state "0"
	 * : unequipped
	 */
	public static int _MBS_NPS_UNEQUIPED = 3;
	
	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state
	 * ~"0" : no blocking - active
	 */
	public static int _MBS_NO_BLOCKING = 0;

	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state
	 * ~"0" : localy blocked
	 */
	public static int _MBS_LOCALY_BLOCKED = 1;
	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state
	 * ~"0" : remotely blocked blocked
	 */
	public static int _MBS_REMOTELY_BLOCKED = 2;

	/**
	 * See Q.763 3.14 Maintenance blocking state - for call processing state
	 * ~"0" : locally and remotely blocked
	 */
	public static int _MBS_LAR_BLOCKED = 3;

	/**
	 * See Q.763 3.14 Call processing state : circuit incoming busy
	 */
	public static int _CPS_CIB = 1;
	/**
	 * See Q.763 3.14 Call processing state : circuit outgoing busy
	 */
	public static int _CPS_COB = 2;
	/**
	 * See Q.763 3.14 Call processing state : idle
	 */
	public static int _CPS_IDLE = 3;

	/**
	 * See Q.763 3.14 Hardware blocking state (Note: if this does not equal "0"
	 * Call Processing State must be equal to "3") : no blocking (active)
	 */
	public static int _HBS_NO_BLOCKING = 0;
	/**
	 * See Q.763 3.14 Hardware blocking state (Note: if this does not equal "0"
	 * Call Processing State must be equal to "3") : locally blocked
	 */
	public static int _HBS_LOCALY_BLOCKED = 1;
	/**
	 * See Q.763 3.14 Hardware blocking state (Note: if this does not equal "0"
	 * Call Processing State must be equal to "3") : remotely blocked
	 */
	public static int _HBS_REMOTELY_BLOCKED = 2;
	/**
	 * See Q.763 3.14 Hardware blocking state (Note: if this does not equal "0"
	 * Call Processing State must be equal to "3") : locally and remotely
	 * blocked
	 */
	public static int _HBS_LAR_BLOCKED = 3;

	public byte[] getCircuitState();

	public void setCircuitState(byte[] circuitState) throws IllegalArgumentException;

	public byte createCircuitState(int MBS, int CPS, int HBS);

	public int getCallProcessingState(byte b);

	public int getHardwareBlockingState(byte b);

	public int getMaintenanceBlockingState(byte b);

}
