package org.mobicents.slee.resource.diameter.sh.client.events;

import net.java.slee.resource.diameter.base.events.avp.AuthSessionStateType;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvpCodes;
import net.java.slee.resource.diameter.sh.client.events.DiameterShMessage;
import net.java.slee.resource.diameter.sh.client.events.avp.DiameterShAvpCodes;
import net.java.slee.resource.diameter.sh.client.events.avp.SupportedFeaturesAvp;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;

import org.mobicents.slee.resource.diameter.sh.client.events.avp.SupportedFeaturesAvpImpl;

public class DiameterShMessageImpl extends DiameterMessageImpl implements DiameterShMessage {

  private static transient Logger logger = Logger.getLogger(DiameterShMessageImpl.class);

  protected String longMessageName = null;
	protected String shortMessageName = null;
	
	public DiameterShMessageImpl(Message msg)
	{
		super(msg);
	}

	@Override
	public String getLongName()
	{
		return this.longMessageName;
	}

	@Override
	public String getShortName()
	{
		return this.shortMessageName;
	}

	public AuthSessionStateType getAuthSessionState()
	{
	  try
    {
      return hasAuthSessionState() ? AuthSessionStateType.fromInt(super.message.getAvps().getAvp(DiameterAvpCodes.AUTH_SESSION_STATE).getInteger32()) : null;
    }
    catch ( AvpDataException e ) {
      logger.error( "Unable to decode Auth-Session-State AVP contents.", e );
    }
    
    return null;
	}

	public SupportedFeaturesAvp[] getSupportedFeatureses()
	{
		AvpSet set = super.message.getAvps().getAvps(DiameterShAvpCodes.SUPPORTED_FEATURES);
		SupportedFeaturesAvp[] returnValue = new SupportedFeaturesAvp[set.size()];
		int counter=0;

		for(Avp rawAvp : set)
		{
			try
			{
				returnValue[counter++] = new SupportedFeaturesAvpImpl(rawAvp.getCode(), rawAvp.getVendorId(), rawAvp.isMandatory() ? 1 : 0, rawAvp.isEncrypted() ? 1 : 0, rawAvp.getRaw());
			}
			catch (AvpDataException e) {
	      logger.error( "Unable to decode Supported-Features AVP contents.", e );
			}
		}
		
		return returnValue;
	}

	public boolean hasAuthSessionState()
	{
		return super.message.getAvps().getAvp(DiameterAvpCodes.AUTH_SESSION_STATE) != null;
	}

	public void setAuthSessionState(AuthSessionStateType authSessionState)
	{
		super.setAvpAsUInt32(DiameterAvpCodes.AUTH_SESSION_STATE, authSessionState.getValue(), true, true);
	}

	public void setSupportedFeatures(SupportedFeaturesAvp supportedFeatures)
	{
		super.setAvpAsGroup(supportedFeatures.getCode(), new SupportedFeaturesAvp[]{supportedFeatures}, true, true);
	}

	public void setSupportedFeatureses(SupportedFeaturesAvp[] supportedFeatureses)
	{
		super.setAvpAsGroup(DiameterShAvpCodes.SUPPORTED_FEATURES, supportedFeatureses, true, true);
	}

	public SupportedFeaturesAvp getSupportedFeatures()
	{
		Avp rawAvp = super.message.getAvps().getAvp(DiameterShAvpCodes.SUPPORTED_FEATURES);
		
		if(rawAvp != null)
		{
			try
			{
				return new SupportedFeaturesAvpImpl(rawAvp.getCode(), rawAvp.getVendorId(), rawAvp.isMandatory() ? 1 : 0, rawAvp.isEncrypted() ? 1 : 0, rawAvp.getRaw());
			}
			catch (AvpDataException e) {
        logger.error( "Unable to decode Supported-Features AVP contents.", e );
			}
		}
			
		return null;
	}

}
