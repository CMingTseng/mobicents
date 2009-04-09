package org.mobicents.javax.media.mscontrol;

import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.networkconnection.NetworkConnectionConfig;
import javax.media.mscontrol.resource.Parameters;

import junit.framework.TestCase;

import org.mobicents.jsr309.mgcp.MgcpStackFactory;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MsControlFactoryImplTest extends TestCase {

	private MsControlFactory msControlFactory = null;

	public void setUp() {

		javax.media.mscontrol.spi.Driver driver = new org.mobicents.javax.media.mscontrol.spi.DriverImpl();
		msControlFactory = driver.getFactory(null);

	}

	public void tearDown() throws Exception {
		MgcpStackFactory mgcpStackFactory = MgcpStackFactory.getInstance();
		mgcpStackFactory.clearMgcpStackProvider(null);

	}

	public void testMediaSessionFactoryImpl() {
		assertNotNull(msControlFactory);
	}

	public void testCreateParameters() {
		Parameters parameters = msControlFactory.createParameters();
		assertNotNull(parameters);

	}

	public void testcreateMediaSession() throws MsControlException {
		MediaSession mediaSession = msControlFactory.createMediaSession();
		assertNotNull(mediaSession);
	}

	public void testgetMediaConfig() throws MediaConfigException {
		NetworkConnectionConfig conf = msControlFactory.getMediaConfig(NetworkConnectionConfig.c_Basic);
		assertNotNull(conf);
	}

}
