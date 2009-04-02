/**
 * Start time:12:54:56 2009-03-30<br>
 * Project: mobicents-jain-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.mobicents.isup.ISUPComponent;

/**
 * Start time:12:54:56 2009-03-30<br>
 * Project: mobicents-jain-isup-stack<br>
 * Simple class to define common methods and fields for all.
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>

 */
public abstract class AbstractParameter implements ISUPParameter,ISUPComponent {

	protected byte[] tag = null;
	protected Logger logger  = Logger.getLogger(this.getClass().getName());
	public byte[] getTag() {
		return this.tag;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.isup.ISUPComponent#encodeElement(java.io.ByteArrayOutputStream
	 * )
	 */
	public int encodeElement(ByteArrayOutputStream bos) throws IOException {
		byte[] b = this.encodeElement();
		return b.length;
	}

}
