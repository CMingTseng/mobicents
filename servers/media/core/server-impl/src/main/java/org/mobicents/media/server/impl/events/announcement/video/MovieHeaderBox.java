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
import java.util.Calendar;
import java.util.Date;

/**
 * This box defines overall information which is media-independent, and relevant 
 * to the entire presentation considered as a whole.
 * 
 * Box Type: mvhd
 * Container: Movie Box
 * Mandatory: Yes
 * Quantity: Exactly one
 * 
 * @author kulikov
 */
public class MovieHeaderBox extends FullBox {

    /** 
     * is an integer that declares the creation time of the presentation 
     * (in seconds since midnight, Jan. 1, 1904, in UTC time 
     */
    private long creationTime;
    /**
     * is an integer that declares the most recent time the presentation was 
     * modified (inseconds since midnight, Jan. 1, 1904, in UTC time)
     */
    private long modificationTime;
    /**
     * is an integer that specifies the time-scale for the entire presentation; 
     * this is the number of time units that pass in one second. For example, a 
     * time coordinate system that measures time in sixtieths of a second has a 
     * time scale of 60.
     */
    private int timescale;
    /**
     * is an integer that declares length of the presentation (in the indicated 
     * timescale). This property is derived from the presentation�s tracks: 
     * the value of this field corresponds to the duration of the longest track 
     * in the presentation
     */
    private long duration;
    
    /** indicates the preferred rate to play the presentation */
    private double rate;
    /** indicates the preferred playback volume */
    private double volume;
    
    /** provides a transformation matrix for the video */
    private int[] matrix = new int[9];
    private int[] predefined = new int[6];
    
    /**
     * is a non-zero integer that indicates a value to use for the track ID of 
     * the next track to be added to this presentation. Zero is not a valid 
     * track ID value. The value of next_track_ID shall be larger than the 
     * largest track-ID in use. If this value is equal to all 1s (32-bit maxint), 
     * and a new mediatrack is to be added, then a search must be made in the 
     * file for an unused track identifier.
     */
    private int nextTrackID;
    private Calendar calendar = Calendar.getInstance();
    
    public MovieHeaderBox(long size, String type) {
        super(size, type);
    }
    
    /**
     * Gets the creation time of this presentation.
     * 
     * @return creation time
     */
    public Date getCreationTime() {
        calendar.set(Calendar.YEAR, 1904);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        calendar.roll(Calendar.SECOND, (int)creationTime);
        return calendar.getTime();
    }

    /**
     * Gets the modification time of this presentation.
     * 
     * @return creation time
     */
    public Date getModificationTime() {
        return new Date(modificationTime);
    }

    /**
     * Gets the time scale of this presentation.
     * 
     * @return the number of time units that pass in one second. For example, a 
     * time coordinate system that measures time in sixtieths of a second has a 
     * time scale of 60.
     */
    public int getTimeScale() {
        return timescale;
    }
    
    /**
     * Gets duration of this presentation.
     * 
     * @return an integer that declares length of the presentation (in the indicated 
     * timescale). This property is derived from the presentation�s tracks: 
     * the value of this field corresponds to the duration of the longest track 
     * in the presentation
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Gets the preffered rate to play the presentation. 
     * 
     * @return the rate value, value 1.0 is normal forward playback
     */
    public double getRate() {
        return rate;
    }
    
    /**
     * Gets the preffered volume to play the presentation. 
     * 
     * @return the rate value, value 1.0 is full volume
     */
    public double getVolume() {        
        return volume;
    }
    
    public int[] getMatrix() {
        return matrix;
    }
    
    /**
     * Gets identifier of next track to use.
     * 
     * @return a non-zero integer that indicates a value to use for the track ID
     */
    public int getNextTrackID() {
        return nextTrackID;
    }
    
    @Override
    protected int load(DataInputStream fin) throws IOException {
        super.load(fin);        
        if (this.getVersion() == 1) {
            this.creationTime = read64(fin);
            this.modificationTime = read64(fin);
            this.timescale = fin.readInt();
            this.duration = read64(fin);
        } else {
            this.creationTime = fin.readInt();
            this.modificationTime = fin.readInt();
            this.timescale = fin.readInt();
            this.duration = fin.readInt();
        }
        
        //reading rate. it is a fixed point 16.16 number that indicates the 
        //preferred rate to play the presentation
        int a = fin.readInt();
        rate = (a >> 16) + (a & 0xffff)/10;
        
        //reading volume. it is a fixed 8.8 number
        volume = fin.readByte() + fin.readByte()/10;
        
        //skip reserved 16bits
        fin.readByte();
        fin.readByte();
        
        fin.readInt();
        fin.readInt();
        
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = fin.readInt();
        }
        
        for (int i = 0; i < predefined.length; i++) {
            predefined[i] = fin.readInt();
        }
        
        this.nextTrackID = fin.readInt();
        return 100;
    }
     
}
