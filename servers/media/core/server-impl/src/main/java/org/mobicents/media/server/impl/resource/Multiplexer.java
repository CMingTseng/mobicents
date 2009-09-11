/*
 * Mobicents, Communications Middleware
 * 
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA  02110-1301  USA
 */
package org.mobicents.media.server.impl.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSinkSet;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.SyncSource;

/**
 * Combines several signals for transmission over a single medium. A
 * demultiplexor completes the process by separating multiplexed signals from a
 * transmission line. Frequently a multiplexor and demultiplexor are combined
 * into a single device capable of processing both outgoing and incoming
 * signals.
 * <br>Multiplexer combines data and sends them, it is used as output for components.
 * @author Oleg Kulikov
 */
public class Multiplexer extends AbstractSinkSet implements Outlet, SyncSource {

    private Format[] inputFormats = null;
    private Output output;
    private Buffer buff;
    private long timestamp;
    
    /**
     * Creates new instance of multiplexer.
     * 
     * @param name the name of the multiplexer to be created.
     */
    public Multiplexer(String name) {
        super(name);
        output = new Output(name);
        output.setSyncSource(this);
    }

    public void connect(MediaSink sink) {
        output.connect(sink);
    }

    public void disconnect(MediaSink sink) {
        output.disconnect(sink);
    }

    @Override
    public AbstractSink createSink(MediaSource otherParty) {
        Input input = new Input(getName() + "[input]");
        input.setEndpoint(getEndpoint());
        input.setConnection(getConnection());
        return input;
    }

    @Override
    public void start() {
        output.start();
    }

    @Override
    public void stop() {
        output.stop();
    }
    
    /**
     * This method allows to access identifier from the inner source 
     * and sink implemetation.
     * 
     * @return the unique identifer of this component.
     */
    private String getIdentifier() {
        return getId();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Outlet#getOutput().
     */
    public MediaSource getOutput() {
        return output;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getFormats().
     */
    public Format[] getFormats() {
        return output.getOtherPartyFormats();
    }

    @Override
    public void setConnection(Connection connection) {
        super.setConnection(connection);
        output.setConnection(connection);

        Collection<AbstractSink> streams = getStreams();
        for (AbstractSink stream : streams) {
            stream.setConnection(connection);
        }
    }

    @Override
    public void setEndpoint(Endpoint endpoint) {
        super.setEndpoint(endpoint);
        output.setEndpoint(endpoint);
        
        Collection<AbstractSink> list = getStreams();
        for (AbstractSink stream : list) {
            stream.setEndpoint(endpoint);
        }
    }
    
    /**
     * Reassemblies the list of used formats. This method is called each time
     * when connected/disconnected source
     */
    private void reassemblyFormats() {
        ArrayList list = new ArrayList();
        Collection<AbstractSink> streams = getStreams();
        for (AbstractSink stream : streams) {
            Format[] fmts = ((Input)stream).getOtherPartyFormats();
            for (Format format : fmts) {
                if (!list.contains(format)) {
                    list.add(format);
                }
            }
        }

        inputFormats = new Format[list.size()];
        list.toArray(inputFormats);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isAcceptable(org.mobicents.media.Format) 
     */
    public boolean isAcceptable(Format fmt) {
        return output.isAcceptable(fmt);
    }

    /**
     * Implement input stream.
     */
    class Input extends AbstractSink {

        /**
         * Creates new input.
         * 
         * @param name the name of parent MUX.
         */
        public Input(String name) {
            super(name);
        }

        @Override
        public String getId() {
            return getIdentifier();
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSink#isAcceptable(org.mobicents.media.Format) 
         */
        public boolean isAcceptable(Format fmt) {
            return output.isAcceptable(fmt);
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.server.impl.AbstractSink#onMediaTransfer(org.mobicents.media.Buffer). 
         */
        public void onMediaTransfer(Buffer buffer) throws IOException {
            buff = buffer;
            timestamp = buffer.getTimeStamp();
            output.run();
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSink#getFormats().
         */
        public Format[] getFormats() {
            return output.getOtherPartyFormats();
        }

        /**
         * Reads supported formats from other party if connected.
         * 
         * @return if other party connected returns array of supported formats
         * or empty array if not connected.
         */
        protected Format[] getOtherPartyFormats() {
            return otherParty.getFormats();
        }

    }

    class Output extends AbstractSource {

        private volatile boolean stopped = true;

        /**
         * Creates Ouput.
         * 
         * @param name the name of parent MUX.
         */
        public Output(String name) {
            super(name);
        }

        @Override
        public void start() {
            stopped = false;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSource#getFormats().
         */
        public Format[] getFormats() {
            reassemblyFormats();
            return inputFormats;
        }

        /**
         * Checks is other party supports specofied format.
         * 
         * @param fmt the format to check
         * @return true if other party supports this format.
         */
        public boolean isAcceptable(Format fmt) {
            return otherParty != null && otherParty.isAcceptable(fmt);
        }

        /**
         * Gets list of formats supported by other party.
         * 
         * @return array of formats or empty array if not connected yet.
         */
        public Format[] getOtherPartyFormats() {
            return otherParty != null ? otherParty.getFormats() : new Format[0];
        }

        @Override
        public void evolve(Buffer buffer, long timestamp, long sequenceNumber) {
            if (!stopped) {
                buffer.copy(buff);
            }
        }
    }

    @Override
    public void onMediaTransfer(Buffer buffer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void destroySink(AbstractSink sink) {
    }

    public void sync(MediaSource mediaSource) {
    }

    public void unsync(MediaSource mediaSource) {
    }

    public long getTimestamp() {
        return timestamp;
    }

}

