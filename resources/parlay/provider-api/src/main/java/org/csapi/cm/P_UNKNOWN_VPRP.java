package org.csapi.cm;

/**
 *	Generated from IDL definition of exception "P_UNKNOWN_VPRP"
 *	@author JacORB IDL compiler 
 */

public final class P_UNKNOWN_VPRP
	extends org.omg.CORBA.UserException
{
	public P_UNKNOWN_VPRP()
	{
		super(org.csapi.cm.P_UNKNOWN_VPRPHelper.id());
	}

	public java.lang.String ExtraInformation;
	public P_UNKNOWN_VPRP(java.lang.String _reason,java.lang.String ExtraInformation)
	{
		super(org.csapi.cm.P_UNKNOWN_VPRPHelper.id()+""+_reason);
		this.ExtraInformation = ExtraInformation;
	}
	public P_UNKNOWN_VPRP(java.lang.String ExtraInformation)
	{
		this.ExtraInformation = ExtraInformation;
	}
}
