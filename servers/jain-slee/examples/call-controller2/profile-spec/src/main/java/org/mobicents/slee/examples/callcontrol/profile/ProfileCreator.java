package org.mobicents.slee.examples.callcontrol.profile;

import java.util.Hashtable;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.slee.Address;
import javax.slee.AddressPlan;

import org.apache.log4j.Logger;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;
import org.mobicents.slee.container.management.jmx.SleeCommandInterface;
import org.mobicents.slee.examples.callcontrol.common.SubscriptionProfileSbb;

public class ProfileCreator {

	private static boolean createdProfiles = false;
	
	private static Logger log = Logger.getLogger(SubscriptionProfileSbb.class);

	public static void createProfiles() {
		
		if (!createdProfiles) {
		// TODO change this code to be an exmaple of how to create Slee Profiles from an external java app
		System.out.println("Creating profiles ------------------------------------");
		try {
			
			String jbossBindAddress = System.getProperty("jboss.bind.address");
			Hashtable env = new Hashtable();
			env.put("java.naming.factory.initial","org.jnp.interfaces.NamingContextFactory");
			env.put("java.naming.provider.url", "jnp://"+jbossBindAddress);
			env.put("java.naming.factory.url.pkgs","org.jboss.naming:org.jnp.interfaces");
			RMIAdaptor adaptor = (RMIAdaptor) new InitialContext(env).lookup("jmx/rmi/RMIAdaptor");
			SleeCommandInterface sci = new SleeCommandInterface("jnp://"
					+ System.getProperty("jboss.bind.address") + ":1099", null, null);
			
			String controllerProfileSpecID = "ProfileSpecificationID[CallControlProfileCMP#org.mobicents#0.1]";
			String profileTableName = "CallControl";
			String domain = "nist.gov";
						
			// create profile table
			sci.invokeOperation("-createProfileTable", controllerProfileSpecID,
					profileTableName, null);
			log.info("*** AddressProfileTable " + profileTableName + " created.");

			Address[] blockedAddresses = {new Address(AddressPlan.SIP, "sip:mobicents@"+domain),new Address(AddressPlan.SIP, "sip:hugo@"+domain)};
			newProfile(adaptor,sci,profileTableName, "torosvi", "sip:torosvi@"+domain, blockedAddresses, null, true);
			log.info("********** CREATED PROFILE: torosvi **********");
			
			newProfile(adaptor,sci,profileTableName, "mobicents", "sip:mobicents@"+domain, null, null, false);
			log.info("********** CREATED PROFILE: mobicents **********");
			
			Address backupAddress = new Address(AddressPlan.SIP, "sip:torosvi@"+domain);
			newProfile(adaptor,sci,profileTableName, "victor", "sip:victor@"+domain, null, backupAddress, false);
			log.info("********** CREATED PROFILE: victor **********");
			
			newProfile(adaptor,sci,profileTableName, "vhros2", "sip:vhros2@"+domain, null, null, true);
			log.info("********** CREATED PROFILE: vhros2 **********");
			
			newProfile(adaptor,sci,profileTableName, "vmail", "sip:vmail@"+domain, null, null, true);
			log.info("********** CREATED PROFILE: vmail **********");
			
			log.info("Finished creation of call-controller2 Profiles!");
		
		} catch (Exception e) {
			log.error("Failed to create call-controller2 Profiles!",e);
		}
			createdProfiles = true;
		}
	}

	private static void newProfile(RMIAdaptor adaptor, SleeCommandInterface sci, String profileTableName, String profileName,
			String callee, Address[] block, Address backup, boolean state)
			throws Exception {
		
		ObjectName profileObjectName = (ObjectName) sci.invokeOperation("-createProfile",
				profileTableName, profileName, null);
		System.out.println("*** AddressProfile " + profileName + " created: " + profileObjectName);
		if (!(Boolean) adaptor.getAttribute(profileObjectName, "ProfileWriteable")) {
			Object[] o = new Object[] {};
			adaptor.invoke(profileObjectName, "editProfile", o, new String[] {});
			System.out.println("*** Setting profile editable.");
		} else {
			System.out.println("********* Profile is editable.");
		}
		// Setting and Committing
		Address userAddress = new Address(AddressPlan.SIP, callee);
		Attribute userAttr = new Attribute("UserAddress", userAddress);
		Attribute blockedAttr = new Attribute("BlockedAddresses", block);
		Attribute backupAttr = new Attribute("BackupAddress", backup);
		Attribute voicemailAttr = new Attribute("VoicemailState", state);
		adaptor.setAttribute(profileObjectName, userAttr);
		adaptor.setAttribute(profileObjectName, blockedAttr);
		adaptor.setAttribute(profileObjectName, backupAttr);
		adaptor.setAttribute(profileObjectName, voicemailAttr);
		System.out.println("*** Profile modifications are not committed yet.");
		adaptor.invoke(profileObjectName, "commitProfile", new Object[] {},new String[] {});
		System.out.println("*** Profile modifications are committed.");
	}
	
}
