package org.mobicents.slee.resource.diameter.base;

import java.io.IOException;

import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.AccountingServerSessionActivity;
import net.java.slee.resource.diameter.base.events.AccountingAnswer;
import net.java.slee.resource.diameter.base.events.AccountingRequest;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentityAvp;

import org.jdiameter.api.Answer;
import org.jdiameter.api.Avp;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Message;
import org.jdiameter.api.Request;
import org.jdiameter.api.Stack;
import org.jdiameter.api.acc.ServerAccSession;
import org.mobicents.slee.resource.diameter.base.events.AccountingAnswerImpl;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;

public class AccountingServerSessionActivityImpl extends AccountingSessionActivityImpl
		implements AccountingServerSessionActivity {

	protected ServerAccSession serverSession = null;
	
	// These are default values, should be overriden by stack.
	protected String originHost = "aaa://127.0.0.1:3868";
	protected String originRealm = "mobicents.org";
	
	public AccountingServerSessionActivityImpl(DiameterMessageFactoryImpl messageFactory, DiameterAvpFactoryImpl avpFactory, ServerAccSession serverSession,
			EventListener<Request, Answer> raEventListener, long timeout, DiameterIdentityAvp destinationHost, DiameterIdentityAvp destinationRealm,SleeEndpoint endpoint,
			Stack stack) 
	{
		super(messageFactory, avpFactory, null, raEventListener, timeout, destinationHost, destinationRealm,endpoint);

		this.serverSession = serverSession;
		this.serverSession.addStateChangeNotification(this);
		
		super.setCurrentWorkingSession(this.serverSession.getSessions().get(0));
		
    this.originHost = stack.getMetaData().getLocalPeer().getUri().toString();
    this.originRealm = stack.getMetaData().getLocalPeer().getRealmName();
	}

	public AccountingAnswer createAccountAnswer(AccountingRequest request, int resultCode)
	{
    try
    {
      // Get the impl
      DiameterMessageImpl implRequest = (DiameterMessageImpl)request;
      
      // Get raw message from impl
      Message rawMessage = implRequest.getGenericData();
      
      // Extract interesting AVPs
      DiameterAvp accRecordNumber = avpFactory.createAvp(Avp.ACC_RECORD_NUMBER, rawMessage.getAvps().getAvp(Avp.ACC_RECORD_NUMBER).getRaw());
      DiameterAvp accRecordType = avpFactory.createAvp(Avp.ACC_RECORD_TYPE, rawMessage.getAvps().getAvp(Avp.ACC_RECORD_TYPE).getRaw());
      
      DiameterAvp originHost = avpFactory.createAvp(Avp.ORIGIN_HOST, this.originHost.getBytes());
      DiameterAvp originRealm = avpFactory.createAvp(Avp.ORIGIN_REALM, this.originRealm.getBytes());
      
      DiameterAvp sessionId = avpFactory.createAvp(Avp.SESSION_ID, rawMessage.getAvps().getAvp(Avp.SESSION_ID).getRaw());
      
      // Add the Result-code
      DiameterAvp resultCodeAvp = avpFactory.createAvp(Avp.RESULT_CODE, resultCode );

      DiameterMessageImpl answer = (DiameterMessageImpl) messageFactory.createMessage( implRequest.getHeader(), new DiameterAvp[]{accRecordNumber, accRecordType, originHost, originRealm, sessionId, resultCodeAvp} );
      
      // RFC3588, Page 119-120
      // One of Acct-Application-Id and Vendor-Specific-Application-Id AVPs
      // MUST be present.  If the Vendor-Specific-Application-Id grouped AVP
      // is present, it must have an Acct-Application-Id inside.

      if(request.hasAcctApplicationId())
      {
        answer.addAvp( avpFactory.createAvp(Avp.ACCT_APPLICATION_ID, request.getAcctApplicationId()) );
      }
      else
      {
        // We should have an Vendor-Specific-Application-Id grouped AVP
        answer.addAvp( request.getVendorSpecificApplicationId() );
      }
      
      // Get the raw Answer
      Message rawAnswer = answer.getGenericData();

      // This is an answer.
      rawAnswer.setRequest(false);

      return new AccountingAnswerImpl( rawAnswer );
    }
    catch ( Exception e )
    {
      logger.error( "", e );
    }

	  return null;
	}
	
	public void sendAccountAnswer(AccountingAnswer answer) throws IOException
	{
		// FIXME: baranowb - add setting of proper session
		//super.sendMessage(answer);
	  
    try
    {
      AccountingAnswerImpl aca = (AccountingAnswerImpl)answer;

      this.serverSession.getSessions().get(0).send( aca.getGenericData() );
    }
    catch ( Exception e )
    {
      logger.error( "Failure sending Account-Answer.", e );
    }
	}

	public void stateChanged(Enum oldState, Enum newState)
	{
		logger.info( "Diameter Base RA :: AccountingServerSession ::stateChanged :: oldState[" + oldState + "] newState [" + newState + "]." );

		//FIXME: alexandre: Complete this method.
	}

}
