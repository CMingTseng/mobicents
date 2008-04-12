package org.openxdm.xcap.server.slee.resource.datasource;

import javax.slee.ActivityContextInterface;
import javax.slee.FactoryException;
import javax.slee.UnrecognizedActivityException;
import javax.slee.resource.ResourceAdaptor;

import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.ResourceAdaptorActivityContextInterfaceFactory;
import org.mobicents.slee.resource.ResourceAdaptorEntity;
import org.mobicents.slee.resource.SleeActivityHandle;
import org.mobicents.slee.runtime.ActivityContextFactory;
import org.mobicents.slee.runtime.ActivityContextInterfaceImpl;

/**
 * @author Eduardo Martins
 * @version 1.0
 *
 */

public class DataSourceActivityContextInterfaceFactoryImpl implements
		ResourceAdaptorActivityContextInterfaceFactory,
		DataSourceActivityContextInterfaceFactory {
	
    private final String jndiName = "java:slee/resources/datasourceacif";
    private String raEntityName;
    private SleeContainer sleeContainer;
    private ActivityContextFactory activityContextFactory;

    public DataSourceActivityContextInterfaceFactoryImpl(SleeContainer sleeContainer, String raEntityName) {
        this.sleeContainer = sleeContainer;
        this.activityContextFactory = sleeContainer.getActivityContextFactory();
        this.raEntityName = raEntityName;
    }
    
	public String getJndiName() {
		return jndiName;
	}

	@SuppressWarnings("deprecation")
    public ActivityContextInterface getActivityContextInterface(DocumentActivity activity) throws NullPointerException,
	UnrecognizedActivityException, FactoryException {
		
		// if parameter is null throw exception
		if (activity == null) {
			throw new NullPointerException();
		}
				
		// check if activity exists
		ResourceAdaptorEntity raEntity = sleeContainer.getResourceAdaptorEnitity(raEntityName);
		ResourceAdaptor ra  = raEntity.getResourceAdaptor();
				
		// if it doesn't exist throw exception
		if (ra.getActivity(activity) == null) {
			throw new UnrecognizedActivityException(activity);
		}
		else {
			return new ActivityContextInterfaceImpl(this.sleeContainer,
				this.activityContextFactory.getActivityContext(new SleeActivityHandle(raEntityName, activity, sleeContainer)).getActivityContextId());
		}
	}

}

