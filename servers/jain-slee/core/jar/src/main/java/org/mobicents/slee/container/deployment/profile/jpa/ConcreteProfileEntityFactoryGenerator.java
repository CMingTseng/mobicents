package org.mobicents.slee.container.deployment.profile.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

import javax.slee.SLEEException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.component.ProfileSpecificationComponent;
import org.mobicents.slee.container.component.deployment.ClassPool;
import org.mobicents.slee.container.component.profile.ProfileAttribute;
import org.mobicents.slee.container.component.profile.ProfileEntity;
import org.mobicents.slee.container.component.profile.ProfileEntityFactory;
import org.mobicents.slee.container.deployment.profile.ClassGeneratorUtils;
import org.mobicents.slee.container.security.Utility;

/**
 * 
 * Generates the concrete profile entity factory class for a profile spec
 * @author martins
 */
public class ConcreteProfileEntityFactoryGenerator {

  private static final Logger logger = Logger.getLogger(ConcreteProfileEntityFactoryGenerator.class);

  private final ProfileSpecificationComponent profileComponent;
  private final Class<?> profileEntityClass;
  private final Map<String, Class<?>> profileEntityAttributeArrayValueClasses;
  
  public ConcreteProfileEntityFactoryGenerator(ProfileSpecificationComponent profileComponent, Class<?> profileEntityClass, Map<String, Class<?>> profileEntityAttributeArrayValueClasses) {
	  this.profileComponent = profileComponent;	  
	  this.profileEntityClass = profileEntityClass;
	  this.profileEntityAttributeArrayValueClasses = profileEntityAttributeArrayValueClasses;
  }

