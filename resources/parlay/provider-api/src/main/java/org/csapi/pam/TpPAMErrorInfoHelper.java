package org.csapi.pam;


/**
 *	Generated from IDL definition of struct "TpPAMErrorInfo"
 *	@author JacORB IDL compiler 
 */

public final class TpPAMErrorInfoHelper
{
	private static org.omg.CORBA.TypeCode _type = null;
	public static org.omg.CORBA.TypeCode type ()
	{
		if (_type == null)
		{
			_type = org.omg.CORBA.ORB.init().create_struct_tc(org.csapi.pam.TpPAMErrorInfoHelper.id(),"TpPAMErrorInfo",new org.omg.CORBA.StructMember[]{new org.omg.CORBA.StructMember("Cause", org.csapi.pam.TpPAMErrorCauseHelper.type(), null),new org.omg.CORBA.StructMember("ErrorData", org.csapi.pam.TpPAMNotificationInfoHelper.type(), null)});
		}
		return _type;
	}

	public static void insert (final org.omg.CORBA.Any any, final org.csapi.pam.TpPAMErrorInfo s)
	{
		any.type(type());
		write( any.create_output_stream(),s);
	}

	public static org.csapi.pam.TpPAMErrorInfo extract (final org.omg.CORBA.Any any)
	{
		return read(any.create_input_stream());
	}

	public static String id()
	{
		return "IDL:org/csapi/pam/TpPAMErrorInfo:1.0";
	}
	public static org.csapi.pam.TpPAMErrorInfo read (final org.omg.CORBA.portable.InputStream in)
	{
		org.csapi.pam.TpPAMErrorInfo result = new org.csapi.pam.TpPAMErrorInfo();
		result.Cause=org.csapi.pam.TpPAMErrorCauseHelper.read(in);
		result.ErrorData=org.csapi.pam.TpPAMNotificationInfoHelper.read(in);
		return result;
	}
	public static void write (final org.omg.CORBA.portable.OutputStream out, final org.csapi.pam.TpPAMErrorInfo s)
	{
		org.csapi.pam.TpPAMErrorCauseHelper.write(out,s.Cause);
		org.csapi.pam.TpPAMNotificationInfoHelper.write(out,s.ErrorData);
	}
}
