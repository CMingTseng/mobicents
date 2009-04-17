package org.mobicents.slee.resource.diameter.sh.server.events;

import net.java.slee.resource.diameter.base.events.avp.AvpNotAllowedException;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.sh.client.events.avp.CurrentLocationType;
import net.java.slee.resource.diameter.sh.client.events.avp.DataReferenceType;
import net.java.slee.resource.diameter.sh.client.events.avp.DiameterShAvpCodes;
import net.java.slee.resource.diameter.sh.client.events.avp.IdentitySetType;
import net.java.slee.resource.diameter.sh.client.events.avp.RequestedDomainType;
import net.java.slee.resource.diameter.sh.client.events.avp.UserIdentityAvp;
import net.java.slee.resource.diameter.sh.server.events.UserDataRequest;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;
import org.mobicents.slee.resource.diameter.base.events.avp.DiameterAvpImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.DiameterShMessageImpl;
import org.mobicents.slee.resource.diameter.sh.client.events.avp.UserIdentityAvpImpl;

/**
 * 
 * UserDataRequestImpl.java
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class UserDataRequestImpl extends DiameterShMessageImpl implements UserDataRequest {

  private static transient Logger logger = Logger.getLogger(UserDataRequestImpl.class);

  public UserDataRequestImpl(Message msg)
  {
    super(msg);

    msg.setRequest(true);

    super.longMessageName = "User-Data-Request";
    super.shortMessageName = "UDR";
  }

  public CurrentLocationType getCurrentLocation()
  {
    if (hasCurrentLocation())
    {
      try
      {
        return CurrentLocationType.fromInt(super.message.getAvps().getAvp(DiameterShAvpCodes.CURRENT_LOCATION, 10415L).getInteger32());
      }
      catch (AvpDataException e) {
        logger.error( "Unable to decode Current-Location AVP contents.", e );
      }
    }

    return null;
  }

  public DataReferenceType[] getDataReferences()
  {
    AvpSet set = super.message.getAvps().getAvps(DiameterShAvpCodes.DATA_REFERENCE, 10415L);
    if(set == null )
    {
      return null;
    }
    else
    {
      DataReferenceType[] returnValue = new DataReferenceType[set.size()];
      int counter = 0;
  
      for(Avp raw:set)
      {
        try
        {
          returnValue[counter++] = DataReferenceType.fromInt(raw.getInteger32());
        }
        catch (AvpDataException e) {
          logger.error( "Unable to decode Experimental-Result AVP contents.", e );
  
          return null;
        }
      }
  
      return returnValue;
    }
  }

  public IdentitySetType getIdentitySet()
  {
    if (hasIdentitySet())
    {
      try
      {
        return IdentitySetType.fromInt(super.message.getAvps().getAvp(DiameterShAvpCodes.DATA_REFERENCE, 10415L).getInteger32());
      }
      catch (AvpDataException e) {
        logger.error( "Unable to decode Data-Reference AVP contents.", e );
      }
    }

    return null;
  }

  public RequestedDomainType getRequestedDomain()
  {
    if (hasRequestedDomain())
    {
      try
      {
        return RequestedDomainType.fromInt(super.message.getAvps().getAvp(DiameterShAvpCodes.REQUESTED_DOMAIN, 10415L).getInteger32());
      }
      catch (AvpDataException e) {
        logger.error( "Unable to decode Requested-Domain AVP contents.", e );
      }
    }

    return null;
  }

  public String getServerName()
  {
    try
    {
      return hasServerName() ? super.message.getAvps().getAvp(DiameterShAvpCodes.SERVER_NAME, 10415L).getUTF8String() : null;
    }
    catch ( AvpDataException e ) {
      logger.error( "Unable to decode Server-Name AVP contents.", e );
    }
  
    return null;
  }

  public byte[][] getServiceIndications()
  {
    AvpSet set = super.message.getAvps().getAvps(DiameterShAvpCodes.SERVICE_INDICATION, 10415L);
  
    if(set == null)
    {
      return null;
    }
    else
    {
      byte[][] returnValue = new byte[set.size()][];
      int counter = 0;
  
      for(Avp raw:set)
      {
        try
        {
          returnValue[counter++] = raw.getRaw();
        }
        catch (AvpDataException e) {
          logger.error( "Unable to decode Service-Indications AVP contents.", e );
  
          return null;
        }
      }
  
      return returnValue;
    }
  }

  public UserIdentityAvp getUserIdentity()
  {
    if (hasUserIdentity())
    {
      try
      {
        Avp rawAvp = super.message.getAvps().getAvp(DiameterShAvpCodes.USER_IDENTITY);
    
        UserIdentityAvpImpl a = new UserIdentityAvpImpl(rawAvp.getCode(), rawAvp.getVendorId(), rawAvp.isMandatory() ? 1 : 0, rawAvp.isEncrypted() ? 1 : 0, new byte[]{});
        
        for(Avp subAvp : rawAvp.getGrouped())
        {
          try
          {
            a.setExtensionAvps( new DiameterAvp[]{new DiameterAvpImpl(subAvp.getCode(), subAvp.getVendorId(), subAvp.isMandatory() ? 1 : 0, subAvp.isEncrypted() ? 1 : 0, subAvp.getRaw(), null)} );
          }
          catch ( AvpNotAllowedException e ) {
            logger.error( "Unable to add child AVPs to User-Identity AVP.", e );
          } 
        }
        
        return a;
      }
      catch (AvpDataException e) {
        logger.error( "Unable to decode User-Identity AVP contents.", e );
      }
    }
    
    return null;
  }

  public boolean hasCurrentLocation()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.CURRENT_LOCATION, 10415L) != null;
  }

  public boolean hasIdentitySet()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.IDENTITY_SET) != null;
  }

  public boolean hasRequestedDomain()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.REQUESTED_DOMAIN) != null;
  }

  public boolean hasServerName()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.SERVER_NAME, 10415L) != null;
  }

  public boolean hasUserIdentity()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.USER_IDENTITY, 10415L) != null;
  }

  public void setCurrentLocation(CurrentLocationType currentLocation)
  {
    if(hasCurrentLocation())
    {
      throw new IllegalStateException("AVP Current-Location is already present in message and cannot be overwritten.");
    }
    else
    {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.CURRENT_LOCATION, 10415L);
      int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
      int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

      super.message.getAvps().addAvp(DiameterShAvpCodes.CURRENT_LOCATION, currentLocation.getValue(), avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1, false);
    }
  }

  public void setDataReference(DataReferenceType dataReference)
  {
    AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.DATA_REFERENCE, 10415L);
    int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
    int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

    super.message.getAvps().addAvp(avpRep.getCode(), dataReference.getValue(), avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
  }

  public void setDataReferences(DataReferenceType[] dataReferences)
  {
    AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.DATA_REFERENCE, 10415L);
    int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
    int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

    for(DataReferenceType dataReference : dataReferences)
    {
      super.message.getAvps().addAvp(avpRep.getCode(), dataReference.getValue(), avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
    }
  }

  public void setIdentitySet(IdentitySetType identitySet)
  {
    if(hasIdentitySet())
    {
      throw new IllegalStateException("AVP Identity-Set is already present in message and cannot be overwritten.");
    }
    else
    {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.IDENTITY_SET, 10415L);
      int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
      int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

      super.message.getAvps().addAvp(avpRep.getCode(), identitySet.getValue(), avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
    }
  }

  public void setRequestedDomain(RequestedDomainType requestedDomain)
  {
    if(hasRequestedDomain())
    {
      throw new IllegalStateException("AVP Requested-Domain is already present in message and cannot be overwritten.");
    }
    else
    {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.REQUESTED_DOMAIN, 10415L);
      int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
      int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

      super.message.getAvps().addAvp(avpRep.getCode(), requestedDomain.getValue(), avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
    }
  }

  public void setServerName(String serverName)
  {
    if(hasServerName())
    {
      throw new IllegalStateException("AVP Server-Name is already present in message and cannot be overwritten.");
    }
    else
    {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.SERVER_NAME, 10415L);
      int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
      int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

      super.message.getAvps().addAvp(avpRep.getCode(), serverName, avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1, false);
    }
  }

  public void setServiceIndication(byte[] serviceIndication)
  {
    AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.SERVICE_INDICATION, 10415L);
    int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
    int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

    super.message.getAvps().addAvp(avpRep.getCode(), serviceIndication, avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
  }

  public void setServiceIndications(byte[][] serviceIndications)
  {
    AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.SERVICE_INDICATION, 10415L);
    int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
    int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

    for(byte[] serviceIndication : serviceIndications)
    {
      super.message.getAvps().addAvp(avpRep.getCode(), serviceIndication, avpRep.getVendorId(), mandatoryAvp == 1, protectedAvp == 1);
    }
  }

  public void setUserIdentity(UserIdentityAvp userIdentity)
  {
    if(hasUserIdentity())
    {
      throw new IllegalStateException("AVP User-Identity is already present in message and cannot be overwritten.");
    }
    else
    {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.USER_IDENTITY, 10415L);
      int mandatoryAvp = avpRep.getRuleMandatory().equals("mustnot") || avpRep.getRuleMandatory().equals("shouldnot") ? 0 : 1;
      // int protectedAvp = avpRep.getRuleProtected().equals("must") ? 1 : 0;

      // FIXME: Alexandre: Need to specify protected!
      super.setAvpAsGroup(avpRep.getCode(), avpRep.getVendorId(), userIdentity.getExtensionAvps(), mandatoryAvp == 1, false);
    }
  }

}
