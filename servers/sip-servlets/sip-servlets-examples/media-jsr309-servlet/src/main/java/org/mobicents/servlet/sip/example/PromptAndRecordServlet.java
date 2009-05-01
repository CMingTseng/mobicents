package org.mobicents.servlet.sip.example;

import java.io.IOException;
import java.net.URI;

import javax.media.mscontrol.JoinEvent;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.StatusEvent;
import javax.media.mscontrol.StatusEventListener;
import javax.media.mscontrol.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.MediaGroupConfig;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.Error;
import javax.media.mscontrol.resource.MediaEventListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

/**
 * 
 * @author amit bhayani
 *
 */
public class PromptAndRecordServlet extends PlayerServlet {

	private static Logger logger = Logger
			.getLogger(PromptAndRecordServlet.class);

	private final static String WELCOME_MSG = "http://"
			+ System.getProperty("jboss.bind.address", "127.0.0.1")
			+ ":8080/media-jsr309-servlet/audio/record_welcome.wav";
	private final static String RECORDER = "test.wav";
	
	private final String RECORDED_FILE = "file://" + System.getProperty("jboss.server.data.dir") + "/" + RECORDER;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
	}

	@Override
	protected void doAck(SipServletRequest req) throws ServletException,
			IOException {
		SipSession sipSession = req.getSession();

		MediaSession ms = (MediaSession) sipSession
				.getAttribute("MEDIA_SESSION");
		try {
			MediaGroup mg = ms
					.createContainer(MediaGroupConfig.c_PlayerRecorderSignalDetector);
			mg.addListener(new MyStatusEventListener());

			NetworkConnection nc = (NetworkConnection) sipSession
					.getAttribute("NETWORK_CONNECTION");
			mg.joinInitiate(Direction.DUPLEX, nc, this);

		} catch (MsControlException e) {
			logger.error(e);
			// Clean up media session
			terminate(sipSession, ms);
		}
	}

	private class MyStatusEventListener implements StatusEventListener {

		public void onEvent(StatusEvent event) {

			MediaGroup mg = (MediaGroup) event.getSource();
			if (event.getError().equals(Error.e_OK)
					&& JoinEvent.ev_Joined.equals(event.getEventType())) {
				// NC Joined to MG

				try {
					Player player = mg.getPlayer();
					player.addListener(new PlayerListener());

					URI prompt = URI.create(WELCOME_MSG);

					player.play(prompt, null, null);

				} catch (MsControlException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				logger.error("Joining of MG and NC failed");
			}
		}

	}

	private class PlayerListener implements MediaEventListener<PlayerEvent> {

		public void onEvent(PlayerEvent event) {

			Player player = event.getSource();
			MediaGroup mg = player.getContainer();
			if (Error.e_OK.equals(event.getError())
					&& Player.ev_PlayComplete.equals(event.getEventType())) {
				try {
					Recorder recoredr = mg.getRecorder();
					URI prompt = URI.create(RECORDED_FILE);
					recoredr.record(prompt, null, null);
					
					//TODO : Set timer
					
				} catch (MsControlException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				logger.error("Player didn't complete successfully ");
			}

		}

	}

}