package org.mobicents.slee.resource.diameter.base.events;

import net.java.slee.resource.diameter.base.events.SessionTerminationAnswer;

import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;

/**
 * 
 * Implementation of Session-Termination-Answer Diameter Message.
 *
 * <br>Super project:  mobicents
 * <br>3:47:25 PM May 21, 2008 
 * <br>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a> 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a> 
 * @author Erick Svenson
 */
public class SessionTerminationAnswerImpl extends ExtensionDiameterMessageImpl implements SessionTerminationAnswer
{

	public SessionTerminationAnswerImpl(Message message) {
    super(message);
  }

  public byte[][] getClassAvps() {
    AvpSet s = message.getAvps().getAvps(25);
    byte[][] rc = new byte[s.size()][];
    for (int i = 0; i < s.size(); i++)
      try {
        rc[i] = s.getAvpByIndex(i).getRaw();
      } catch (Exception e) {
        log.debug(e);
      }
    return rc;
  }

  public void setClassAvp(byte[] classAvp) {
    message.getAvps().addAvp(25, classAvp, true, false);
  }

  public void setClassAvps(byte[][] classAvps) {
    for (byte[] i : classAvps)
      setClassAvp(i);
  }

	@Override
	public String getLongName() {
		
		return "Session-Termination-Answer";
	}

	@Override
	public String getShortName() {

		return "STA";
	}

}
