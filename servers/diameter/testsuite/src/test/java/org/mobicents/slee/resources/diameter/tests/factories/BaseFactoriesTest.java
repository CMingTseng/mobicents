package org.mobicents.slee.resources.diameter.tests.factories;

import static org.junit.Assert.*;

import static org.jdiameter.client.impl.helpers.Parameters.AcctApplId;
import static org.jdiameter.client.impl.helpers.Parameters.ApplicationId;
import static org.jdiameter.client.impl.helpers.Parameters.Assembler;
import static org.jdiameter.client.impl.helpers.Parameters.AuthApplId;
import static org.jdiameter.client.impl.helpers.Parameters.OwnDiameterURI;
import static org.jdiameter.client.impl.helpers.Parameters.OwnRealm;
import static org.jdiameter.client.impl.helpers.Parameters.OwnVendorID;
import static org.jdiameter.client.impl.helpers.Parameters.PeerName;
import static org.jdiameter.client.impl.helpers.Parameters.PeerRating;
import static org.jdiameter.client.impl.helpers.Parameters.PeerTable;
import static org.jdiameter.client.impl.helpers.Parameters.RealmEntry;
import static org.jdiameter.client.impl.helpers.Parameters.RealmTable;
import static org.jdiameter.client.impl.helpers.Parameters.VendorId;

import net.java.slee.resource.diameter.base.events.AbortSessionAnswer;
import net.java.slee.resource.diameter.base.events.AbortSessionRequest;
import net.java.slee.resource.diameter.base.events.AccountingAnswer;
import net.java.slee.resource.diameter.base.events.AccountingRequest;
import net.java.slee.resource.diameter.base.events.CapabilitiesExchangeAnswer;
import net.java.slee.resource.diameter.base.events.CapabilitiesExchangeRequest;
import net.java.slee.resource.diameter.base.events.DeviceWatchdogAnswer;
import net.java.slee.resource.diameter.base.events.DeviceWatchdogRequest;
import net.java.slee.resource.diameter.base.events.DisconnectPeerAnswer;
import net.java.slee.resource.diameter.base.events.DisconnectPeerRequest;
import net.java.slee.resource.diameter.base.events.ReAuthAnswer;
import net.java.slee.resource.diameter.base.events.ReAuthRequest;
import net.java.slee.resource.diameter.base.events.SessionTerminationAnswer;
import net.java.slee.resource.diameter.base.events.SessionTerminationRequest;

import org.jdiameter.api.Stack;
import org.jdiameter.client.impl.helpers.EmptyConfiguration;
import org.junit.Test;
import org.mobicents.slee.resource.diameter.base.DiameterMessageFactoryImpl;

public class BaseFactoriesTest {

  private static String clientHost = "127.0.0.1";
  private static String clientPort = "21812";
  private static String clientURI  = "aaa://" + clientHost + ":" + clientPort;
  
  private static String serverHost = "localhost";
  private static String serverPort = "1812";
  private static String serverURI = "aaa://" + serverHost + ":" + serverPort;
  
  private static String realmName = "mobicents.org";

  private static DiameterMessageFactoryImpl messageFactory;
  
  static
  {
    Stack stack = new org.jdiameter.client.impl.StackImpl();
    try
    {
      stack.init(new MyConfiguration());
    }
    catch ( Exception e )
    {
      throw new RuntimeException("");
    }
    
    messageFactory = new DiameterMessageFactoryImpl(stack);    
  }
  
