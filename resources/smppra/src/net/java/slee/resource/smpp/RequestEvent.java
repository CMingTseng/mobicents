/*
 * RequestEvent.java
 *
 * Created on 6 ������� 2006 �., 10:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.slee.resource.smpp;

/**
 *
 * @author Oleg Kulikov
 */
public interface RequestEvent extends SmppEvent {
    public ServerTransaction getTransaction();
}
