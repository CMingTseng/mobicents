package org.csapi.cm;

/**
 *	Generated from IDL definition of exception "P_UNKNOWN_VPRN"
 *	@author JacORB IDL compiler 
 */

public final class P_UNKNOWN_VPRNHolder
	implements org.omg.CORBA.portable.Streamable
{
	public org.csapi.cm.P_UNKNOWN_VPRN value;

	public P_UNKNOWN_VPRNHolder ()
	{
	}
	public P_UNKNOWN_VPRNHolder(final org.csapi.cm.P_UNKNOWN_VPRN initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return org.csapi.cm.P_UNKNOWN_VPRNHelper.type ();
	}
	public void _read(final org.omg.CORBA.portable.InputStream _in)
	{
		value = org.csapi.cm.P_UNKNOWN_VPRNHelper.read(_in);
	}
	public void _write(final org.omg.CORBA.portable.OutputStream _out)
	{
		org.csapi.cm.P_UNKNOWN_VPRNHelper.write(_out, value);
	}
}
