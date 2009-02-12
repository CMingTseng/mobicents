/**
 * Start time:16:00:31 2009-01-25<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.slee.container.component;

import java.util.Set;

import javax.slee.ComponentID;
import javax.slee.ServiceID;
import javax.slee.management.DependencyException;
import javax.slee.management.DeploymentException;

import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ServiceDescriptorImpl;

/**
 * Start time:16:00:31 2009-01-25<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class ServiceComponent extends SleeComponent {

	/**
	 * the service descriptor
	 */
	private final ServiceDescriptorImpl descriptor;
	
	/**
	 * 
	 * @param descriptor
	 */
	public ServiceComponent(ServiceDescriptorImpl descriptor) {
		this.descriptor = descriptor;
	}
	
	/**
	 * Retrieves the service descriptor
	 * @return
	 */
	public ServiceDescriptorImpl getDescriptor() {
		return descriptor;
	}

	/**
	 * Retrieves the id of the service
	 * @return
	 */
	public ServiceID getServiceID() {
		return descriptor.getServiceID();
	}
	
	@Override
	void addToDeployableUnit() {
		getDeployableUnit().getServiceComponents().put(getServiceID(), this);
	}
	
	@Override
	public Set<ComponentID> getDependenciesSet() {
		return descriptor.getDependenciesSet();
	}
	
	@Override
	public boolean isSlee11() {
		return descriptor.isSlee11();
	}	
	
	@Override
	public ComponentID getComponentID() {
		return getServiceID();
	}
	
	@Override
	public boolean validate() throws DependencyException, DeploymentException {
		// FIXME use validator when available
		return true;
	}
}