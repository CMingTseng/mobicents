package org.mobicents.slee.resource.sip11.wrappers;

import javax.sip.Transaction;

import org.mobicents.slee.resource.sip11.SipActivityHandle;

public abstract class SuperTransactionWrapper implements WrapperSuperInterface{

	protected SipActivityHandle sipActivityHandle;
	protected Transaction wrappedTransaction;

	public Object getApplicationData() {
		throw new SecurityException();
	}

	public void setApplicationData(Object arg0) {
		throw new SecurityException();
	}
	
	public SipActivityHandle getActivityHandle() {
		return this.sipActivityHandle;
	}

	public Transaction getWrappedTransaction() {
		return wrappedTransaction;
	}	
	
}
