/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/
package org.mobicents.ant.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mobicents.ant.SubTask;

import org.mobicents.slee.container.management.jmx.SleeCommandInterface;

public class UninstallTask implements SubTask {
	// Obtain a suitable logger.
    private static Logger logger = Logger.getLogger(org.mobicents.ant.tasks.UninstallTask.class.getName());

    public void run(SleeCommandInterface slee) {

		try {
    		// Invoke the getDeployableUnit operation
			Object result = slee.invokeOperation("-uninstall", url, null, null);
		
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
    
	// The setter for the "url" attribute
    public void setUrl(String url) {
        this.url = url;
    }
    
    private String url = null;	
}