package org.mobicents.slee.runtime.eventrouter.routingtask;

import javax.slee.ActivityContextInterface;
import javax.transaction.SystemException;

import org.apache.log4j.Logger;
import org.mobicents.slee.runtime.eventrouter.SbbInvocationState;
import org.mobicents.slee.runtime.sbb.SbbObject;
import org.mobicents.slee.runtime.sbb.SbbObjectState;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

public class HandleRollback {

	private static final Logger logger = Logger.getLogger(HandleRollback.class);
	
	/**
	 * 
	 * @param sbbObject
	 *            The sbb object that was being invoked when the exception was
	 *            caught - can be null if SLEE wasn't calling an sbb object when
	 *            the exception was thrown
	 * @param eventObject
	 *            The slee event - only specified if the transaction that rolled
	 *            back tried to deliver an event
	 * @param aci
	 *            The aci where the event was fired - only specified if the transaction that rolled
	 *            back tried to deliver an event
	 * @param e -
	 *            the exception caught
	 * @param contextClassLoader
	 * 
	 * @return
	 */
	public boolean handleRollback(SbbObject sbbObject, Object eventObject, ActivityContextInterface aci,
			Exception e, ClassLoader contextClassLoader,SleeTransactionManager txMgr) {
		
		txMgr.assertIsInTx();

		boolean invokeSbbRolledBack = false;

		if (e != null && e instanceof RuntimeException) {

			// See spec. 9.12.2 for full details of what we do here
			if (logger.isInfoEnabled())
				logger.info("Caught RuntimeException in invoking SLEE originated invocation",e);

			// We only invoke sbbExceptionThrown if there is an sbb Object *and* an sbb object method was being invoked when the exception was thrown
			if (sbbObject != null && sbbObject.getInvocationState() != SbbInvocationState.NOT_INVOKING) {
				
				if (logger.isDebugEnabled()) {
					logger.debug("sbbObject is not null");
				}
				// Invoke sbbExceptionThrown method but only if it was a sbb method that threw the RuntimeException

				ClassLoader oldClassLoader = Thread.currentThread()
						.getContextClassLoader();
				
				try {
					Thread.currentThread().setContextClassLoader(
							contextClassLoader);

					try {
						txMgr.setRollbackOnly();
					} catch (SystemException ex) {
						throw new RuntimeException("Unexpected exception ! ",
								ex);
					}
					
					// Spec. 6.9. event and activity are null if exception was not thrown at event handler
					if (logger.isDebugEnabled()) {
						logger.debug("Calling sbbExceptionThrown");
					}
					try {
						sbbObject.sbbExceptionThrown(e, eventObject, aci);
					} catch (Exception ex) {

						// If method throws an exception , just log it.
						if (logger.isDebugEnabled()) {
							logger.debug("Threw an exception while invoking sbbExceptionThrown ",ex);
						}
					}

					// Spec section 6.10.1
					// The sbbRolledBack method is only invoked on SBB objects
					// in the Ready state.
					invokeSbbRolledBack = sbbObject.getState() == SbbObjectState.READY;

					// now we move the object to the does not exist state
					// (6.9.3)
					sbbObject.setState(SbbObjectState.DOES_NOT_EXIST);
					
					if (logger.isDebugEnabled()) {
						logger.debug("handleRollback done");
					}
					
				} finally {
					Thread.currentThread()
							.setContextClassLoader(oldClassLoader);
				}
			}

		} else {
			
			if (logger.isDebugEnabled()) {
				logger.debug("Runtime exception was not thrown");
			}
			// See 9.12.2

			// We do this block if either the invocation sequence completed successfully OR only a checked exception was thrown

			if (sbbObject != null && sbbObject.getSbbContext().getRollbackOnly()) {
				
				if (logger.isDebugEnabled()) {
					logger.debug("object is set rollbackonly=true");
					// The SBB signaled that a rollback is needed. run the rollback method.
					logger.debug("sbb rolled back context "
							+ sbbObject.getSbbContext());
				}
				
				// Spec section 6.10.1
				// The sbbRolledBack method is only invoked on SBB objects in the Ready state.
				invokeSbbRolledBack = sbbObject.getState() == SbbObjectState.READY;

			}
		}

		if (sbbObject == null && e != null) {
			invokeSbbRolledBack = true;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("InvokeSbbRolledBack?:" + invokeSbbRolledBack);
		}
		
		return invokeSbbRolledBack;
	}
	
}
