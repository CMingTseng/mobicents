/***************************************************
 *                                                 *
 *  Mobicents: The Open Source VoIP Platform       *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/
package org.mobicents.ant.sbbconfigurator;

import java.io.File;

/**
 * Interface for SbbConfigurator ant sub tasks.
 * @author Eduardo Martins / 2006 PT Inova��o (www.ptinovacao.pt)
 */
public interface SubTask {
		
	public abstract void run(File sbbDescriptor);
}