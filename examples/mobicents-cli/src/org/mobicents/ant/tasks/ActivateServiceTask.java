package org.mobicents.ant.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mobicents.ant.SubTask;

import org.mobicents.slee.container.management.ComponentKey;
import org.mobicents.slee.container.management.ServiceIDImpl;
import org.mobicents.slee.container.management.jmx.SleeCommandInterface;

public class ActivateServiceTask implements SubTask {
	// Obtain a suitable logger.
    private static Logger logger = Logger.getLogger(org.mobicents.ant.tasks.ActivateServiceTask.class.getName());
	
    public void run(SleeCommandInterface slee) {
		// TODO Auto-generated method stub
    	try {
    		ComponentKey id = new ComponentKey(this.id);
    		ServiceIDImpl service = new ServiceIDImpl(id);
    		
    		// Invoke the operation
    		Object result = slee.invokeOperation("-activateService", service.toString(), null, null);
			
    		if (result == null)
    		{
    			logger.info("No response");
    		}
    		else
    		{
    			logger.info(result.toString());
    		}
		}
    	
    	catch (Exception ex)
		{
    		// Log the error
            logger.log(Level.WARNING, "Bad result: " + slee.commandBean + "." + slee.commandString +
            		"\n" + ex.getCause().toString());
		}
	}
    
	// The setter for the "id" attribute
    public void setServiceID(String id) {
        this.id = id;
    }
    
    private String id = null;
}