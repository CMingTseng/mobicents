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
package org.mobicents.media.server.impl.events.announcement.video;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The video media header contains general presentation information, independent 
 * of the coding, for video media. Note that the flags field has the value 1.
 * 
 * @author kulikov
 */
public class VideoMediaHeaderBox extends FullBox {

    private int graphicsMode;
    private int[] opColors = new int[3];
    
    public VideoMediaHeaderBox(long size, String type) {
        super(size, type);
    }
    
    @Override
    protected int load(DataInputStream fin) throws IOException {
        super.load(fin);
        graphicsMode = (fin.readByte() << 8) | fin.readByte();
        
        for (int i = 0; i < 3; i++) {
            opColors[i] = (fin.readByte() << 8) | fin.readByte();
        }
        return (int) getSize();
    }    
    
}
