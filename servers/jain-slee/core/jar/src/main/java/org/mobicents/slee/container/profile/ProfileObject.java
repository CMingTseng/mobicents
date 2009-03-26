/**
 * Start time:16:46:52 2009-03-13<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.slee.container.profile;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.slee.CreateException;
import javax.slee.profile.Profile;
import javax.slee.profile.ProfileContext;
import javax.slee.profile.ProfileSpecificationID;
import javax.slee.profile.ProfileVerificationException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.SleeContainerUtils;
import org.mobicents.slee.container.component.ProfileSpecificationComponent;

/**
 * Start time:16:46:52 2009-03-13<br>
 * Project: mobicents-jainslee-server-core<br>
 * Class representing Profile Object - this object servers as place holder for
 * selected profiles. ProfileObject can belong to only one profile table during
 * its lifecycle, ever.
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class ProfileObject {

	private static final Logger logger = Logger.getLogger(ProfileObject.class);

	private ProfileObjectState state = ProfileObjectState.DOES_NOT_EXIST;
	private ProfileTableConcrete profileTableConcrete = null;
	private ProfileConcrete profileConcrete = null;
	private ProfileContextImpl profileContext = null;
	private String profileName = null;
	private SleeContainer sleeContainer;
	private boolean managementView = false;
	private boolean writeable = false;

	/**
	 * This indicates wheather we are service as snapshot, in that case we are
	 * out of sync, and we can not commit. This is used in update, remove
	 * events.
	 */
	private boolean snapshot = false;
	/**
	 * this flag indicates wheather profile CMP state can be accessed - see
	 * profileActivate method.
	 */
	private boolean canAccessCMP = true;
	private ProfileSpecificationComponent profileSpecificationComponent = null;

	public ProfileObject(ProfileTableConcrete profileTableConcrete, ProfileSpecificationID profileSpecificationId) throws NullPointerException {
		super();
		if (profileTableConcrete == null || sleeContainer == null || profileSpecificationComponent == null) {
			throw new NullPointerException("Parameters must not be null");
		}
		this.profileTableConcrete = profileTableConcrete;
		this.sleeContainer = profileTableConcrete.getProfileManagement().getSleeContainer();
		// this.profileContext = new
		// ProfileContextImpl(this.profileTableConcrete,this.sleeContainer);
		// this.profileContext.setProfileObject(this);
		this.profileSpecificationComponent = this.profileTableConcrete.getProfileManagement().getProfileSpecificationComponent(profileSpecificationId);
		createConcrete();
	}

	private void createConcrete() {
		try {

			// logger.debug(sbbDescriptor.getConcreteSbbClass());
			// Concrete class of the Sbb. the concrete sbb class is the
			// class that implements the Sbb methods. This is obtained
			// from the deployment descriptor and the abstract sbb class.
			this.profileConcrete = (ProfileConcrete) this.profileSpecificationComponent.getProfileCmpConcreteClass().newInstance();
			this.profileConcrete.setProfileObject(this);
			this.profileConcrete.setProfileTableConcrete(this.profileTableConcrete);

		} catch (Exception ex) {

			ex.printStackTrace();
			logger.error("unexpected exception creating concrete class!", ex);
			throw new RuntimeException("Unexpected exception creating concrete class for profile: " + this.profileName + ", from profile table: " + this.profileTableConcrete.getProfileTableName()
					+ " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID(), ex);
		}

	}

	public boolean isSnapshot() {
		return this.snapshot;
	}

	public void setSnapshot() {
		this.snapshot = true;
	}

	/**
	 * if this return true changes to CMPs must be allowed, even if profile is
	 * marked read only, this is true ONLY when management side accesses
	 * profile.
	 * 
	 * @return
	 */
	public boolean isManagementView() {
		return managementView;
	}

	public void setManagementView(boolean managementView) {
		this.managementView = managementView;
	}

	public boolean isWriteable() {
		return writeable;
	}

	public void setWriteable(boolean writeable) {
		this.writeable = writeable;
	}

	/**
	 * In some cases when Profile method is invoked CMP state MUST not be
	 * accessed, this flag indicates by it value if this is taht kind of
	 * situation - for instance see profileActivate
	 * 
	 * @return <ul>
	 *         <li><b>true</b> - if CMP can be accessed</li>
	 *         <li><b>false</b> - if CMP must not be accessed</li>
	 *         </ul>
	 */
	public boolean isCanAccessCMP() {
		return canAccessCMP;
	}

	public boolean isProfileSpecificationWriteable() {
		return !this.profileSpecificationComponent.getDescriptor().getReadOnly();
	}

	public boolean isProfileReentrant() {
		return !this.profileSpecificationComponent.getDescriptor().getProfileAbstractClass().getReentrant();
	}

	public void setCanAccessCMP(boolean canAccessCMP) {
		this.canAccessCMP = canAccessCMP;
	}

	public ProfileObjectState getState() {
		return state;
	}

	public ProfileTableConcrete getProfileTableConcrete() {
		return profileTableConcrete;
	}

	public ProfileConcrete getProfileConcrete() {
		return profileConcrete;
	}

	public void setState(ProfileObjectState state) {
		this.state = state;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		if (profileName == null) {
			throw new NullPointerException("Profile name must not be null");
		}
		this.profileName = profileName;
		this.profileConcrete.setProfileName(profileName);

	}

	public ProfileSpecificationComponent getProfileSpecificationComponent() {
		return profileSpecificationComponent;
	}

	public ProfileContextImpl getProfileContext() {
		return profileContext;
	}

	// FIXME: determine if there is something more to do here
	public void profileActivate() {
		if (this.getState() != ProfileObjectState.POOLED) {
			logger.error("Profile initialize, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileActivate();
		this.state = ProfileObjectState.READY;

	}

	public void profileInitialize() {
		// this is called for default profile when its created

		if (this.getState() != ProfileObjectState.POOLED) {
			logger.error("Profile initialize, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileInitialize();
		this.state = ProfileObjectState.READY;

	}

	public void profileLoad() {
		if (this.getState() != ProfileObjectState.POOLED) {
			logger.error("Profile load, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileLoad();

	}

	public void profilePassivate() {
		// This must be called
		if (this.getState() != ProfileObjectState.READY) {
			logger.error("Profile passivate, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profilePassivate();
		this.state = ProfileObjectState.POOLED;

	}

	public void profilePostCreate() throws CreateException {
		if (this.getState() != ProfileObjectState.POOLED) {
			logger.error("Profile post create, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profilePostCreate();

	}

	public void profileRemove() {
		if (this.getState() != ProfileObjectState.READY) {
			logger.error("Profile remove, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileRemove();
		this.state = ProfileObjectState.POOLED;
	}

	public void profileStore() {
		if (this.getState() != ProfileObjectState.READY) {
			logger.error("Profile store, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileStore();

	}

	public void profileVerify() throws ProfileVerificationException {
		if (this.getState() != ProfileObjectState.READY) {
			logger.error("Profile verify, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		this.profileConcrete.profileVerify();

	}

	/**
	 * Invoked from pool.
	 * 
	 * @param profileContext
	 */
	public void setProfileContext(ProfileContextImpl profileContext) {
		if (logger.isDebugEnabled()) {
			logger.debug("setProfileContext, for profile: " + this.profileName + ", from profile table: " + this.profileTableConcrete.getProfileTableName() + " with specification: "
					+ this.profileSpecificationComponent.getProfileSpecificationID());
		}

		if (profileContext == null) {
			throw new NullPointerException("Passed context must not be null.");
		}
		if (this.state != ProfileObjectState.DOES_NOT_EXIST) {
			throw new IllegalStateException("Wrong state: " + this.state + ",on profile set context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}
		final ClassLoader oldClassLoader = SleeContainerUtils.getCurrentThreadClassLoader();

		// FIXME: is this needed ?
		try {
			final ClassLoader cl = this.profileSpecificationComponent.getClassLoader();
			if (SleeContainer.isSecurityEnabled())
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						Thread.currentThread().setContextClassLoader(cl);
						return null;

					}
				});
			else
				Thread.currentThread().setContextClassLoader(cl);
			if (this.profileConcrete != null) {
				try {
					this.profileContext = profileContext;
					this.profileConcrete.setProfileContext(this.profileContext);
					this.profileContext.setProfileObject(this);
				} catch (Exception ex) {
					if (logger.isDebugEnabled())
						logger.debug("Exception encountered while setting profile context for profile: " + this.profileName + ", from profile table: "
								+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID(), ex);
				}
			}

		} finally {
			if (SleeContainer.isSecurityEnabled())
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						Thread.currentThread().setContextClassLoader(oldClassLoader);
						return null;

					}
				});
			else
				Thread.currentThread().setContextClassLoader(oldClassLoader);

		}

	}

	/**
	 * Invoked when pool removes object
	 */
	public void unsetProfileContext() {

		if (this.state != ProfileObjectState.POOLED) {
			if (logger.isDebugEnabled()) {
				logger.debug("unsetProfileContext, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
						+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
			}
			throw new IllegalStateException("unsetProfileContext, wrong state: " + this.state + ",on profile unset context operation, for profile: " + this.profileName + ", from profile table: "
					+ this.profileTableConcrete.getProfileTableName() + " with specification: " + this.profileSpecificationComponent.getProfileSpecificationID());
		}

		final ClassLoader oldClassLoader = SleeContainerUtils.getCurrentThreadClassLoader();

		// FIXME: is this needed ?
		try {
			final ClassLoader cl = this.profileSpecificationComponent.getClassLoader();
			if (SleeContainer.isSecurityEnabled())
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						Thread.currentThread().setContextClassLoader(cl);
						return null;

					}
				});
			else
				Thread.currentThread().setContextClassLoader(cl);
			if (this.profileConcrete != null) {
				this.profileConcrete.unsetProfileContext();
				this.profileContext.setProfileObject(null);
			}

		} finally {
			if (SleeContainer.isSecurityEnabled())
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						Thread.currentThread().setContextClassLoader(oldClassLoader);
						return null;

					}
				});
			else
				Thread.currentThread().setContextClassLoader(oldClassLoader);

		}

	}

}
