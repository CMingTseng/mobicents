/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.tools.sip.balancer;


/**
 * @deprecated
 * @author baranowb
 *
 */
public class BalancerMessageImpl implements BalancerMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2237779561165576445L;
	private Object content;
	private BalancerMessageType type;
	
	public Object getContent() {
		
		return content;
	}

	public BalancerMessageType getType() {

		return type;
	}

	public BalancerMessageImpl(Object content, BalancerMessageType type) {
		super();
		this.content = content;
		this.type = type;
	}

	public String toString()
	{
		return "BalancerMessageImpl[  Type["+this.type+"] Content["+this.content+"]   ]";
	}
	
}
