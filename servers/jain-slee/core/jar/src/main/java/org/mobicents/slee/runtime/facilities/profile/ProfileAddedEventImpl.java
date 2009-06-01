/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/

package org.mobicents.slee.runtime.facilities.profile;

import javax.slee.EventTypeID;
import javax.slee.profile.ProfileAddedEvent;
import javax.slee.profile.ProfileLocalObject;

import org.mobicents.slee.container.component.profile.ProfileEntity;

/**
 * Profile added event implementation.
 * 
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski</a>
 * @author martins
 * 
 */
public class ProfileAddedEventImpl extends AbstractProfileEvent implements ProfileAddedEvent {

	public static EventTypeID EVENT_TYPE_ID = new EventTypeID("javax.slee.profile.ProfileAddedEvent", "javax.slee", "1.0");

	public ProfileAddedEventImpl(ProfileEntity profileEntity) {
		super(profileEntity);
	}

	@Override
	public EventTypeID getEventTypeID() {
		return EVENT_TYPE_ID;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.profile.ProfileAddedEvent#getAddedProfile()
	 */
	public Object getAddedProfile() {
		if (isProfileClassVisible()) {
			return getProfileObjectValidInCurrentTransaction(getProfileConcreteAfterAction()).getProfileCmpSlee10Wrapper();
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.profile.ProfileAddedEvent#getAddedProfileLocal()
	 */
	public ProfileLocalObject getAddedProfileLocal() {
		if (isProfileClassVisible()) {
			return getProfileObjectValidInCurrentTransaction(getProfileConcreteAfterAction()).getProfileLocalObject();
		} else {
			return null;
		}
	}
}
