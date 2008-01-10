/*
 * Created on Jul 30, 2004
 *
 *The Open SLEE project
 *
 * The source code contained in this file is in in the public domain.          
 * It can be used in any project or product without prior permission, 	      
 * license or royalty payments. There is no claim of correctness and
 * NO WARRANTY OF ANY KIND provided with this code.
 *
 */

package org.mobicents.slee.runtime;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;
import javax.transaction.SystemException;

import org.jboss.logging.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.SleeActivityHandle;
import org.mobicents.slee.runtime.sbbentity.SbbEntity;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

/**
 * 
 * Activity context interface - default implementation. The Sbb deployer has to
 * imbed an instance of this as a proxy object in each sbb ACI.
 * 
 * This is the SLEE wrapper data structure for Activity Contexts. The Sbb gets
 * to access this rather than the activity. The reason this exists is because
 * the activity context can be at a different location than the activity context
 * interface (does not need to be co-located in the same jvm. )
 * 
 * @author M. Ranganathan
 * @author Ralf Siedow
 *  
 */
public class ActivityContextInterfaceImpl implements
        ActivityContextIDInterface, ActivityContextInterface {
    // anchor
    private SleeContainer serviceContainer;

    private ActivityContextFactory acif;

    private String acId;

    private static Logger logger;

    static {
        logger = Logger.getLogger(ActivityContextInterfaceImpl.class);
    }

    /**
     * This is allocated by the Slee to wrap an incoming event (activity).
     * 
     * @param serviceContainer
     * @param activity
     * @param activity
     */
    public ActivityContextInterfaceImpl(SleeContainer serviceContainer,
            String activityContextId) {
        if (activityContextId == null)
            throw new NullPointerException("Null activityContextId Crap!");
        this.serviceContainer = serviceContainer;
        this.acif = serviceContainer.getActivityContextFactory();
        this.acId = activityContextId;

    }

    public ActivityContext getActivityContext() {
        if (logger.isDebugEnabled())
            logger.debug("Getting activity context  ID["+acId+"]");

        return this.acif.getActivityContextById(acId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.ActivityContextInterface#getActivity()
     */
    public Object getActivity() throws TransactionRequiredLocalException,
            SLEEException {
        SleeContainer.getTransactionManager().mandateTransaction();
        Object activity = this.acif.getActivityFromKey(acId);
        if (logger.isDebugEnabled()) {
            logger.debug("getActivity() : activity = " + activity);
        }

        if (activity instanceof SleeActivityHandle) {

            SleeActivityHandle sleeHandle = (SleeActivityHandle) activity;
            Object ret = sleeHandle.getActivity();
            if (logger.isDebugEnabled()) {
                logger.debug("getActivity(): returning " + ret);
            }
            return ret;

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("getActivity(): returning " + activity);
            }
            return activity;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.ActivityContextInterface#attach(javax.slee.SbbLocalObject)
     */
    public void attach(SbbLocalObject sbbLocalObject)
            throws NullPointerException, TransactionRequiredLocalException,
            TransactionRolledbackLocalException, SLEEException {

        if (sbbLocalObject == null)
            throw new NullPointerException("null SbbLocalObject !");
        SleeTransactionManager txMgr = SleeContainer.getTransactionManager();
        txMgr.mandateTransaction();
        SbbLocalObjectImpl sbbLocalObjectImpl = (SbbLocalObjectImpl) sbbLocalObject;

        String sbbeId = sbbLocalObjectImpl.getSbbEntityId();

        boolean attached = getActivityContext().attachSbbEntity(sbbeId);

        if (logger.isDebugEnabled()) {
            logger
                    .debug("ActivityContextInterface.attach(): ACI attach Called for "
                            + sbbLocalObject
                            + " ACID = "
                            + this.acId
                            + "SbbEntityId " + sbbeId);
        }
        
        boolean setRollbackAndThrowException = false;
        try {
        	SbbEntity sbbEntity = sbbLocalObjectImpl.getSbbEntity();
        	if (sbbEntity.isRemoved()) {
        		setRollbackAndThrowException = true;
        	}
        	else {
        		// attach entity from ac
        		sbbEntity.afterACAttach(acId);
        	}
        }
        catch (Exception e) {
        	setRollbackAndThrowException = true;
        }
        if (setRollbackAndThrowException) {
        	try {
				txMgr.setRollbackOnly();
			} catch (SystemException e) {
				logger.warn("failed to set rollback flag while asserting valid sbb entity",e);
			}
            throw new TransactionRolledbackLocalException(
                        "Failed to attach invalid sbb entity. SbbID " + sbbeId);
        }
        
        if (attached) {
            ActivityContext localAc = acif.getActivityContextById(acId);
        	//            	JSLEE 1.0 Spec, Section 8.5.8 excerpt:
        	//        		The SLEE delivers the event to an SBB entity that stays attached once. The SLEE may deliver the
        	//        		event to the same SBB entity more than once if it has been detached and then re -attached. 
            if (localAc.removeFromDeliveredSet(sbbeId)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removed the SBB Entity [" + sbbeId
                            + "] from the delivered set of activity context ["
                            + localAc.getActivityContextId()
                            + "]. Seems to be a reattachment after detachment in the same event delivery transaction. See JSLEE 1.0 Spec, Section 8.5.8.");
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.ActivityContextInterface#detach(javax.slee.SbbLocalObject)
     */
    public void detach(SbbLocalObject sbbLocalObject)
            throws NullPointerException, TransactionRequiredLocalException,
            TransactionRolledbackLocalException, SLEEException {
        if (logger.isDebugEnabled()) {
            logger.debug("ACI detach called for : " + sbbLocalObject
                    + " ACID = " + this.acId);
        }

        if (sbbLocalObject == null)
            throw new NullPointerException("null SbbLocalObject !");
        
        SleeTransactionManager txMgr = serviceContainer.getTransactionManager();
        
        txMgr.mandateTransaction();
        
        SbbLocalObjectImpl sbbLocalObjectImpl = (SbbLocalObjectImpl) sbbLocalObject;

        String sbbeId = sbbLocalObjectImpl.getSbbEntityId();

        // detach ac from entity
        acif.getActivityContextById(acId).detachSbbEntity(sbbeId);

        boolean setRollbackAndThrowException = false;
        try {
        	SbbEntity sbbEntity = sbbLocalObjectImpl.getSbbEntity();
        	if (sbbEntity.isRemoved()) {
        		setRollbackAndThrowException = true;
        	}
        	else {
        		// detach entity from ac
        		sbbEntity.afterACDetach(acId);
        	}
        }
        catch (Exception e) {
        	setRollbackAndThrowException = true;
        }
        if (setRollbackAndThrowException) {
        	try {
				txMgr.setRollbackOnly();
			} catch (SystemException e) {
				logger.warn("failed to set rollback flag while asserting valid sbb entity",e);
			}
            throw new TransactionRolledbackLocalException(
                        "Failed to detach invalid sbb entity. SbbID " + sbbeId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.ActivityContextInterface#isEnding()
     */
    public boolean isEnding() throws TransactionRequiredLocalException,
            SLEEException {
        SleeContainer.getTransactionManager().mandateTransaction();
        ActivityContext localAc = acif.getActivityContextById(acId);
        return localAc.isEnding();
    }

    public String retrieveActivityContextID() {

        return acId;
    }

    public ActivityContext retrieveActivityContext() {

        return this.getActivityContext();
    }

}