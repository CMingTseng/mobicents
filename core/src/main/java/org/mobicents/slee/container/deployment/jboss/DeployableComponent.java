package org.mobicents.slee.container.deployment.jboss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.slee.ComponentID;
import javax.slee.EventTypeID;
import javax.slee.profile.ProfileSpecificationID;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.component.ComponentKey;
import org.mobicents.slee.container.component.EventTypeIDImpl;
import org.mobicents.slee.container.component.MobicentsEventTypeDescriptorInternalImpl;
import org.mobicents.slee.container.component.MobicentsSbbDescriptorInternalImpl;
import org.mobicents.slee.container.component.ProfileSpecificationDescriptorImpl;
import org.mobicents.slee.container.component.ResourceAdaptorEntityBinding;
import org.mobicents.slee.container.component.ResourceAdaptorIDImpl;
import org.mobicents.slee.container.component.SbbIDImpl;
import org.mobicents.slee.container.component.ServiceIDImpl;
import org.mobicents.slee.container.component.deployment.EventTypeDeploymentDescriptorParser;
import org.mobicents.slee.container.component.deployment.ProfileSpecificationDescriptorParser;
import org.mobicents.slee.container.component.deployment.ResourceAdaptorDescriptorParser;
import org.mobicents.slee.container.component.deployment.ResourceAdaptorTypeDescriptorParser;
import org.mobicents.slee.container.component.deployment.SbbDeploymentDescriptorParser;
import org.mobicents.slee.container.management.xml.XMLConstants;
import org.mobicents.slee.container.management.xml.XMLUtils;
import org.mobicents.slee.resource.ResourceAdaptorDescriptorImpl;
import org.mobicents.slee.resource.ResourceAdaptorTypeDescriptorImpl;
import org.mobicents.slee.resource.ResourceAdaptorTypeIDImpl;

/**
 * This class represents a SLEE Deployable Component such as a Profile Specification,
 * Event Type Definition, RA Type, Resource Adaptor, SBB or Service. It also contains
 * the dependencies and the install/uninstall actions needed for that component.
 * 
 * @author Alexandre Mendon�a
 * @version 1.0
 */
public class DeployableComponent
{
  // The logger.
  private static Logger logger = Logger.getLogger( DeployableComponent.class );
  
  public final static int PROFILESPEC_COMPONENT = 1;
  public final static int EVENTTYPE_COMPONENT   = 2;
  public final static int RATYPE_COMPONENT      = 3;
  public final static int RA_COMPONENT          = 4;
  public final static int SBB_COMPONENT         = 5;
  public final static int SERVICE_COMPONENT     = 6;
  
  // The deployable component DeploymentInfo.
  private DeploymentInfo di;
  
  // The ID of the component.
  private ComponentID componentID;
  
  // The dependencies for this Component.
  private Collection<String> dependencies = new ArrayList<String>();
  
  // The actions needed to perform installation for this component.
  private Collection<String[]> installActions = new ArrayList<String[]>();
  
  // The actions needed to perform installation for this component.
  private Collection<String[]> uninstallActions = new ArrayList<String[]>();
  
  // The key identifying the component (type[name#vendor#version]).
  private String componentKey;
  
  // An indicator for the component type.
  private int componentType = -1;
  
  // The components inside this component
  private Collection<DeployableComponent> subComponents = new ArrayList<DeployableComponent>();
  
  /**
   * Private constructor for the sub-components.
   * @param dc the base DeployableComponent
   * @throws Exception
   */
  private DeployableComponent( DeployableComponent dc ) throws Exception
  {
    this.di = dc.di;
    
    // We want no sub-sub-components...
    this.subComponents = null;
  }
  
  /**
   * Constructor.
   * @param di the DeploymentInfo for this component.
   * @throws Exception
   */
  public DeployableComponent( DeploymentInfo di ) throws Exception
  {
    this.di = di;
    
    // Parse the component descriptor to obtain dependencies.
    this.subComponents = parseDescriptor();
  }

  /**
   * Getter for this component dependencies.
   * @return a Collection<String> with dependencies IDs.
   */
  public Collection<String> getDependencies()
  {
    return this.dependencies;
  }
  

