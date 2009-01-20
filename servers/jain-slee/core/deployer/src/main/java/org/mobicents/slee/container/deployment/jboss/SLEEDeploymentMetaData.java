package org.mobicents.slee.container.deployment.jboss;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SLEEDeploymentMetaData// extends ServiceDynamicMBeanSupport
{
  protected static enum ComponentType {
    
    UNKNOWN(-1),
    
    EVENT(1),
    SBB(2),
    PROFILE(3),
    RATYPE(4),
    RA(5),
    SERVICE(6),
    DU(9);
    
    private ComponentType(int value)
    {
      this.value = value;
    }
    
    int value;
    
    public int getValue()
    {
      return this.value;
    }
    
  }

  // Variables -----------------------------------------------------
  
  protected ComponentType componentType = ComponentType.UNKNOWN;
  
  protected ArrayList<String> duContents = new ArrayList<String>();
  
  // Constructors --------------------------------------------------

  public SLEEDeploymentMetaData(DeploymentUnit du)
  {
    InputStream is = null;
    
    if((is = du.getResourceClassLoader().getResourceAsStream( "META-INF/deployable-unit.xml" )) != null)
    {
      // This is a SLEE DU
      this.componentType = ComponentType.DU;
      
      this.parseDUContents(is);
    }
    else if(du.getResourceClassLoader().getResourceAsStream( "META-INF/event-jar.xml" ) != null)
    {
      this.componentType = ComponentType.EVENT;
    }
    else if(du.getResourceClassLoader().getResourceAsStream( "META-INF/sbb-jar.xml" ) != null)
    {
      this.componentType = ComponentType.SBB;
    }
    else if(du.getResourceClassLoader().getResourceAsStream( "META-INF/profile-spec-jar.xml" ) != null)
    {
      this.componentType = ComponentType.PROFILE;
    }
    else if(du.getResourceClassLoader().getResourceAsStream( "META-INF/resource-adaptor-type-jar.xml" ) != null)
    {
      this.componentType = ComponentType.RATYPE;
    }
    else if(du.getResourceClassLoader().getResourceAsStream( "META-INF/resource-adaptor-jar.xml" ) != null)
    {
      this.componentType = ComponentType.RA;
    }
    //FIXME: need to find out about service's!
  }
  
  private void parseDUContents(InputStream is)
  {
    try
    {
      // Parse the DU to see which jars we should process
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      
      // Set the Entity Resolver
      docBuilder.setEntityResolver(SLEEDeployerEntityResolver.INSTANCE);
      
      Document doc = docBuilder.parse (is);
      
      NodeList nodeList = doc.getDocumentElement().getChildNodes();
      
      for( int i = 0; i < nodeList.getLength(); i++ )
      {
        if(nodeList.item(i) instanceof Element)
        {
          Element elem = (Element) nodeList.item(i);
          
          duContents.add( elem.getTextContent() );
        }
      }
    }
    catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }    
  }
}
