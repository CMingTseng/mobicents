package org.mobicents.media.server.impl.resource.tone;

import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.impl.NotifyEventImpl;

/**
 * 
 * @author amit.bhayani
 * 
 */
public class MultiFreqToneEvent extends NotifyEventImpl {

	public static final int MF_TONE_EVENTID = 1000;

	public MultiFreqToneEvent(BaseComponent component) {
		super(component, MF_TONE_EVENTID);
	}

	

}
