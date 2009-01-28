package org.mobicents.servlet.sip.seam.entrypoint.media;

import java.util.HashMap;

import javax.servlet.sip.SipSession;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.mobicents.mscontrol.MsConnection;
import org.mobicents.mscontrol.MsEndpoint;
import org.mobicents.mscontrol.MsLink;
import org.mobicents.mscontrol.MsLinkMode;
import org.mobicents.mscontrol.MsNotificationListener;
import org.mobicents.mscontrol.MsSession;
import org.mobicents.mscontrol.events.MsRequestedEvent;
import org.mobicents.mscontrol.events.MsRequestedSignal;
import static org.jboss.seam.annotations.Install.FRAMEWORK;;

@Name("mediaController")
@Scope(ScopeType.APPLICATION)
@Install(precedence=FRAMEWORK)
@Startup
public class MediaController {
	
	@In(required=false) private SipSession sipSession;
	@In(required=false) private MsSession msSession;
	
	private HashMap<Object, MsNotificationListener> listenerMap = 
		new HashMap<Object, MsNotificationListener>();
	
	public MediaController() {
	}
	
	public MsConnection createConnection(String endpoint) {
		MsConnection msConnection = msSession.createNetworkConnection(endpoint);
		msConnection.addConnectionListener(new ConnectionListener(sipSession));
		return msConnection;
	}
	
	public MsLink createLink(MsLinkMode mode) {
		MsLink link = msSession.createLink(mode);
		link.addLinkListener(new LinkListener(sipSession));
		return link;
	}
	
	public void execute(MsEndpoint endpoint,
			MsRequestedSignal[] signals,
			MsRequestedEvent[] events) {
		endpoint.execute(signals, events);
		if(listenerMap.get(endpoint) == null) {
			MsNotificationListener newListener = 
				new NotificationListener(sipSession, msSession, endpoint, null);
			listenerMap.put(endpoint, newListener);
			endpoint.addNotificationListener(newListener);
		}
	}
	
	public void execute(MsEndpoint endpoint,
			MsRequestedSignal[] signals,
			MsRequestedEvent[] events,
			MsConnection connection) {
		endpoint.execute(signals, events, connection);
		if(listenerMap.get(connection) == null) {
			MsNotificationListener newListener = 
				new NotificationListener(sipSession, msSession, endpoint, connection);
			listenerMap.put(connection, newListener);
			connection.addNotificationListener(newListener);
		}
	}
	
	public void execute(MsEndpoint endpoint,
			MsRequestedSignal[] signals,
			MsRequestedEvent[] events,
			MsLink link) {
		endpoint.execute(signals, events, link);
		if(listenerMap.get(link) == null) {
			MsNotificationListener newListener = 
				new NotificationListener(sipSession, msSession, endpoint, link);
			listenerMap.put(link, newListener);
			link.addNotificationListener(newListener);
		}
	}
	
}