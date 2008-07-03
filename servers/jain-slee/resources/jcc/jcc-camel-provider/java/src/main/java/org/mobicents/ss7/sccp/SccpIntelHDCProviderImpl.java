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

import java.io.FileInputStream;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Properties;


import org.mobicents.gct.InterProcessCommunicator;
import org.apache.log4j.Logger;

import java.net.DatagramSocket;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 *
 * @author $Author: kulikoff $
 * @version $Revision: 1.1 $
 */
public class SccpIntelHDCProviderImpl implements Runnable, SccpProvider {
    
    private Properties props;
    private InterProcessCommunicator ipc;
    private DatagramSocket socket;
    private SccpListener listener;
    
    private int src = 0;
    private int dst = 0;
    
    private int opc;
    private int dpc;
    private int sls;
    private int ssf;
    private int si;
    
    private boolean stopped = false;
    private PooledExecutor threadPool = new PooledExecutor(10);
    private Logger logger = Logger.getLogger(SccpIntelHDCProviderImpl.class);
    
    
    
    /** Creates a new instance of SccpProviderImpl */
    public SccpIntelHDCProviderImpl(Properties props) {
        this.props = props;
        
        src = Integer.parseInt(props.getProperty("module.src"));
        dst = Integer.parseInt(props.getProperty("module.dest"));
        
        opc = Integer.parseInt(props.getProperty("sccp.opc"));
        dpc = Integer.parseInt(props.getProperty("sccp.dpc"));
        sls = Integer.parseInt(props.getProperty("sccp.sls"));
        ssf = Integer.parseInt(props.getProperty("sccp.ssf"));
        si  = Integer.parseInt(props.getProperty("sccp.si"));
        
        ipc = new InterProcessCommunicator(src, dst);
        logger.info("Started IPC");
        
        new Thread(this).start();
        logger.info("Started main loop");
    }
    
    public void addSccpListener(SccpListener listener) {
        this.listener = listener;
    }
    
    public SccpListener getListener() {
        return listener;
    }
    
    public synchronized void send(SccpAddress calledParty,
            SccpAddress callingParty, byte[] data) throws IOException {
        
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
        byte[] buf = out.toByteArray();
        
        ipc.send(buf);
        
        if (logger.isDebugEnabled()) {
            logger.debug(org.mobicents.util.Utils.hexDump("Sent message\n", buf));
        }
    }
    
    
    public void run() {
        while (!stopped) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Waiting for packet delivery");
                }
                
                byte[] packet = ipc.receive();
                
                if (logger.isDebugEnabled()) {
                    logger.debug(org.mobicents.util.Utils.hexDump("Packet received\n",packet));
                }
                
                try {
                    threadPool.execute(new Handler(this,packet));
                } catch (InterruptedException ie) {
                    logger.error("Thread pool interrupted", ie);
                    stopped = true;
                }
            } catch (Exception e) {
                logger.error("I/O error occured while sending data to MTP3 driver", e);
            }
        }
        logger.info("Close main loop");
    }
    
    public void shutdown() {
        threadPool.shutdownNow();
        stopped = true;
    }
    
}
