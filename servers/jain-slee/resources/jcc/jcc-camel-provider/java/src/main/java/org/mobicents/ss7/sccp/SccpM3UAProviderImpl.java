/*
 * The Java Call Control API for CAMEL 2
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.mobicents.ss7.sccp;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.mobicents.util.Utils;

/**
 *
 * @author Oleg Kulikov
 */
public class SccpM3UAProviderImpl implements Runnable, SccpProvider {
    
    private SccpListener listener;
    private DatagramSocket socket;
    
    private Properties props;
    
    private String remoteAddress;
    private String localAddress;
    private int localPort;
    private int remotePort;
    
    private int opc;
    private int dpc;
    private int sls;
    private int ssf;
    private int si;
    
    private PooledExecutor threadPool = new PooledExecutor(10);
    private boolean stopped = false;
    
    private Logger logger = Logger.getLogger(SccpM3UAProviderImpl.class);
    
    /** Creates a new instance of SccpM3UAProviderImpl */
    public SccpM3UAProviderImpl(Properties props) throws Exception {
        this.props = props;
        remoteAddress = props.getProperty("remote.address");
        localAddress = props.getProperty("local.address");
        
        localPort = Integer.parseInt(props.getProperty("local.port"));
        remotePort = Integer.parseInt(props.getProperty("remote.port"));
        
        opc = Integer.parseInt(props.getProperty("sccp.opc"));
        dpc = Integer.parseInt(props.getProperty("sccp.dpc"));
        sls = Integer.parseInt(props.getProperty("sccp.sls"));
        ssf = Integer.parseInt(props.getProperty("sccp.ssf"));
        si  = Integer.parseInt(props.getProperty("sccp.si"));
        
        socket = new DatagramSocket(localPort, InetAddress.getByName(localAddress));
        new Thread(this).start();
        logger.info("Running main thread");
    }
    
    public void run() {
        while (!stopped) {
            try {
                byte[] buffer = new byte[8446];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                socket.receive(packet);
                if (logger.isDebugEnabled()) {
                    logger.debug(org.mobicents.util.Utils.hexDump("Packet received\n",packet));
                }
                
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);
                
                try {
                    logger.debug("--->" + Utils.hexDump(data));
                    threadPool.execute(new Handler(this, data));
                } catch (InterruptedException ie) {
                    logger.error("Thread pool interrupted", ie);
                    stopped = true;
                }
            } catch (Exception e) {
                logger.error("I/O error occured while sending data to MTP3 driver", e);
            }
        }
    }
    
    public void addSccpListener(SccpListener listener) {
        this.listener = listener;
    }
    
    public SccpListener getListener() {
        return listener;
    }
    
    public void send(SccpAddress calledParty, SccpAddress callingParty, byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        byte b = (byte)(ssf << 4 | si);
        out.write(b);
        b = (byte) dpc;
        out.write(b);
        
        b = (byte)(((dpc >> 8) & 0x3f) |((opc & 0x03) << 6));
        out.write(b);
        
        b = (byte)(opc >> 2);
        out.write(b);
        
        b = (byte)(((opc >> 10) & 0x0f)| (sls << 4));
        out.write(b);
        
        
        UnitData unitData = new UnitData(
                new ProtocolClass(0,0), calledParty, callingParty, data);
        unitData.encode(out);
        byte[] buff = out.toByteArray();
        
        DatagramPacket packet = new DatagramPacket(buff, buff.length,
                InetAddress.getByName(remoteAddress), remotePort);
        logger.debug(Utils.hexDump(buff));
        socket.send(packet);
    }
    
    public void shutdown() {
        stopped = true;
        threadPool.shutdownNow();
        try {
            socket.disconnect();
            socket.close();
        } catch (Exception e) {
        }
    }
}