  @Test
  public void isRequestASR() throws Exception
  {
    AbortSessionRequest asr = messageFactory.createAbortSessionRequest();
    assertTrue("Request Flag in Abort-Session-Request is not set.", asr.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerASA() throws Exception
  {
    AbortSessionAnswer asa = messageFactory.createAbortSessionAnswer();
    assertFalse("Request Flag in Abort-Session-Answer is set.", asa.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostASA() throws Exception
  {
    AbortSessionAnswer asa = messageFactory.createAbortSessionAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", asa.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmASA() throws Exception
  {
    AbortSessionAnswer asa = messageFactory.createAbortSessionAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", asa.getDestinationRealm());    
  }
  
  @Test
  public void isRequestACR() throws Exception
  {
    AccountingRequest acr = messageFactory.createAccountingRequest();
    assertTrue("Request Flag in Accounting-Request is not set.", acr.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerACA() throws Exception
  {
    AccountingAnswer aca = messageFactory.createAccountingAnswer();
    assertFalse("Request Flag in Abort-Session-Answer is set.", aca.getHeader().isRequest());
  }

  @Test
  public void hasDestinationHostACA() throws Exception
  {
    AccountingAnswer aca = messageFactory.createAccountingAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", aca.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmACA() throws Exception
  {
    AccountingAnswer aca = messageFactory.createAccountingAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", aca.getDestinationRealm());    
  }
  
  @Test
  public void isRequestCER() throws Exception
  {
    CapabilitiesExchangeRequest cer = messageFactory.createCapabilitiesExchangeRequest();
    assertTrue("Request Flag in Capabilities-Exchange-Request is not set.", cer.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerCEA() throws Exception
  {
    CapabilitiesExchangeAnswer cea = messageFactory.createCapabilitiesExchangeAnswer();
    assertFalse("Request Flag in Capabilities-Exchange-Answer is set.", cea.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostCEA() throws Exception
  {
    CapabilitiesExchangeAnswer cea = messageFactory.createCapabilitiesExchangeAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", cea.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmCEA() throws Exception
  {
    CapabilitiesExchangeAnswer cea = messageFactory.createCapabilitiesExchangeAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", cea.getDestinationRealm());    
  }

  @Test
  public void isRequestDWR() throws Exception
  {
    DeviceWatchdogRequest dwr = messageFactory.createDeviceWatchdogRequest();
    assertTrue("Request Flag in Device-Watchdog-Request is not set.", dwr.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerDWA() throws Exception
  {
    DeviceWatchdogAnswer dwa = messageFactory.createDeviceWatchdogAnswer();
    assertFalse("Request Flag in Device-Watchdog-Answer is set.", dwa.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostDWA() throws Exception
  {
    DeviceWatchdogAnswer dwa = messageFactory.createDeviceWatchdogAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", dwa.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmDWA() throws Exception
  {
    DeviceWatchdogAnswer dwa = messageFactory.createDeviceWatchdogAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", dwa.getDestinationRealm());    
  }

  @Test
  public void isRequestDPR() throws Exception
  {
    DisconnectPeerRequest dpr = messageFactory.createDisconnectPeerRequest();
    assertTrue("Request Flag in Disconnect-Peer-Request is not set.", dpr.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerDPA() throws Exception
  {
    DisconnectPeerAnswer dpa = messageFactory.createDisconnectPeerAnswer();
    assertFalse("Request Flag in Disconnect-Peer-Answer is set.", dpa.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostDPA() throws Exception
  {
    DisconnectPeerAnswer dpa = messageFactory.createDisconnectPeerAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", dpa.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmDPA() throws Exception
  {
    DisconnectPeerAnswer dpa = messageFactory.createDisconnectPeerAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", dpa.getDestinationRealm());    
  }

  @Test
  public void isRequestRAR() throws Exception
  {
    ReAuthRequest rar = messageFactory.createReAuthRequest();
    assertTrue("Request Flag in Disconnect-Peer-Request is not set.", rar.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerRAA() throws Exception
  {
    ReAuthAnswer raa = messageFactory.createReAuthAnswer();
    assertFalse("Request Flag in Disconnect-Peer-Answer is set.", raa.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostRAA() throws Exception
  {
    ReAuthAnswer raa = messageFactory.createReAuthAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", raa.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmRAA() throws Exception
  {
    ReAuthAnswer raa = messageFactory.createReAuthAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", raa.getDestinationRealm());    
  }
  
  @Test
  public void isRequestSTR() throws Exception
  {
    SessionTerminationRequest str = messageFactory.createSessionTerminationRequest();
    assertTrue("Request Flag in Disconnect-Peer-Request is not set.", str.getHeader().isRequest());
  }
  
  @Test
  public void isAnswerSTA() throws Exception
  {
    SessionTerminationAnswer sta = messageFactory.createSessionTerminationAnswer();
    assertFalse("Request Flag in Disconnect-Peer-Answer is set.", sta.getHeader().isRequest());
  }
  
  @Test
  public void hasDestinationHostSTA() throws Exception
  {
    SessionTerminationAnswer sta = messageFactory.createSessionTerminationAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", sta.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmSTA() throws Exception
  {
    SessionTerminationAnswer sta = messageFactory.createSessionTerminationAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", sta.getDestinationRealm());    
  }
  
  /**
   * Class representing the Diameter Configuration  
   */
  public static class MyConfiguration extends EmptyConfiguration 
  {
    public MyConfiguration() 
    {
      super();
      
      add(Assembler, Assembler.defValue());
      add(OwnDiameterURI, clientURI);
      add(OwnRealm, realmName);
      add(OwnVendorID, 193L);
      // Set Ericsson SDK feature
      //add(UseUriAsFqdn, true);
      // Set Common Applications
      add(ApplicationId,
          // AppId 1
          getInstance().
          add(VendorId,   193L).
          add(AuthApplId, 0L).
          add(AcctApplId, 19302L)
      );
      // Set peer table
      add(PeerTable,
          // Peer 1
          getInstance().
          add(PeerRating, 1).
          add(PeerName, serverURI));
      // Set realm table
      add(RealmTable,
          // Realm 1
          getInstance().
          add(RealmEntry, realmName + ":" + clientHost + "," + serverHost)
      );
    }
  }

}
