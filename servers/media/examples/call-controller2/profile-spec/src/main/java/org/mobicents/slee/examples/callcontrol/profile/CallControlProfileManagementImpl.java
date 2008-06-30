/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/
package org.mobicents.slee.examples.callcontrol.profile;

import javax.slee.Address;
import javax.slee.AddressPlan;
import javax.slee.profile.ProfileManagement;
import javax.slee.profile.ProfileVerificationException;

/**
 * Profile Management implementation class.
 */
public abstract class CallControlProfileManagementImpl implements ProfileManagement, CallControlProfileCMP {

	/**
	 * Initialize the profile with its default values.
	 */
    public void profileInitialize() {
    	setUserAddress(null);
        setBlockedAddresses(null);
        setBackupAddress(null);
        setVoicemailState(false);  
    }

    public void profileLoad() {}
    public void profileStore() {}

	/**
	 * Verify the profile's CMP field settings.
	 * @throws ProfileVerificationException if any CMP field contains an invalid value
     */
    public void profileVerify() throws ProfileVerificationException {
    	// Verify Called User Address
    	Address address = getUserAddress();
    	if (address != null) verifyAddress(address);
        // Verify Blocked Addresses
        Address[] blockedAddresses = getBlockedAddresses();
        if (blockedAddresses != null) {
        	for (int i = 0; i < blockedAddresses.length; i++) {
            	if (blockedAddresses[i] != null) verifyAddress(blockedAddresses[i]);
            }
        }
        // Verify Backup Address
        Address backupAddress = getBackupAddress();
        if (backupAddress != null) verifyAddress(backupAddress);
    }
    
    public void verifyAddress(Address address) throws ProfileVerificationException {
        // Check address plan
        if (address.getAddressPlan() != AddressPlan.SIP)
            throw new ProfileVerificationException("Address \"" + address +
            		"\" is not a SIP address");
        // Check URI scheme - must be sip: or sips:
        String uri = address.getAddressString().toLowerCase();
        if (!(uri.startsWith("sip:") || uri.startsWith("sips:")))
            throw new ProfileVerificationException("Address \"" + address +
            		"\" is not a SIP address");            
    }
}