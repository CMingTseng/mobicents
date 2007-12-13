/*
 * IVRContext.java
 *
 * Created on 7 ���� 2007 �., 18:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mobicents.slee.resource.media.ratype;

import java.net.URL;

/**
 *
 * @author Oleg Kulikov
 */
public interface IVRContext extends AnnouncementContext {
    public void record(URL file);
    public void stopRecorder();
    public boolean isRecording();
}
