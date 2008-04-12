/*
 * Mobicents: The Open Source VoIP Middleware Platform
 *
 * Copyright 2003-2006, Mobicents, 
 * and individual contributors as indicated
 * by the @authors tag. See the copyright.txt 
 * in the distribution for a full listing of   
 * individual contributors.
 *
 * This is free software; you can redistribute it
 * and/or modify it under the terms of the 
 * GNU General Public License (GPL) as
 * published by the Free Software Foundation; 
 * either version 2 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that 
 * it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the 
 * GNU General Public
 * License along with this software; 
 * if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, 
 * Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site:
 * http://www.fsf.org.
 */
package org.mobicents.slee.container.management.console.server.activity;

import java.util.HashMap;

import org.mobicents.slee.container.management.console.client.ManagementConsoleException;
import org.mobicents.slee.container.management.console.client.activity.*;
import org.mobicents.slee.container.management.console.server.ManagementConsole;
import org.mobicents.slee.container.management.console.server.mbeans.ActivityManagementMBeanUtils;
import org.mobicents.slee.container.management.console.server.mbeans.SleeMBeanConnection;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author Vladimir Ralev
 * 
 */
public class ActivityServiceImpl extends RemoteServiceServlet implements ActivityService{

    private static final long serialVersionUID = -5366129803477778369L;

    public static final ManagementConsole managementConsole = ManagementConsole
            .getInstance();

    private SleeMBeanConnection sleeConnection = managementConsole
            .getSleeConnection();
    
    public Long getActivityContextTimeout() throws ManagementConsoleException
    {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       return activity.getActivityContextTimeout();
    }


    public ActivityServiceImpl() {

    }

    public ActivityContextInfo[] listActivityContexts() throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.listActivityContexts(true);
       return objectToActivityContextArray(acs);
    }
    
    public void endActivity(String id) throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       activity.endActivity(id);
    }
    
    public ActivityContextInfo retrieveActivityContextDetails(String id) throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.retrieveActivityContextDetails(id);
       return objectToActivityContext(acs);
    }
    
    public ActivityContextInfo[] retrieveActivityContextIDByResourceAdaptorEntityName(String id) throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.retrieveActivityContextIDByResourceAdaptorEntityName(id);
       return idsToActivityInfo(acs);
    }
    
    public ActivityContextInfo[] retrieveActivityContextIDByActivityType(String id) throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.retrieveActivityContextIDByActivityType(id);
       return idsToActivityInfo(acs);
    }
    
    public ActivityContextInfo[] retrieveActivityContextIDBySbbEntityID(String id) throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.retrieveActivityContextIDBySbbEntityID(id);
       return idsToActivityInfo(acs);
    }
    
    public void queryActivityContextLiveness() throws ManagementConsoleException {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       activity.queryActivityContextLiveness();
    }
    
    public ActivityContextInfo[] objectToActivityContextArray(Object[] acs){
       ActivityContextInfo[] acsTransport;
       if(acs == null)
          return new ActivityContextInfo[0];
       acsTransport = new ActivityContextInfo[acs.length];
       for(int q=0; q<acs.length; q++)
       {
          Object[] subItems = (Object[]) acs[q];
          acsTransport[q] = objectToActivityContext(subItems);
       }
       return acsTransport;
    }
    
    public static String[] objectToStringArray(Object[] acs)
    {
       String[] strings = new String[acs.length];
       for(int q=0; q<strings.length; q++)
       {
          strings[q] = (String) acs[q];
       }
       return strings;
    }
    
    public ActivityContextInfo[] idsToActivityInfo(Object[] ids)
    {
       if(ids == null) return null;
       ActivityContextInfo[] acis = new ActivityContextInfo[ids.length];
       try
       {
          ActivityContextInfo[] all = listActivityContexts();
          HashMap allMapped = new HashMap();
          for(int q=0; q<all.length; q++)
             allMapped.put(all[q].getId(), all[q]);
          for(int q=0; q<ids.length; q++)
             acis[q] = (ActivityContextInfo)allMapped.get(ids[q]);
       }
       catch(Exception e)
       {
          e.printStackTrace();
          return null;
       }
       return acis;
    }
    
    public ActivityContextInfo objectToActivityContext(Object[] subItems)
    {
       ActivityContextInfo acsTransport = new ActivityContextInfo();
       acsTransport.setId(subItems[0].toString()); 
       acsTransport.setActivityClass(subItems[1].toString()); 
       acsTransport.setLastAccessTime(toTimeStamp(subItems[2].toString())); 
       acsTransport.setRaEntityId((String)subItems[3]);
       acsTransport.setSbbAttachments((String[])subItems[4]); 
       acsTransport.setNamesBoundTo((String[])subItems[5]); 
       acsTransport.setAttachedTimers((String[])subItems[6]); 
       acsTransport.setDataAttributes((String[])subItems[7]);
       
       long timeout;
       try
       {
          timeout = getActivityContextTimeout();
       }
       catch(Exception ex)
       {
          ex.printStackTrace();
          timeout = -1;
       }
       if(timeout == -1)
          acsTransport.setTTL("N/A");
       else
          acsTransport.setTTL(toTTL(subItems[2].toString(), timeout));
       
       return acsTransport;
    }
    
    private static ActivityContextInfo[] testACS()
    {
       ActivityContextInfo[] test = new ActivityContextInfo[5];
       for(int q=0; q<test.length; q++)
       {
          test[q] = new ActivityContextInfo();
          test[q].setId("ID" + new Long(new java.util.Random().nextLong())); 
          test[q].setActivityClass("com.jboss.class"); 
          test[q].setLastAccessTime(toTimeStamp(new Long(new java.util.Random().nextLong()).toString())); 
          test[q].setSbbAttachments(new String[] {"sbb1", "sbb2", "sbb3"}); 
          test[q].setNamesBoundTo(new String[] {"name1", "name2", "name3"}); 
          test[q].setAttachedTimers(new String[] {"timer1", "t2", "t3"}); 
          test[q].setDataAttributes(new String[] {"data1", "d2", "d3"}); 
       }
       return test;
    }
       
    private static String toTimeStamp(String millis)
    {
       long lmillis = Long.parseLong(millis);
       java.util.Date date = new java.util.Date(lmillis);
       java.text.SimpleDateFormat dateFormat = 
          new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
       return dateFormat.format(date);
    }
    
    private static String toTTL(String millis, long timeout)
    {
       long lastAccess = Long.parseLong(millis);
       java.util.Date date = new java.util.Date();
       long current = date.getTime();
       long ttl = timeout - (current - lastAccess);
       ttl /= 1000;
       return Long.toString(ttl);
    }
    
    public ActivityContextInfo[] retrieveActivityContextIDBySbbID(String id) throws ManagementConsoleException
    {
       ActivityManagementMBeanUtils activity = 
          sleeConnection.getSleeManagementMBeanUtils()
          .getActivityManagementMBeanUtils();
       Object[] acs = activity.retrieveActivityContextIDBySbbID(id);
       return idsToActivityInfo(acs);
    }
}
