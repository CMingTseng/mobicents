package org.mobicents.slee.services.sip.location;

import java.io.Serializable;

import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.header.ContactHeader;

/**
 * TODO
 * @author martins
 *
 */
public abstract class RegistrationBinding implements Serializable {
	
	public abstract String getCallId();
    
    public abstract void setCallId(String callId);

    public abstract String getComment();
    
    public abstract void setComment(String comment);
       
    public abstract long getCSeq();
    
    public abstract void setCSeq(long cSq);
    
	public abstract long getExpires();

	public abstract void setExpires(long expires);
    
	public abstract float getQValue();
    
    public abstract void setQValue(float q);

    public abstract long getRegistrationDate();
    
    public abstract void setRegistrationDate(long registrationDate);
    
    public abstract Address getContactAddress(); 
	
	public abstract String getSipAddress();
	
    // --- logic methods
    
    /**
     * Returns number of mseconds till this entry expires
     * May be 0 or -ve if already expired
     */
	public long getExpiresDelta() {
        return ((getExpires()-(System.currentTimeMillis()-getRegistrationDate())/1000));  
    }

    public ContactHeader getContactHeader(javax.sip.address.AddressFactory af, javax.sip.header.HeaderFactory hf) throws InvalidArgumentException {
        
    	int expiresDelta = (int) getExpiresDelta();
    	if (expiresDelta <= 0) {
    		// This binding has expired
    		return null;
    	}
    	javax.sip.header.ContactHeader contact = hf.createContactHeader(getContactAddress());
    	// String comment = getComment();
    	contact.setExpires(expiresDelta);
    	contact.setQValue(getQValue());
    	return contact;
    }

	public String toString() {
		return "RegistrationBinding[sipAddress=" + getSipAddress() + ",contactAddress=" +getContactAddress().getURI()+ ",comment=" + getComment() + ",expires=" +
			getExpires() + ",qValue=" + getQValue() + ",callId=" + getCallId() + ",cSeq=" + getCSeq() + "]";
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getContactAddress() == null) ? 0 : getContactAddress().hashCode());
		result = prime * result
				+ ((getSipAddress() == null) ? 0 : getSipAddress().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == this.getClass()) {
			RegistrationBinding other = (RegistrationBinding) obj;
			return this.getSipAddress().equals(other.getSipAddress()) && this.getContactAddress().equals(other.getContactAddress()); 
		}
		else {
			return false;
		}
	}
}