  /**
   * Method for checking if the component is deployable, ie, meets the pre-reqs.
   * @param deployedComponents a Collection<String> with the IDs of the already deployed components.
   * @return true if it can be deployed.
   */
  public boolean isDeployable(Collection<String> deployedComponents)
  {
    return deployedComponents.containsAll( dependencies );
  }
  
  /**
   * Getter for the Component Key String.
   * @return a String with the component key (type[name#vendor#version]).
   */
  public String getComponentKey()
  {
    return this.componentKey;
  }
  
  /**
   * Parser for the deployment descriptor. Minimal version obtained from Container.
   * @return a String containing the Component ID. 
   * @throws IOException
   */
  private Collection<DeployableComponent> parseDescriptor() throws IOException
  {
    if( logger.isDebugEnabled() )
      logger.debug( "Parsing Descriptor for " + di.url.toString() );
    
    Collection<DeployableComponent> deployableComponents = new ArrayList<DeployableComponent>();
    
    // Special case for the services...
    if( di.shortName.endsWith( ".xml" ) )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing Service Descriptor." );
      
      try
      {
        // Parse the descriptor
        Document doc2 = new SAXReader().read( di.url );
        
        List<Element> nodeList = ((Node)doc2.getRootElement()).selectNodes( "service" );
        
        for( Element serviceNode : nodeList )
        {
          DeployableComponent dc = new DeployableComponent( this );
          
          String serviceName = serviceNode.selectSingleNode( "service-name" ).getText(); 
          String serviceVendor = serviceNode.selectSingleNode( "service-vendor" ).getText();
          String serviceVersion = serviceNode.selectSingleNode( "service-version" ).getText();

          dc.componentID = new ServiceIDImpl( new ComponentKey( serviceName, serviceVendor, serviceVersion ) );
          
          dc.componentKey = dc.componentID.toString();
          
          dc.componentType = SERVICE_COMPONENT;
          
          if( logger.isDebugEnabled() )
            logger.debug( "Component ID: " + dc.componentKey );
          
          String rootSbbName = serviceNode.selectSingleNode( XMLConstants.ROOT_SBB_ND + "/" + XMLConstants.SBB_NAME_ND ).getText();
          String rootSbbVendor = serviceNode.selectSingleNode( XMLConstants.ROOT_SBB_ND + "/" + XMLConstants.SBB_VENDOR_ND ).getText();
          String rootSbbVersion = serviceNode.selectSingleNode( XMLConstants.ROOT_SBB_ND + "/" + XMLConstants.SBB_VERSION_ND ).getText();
          
          String rootSbb = new SbbIDImpl( new ComponentKey(rootSbbName, rootSbbVendor, rootSbbVersion) ).toString();

          if( logger.isDebugEnabled() )
          {
            logger.debug( "------------------------------ Dependencies ------------------------------" );
            logger.debug( rootSbb );
            logger.debug( "--------------------------- End of Dependencies --------------------------" );
          }

          dc.dependencies.add( rootSbb );
          
          dc.installActions.add( new String[] {"-activateService", dc.componentKey} );

          dc.uninstallActions.add( new String[] {"-deactivateService", dc.componentKey} );
          
          deployableComponents.add( dc );
        }
        
        return deployableComponents;
      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }
    }

    
    JarFile componentJarFile = new JarFile( di.url.toString().replaceAll( "file:", "" ) );
    
    JarEntry descriptorXML = null;

    org.w3c.dom.Document doc = null;
    
    // Determine whether the type of this instance is an sbb, event, RA type, etc.
    if( ( descriptorXML = componentJarFile.getJarEntry( "META-INF/sbb-jar.xml" ) ) != null )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing SBB Descriptor." );

