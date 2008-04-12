//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.3-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.07.24 at 07:06:57 PM BST 
//


package org.openxdm.xcap.client.appusage.presrules;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * <p>Java class for identityType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="identityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="one" type="{urn:ietf:params:xml:ns:common-policy}oneType"/>
 *         &lt;element name="many" type="{urn:ietf:params:xml:ns:common-policy}manyType"/>
 *         &lt;any/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "identityType", propOrder = {
    "oneOrManyOrAny"
})
@XmlRootElement(name="identity")
public class IdentityType {

    @XmlElementRefs({
        @XmlElementRef(name = "many", namespace = "urn:ietf:params:xml:ns:common-policy", type = JAXBElement.class),
        @XmlElementRef(name = "one", namespace = "urn:ietf:params:xml:ns:common-policy", type = JAXBElement.class)
    })
    @XmlAnyElement(lax = true)
    protected List<Object> oneOrManyOrAny;

    /**
     * Gets the value of the oneOrManyOrAny property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the oneOrManyOrAny property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOneOrManyOrAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * {@link JAXBElement }{@code <}{@link OneType }{@code >}
     * {@link JAXBElement }{@code <}{@link ManyType }{@code >}
     * {@link Element }
     * 
     * 
     */
    public List<Object> getOneOrManyOrAny() {
        if (oneOrManyOrAny == null) {
            oneOrManyOrAny = new ArrayList<Object>();
        }
        return this.oneOrManyOrAny;
    }

}
