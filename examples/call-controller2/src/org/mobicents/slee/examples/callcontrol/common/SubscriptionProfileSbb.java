/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/
package org.mobicents.slee.examples.callcontrol.common;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.sip.RequestEvent;
import javax.sip.address.*;
import javax.sip.header.CallIdHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import javax.slee.*;
import javax.slee.facilities.FacilityException;
import javax.slee.profile.AttributeNotIndexedException;
import javax.slee.profile.AttributeTypeMismatchException;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileID;
import javax.slee.profile.UnrecognizedAttributeException;
import javax.slee.profile.UnrecognizedProfileNameException;
import javax.slee.profile.UnrecognizedProfileTableNameException;

import org.apache.log4j.Logger;

import org.mobicents.slee.examples.callcontrol.profile.*;
import org.mobicents.slee.resource.sip.SipFactoryProvider;

/**
 * Base SBB offering profile lookup funcationality
 * @author torosvi
 * @author iivanov
 *
 */
public abstract class SubscriptionProfileSbb implements Sbb {
	protected Logger log = Logger.getLogger(this.getClass());
	
    public void setSbbContext(SbbContext context) {
		this.sbbContext = context;
		try {
			//"If NamingException is thrown check jmx-console -> JNDIView -> 
			// list or sbb-jar (check entity-bindning) for proper JNDI path!!!!"
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            // Getting JAIN SIP Resource Adaptor interfaces
			fp = (SipFactoryProvider) myEnv.lookup("slee/resources/jainsip/1.2/provider");
            // To create Address objects from a particular implementation of JAIN SIP
            addressFactory = fp.getAddressFactory();
	        // To create Request and Response messages from a particular implementation of JAIN SIP    	                       
            messageFactory = fp.getMessageFactory();
            // To allow SBB entities to interrogate the profile database to find
            // profiles that match a selection criteria         
            profileFacility = (ProfileFacility) myEnv.lookup("slee/facilities/profile");
            
        } catch (NamingException ne){
        	log.error("COULD NOT LOCATE RESOURCE IN JNDI: Check JNDI TREE or entity-binding for proper path!!!", ne);
        }		      
	}
    
    /**
     * Looking up the called user data -> First we need a Profile CMP object to
     * access them.
     * @param address: Called user address.
     * @return CallControlProfileCMP: The SBB uses the returned Profile CMP object
     * to access the attributes of the Profile identified by the profileID.
     */
    protected CallControlProfileCMP lookup(javax.slee.Address address) {
		String profileTableName = new String();
    	ProfileID profileID = null;
    	CallControlProfileCMP profile = null;
    	 	
    	try {
    		profileTableName = "CallControl";
    		
    		// Looking for the ProfileID of the caller (address)
    		profileID = getProfileFacility().getProfileByIndexedAttribute(profileTableName, "userAddress", address);
    		
    		if (profileID != null) {
    			profile = getCallControlProfileCMP(profileID);	
    		}
    		
        }catch (NullPointerException  e) {
        	log.error("Exception using the getProfileByIndexedAttribute method", e);
        }catch (UnrecognizedProfileTableNameException e) {
        	log.error("Exception in getting the Profile Specification in getControllerProfileCMP(profileID):" +
        			"The ProfileID object does not identify a Profile Table created from the Profile Specification", e);
        }catch (UnrecognizedProfileNameException e) {
        	log.error("Exception in getting the Profile Specification in getControllerProfileCMP(profileID):" +
        			"The ProfileID object does not identify a Profile within the Profile Table", e);
        }catch (TransactionRolledbackLocalException e) {
        	log.error(e.getMessage(), e);
		}catch (FacilityException e) {
			log.error(e.getMessage(), e);
		}catch (UnrecognizedAttributeException e) {
			log.error(e.getMessage(), e);
		}catch (AttributeNotIndexedException e) {
			log.error(e.getMessage(), e);
		}catch (AttributeTypeMismatchException e) {
			log.error(e.getMessage(), e);
		}
        
        return profile;
	}
	
    public void unsetSbbContext() { this.sbbContext = null; }

    public void sbbCreate() throws CreateException { }

    public void sbbPostCreate() throws CreateException { }

    public void sbbRemove() { }

    public void sbbPassivate() { }

    public void sbbActivate() { }

    public void sbbLoad() { }

    public void sbbStore() { }

    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface aci) { }

    public void sbbRolledBack(RolledBackContext context) { }

    protected final SbbContext getSbbContext() { return sbbContext; }

    protected final ProfileFacility getProfileFacility() { return profileFacility; }

    protected final SbbLocalObject getSbbLocalObject() { return sbbContext.getSbbLocalObject(); }
    
    protected final SipFactoryProvider getSipFactoryProvider() { return fp; }
    
    protected final AddressFactory getAddressFactory() { return addressFactory; }
    
    protected final MessageFactory getMessageFactory() { return messageFactory; }
    	
	private SbbContext sbbContext;
	private SipFactoryProvider fp;
	private AddressFactory addressFactory;
	private MessageFactory messageFactory;
	private ProfileFacility profileFacility;
	
	public abstract CallControlProfileCMP getCallControlProfileCMP(javax.slee.profile.ProfileID profileID) 
			throws UnrecognizedProfileNameException, UnrecognizedProfileTableNameException;

	public InitialEventSelector callIDSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		String callID = null;
		
		if (event instanceof RequestEvent) {
			// If request event, the convergence name to callId
			Request request = ((RequestEvent) event).getRequest();
			callID = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
		}

		ies.setCustomName(callID);
        return ies;
    }
}
