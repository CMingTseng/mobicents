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
package org.mobicents.slee.container.management.console.client.sbb.entity;

import org.mobicents.slee.container.management.console.client.Logger;
import org.mobicents.slee.container.management.console.client.ServerCallback;
import org.mobicents.slee.container.management.console.client.ServerConnection;
import org.mobicents.slee.container.management.console.client.activity.ActivityContextInfo;
import org.mobicents.slee.container.management.console.client.activity.ActivityListPanel;
import org.mobicents.slee.container.management.console.client.activity.ActivityServiceAsync;
import org.mobicents.slee.container.management.console.client.common.BrowseContainer;
import org.mobicents.slee.container.management.console.client.common.PropertiesPanel;
import org.mobicents.slee.container.management.console.client.components.SimpleComponentNameLabel;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HorizontalPanel;
/**
 * @author Vladimir Ralev
 *
 */
public class SbbEntityPropertiesPanel extends PropertiesPanel {
   
   private BrowseContainer browseContainer;
   
   public SbbEntityPropertiesPanel(BrowseContainer browseContainer, final SbbEntityInfo info) {
       super();

       this.browseContainer = browseContainer;
       add("Entity ID", info.getSbbEntityId());  
       add("Node name", info.getNodeName()); 
       add("Parent SBB Entity", new SbbEntityLabel(info.getParentId(), info.getSbbEntityId(), browseContainer));
       add("Root SBB Entity", new SbbEntityLabel(info.getRootId(), info.getSbbEntityId(), browseContainer)); 
       add("SBB", new SimpleComponentNameLabel(info.getSbbId(), browseContainer)); 
       add("Priority", info.getPriority());
       add("Service Convergence Name", info.getServiceConvergenceName());
       add("Usage Parameter Path", info.getUsageParameterPath());
       add("Service", new SimpleComponentNameLabel(info.getServiceId(), browseContainer));
       add("Current Event", info.getCurrentEvent());
   }

}