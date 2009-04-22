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
package org.mobicents.media.server.impl.dsp;

import java.util.List;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.dsp.Codec;

/**
 *
 * @author kulikov
 */
public class CompositeCodec implements Codec {
    
    private List<Codec> sequence;
    private int index;
    
    public CompositeCodec(List<Codec> sequence) {
        this.sequence = sequence;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.dsp.Codec#getSupportedInputFormat(). 
     */
    public Format getSupportedInputFormat() {
        return sequence.get(0).getSupportedInputFormat();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.dsp.Codec#getSupportedOutputFormat(). 
     */
    public Format getSupportedOutputFormat() {
        return sequence.get(sequence.size() - 1).getSupportedOutputFormat();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.dsp.Codec#process();
     */
    public void process(Buffer buffer) {
        sequence.get(index).process(buffer);
        if (buffer.getFlags() == Buffer.FLAG_FLUSH) {
            index = index == sequence.size() - 1 ? 0 : index + 1;
        }
    }
}
