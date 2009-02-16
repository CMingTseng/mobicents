package org.mobicents.slee.container.component.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.slee.management.DeploymentException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.component.ServiceComponent;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ServiceDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ServiceDescriptorImpl;

/**
 * Service Component builder
 * @author martins
 *
 */
public class DeployableUnitServiceComponentBuilder {

	private static final Logger logger = Logger.getLogger(DeployableUnitServiceComponentBuilder.class);
	
	/**
	 * Builds a service component contained in the specified du jar file, with the specified and adds it to the specified deployable unit.
	 * 
	 * @param serviceDescriptorFileName
	 * @param deployableUnitJar
	 * @param deployableUnit
	 * @param documentBuilder
	 * @throws DeploymentException
	 */
	public List<ServiceComponent> buildComponents(String serviceDescriptorFileName, JarFile deployableUnitJar) throws DeploymentException {
    	
		// make component jar entry
		JarEntry componentDescriptor = deployableUnitJar.getJarEntry(serviceDescriptorFileName);
		InputStream componentDescriptorInputStream = null;
		List<ServiceComponent> result = new ArrayList<ServiceComponent>();
    	try {
    		componentDescriptorInputStream = deployableUnitJar.getInputStream(componentDescriptor);
    		ServiceDescriptorFactory descriptorFactory = new ServiceDescriptorFactory();
    		for (ServiceDescriptorImpl descriptor : descriptorFactory.parse(componentDescriptorInputStream)) {
    			result.add(new ServiceComponent(descriptor));
    		}
    	} catch (IOException e) {
    		throw new DeploymentException("failed to parse service descriptor from "+componentDescriptor.getName(),e);
    	}
    	finally {
    		if (componentDescriptorInputStream != null) {
    			try {
    				componentDescriptorInputStream.close();
    			} catch (IOException e) {
    				logger.error("failed to close inputstream of descriptor for jar "+componentDescriptor.getName());
    			}
    		}
    	}        
    	return result;
    }
	
}
