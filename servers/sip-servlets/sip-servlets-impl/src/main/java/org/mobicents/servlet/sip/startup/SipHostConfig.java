/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.startup;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.Context;
import org.apache.catalina.startup.HostConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jean Deruelle
 *
 */
public class SipHostConfig extends HostConfig {
	private static final String WAR_EXTENSION = ".war";
	private static final String SAR_EXTENSION = ".sar";
	public static final String SIP_CONTEXT_CLASS = "org.mobicents.servlet.sip.startup.SipStandardContext";
	public static final String SIP_CONTEXT_CONFIG_CLASS = "org.mobicents.servlet.sip.startup.SipContextConfig";
	
	private static transient Log logger = LogFactory
		.getLog(SipHostConfig.class);
	/**
	 * 
	 */
	public SipHostConfig() {
		super();				
	}

	@Override
	protected void deployApps() {		
		super.deployApps();		
	}
	
	@Override
	protected void deployApps(String name) {		
		super.deployApps(name);
		String docBase = getConfigFile(name);
		// Deploy SARs, and loop if additional descriptors are found
        File sar = new File(appBase, docBase + SAR_EXTENSION);
        if (sar.exists()) {
            deploySAR(name, sar, docBase + SAR_EXTENSION);
        }
	}
	
	/**
	 * 
	 * @param name
	 * @param sar
	 * @param string
	 */
	private void deploySAR(String contextPath, File sar, String file) {
		if (deploymentExists(contextPath))
            return;
		if(logger.isDebugEnabled()) {
    		logger.debug(SipContext.APPLICATION_SIP_XML + " found in " 
    				+ sar + ". Enabling sip servlet archive deployment");
    	}
		String initialHostConfigClass = host.getConfigClass();
		host.setConfigClass(SIP_CONTEXT_CONFIG_CLASS);
		deployWAR(contextPath, sar, file);
		host.setConfigClass(initialHostConfigClass);
	}

	@Override
	protected void deployDirectory(String contextPath, File dir, String file) {
		if (deploymentExists(contextPath))
            return;
		
		boolean isSipServletApplication = isSipServletDirectory(dir);
		if(isSipServletApplication) {
			if(logger.isDebugEnabled()) {
        		logger.debug(SipContext.APPLICATION_SIP_XML + " found in " 
        				+ dir + ". Enabling sip servlet archive deployment");
        	}
			String initialConfigClass = configClass;
			String initialContextClass = contextClass;
			host.setConfigClass(SIP_CONTEXT_CONFIG_CLASS);
			setConfigClass(SIP_CONTEXT_CONFIG_CLASS);
			setContextClass(SIP_CONTEXT_CLASS);
			super.deployDirectory(contextPath, dir, file);
			host.setConfigClass(initialConfigClass);
			configClass = initialConfigClass;
	        contextClass = initialContextClass;
		} else {
			super.deployDirectory(contextPath, dir, file);
		}
	}
	
	@Override
	protected void deployDescriptor(String contextPath, File contextXml, String file) {
		super.deployDescriptor(contextPath, contextXml, file);
	}
	
	@Override
	protected void deployWARs(File appBase, String[] files) {		
		if (files == null)
            return;
        
        for (int i = 0; i < files.length; i++) {
            
            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            File dir = new File(appBase, files[i]);
            boolean isSipServletApplication = isSipServletArchive(dir);
            if(isSipServletApplication) {            	            	
                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                int period = contextPath.lastIndexOf(".");
                if (period >= 0)
                    contextPath = contextPath.substring(0, period);
                if (contextPath.equals("/ROOT"))
                    contextPath = "";
                
                if (isServiced(contextPath))
                    continue;
                
                String file = files[i];
                                
                String initialConfigClass = configClass;
        		String initialContextClass = contextClass;
        		host.setConfigClass(SIP_CONTEXT_CONFIG_CLASS);
        		setConfigClass(SIP_CONTEXT_CONFIG_CLASS);
        		setContextClass(SIP_CONTEXT_CLASS);
                deploySAR(contextPath, dir, file);
                host.setConfigClass(initialConfigClass);
                configClass = initialConfigClass;
                contextClass = initialContextClass;        		
            }                        
        }
        super.deployWARs(appBase, files);
	}
	
	/**
	 * Check if the file given in parameter match a sip servlet application, i.e.
	 * if it contains a sip.xml in its WEB-INF directory
	 * @param file the file to check (war or sar)
	 * @return true if the file is a sip servlet application, false otherwise
	 */
	private boolean isSipServletArchive(File file) {
		if (file.getName().toLowerCase().endsWith(SAR_EXTENSION)) {
			return true;
		} else if (file.getName().toLowerCase().endsWith(WAR_EXTENSION)) {
			try{
				JarFile jar = new JarFile(file);			          
				JarEntry entry = jar.getJarEntry(SipContext.APPLICATION_SIP_XML);
				if(entry != null) {
					return true;
				}
			} catch (IOException e) {
				logger.error("An unexpected Exception occured " +
						"while trying to check if a sip.xml file exists in " + file, e);
				return false;
			}
		} 		
		return false;
	}

	/**
	 * Check if the file given in parameter match a sip servlet application, i.e.
	 * if it contains a sip.xml in its WEB-INF directory
	 * @param file the file to check (war or sar)
	 * @return true if the file is a sip servlet application, false otherwise
	 */
	private boolean isSipServletDirectory(File dir) {
		 if(dir.isDirectory()) {
			 //Fix provided by Thomas Lenesey for exploded directory deployments
			File sipXmlFile = new File(dir.getAbsoluteFile(), SipContext.APPLICATION_SIP_XML);
			if(sipXmlFile.exists()) {
				return true;
			}
		}		
		return false;
	}

	
	@Override
	public void manageApp(Context arg0) {		
		super.manageApp(arg0);
	}
}
