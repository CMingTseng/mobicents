//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.04.25 at 12:02:30 AM WEST 
//


package org.mobicents.slee.sippresence.pojo.datamodel;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import org.mobicents.slee.sippresence.pojo.commonschema.Empty;
import org.mobicents.slee.sippresence.pojo.commonschema.NoteT;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.mobicents.slee.sippresence.pojo.datamodel package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DeviceID_QNAME = new QName("urn:ietf:params:xml:ns:pidf:data-model", "deviceID");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.mobicents.slee.sippresence.pojo.datamodel
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link NoteT }
     * 
     */
    public NoteT createNoteT() {
        return new NoteT();
    }

    /**
     * Create an instance of {@link Empty }
     * 
     */
    public Empty createEmpty() {
        return new Empty();
    }

    /**
     * Create an instance of {@link Person }
     * 
     */
    public Person createPerson() {
        return new Person();
    }

    /**
     * Create an instance of {@link Device }
     * 
     */
    public Device createDevice() {
        return new Device();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ietf:params:xml:ns:pidf:data-model", name = "deviceID")
    public JAXBElement<String> createDeviceID(String value) {
        return new JAXBElement<String>(_DeviceID_QNAME, String.class, null, value);
    }

}
