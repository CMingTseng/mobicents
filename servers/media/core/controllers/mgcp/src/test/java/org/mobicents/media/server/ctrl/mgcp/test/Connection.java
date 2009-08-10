/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.ctrl.mgcp.test;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

/**
 *
 * @author kulikov
 */
public class Connection {
    private ConnectionIdentifier id;
    private ConnectionIdentifier secondConnId;
    private String sdp;
    private String remoteSDP;
    private EndpointIdentifier endpoint;
    private EndpointIdentifier secondEndpoint;

    public Connection(ConnectionIdentifier id) {
        this.id = id;
    }

    public EndpointIdentifier getEndpoint() {
        return endpoint;
    }

    public ConnectionIdentifier getId() {
        return id;
    }

    public String getLocalSdp() {
        return sdp;
    }

    public void setEndpoint(EndpointIdentifier endpoint) {
        this.endpoint = endpoint;
    }

    public void setLocalSdp(String sdp) {
        this.sdp = sdp;
    }

    public void setRemoteSDP(String remoteSDP) {
        this.remoteSDP = remoteSDP;
    }

    public String getRemoteSDP() {
        return remoteSDP;
    }

	public EndpointIdentifier getSecondEndpoint() {
		return secondEndpoint;
	}

	public void setSecondEndpoint(EndpointIdentifier secondEndpoint) {
		this.secondEndpoint = secondEndpoint;
	}

	public ConnectionIdentifier getSecondConnId() {
		return secondConnId;
	}

	public void setSecondConnId(ConnectionIdentifier secondConnId) {
		this.secondConnId = secondConnId;
	}
    
    
}
