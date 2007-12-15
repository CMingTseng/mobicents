package org.mobicents.ant.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mobicents.ant.SubTask;

import org.mobicents.slee.container.management.ComponentKey;
import org.mobicents.slee.container.management.ProfileSpecificationIDImpl;
import org.mobicents.slee.container.management.jmx.SleeCommandInterface;

public class CreateProfileTableTask implements SubTask {
	// Obtain a suitable logger.
    private static Logger logger = Logger.getLogger(org.mobicents.ant.tasks.CreateProfileTableTask.class.getName());
	
    public void run(SleeCommandInterface slee) {
		// TODO Auto-generated method stub
    	try {
			ComponentKey id = new ComponentKey(this.id);
    		ProfileSpecificationIDImpl profile = new ProfileSpecificationIDImpl(id);
    		// Invoke the operation
			Object result = slee.invokeOperation("-createProfileTable", profile.toString(), profileTableName, null);
			
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
    public void setProfileSpec(String id) {
        this.id = id;
    }
    
    // The setter for the "profileTableName" attribute
    public void setTableName(String profileTableName) {
        this.profileTableName = profileTableName;
    }
    
    private String id = null;
    private String profileTableName = null;
}