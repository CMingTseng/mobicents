/*
 * ResponseEvent.java
 *
 * Created on 6 ������� 2006 �., 10:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.slee.resource.smpp;

/**
 *
 * @author Oleg Kulikov
 */
public interface ResponseEvent extends SmppEvent {
    public ClientTransaction getTransaction();
}
