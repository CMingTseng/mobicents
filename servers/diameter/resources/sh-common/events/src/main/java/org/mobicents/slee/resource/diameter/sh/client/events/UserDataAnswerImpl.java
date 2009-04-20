package org.mobicents.slee.resource.diameter.sh.client.events;

import java.io.ByteArrayInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import net.java.slee.resource.diameter.base.events.avp.DiameterAvpCodes;
import net.java.slee.resource.diameter.base.events.avp.ExperimentalResultAvp;
import net.java.slee.resource.diameter.sh.client.events.UserDataAnswer;
import net.java.slee.resource.diameter.sh.client.events.avp.DiameterShAvpCodes;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.Message;
import org.mobicents.slee.resource.diameter.base.events.avp.ExperimentalResultAvpImpl;

public class UserDataAnswerImpl extends DiameterShMessageImpl implements UserDataAnswer {

  private static transient Logger logger = Logger.getLogger(UserDataAnswerImpl.class);

  private static DocumentBuilder docBuilder = null;

  static
  {
    try
    {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = schemaFactory.newSchema(UserDataAnswerImpl.class.getClassLoader().getResource("ShDataType.xsd"));
  
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setSchema( schema );
  
      docBuilder = factory.newDocumentBuilder();
    }
    catch (Exception e) {
      logger.error( "Failed to initialize Sh-Data schema validator. No validation will be available.", e );
    }
  }
  
  public UserDataAnswerImpl(Message msg)
  {
    super(msg);
    msg.setRequest(false);
    super.longMessageName = "User-Data-Answer";
    super.shortMessageName = "UDA";
  }

  public boolean hasUserData()
  {
    return super.message.getAvps().getAvp(DiameterShAvpCodes.USER_DATA) != null;
  }

  public String getUserData()
  {
    if (hasUserData())
    {
      try
      {
        return new String(super.message.getAvps().getAvp(DiameterShAvpCodes.USER_DATA).getRaw());
      }
      catch (AvpDataException e) {
        logger.error( "Unable to decode User-Data AVP contents.", e );
      }
    }

    return null;
  }

  public void setUserData(byte[] userData)
  {
    if(docBuilder != null)
    {
      try
      {
        docBuilder.parse( new ByteArrayInputStream(userData) );
      }
      catch (Exception e) {
        logger.error( "Failure while validating User-Data:", e );
        return;
      }
    }

    super.message.getAvps().removeAvp(DiameterShAvpCodes.USER_DATA);
    super.message.getAvps().addAvp(DiameterShAvpCodes.USER_DATA, userData, 10415L, true, false);
  }

  public ExperimentalResultAvp getExperimentalResult()
  {
    ExperimentalResultAvp avp = null;
    
    try
    {
      Avp rawAvp = super.message.getAvps().getAvp(DiameterAvpCodes.EXPERIMENTAL_RESULT);
     
      if(rawAvp != null)
      {
        Avp ercAvp = rawAvp.getGrouped().getAvp( DiameterAvpCodes.EXPERIMENTAL_RESULT_CODE );
        Avp vidAvp = rawAvp.getGrouped().getAvp( DiameterAvpCodes.VENDOR_ID );
        
        avp = new ExperimentalResultAvpImpl(rawAvp.getCode(), rawAvp.getVendorId(), rawAvp.isMandatory() ? 1 : 0, rawAvp.isEncrypted() ? 1 : 0, new byte[]{});
      
        if(ercAvp != null)
        {
          avp.setExperimentalResultCode( ercAvp.getUnsigned32() );
        }
        
        if(vidAvp != null)
        {
          avp.setVendorId( vidAvp.getUnsigned32() );
        }
      }
    }
    catch (AvpDataException e) {
      logger.error( "Unable to decode Experimental-Result AVP contents.", e );
    }

    return avp;
  }

  public boolean hasExperimentalResult()
  {
    return super.message.getAvps().getAvp(DiameterAvpCodes.EXPERIMENTAL_RESULT) != null;
  }

  public void setExperimentalResult( ExperimentalResultAvp experimentalResult )
  {
    super.setAvpAsGroup(experimentalResult.getCode(), experimentalResult.getExtensionAvps(), experimentalResult.getMandatoryRule() == 1, true);
  }
}
