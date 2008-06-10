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
package org.mobicents.servlet.sip.startup.failover;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.ProtocolHandler;
import org.mobicents.servlet.sip.startup.SipProtocolHandler;
import org.mobicents.servlet.sip.startup.SipStandardService;
import org.mobicents.servlet.sip.utils.Inet6Util;
import org.mobicents.tools.sip.balancer.NodeRegisterRMIStub;
import org.mobicents.tools.sip.balancer.SIPNode;

/**
 *  <p>Sip Servlet implementation of the <code>Service</code> interface.</p>
 *   
 *  <p>This implementation extends the <code>SipStandardService</code> (that allows Tomcat to become a converged container)
 *  with the failover features.<br/>
 *  This implementation will send heartbeats and health information to the sip balancers configured for this service 
 *  (configured in the server.xml as balancers attribute fo the Service Tag)</p>
 * 
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public class SipStandardBalancerNodeService extends SipStandardService implements SipBalancerNodeService {
	//the logger
	private static Log logger = LogFactory.getLog(SipStandardBalancerNodeService.class);
	/**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.mobicents.servlet.sip.startup.failover.SipStandardBalancerNodeService/1.0";
    
    //the balancers to send heartbeat to and our health info
	private String balancers;
    //the balancers names to send heartbeat to and our health info
	private List<String> balancerNames = new ArrayList<String>();
	private Map<String, InetAddress> register = new HashMap<String, InetAddress>();
	//heartbeat interval, can be modified through JMX
	private long heartBeatInterval = 5000;
	private Timer heartBeatTimer = new Timer();
	private TimerTask hearBeatTaskToRun = null;

    private boolean started = false;
    
    @Override
    public String getInfo() {
        return (info);
    }
    
    @Override
    public void start() throws LifecycleException {
    	super.start();
    	if (!started) {
			if (balancers != null && balancers.length() > 0) {
				String[] balancerAddresses = balancers.split(";");
				for (String balancerAddress : balancerAddresses) {
					if(Inet6Util.isValidIP6Address(balancerAddress) || Inet6Util.isValidIPV4Address(balancerAddress)) {
						try {
							this.addBalancerAddress(InetAddress.getByName(balancerAddress).getHostAddress());
						} catch (UnknownHostException e) {
							throw new LifecycleException("Impossible to parse the following sip balancer address " + balancerAddress, e);
						}
					} else {
						this.addBalancerAddress(balancerAddress, 0);
					}
				}
			}		
			started = true;
		}
    	if(logger.isDebugEnabled()) {
    		logger.debug("[Bean] creating tasks");
    	}
		this.hearBeatTaskToRun = new BalancerPingTimerTask();
		this.heartBeatTimer.scheduleAtFixedRate(this.hearBeatTaskToRun, 0,
				this.heartBeatInterval);
		if(logger.isDebugEnabled()) {
			logger.debug("[Bean] Created and scheduled tasks.");
		}
    }
    
    @Override
    public void stop() throws LifecycleException {
    	this.hearBeatTaskToRun.cancel();
		this.hearBeatTaskToRun = null;
		started = false;
    	super.stop();    	
    }
	
    /**
     * {@inheritDoc}
     */
	public long getHeartBeatInterval() {
		return heartBeatInterval;
	}
	/**
     * {@inheritDoc}
     */
	public void setHeartBeatInterval(long heartBeatInterval) {
		if (heartBeatInterval < 100)
			return;
		this.heartBeatInterval = heartBeatInterval;
		this.hearBeatTaskToRun.cancel();
		this.hearBeatTaskToRun = new BalancerPingTimerTask();
		this.heartBeatTimer.scheduleAtFixedRate(this.hearBeatTaskToRun, 0,
				this.heartBeatInterval);

	}

	/**
	 * 
	 * @param hostName
	 * @param index
	 * @return
	 */
	private InetAddress fetchHostAddress(String hostName, int index) {
		if (hostName == null)
			throw new NullPointerException("Host name cant be null!!!");

		InetAddress[] hostAddr = null;
		try {
			hostAddr = InetAddress.getAllByName(hostName);
		} catch (UnknownHostException uhe) {
			throw new IllegalArgumentException(
					"HostName is not a valid host name or it doesnt exists in DNS",
					uhe);
		}

		if (index < 0 || index >= hostAddr.length) {
			throw new IllegalArgumentException(
					"Index in host address array is wrong, it should be [0]<x<["
							+ hostAddr.length + "] and it is [" + index + "]");
		}

		InetAddress address = hostAddr[index];
		return address;
	}

	/**
     * {@inheritDoc}
     */
	public String[] getBalancers() {
		return this.balancerNames.toArray(new String[balancerNames.size()]);
	}

	/**
     * {@inheritDoc}
     */
	public void removeBalancerAddress(int index)
			throws IllegalArgumentException {
		if(logger.isDebugEnabled()) {
			logger.debug("[removeBalancerAddress]");
		}
		if (index < 0 || index >= this.balancerNames.size())
			throw new IllegalArgumentException(
					"Index is wrong, it should be [0]<x<["
							+ balancerNames.size() + "] and it is ["
							+ index + "]");

		// This code is clone, as this is ultimate cleaner method to get rid
		// of
		// errors
		String balancerName = null;

		balancerName = balancerNames.get(index);

		if(logger.isDebugEnabled()) {
			logger.debug("Removing following balancer " + balancerName);
		}
		balancerNames.remove(balancerName);
		register.remove(balancerName);

		// balancerInfoSources.remove(balancerName);
		if(logger.isDebugEnabled()) {
			logger.debug("[removeBalancerAddress] END");
		}
	}

	/**
     * {@inheritDoc}
     */
	public boolean addBalancerAddress(String addr) {
		if (addr == null)
			throw new NullPointerException("addr cant be null!!!");

		InetAddress address = null;
		try {
			address = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(
					"Somethign wrong with host creation.", e);
		}		
		String balancerName = address.getCanonicalHostName();

		if (balancerNames.contains(balancerName))
			return false;

		if(logger.isDebugEnabled()) {
			logger.debug("Adding following balancer name : " + balancerName +"/address:"+ addr);
		}
		balancerNames.add(balancerName);

		register.put(balancerName, address);

		return true;
	}

	/**
     * {@inheritDoc}
     */
	public boolean addBalancerAddress(String hostName, int index) {
		return this.addBalancerAddress(fetchHostAddress(hostName, index)
				.getHostAddress());
	}

	/**
     * {@inheritDoc}
     */
	public boolean removeBalancerAddress(String addr) {
		if (addr == null)
			throw new NullPointerException("addr cant be null!!!");

		InetAddress address = null;
		try {
			address = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(
					"Something wrong with host creation.", e);
		}

		String balancerName = address.getCanonicalHostName();

		if(logger.isDebugEnabled()) {
			logger.debug("Removing following balancer name : " + balancerName +"/address:"+ addr);
		}
		if (!balancerNames.contains(balancerName))
			return false;

		this.removeBalancerAddress(this.balancerNames.indexOf(balancerName));

		return true;
	}

	/**
     * {@inheritDoc}
     */
	public boolean removeBalancerAddress(String hostName, int index) {
		InetAddress[] hostAddr = null;
		try {
			hostAddr = InetAddress.getAllByName(hostName);
		} catch (UnknownHostException uhe) {
			throw new IllegalArgumentException(
					"HostName is not a valid host name or it doesnt exists in DNS",
					uhe);
		}

		if (index < 0 || index >= hostAddr.length) {
			throw new IllegalArgumentException(
					"Index in host address array is wrong, it should be [0]<x<["
							+ hostAddr.length + "] and it is [" + index + "]");
		}

		InetAddress address = hostAddr[index];

		return this.removeBalancerAddress(address.getHostAddress());
	}

	/**
	 * 
	 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
	 *
	 */
	class BalancerPingTimerTask extends TimerTask {

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			if(logger.isDebugEnabled()) {
				logger.debug("[BalancerPingTimerTask] Start");
			}
			ArrayList<SIPNode> info = new ArrayList<SIPNode>();
			// Gathering info about server' sip listening points
			for (Connector connector : connectors) {
				ProtocolHandler protocolHandler = connector.getProtocolHandler();
				if(protocolHandler instanceof SipProtocolHandler) {
					SipProtocolHandler sipProtocolHandler = (SipProtocolHandler) protocolHandler;
					String address = sipProtocolHandler.getIpAddress();
					int port = sipProtocolHandler.getPort();
					String transport = sipProtocolHandler.getSignalingTransport();
					String[] transports = new String[] {transport};
					
					String hostName = null;
					try {
						InetAddress[] aArray = InetAddress
								.getAllByName(address);
						if (aArray != null && aArray.length > 0) {
							// Damn it, which one we should pick?
							hostName = aArray[0].getCanonicalHostName();
						}
					} catch (UnknownHostException e) {
						logger.error("An exception occurred while trying to retrieve the hostname of a sip connector", e);
					}
	
					SIPNode node = new SIPNode(hostName, address, port,
							transports);
	
					info.add(node);
				}
			}
						
			for(InetAddress ah:new HashSet<InetAddress>(register.values())) {
				try {
					Registry registry = LocateRegistry.getRegistry(ah.getHostAddress(),2000);
					NodeRegisterRMIStub reg=(NodeRegisterRMIStub) registry.lookup("SIPBalancer");
					reg.handlePing(info);
				} catch (RemoteException e) {
					logger.error("Cannot acces the sip load balancer RMI registry", e);
				} catch (NotBoundException e) {
					logger.error("Cannot acces the sip load balancer RMI registry", e);
				}
			}
			if(logger.isDebugEnabled()) {
				logger.debug("[BalancerPingTimerTask] Finished gathering");
				logger.debug("[BalancerPingTimerTask] Gathered info[" + info + "]");
			}
		}
	}

	/**
	 * @param balancers the balancers to set
	 */
	public void setBalancers(String balancers) {
		this.balancers = balancers;
	}
}
