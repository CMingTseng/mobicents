package org.mobicents.slee.container.component.deployment.jaxb.descriptors.profile;

/**
 * Start time:16:51:40 2009-01-18<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class MIndexHint {

	private org.mobicents.slee.container.component.deployment.jaxb.slee11.profile.IndexHint indexHint11;
	
	private String queryOperator;
	private String collatorRef;

	public MIndexHint(org.mobicents.slee.container.component.deployment.jaxb.slee11.profile.IndexHint indexHint11)
	{
	  this.indexHint11 = indexHint11;
	  
		this.queryOperator = indexHint11.getQueryOperator();
		this.collatorRef = indexHint11.getCollatorRef();
	}

	public String getQueryOperator() {
		return queryOperator;
	}

	public String getCollatorRef() {
		return collatorRef;
	}
}
