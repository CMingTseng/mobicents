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
package org.mobicents.media.server.impl.enp.cnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.CachedBuffersPool;

/**
 *
 * @author Oleg Kulikov
 */
public class MixerInputStream extends AbstractSink {

    private List<Buffer> buffers = Collections.synchronizedList(new ArrayList());
    private int jitter;
    private long duration;
    protected AudioMixer mixer;
    
    private Logger logger = Logger.getLogger(MixerInputStream.class);
    
    public MixerInputStream(AudioMixer mixer, int jitter) {
        this.mixer = mixer;
        this.jitter = jitter;
    }
    
    public boolean isReady() {
        return !buffers.isEmpty();
    }

    /**
     * Gets the current jitter.
     * 
     * @return jitter value in milliseconds.
     */
    public long getJitter() {
        return duration;
    }

    public void receive(Buffer buffer) {
        if (mixer == null) {
            CachedBuffersPool.release(buffer);
            return;
        }
        
        if (duration + buffer.getDuration() > jitter) {
            //silently discard packet
            CachedBuffersPool.release(buffer);
            return;
        }

        duration += buffer.getDuration();
        buffers.add(buffer);
    }
    
    /**
     * Reads media buffer from this stream with specified duration.
     * 
     * @param duration the duration of the requested buffer in milliseconds.
     * @return buffer which contains duration ms media for 8000Hz, 16bit, linear audio.
     */
    public byte[] read(int duration) {
        int size = 16 * duration;
        byte[] media = new byte[size];

        int count = 0;
        while (count < size && !buffers.isEmpty()) {
            Buffer buff = buffers.get(0);
            
            if (buff.isEOM() || buff.getData() == null) {
                buffers.remove(buff);
                this.duration -= buff.getDuration();
                break;
            }
            
            int len = Math.min(size - count, buff.getLength() - buff.getOffset());
            System.arraycopy(buff.getData(), buff.getOffset(), media, count, len);

            count += len;
            buff.setOffset(buff.getOffset() + len);

            if (buff.getOffset() == buff.getLength()) {
                buffers.remove(buff);
                CachedBuffersPool.release(buff);
                this.duration -= buff.getDuration();
            }
        }

        return media;
    }
    
    @Override
    public String toString() {
        return mixer.toString();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink.isAcceptable(Format).
     */
    public boolean isAcceptable(Format fmt) {
        return fmt.matches(AudioMixer.LINEAR);
    }

    public Format[] getFormats() {
        return AudioMixer.formats;
    }

}