  /*
   * Generates the profile entity factory class
   */
  public Class<?> generateClass() {
    	  
	  CtClass ctClass = null;
	  
	  try {      
    	  
		  ClassPool classPool = profileComponent.getClassPool();
		  Collection<ProfileAttribute> profileAttributes = profileComponent.getProfileAttributes().values();

		  String className = profileEntityClass.getName() + "F";

		  ctClass = classPool.makeClass(className);

		  CtClass profileEntityFactoryClass = classPool.get(ProfileEntityFactory.class.getName());
		  CtClass[] interfaces = new CtClass[] { profileEntityFactoryClass };
		  ctClass.setInterfaces(interfaces);

		  // copy newInstance method from interface and generate body
		  CtMethod newInstanceMethod = profileEntityFactoryClass.getDeclaredMethod("newInstance");
		  CtMethod newInstanceMethodCopy = CtNewMethod.copy(newInstanceMethod, ctClass, null);
		  String newInstanceMethodCopyBody = 
			  "{ "+
			  	profileEntityClass.getName()+" profileEntity = new "+profileEntityClass.getName()+"();" +
			  	" profileEntity.setTableName($1); " +
			  	" profileEntity.setProfileName($2); " +
			  	" return profileEntity; " +
			  "}";
		  if (logger.isDebugEnabled()) {
			  logger.debug( "\nAdding method named "+newInstanceMethodCopy.getName()+" with body "+newInstanceMethodCopy+" , into class " + className );
		  }
		  newInstanceMethodCopy.setBody(newInstanceMethodCopyBody);
		  ctClass.addMethod(newInstanceMethodCopy);
		
		  // copy copyAttributes method from interface and generate body
		  CtMethod copyAttributesMethod = profileEntityFactoryClass.getDeclaredMethod("copyAttributes");
		  CtMethod copyAttributesMethodCopy = CtNewMethod.copy(copyAttributesMethod, ctClass, null);
		  // body header
		  String copyAttributesMethodCopyBody = 
			  "{ "+
			  	profileEntityClass.getName()+" newProfileEntity = ("+profileEntityClass.getName()+") $2; " +
			  	profileEntityClass.getName()+" oldProfileEntity = ("+profileEntityClass.getName()+") $1; ";
		  // process fields copy
		  String profileEntityAttributeArrayValueClassName = null;
		  if(System.getSecurityManager()==null)
		  {
		  
		  for (ProfileAttribute profileAttribute : profileAttributes) {
			  String accessorMethodSufix = ClassGeneratorUtils.getPojoCmpAccessorSufix(profileAttribute.getName());
			  if (profileAttribute.isArray()) {
				  profileEntityAttributeArrayValueClassName = profileEntityAttributeArrayValueClasses.get(profileAttribute.getName()).getName();
				  copyAttributesMethodCopyBody += 
					  "if (oldProfileEntity.get"+accessorMethodSufix+"() != null) { " +
					  // if the target list already exists then empty it so elements get deleted, otherwise create new
					  List.class.getName() + " new"+ profileAttribute.getName() + " = newProfileEntity.get"+accessorMethodSufix+"(); " +
					  "if (new"+profileAttribute.getName()+" == null) { new"+ profileAttribute.getName() + " = new " + LinkedList.class.getName() + "(); } else { new"+profileAttribute.getName()+".clear(); } " +
					  // extract list to copy
					  List.class.getName() + " old"+ profileAttribute.getName() + " = oldProfileEntity.get" + accessorMethodSufix + "(); " +
					  // iterate each list element
					  "for ("+Iterator.class.getName()+" i = old"+profileAttribute.getName()+".iterator(); i.hasNext();) { " +
					  profileEntityAttributeArrayValueClassName+" oldArrayValue = ("+profileEntityAttributeArrayValueClassName+") i.next(); " +
					  profileEntityAttributeArrayValueClassName+" newArrayValue = new "+profileEntityAttributeArrayValueClassName+"(); " +
					  // link to profile entity
					  "newArrayValue.setOwner( newProfileEntity ); " +
					  // copy fields
					  "newArrayValue.setString( oldArrayValue.getString() ); " +
					  "newArrayValue.setSerializable( ("+Serializable.class.getName()+") "+ProfileEntity.class.getName()+".makeDeepCopy(oldArrayValue.getSerializable()) ); " +
					  "new"+profileAttribute.getName()+".add(newArrayValue); " +
					  "} " +
					  "newProfileEntity.set"+accessorMethodSufix+"(new"+profileAttribute.getName()+"); };";					  
			  }
			  else {
				  if (profileAttribute.isPrimitive()) {
					  // just copy value
					  copyAttributesMethodCopyBody += 
						  " newProfileEntity.set"+accessorMethodSufix+"(oldProfileEntity.get"+accessorMethodSufix+"()); ";
				  }
				  else {
					// just copy value but do a deep copy
					  copyAttributesMethodCopyBody += 
						  " if (oldProfileEntity.get"+accessorMethodSufix+"() != null) { newProfileEntity.set"+accessorMethodSufix+"(("+profileAttribute.getType().getName()+")"+ProfileEntity.class.getName()+".makeDeepCopy(oldProfileEntity.get"+accessorMethodSufix+"())); }; ";
				  }
			  }
		  }
		  }else
		  {
			  for (ProfileAttribute profileAttribute : profileAttributes) {
			  	  String accessorMethodSufix = ClassGeneratorUtils.getPojoCmpAccessorSufix(profileAttribute.getName());
			  		  if (profileAttribute.isArray()) {
			  		  profileEntityAttributeArrayValueClassName = profileEntityAttributeArrayValueClasses.get(profileAttribute.getName()).getName();
			  		  copyAttributesMethodCopyBody += 
			  						  "if ("+Utility.class.getName()+".makeSafeProxyCall(oldProfileEntity,\"get"+accessorMethodSufix+"\",null,null) != null) {" +
			  						  		"" +
			  						  // if the target list already exists then empty it so elements get deleted, otherwise create new
			  						  List.class.getName() + " new"+ profileAttribute.getName() + " = newProfileEntity.get"+accessorMethodSufix+"(); " +
			  						  "if (new"+profileAttribute.getName()+" == null) { new"+ profileAttribute.getName() + " = new " + LinkedList.class.getName() + "(); } else { new"+profileAttribute.getName()+".clear(); } " +
			  						  // extract list to copy
			  						  //List.class.getName() + " old"+ profileAttribute.getName() + " = oldProfileEntity.get" + accessorMethodSufix + "(); " +
			  						  List.class.getName() + " old"+ profileAttribute.getName() + " = ("+List.class.getName()+")"+Utility.class.getName()+".makeSafeProxyCall(oldProfileEntity,\"get"+accessorMethodSufix+"\",null,null);"+
			  						  // iterate each list element 
			  						  Iterator.class.getName()+" i = "+Utility.class.getName()+".makeSafeProxyCall(old"+profileAttribute.getName()+",\"iterator\",null,null);"+
			  						  "for (; "+Utility.class.getName()+".evaluateNext(i);) { " +
			  						  profileEntityAttributeArrayValueClassName+" oldArrayValue = ("+profileEntityAttributeArrayValueClassName+") i.next();"+
			  						  profileEntityAttributeArrayValueClassName+" newArrayValue = new "+profileEntityAttributeArrayValueClassName+"(); " +
			  						  // link to profile entity
			  						  "newArrayValue.setOwner( newProfileEntity ); " +
			  						  // copy fields
			  					  "newArrayValue.setString( oldArrayValue.getString() ); " +
			  						  "newArrayValue.setSerializable( ("+Serializable.class.getName()+") "+ProfileEntity.class.getName()+".makeDeepCopy(oldArrayValue.getSerializable()) ); " +
			  						  "new"+profileAttribute.getName()+".add(newArrayValue); " +
			  						  "} " +
			  						  //"newProfileEntity.set"+accessorMethodSufix+"(new"+profileAttribute.getName()+"); };";
			  						  Utility.class.getName()+".makeSafeProxyCall(newProfileEntity,\"set"+accessorMethodSufix+"\",new Class[]{"+Utility.class.getName()+".getReturnType(oldProfileEntity,\"get"+accessorMethodSufix+"\")},new Object[]{new"+profileAttribute.getName()+"});}";
			  				  }
			  				  else {
			  					  if (profileAttribute.isPrimitive()) {
			  						  // just copy value
			  						  //copyAttributesMethodCopyBody += 
			  						  //	  " newProfileEntity.set"+accessorMethodSufix+"(oldProfileEntity.get"+accessorMethodSufix+"()); ";
			  						  copyAttributesMethodCopyBody +="Object value = "+Utility.class.getName()+".makeSafeProxyCall(oldProfileEntity,\"get"+accessorMethodSufix+"\",null,null);";
			  						  copyAttributesMethodCopyBody +=Utility.class.getName()+".makeSafeProxyCall(newProfileEntity,\"set"+accessorMethodSufix+"\",new Class[]{"+Utility.class.getName()+".getReturnType(oldProfileEntity,\"get"+accessorMethodSufix+"\")},new Object[]{value});";
			  					  }
			  					  else {
			  						// just copy value but do a deep copy
			  						  copyAttributesMethodCopyBody += 
			  							  "if ("+Utility.class.getName()+".makeSafeProxyCall(oldProfileEntity,\"get"+accessorMethodSufix+"\",null,null) != null)  " 
			  							  		+"{ Object value = "+Utility.class.getName()+".makeSafeProxyCall(oldProfileEntity,\"get"+accessorMethodSufix+"\",null,null);"
			  							  			
			  							  			//+"newProfileEntity.set"+accessorMethodSufix+"(("+profileAttribute.getType().getName()+")"+ProfileEntity.class.getName()+".makeDeepCopy(value)); }; "
			  							  		+Utility.class.getName()+".makeSafeProxyCall(newProfileEntity,\"set"+accessorMethodSufix+"\",new Class[]{"+Utility.class.getName()+".getReturnType(oldProfileEntity,\"get"+accessorMethodSufix+"\")},new Object[]{"+ProfileEntity.class.getName()+".makeDeepCopy(value)});"
			  							  			+"}";
			  					  }
			  				  }
			  			  }
			  		  }
		  // body footer
		  copyAttributesMethodCopyBody += " }";
		  if (logger.isDebugEnabled()) {
			  logger.debug( "\nAdding method named "+copyAttributesMethodCopy.getName()+" with body "+copyAttributesMethodCopyBody+" , into class " + className );
		  }
		  copyAttributesMethodCopy.setBody(copyAttributesMethodCopyBody);
		  ctClass.addMethod(copyAttributesMethodCopy);
		  
      String deployDir = profileComponent.getDeploymentDir().getAbsolutePath();
      
      // write class
      if (logger.isDebugEnabled())
    	  logger.debug( "Writing ProfileEntityFactory generated class ( "+ctClass.getName()+" ) to: " + deployDir );
      ctClass.writeFile( deployDir );
      return Thread.currentThread().getContextClassLoader().loadClass(className);      
      
    } catch (Throwable e) {
    	throw new SLEEException(e.getMessage(),e);
    }
    
    finally {
    	
    	if (ctClass != null) {
    		try {
    			ctClass.defrost();
    		}
    		catch (Throwable e) {
				logger.error(e.getMessage(),e);
			}
    	}
    	
    }
    
  }
  
}
