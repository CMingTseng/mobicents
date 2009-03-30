package org.mobicents.javax.media.mscontrol;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;

import java.io.Serializable;
import java.net.URI;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.Joinable;
import javax.media.mscontrol.JoinableContainer;
import javax.media.mscontrol.JoinableStream;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.StatusEvent;
import javax.media.mscontrol.StatusEventListener;
import javax.media.mscontrol.JoinableStream.StreamType;

import org.mobicents.jsr309.mgcp.MgcpWrapper;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class AbstractJoinableContainer implements JoinableContainer {

	protected final String id = (new UID()).toString();
	protected MediaSessionImpl mediaSession = null;
	protected CopyOnWriteArrayList<StatusEventListener> statusEventListenerList = new CopyOnWriteArrayList<StatusEventListener>();

	protected AudioJoinableStream audioJoinableStream = null;
	protected String endpoint = null;
	protected MgcpWrapper mgcpWrapper;
	protected MediaObjectState state = null;

	protected int maxJoinees = 1;

	public AbstractJoinableContainer(MediaSessionImpl mediaSession, MgcpWrapper mgcpWrapper, int maxJoinees,
			String endpointName) {
		this.mediaSession = mediaSession;
		this.mgcpWrapper = mgcpWrapper;
		this.maxJoinees = maxJoinees;
		this.endpoint = endpointName;
		this.state = MediaObjectState.IDLE;
	}

	protected String getId() {
		return this.id;
	}

	public JoinableStream getJoinableStream(StreamType value) throws MsControlException {
		checkState();
		if (value.equals(StreamType.audio)) {
			return audioJoinableStream;
		}
		return null;
	}

	public JoinableStream[] getJoinableStreams() throws MsControlException {
		checkState();
		if (audioJoinableStream != null) {
			return new JoinableStream[] { audioJoinableStream };
		} else {
			return new JoinableStream[] {};
		}
	}

	public Joinable[] getJoinees() throws MsControlException {
		checkState();
		Joinable[] j = null;

		if (this.audioJoinableStream != null) {
			j = new Joinable[this.audioJoinableStream.audJoinStrVsDirMap.size()];
			int count = 0;
			for (AudioJoinableStream ajs : this.audioJoinableStream.audJoinStrVsDirMap.keySet()) {
				j[count] = ajs.getContainer();
				count++;
			}
		} else {
			j = new Joinable[] {};
		}

		return j;
	}

	public Joinable[] getJoinees(Direction direction) throws MsControlException {
		checkState();
		if (this.audioJoinableStream == null) {
			return new Joinable[] {};
		}
		List<JoinableContainer> absJoiCon = new ArrayList<JoinableContainer>();
		for (AudioJoinableStream key : this.audioJoinableStream.audJoinStrVsDirMap.keySet()) {
			Direction d = this.audioJoinableStream.audJoinStrVsDirMap.get(key);
			if (d.equals(direction) || d.equals(Direction.DUPLEX)) {
				absJoiCon.add(key.getContainer());
			}
		}

		Joinable[] j = new Joinable[absJoiCon.size()];
		int count = 0;
		for (JoinableContainer a : absJoiCon) {
			j[count] = a;
			count++;
		}

		return j;
	}

	public void join(Direction direction, Joinable other) throws MsControlException {
		throw new MsControlException("Not supported yet. Use joinInitiate()");
	}

	public void joinInitiate(Direction direction, Joinable other, Serializable context) throws MsControlException {
		checkState();

		AbstractJoinableContainer absJoiConOther = (AbstractJoinableContainer) other;

		absJoiConOther.checkState();

		if (other.equals(this)) {
			throw new MsControlException("Container " + this.getURI() + " cannot join to itself");
		}

		if (this.audioJoinableStream == null) {
			this.audioJoinableStream = new AudioJoinableStream(this);
		}

		if (absJoiConOther.audioJoinableStream == null) {
			absJoiConOther.audioJoinableStream = new AudioJoinableStream(absJoiConOther);
		}
		this.audioJoinableStream.joinInitiate(direction, absJoiConOther.audioJoinableStream, context);
	}

	public void unjoin(Joinable other) throws MsControlException {
		throw new MsControlException("Not supported yet. Use unjoinInitiate()");
	}

	public void unjoinInitiate(Joinable other, Serializable context) throws MsControlException {
		checkState();

		AbstractJoinableContainer absJoiConOther = (AbstractJoinableContainer) other;

		absJoiConOther.checkState();

		if (this.audioJoinableStream == null) {
			throw new MsControlException("No stream present with this container" + this.getURI() + " for unjoin");
		}

		if (absJoiConOther.audioJoinableStream == null) {
			throw new MsControlException("No stream present with other container " + absJoiConOther.getURI()
					+ "for unjoin");
		}

		this.audioJoinableStream.unjoinInitiate(absJoiConOther.audioJoinableStream, context);
	}

	public void addListener(StatusEventListener listener) {
		statusEventListenerList.add(listener);
	}

	public MediaSession getMediaSession() {
		return this.mediaSession;
	}

	public void removeListener(StatusEventListener listener) {
		statusEventListenerList.remove(listener);
	}

	public CopyOnWriteArrayList<StatusEventListener> getStatusEventListenerList() {
		return this.statusEventListenerList;
	}

	private void update(StatusEvent anEvent) {
		for (StatusEventListener s : this.statusEventListenerList) {
			s.onEvent(anEvent);
		}
	}

	protected void updateJoined(StatusEvent anEvent, ConnectionIdentifier thisConnId, ConnectionIdentifier otherConnId,
			AbstractJoinableContainer otheContainer, boolean error) {
		if (!error) {
			this.state = MediaObjectState.JOINED;
			otheContainer.state = MediaObjectState.JOINED;

			joined(thisConnId, otherConnId);
			otheContainer.joined(otherConnId, thisConnId);
		}
		update(anEvent);

	}

	protected void updateUnjoined(StatusEvent anEvent, ConnectionIdentifier thisConnId,
			ConnectionIdentifier otherConnId, AbstractJoinableContainer otheContainer, boolean error) {
		if (!error) {
			if (this.audioJoinableStream.audJoinStrVsDirMap.size() == 0) {
				this.state = MediaObjectState.IDLE;
				resetContainer();
			}

			if (otheContainer.audioJoinableStream.audJoinStrVsDirMap.size() == 0) {
				otheContainer.state = MediaObjectState.IDLE;
				otheContainer.resetContainer();
			}
			unjoined(thisConnId, otherConnId);
			otheContainer.unjoined(otherConnId, thisConnId);
		}
		update(anEvent);
	}

	protected ConnectionIdentifier getConnectionIdentifier(AudioJoinableStream audioJoiStreamOther) {
		ConnectionIdentifier connId = null;
		if (this.audioJoinableStream != null) {
			connId = this.audioJoinableStream.getConnectionIdentifier(audioJoiStreamOther);
		}
		return connId;
	}

	protected abstract void joined(ConnectionIdentifier thisConnId, ConnectionIdentifier otherConnId);

	protected abstract void unjoined(ConnectionIdentifier thisConnId, ConnectionIdentifier otherConnId);

	protected abstract void checkState();
	
	protected abstract MediaObjectState getState();

	protected abstract void resetContainer();

	protected abstract URI getURI();

	@Override
	public String toString() {
		return (this.getURI() + " Endpoint = " + this.endpoint);
	}
}
