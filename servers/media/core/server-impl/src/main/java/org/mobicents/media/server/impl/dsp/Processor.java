/*
 * Mobicents Media Gateway
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
package org.mobicents.media.server.impl.dsp;

import java.util.ArrayList;

import java.util.List;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;

/**
 * Implements DSP features.
 * 
 * Processor has input and output and is used to perform required 
 * transcoding if needed for packets followed from source to consumer. 
 * Processor is transparent for packets with format acceptable by consumer 
 * by default. 
 * 
 * @author Oleg Kulikov
 */
public class Processor extends BaseComponent implements SignalingProcessor {

    
    private ArrayList<Format> inputFormats = new ArrayList();
    private ArrayList<Format> outputFormats = new ArrayList();
    
    private Input input;
    private Output output;
    
    private transient ArrayList<Codec> codecs = new ArrayList();    
    private Codec codec;
    
    public Processor(String name) {
        super(name);
        input = new Input();
        output = new Output();
    }

    private void append(List<Format> list, Format fmt) {
        for (Format f : list) {
            if (f.matches(fmt)) return;
        }
        list.add(fmt);
    }
    
    private Format[] toArray(List<Format> list) {
        Format[] array = new Format[list.size()];
        list.toArray(array);
        return array;
    }
    
    protected void add(Codec codec) {
        codecs.add(codec);        
        append(inputFormats, codec.getSupportedInputFormat());
        append(outputFormats, codec.getSupportedOutputFormat());
    }
    
    /**
     * Gets the input for original media
     * 
     * @return media handler for input media.
     */
    public MediaSink getInput() {
        return input;
    }

    /**
     * Gets the output stream with transcoded media.
     * 
     * @return media stream.
     */
    public MediaSource getOutput() {
        return output;
    }

    /**
     * Checks is format presented in the list.
     * 
     * @param fmts the list of formats to check
     * @param fmt the format instance to check.
     * @return true if fmt is in list of fmts.
     */
    private boolean contains(Format[] fmts, Format fmt) {
        for (int i = 0; i < fmts.length; i++) {
            if (fmts[i].matches(fmt)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements output of the processor.
     */
    private class Output extends AbstractSource {
        //references to the format of last processed packet
        private Format format;
        protected boolean connected = false;
        protected int failDeliveryCount = 0;

        private boolean started = false;
        
        public Output() {
            super("Processor.Output");
        }

        @Override
        public void connect(MediaSink sink) {
            super.connect(sink);
        }

        protected boolean isConnected() {
            return otherParty!= null;
        }
        
        protected Format[] getOtherPartyFormats() {
            return otherParty.getFormats();
        }
        
        public void start() {            
            started = true;
        }

        public void stop() {
            started = false;
        }

        public Format[] getFormats() {
            return toArray(outputFormats);
        }

        /**
         * Transmits buffer to the output handler.
         * 
         * @param buffer the buffer to transmit
         */
        protected void transmit(Buffer buffer) {
            if (!started) {
            	buffer.dispose();
                return;
            }
            //Here we work in ReceiveStream.run method, which runs in local ReceiveStreamTimer
            // Discard packet silently if output handler is not assigned yet
            if (otherParty == null) {
            	buffer.dispose();
                return;
            }

            //compare format of the currently processing packet with last one
            //and if same use same codec also else reassign codec
            if (format == null || !format.equals(buffer.getFormat())) {
                //disable last used transformator
                for (Codec c: codecs) {
                    boolean found = false;
                    if (c.getSupportedInputFormat().matches(buffer.getFormat())) {
                        Format[] otherFormats = output.getOtherPartyFormats();
                        for (Format f : otherFormats) {
                            if (f.matches(c.getSupportedOutputFormat())) {
                                codec = c;
                                format = buffer.getFormat();
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
            }
            
            if (codec != null) {
                codec.process(buffer);
            }
            // Codec can delay media transition if it has not enouph media
            // to perform its job. 
            // It means that Processor should check FLAGS after codec's 
            // work and discard packet if required
            if (buffer.getFlags() == Buffer.FLAG_DISCARD) {
            	buffer.dispose();
                return;
            }

            //may be a situation when original format can not be trancoded to 
            //one of the required output. In this case codec map will have no 
            //entry for this format. also codec may has no entry in case of when 
            //transcoding is not required. to differentiate these two cases check
            //if this format is acceptable by the consumer.

            //deliver packet to the consumer
            try {
                otherParty.receive(buffer);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Implements output of the processor.
     */
    private class Input extends AbstractSink {

        public Input() {
            super("Processor.Input");
        }

        @Override
        public void connect(MediaSource source) {
            super.connect(source);
            Format[] fmts = otherParty.getFormats();
            for (Format f: fmts) {
                append(outputFormats, f);
            }
        }
        
        public boolean isAcceptable(Format format) {
            return true;
        }

        public void receive(Buffer buffer) {
            output.transmit(buffer);
        }

        protected boolean isConnected() {
            return otherParty!= null;
        }
        
        protected Format[] getOtherPartyFormats() {
            return otherParty.getFormats();
        }
        
        public Format[] getFormats() {
            return toArray(inputFormats);
        }
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.dsp.SignalingProcessor.configure(Format[], Format[]).
     */
    public void configure(Format[] inputFormats, Format[] outputFormats) {
    }

}