      try
      {
        // Create the XML document from file
        doc = XMLUtils.parseDocument( componentJarFile.getInputStream( descriptorXML ), true );
        org.w3c.dom.Element sbbJarNode = doc.getDocumentElement();

        // Get a list of the SBBs in the descriptor
        List<org.w3c.dom.Element> sbbNodes = XMLUtils.getAllChildElements( sbbJarNode, XMLConstants.SBB_ND );
        
        if( sbbNodes.size() == 0 )
        {
          logger.warn( "The " + componentJarFile.getName() + " deployment descriptor contains no sbb definitions" );
          return null;
        }

        for( int i = sbbNodes.size() - 1; i >= 0; i-- )
        {
          DeployableComponent dc = new DeployableComponent( this );
          
          dc.componentType = SBB_COMPONENT;
                    
          org.w3c.dom.Element sbbNode = sbbNodes.get( i );
          SbbDeploymentDescriptorParser parser = new SbbDeploymentDescriptorParser();
          MobicentsSbbDescriptorInternalImpl descriptor = (MobicentsSbbDescriptorInternalImpl) parser.parseSbbComponent( sbbNode, new MobicentsSbbDescriptorInternalImpl() );

          // Get the Component ID
          dc.componentID = descriptor.getID();

          // Get the Component Key
          dc.componentKey = dc.componentID.toString();

          if( logger.isDebugEnabled() )
          {
            logger.debug( "Component ID: " + dc.componentKey );

            logger.debug( "------------------------------ Dependencies ------------------------------" );
          }
          
          // Get the list of RA Types in the descriptor 
          ResourceAdaptorTypeID[] rasList = descriptor.getResourceAdaptorTypes();
          
          // Iterate through the RA Types nodes          
          for( int n = 0; n < rasList.length; n++ )
          {
            // Add the RA Type to the dependencies (maybe not needed due to the link)
            dc.dependencies.add( rasList[n].toString() );
            
            if( logger.isDebugEnabled() )
              logger.debug( rasList[n] );

            // Get the entity links from the descriptor
            Iterator<ResourceAdaptorEntityBinding> raebIt = descriptor.getResourceAdaptorEntityBindings( (ResourceAdaptorTypeIDImpl) rasList[n] );
            
            while( raebIt.hasNext() )
            {
              // Generate a special identifier for the links: linkname_@_RAType[name#vendor#version]
              String raLink = raebIt.next().getResourceAdaptorEntityLink() + "_@_" + rasList[n];
              
              // Add it to dependencies
              dc.dependencies.add( raLink );
              
              if( logger.isDebugEnabled() )
                logger.debug( raLink );
            }
          }
          
          // Get the event types from descriptor...
          EventTypeID[] eventsList = descriptor.getEventTypes();
          for( int n = 0; n < eventsList.length; n++ )
          {
            dc.dependencies.add( eventsList[n].toString() );
            
            if( logger.isDebugEnabled() )
              logger.debug( eventsList[n] );
          }

          // Get the profile specifications from descriptor...
          ProfileSpecificationID[] profilesList = descriptor.getProfileSpecifications();
          for( int n = 0; n < profilesList.length; n++ )
          {
            dc.dependencies.add( profilesList[n].toString() );
            
            if( logger.isDebugEnabled() )
              logger.debug( profilesList[n] );
          }
          if( logger.isDebugEnabled() )
            logger.debug( "--------------------------- End of Dependencies --------------------------" );
          
          deployableComponents.add( dc );
        }
      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }

    }
    else if( ( descriptorXML = componentJarFile.getJarEntry( "META-INF/profile-spec-jar.xml" ) ) != null )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing Profile Specification Descriptor." );

      try
      {
        // Create the XML document from file
        doc = XMLUtils.parseDocument( componentJarFile.getInputStream( descriptorXML ), true );
        org.w3c.dom.Element profileJarNode = doc.getDocumentElement();

        // Get a list of the profile specifications in the deployable unit.
        List<org.w3c.dom.Element> profileSpecNodes = XMLUtils.getAllChildElements( profileJarNode, XMLConstants.PROFILE_SPEC_ND );
        if( profileSpecNodes.size() == 0 )
        {
          logger.warn( "The " + componentJarFile.getName() + " deployment descriptor contains no profile-spec definitions" );
          return null;
        }

        // Iterate through the profile spec nodes
        for( int i = profileSpecNodes.size() - 1; i >= 0; i-- )
        {
          DeployableComponent dc = new DeployableComponent( this );
          
          // Set Component Type
          dc.componentType = PROFILESPEC_COMPONENT;
          
          // Do the parsing...
          org.w3c.dom.Element profileSpecNode = profileSpecNodes.get( i );
          ProfileSpecificationDescriptorParser parser = new ProfileSpecificationDescriptorParser();
          ProfileSpecificationDescriptorImpl descriptor = parser.parseProfileComponent( profileSpecNode, new ProfileSpecificationDescriptorImpl() );
          
          // Get the Component ID
          dc.componentID = descriptor.getID();
          
          // Get the Component Key
          dc.componentKey = dc.componentID.toString();

          if( logger.isDebugEnabled() )
            logger.debug( "Component ID: " + dc.componentKey );
          
          deployableComponents.add( dc );
        }
        
      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }
    }
    else if( ( descriptorXML = componentJarFile.getJarEntry( "META-INF/event-jar.xml" ) ) != null )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing Event Definition Descriptor." );

      try
      {
        // Create the XML document from file
        doc = XMLUtils.parseDocument( componentJarFile.getInputStream( descriptorXML ), true );
        org.w3c.dom.Element docElement = doc.getDocumentElement();

        // Get a list of the event definitions in the deployment descriptor 
        List<org.w3c.dom.Element> nodes = XMLUtils.getAllChildElements( docElement, XMLConstants.EVENT_DEFINITION_ND );
        
        for( int i = 0; i < nodes.size(); i++ )
        {
          DeployableComponent dc = new DeployableComponent( this );
          
          // Set Component Type
          dc.componentType = EVENTTYPE_COMPONENT;
          
          // Do the parsing...
          MobicentsEventTypeDescriptorInternalImpl descriptorImpl = new MobicentsEventTypeDescriptorInternalImpl();
          org.w3c.dom.Element eventDefinitionNode = nodes.get( i );
          EventTypeDeploymentDescriptorParser parser = new EventTypeDeploymentDescriptorParser();
          parser.parse( eventDefinitionNode, descriptorImpl );

          // Get the Component ID
          dc.componentID = new EventTypeIDImpl( new ComponentKey(descriptorImpl.getName(), descriptorImpl.getVendor(), descriptorImpl.getVersion() ));
          
          // Get the Component Key
          dc.componentKey = dc.componentID.toString().substring( 0, dc.componentID.toString().indexOf( ',' ) );

          if( logger.isDebugEnabled() )
            logger.debug( "Component ID: " + dc.componentKey );
          
          deployableComponents.add( dc );
        }

      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }
    }
    else if( ( descriptorXML = componentJarFile.getJarEntry( "META-INF/resource-adaptor-type-jar.xml" ) ) != null )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing Resource Adaptor Type Descriptor." );

      try
      {
        // Create the XML document from file
        doc = XMLUtils.parseDocument( componentJarFile.getInputStream( descriptorXML ), true );
        org.w3c.dom.Element raJarNode = doc.getDocumentElement();

        // Get a list of resource adaptor types in the deployable unit.
        List<org.w3c.dom.Element> raTypeNodes = XMLUtils.getAllChildElements( raJarNode, XMLConstants.RESOURCE_ADAPTOR_TYPE_ND );
        
        if( raTypeNodes != null )
        {
          // Go through all the Resource Adaptor Type Elements          
          for( Iterator<org.w3c.dom.Element> it = raTypeNodes.iterator(); it.hasNext(); )
          {
            DeployableComponent dc = new DeployableComponent( this );
            
            // Set Component Type
            dc.componentType = RATYPE_COMPONENT;        

            // Do the parsing...
            ResourceAdaptorTypeDescriptorImpl raTypeDescriptor = new ResourceAdaptorTypeDescriptorImpl();
            ResourceAdaptorTypeDescriptorParser parser = new ResourceAdaptorTypeDescriptorParser();
            parser.parseResourceAdaptorTypeDescriptor( it.next(), raTypeDescriptor );

            // Get the Component ID
            dc.componentID = raTypeDescriptor.getID();
   
            // Get the Component Key
            dc.componentKey = dc.componentID.toString();

            if( logger.isDebugEnabled() )
            {
              logger.debug( "Component ID: " + dc.componentKey );

              logger.debug( "------------------------------ Dependencies ------------------------------" );
            }

            // Get the events this RA Type depends on
            ComponentKey[] eventsList = raTypeDescriptor.getEventTypeRefEntries();
            
            // Iterate them...
            for( int i = 0; i < eventsList.length; i++ )
            {
              if( logger.isDebugEnabled() )
                logger.debug( "EventTypeID[" + eventsList[i].toString() + "]" );
              
              // Add it to the dependencies list
              dc.dependencies.add( "EventTypeID[" + eventsList[i].toString() + "]" );
            }
            
            if( logger.isDebugEnabled() )
              logger.debug( "--------------------------- End of Dependencies --------------------------" );
            
            deployableComponents.add( dc );
          }
        }
      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }
    }
    else if( ( descriptorXML = componentJarFile.getJarEntry( "META-INF/resource-adaptor-jar.xml" ) ) != null )
    {
      if( logger.isDebugEnabled() )
        logger.debug( "Parsing Resource Adaptor Descriptor." );

      try
      {
        // Create the XML document from file
        doc = XMLUtils.parseDocument( componentJarFile.getInputStream( descriptorXML ), true );

        // Go through all the Resource Adaptor Elements
        for( Iterator<org.w3c.dom.Element> it = XMLUtils.getAllChildElements( doc.getDocumentElement(), XMLConstants.RESOURCE_ADAPTOR ).iterator(); it.hasNext(); )
        {
          DeployableComponent dc = new DeployableComponent( this );
          
          // Set Component Type
          dc.componentType = RA_COMPONENT;
          
          // Get the next element
          org.w3c.dom.Element raNode = it.next();

          // Do the parsing...
          ResourceAdaptorDescriptorImpl raDescriptor = new ResourceAdaptorDescriptorImpl();
          ResourceAdaptorDescriptorParser parser = new ResourceAdaptorDescriptorParser();
          parser.parseResourceAdaptorDescriptor( raNode, raDescriptor );

          // Set the Component ID
          dc.componentID = raDescriptor.getID();
          
          // Set the Component Key
          dc.componentKey = dc.componentID.toString();

          // Add the dependencies
          dc.dependencies.add( raDescriptor.getResourceAdaptorType().toString() );

          if( logger.isDebugEnabled() )
          {
            logger.debug( "Component ID: " + dc.componentKey );
  
            logger.debug( "------------------------------ Dependencies ------------------------------" );
            logger.debug( raDescriptor.getResourceAdaptorType() );
            logger.debug( "--------------------------- End of Dependencies --------------------------" );
          }

          deployableComponents.add( dc );
        }
      }
      catch ( Exception e )
      {
        logger.error( "", e );
      }
    }
    else
    {
      logger.warn( "No Deployment Descriptor found in the " + componentJarFile.getName() + " entry of a deployable unit." );
      return null;
    }
    
    return deployableComponents;
  }

  /**
   * Getter for Install Actions.
   * @return a Collection of String[] with the actions needed to install this component.
   */
  public Collection<String[]> getInstallActions()
  {
    return installActions;
  }

  /**
   * Getter for Uninstall Actions.
   * @return a Collection of String[] with the actions needed to uninstall this component.
   */
  public Collection<String[]> getUninstallActions()
  {
    return uninstallActions;
  }

  /**
   * Getter for component type.
   * @return an int identifying the component type.
   */
  public int getComponentType()
  {
    return componentType;
  }
  
  /**
   * Getter for the sub components.
   * @return Collection of DeployableComponents
   */
  public Collection<DeployableComponent> getSubComponents()
  {
    return this.subComponents;
  }

  /**
   * Perform extra verifications for undeployment.
   * @param du the containing Deployable Unit
   * @return true if the the component can be removed.
   */
  public boolean isUndeployable(DeployableUnit du)
  {
    SleeContainer sC = SleeContainer.lookupFromJndi();
    
    boolean result = false;

    switch( componentType )
    {
    case PROFILESPEC_COMPONENT:
      result = true;
      break;
    case EVENTTYPE_COMPONENT:
      result = true;
      break;
    case RATYPE_COMPONENT:
      // Get the DU componennts
      Collection<String> duComponents = du.getComponents();
      
      // Obtaining the RAs using this RA Type 
      HashSet<ResourceAdaptorIDImpl> raIDs = sC.getResourceAdaptorType( (ResourceAdaptorTypeID) componentID ).getResourceAdaptorIDs();

      // Check if the referring RAs are in the same DU
      for( ResourceAdaptorIDImpl raID : raIDs )
        if( !duComponents.contains( raID.getAsText() ) )
          return false;

      // All aboard! Move on..
      result = true;
      
      break;
    case RA_COMPONENT:
      result = true;
      break;
    case SBB_COMPONENT:
      result = true;
      break;
    case SERVICE_COMPONENT:
      result = true;
      break;
    }

    return result;
  }
  
}