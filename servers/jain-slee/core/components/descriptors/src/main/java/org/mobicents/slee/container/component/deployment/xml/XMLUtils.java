package org.mobicents.slee.container.component.deployment.xml;


import org.w3c.dom.Document;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.util.*;

/**
 * The class offers varios static methods for parsing XML files and extracting
 * xml properties
 *
 * @author Emil Ivov
 * @version 1.0
 */

public class XMLUtils
{
    public XMLUtils()
    {
    }

    /**
     * Parses the xml file pointed by the specified url, and then creates and
     * returns a Document object out of it.
     * @param url The location of the content to be parsed.
     * @param validating true if the parser used  will validate documents as
     *                   they are parsed; false otherwise
     * @throws IOException if any error occurs during parsing.
     * @throws IllegalArgumentException if <code>url</code> is null
     * @return Document A new DOM Document object created from the specified url
     */
    public static Document parseDocument(String url, boolean validating)
        throws IOException, IllegalArgumentException
    {
        if(url == null)
            throw new IllegalArgumentException("The url may not be null.");
       
       
        
        return parseDocument(new InputSource(url), validating,
                             DefaultSleeEntityResolver.getInstance());
    }


    /**
     * Parses the xml file pointed by the specified url, and then creates and
     * returns a Document object out of it.
     * @param is InputStream containing the content to be parsed.
     * @param validating true if the parser used  will validate documents as
     *                   they are parsed; false otherwise
     * @throws IOException if any error occurs during parsing.
     * @throws IllegalArgumentException if <code>is</code> is null
     * @return Document A new DOM Document object created from the specified url
     */
    public static Document parseDocument(InputStream is, boolean validating)
        throws IOException, IllegalArgumentException
    {
        if(is == null)
            throw new IllegalArgumentException("The input stream may not be null");
        return parseDocument(new InputSource(is), validating,
                             DefaultSleeEntityResolver.getInstance());
    }

    /**
     * Parses the xml file pointed by the specified url, and then creates and
     * returns a Document object out of it.
     * @param in nputSource containing the content to be parsed.
     * @param validating true if the parser used  will validate documents as
     *                   they are parsed; false otherwise
     * @throws IOException if any error occurs during parsing.
     * @return Document A new DOM Document object created from the specified url
     */
    public static Document parseDocument(InputSource in, boolean validating)
        throws IOException
    {
        //BUGBUG
        validating = false;
        return parseDocument(in, validating,
                             DefaultSleeEntityResolver.getInstance());
    }

    /**
     * Parses the xml file pointed by the specified url, and then creates and
     * returns a Document object out of it.
     * @param in nputSource containing the content to be parsed.
     * @param validating true if the parser used  will validate documents as
     *                   they are parsed; false otherwise
     * @param resolver The EntityResolver to be used to resolve entities present
     *                 in the XML document to be parsed.
     * @throws IOException if any error occurs during parsing.
     * @return Document A new DOM Document object created from the specified url
     */
    public static Document parseDocument(InputSource in, boolean validating,
                                         EntityResolver resolver)
        throws IOException
    {
        //BUGBUG
        validating = false;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);
            DocumentBuilder builder = factory.newDocumentBuilder();
            if(resolver != null)
            {
                builder.setEntityResolver(resolver);
            }

            /** @todo define and set an error handler */
            // builder.setErrorHandler(new ParseErrorHandler());
            return builder.parse(in);
        }
        catch(SAXParseException e)
        {
            throw new XMLException("A parse error occurred at "
                                   + e.getSystemId() + ":" + e.getLineNumber(),
                                   e);
        }
        catch(SAXException e)
        {
            throw new XMLException("An Error occurred while reading document", e);
        }
        catch(ParserConfigurationException e)
        {
            throw new XMLException("Conguration error.", e);
        }
    }

    /**
     * Extracts the text content of the specified element.
     * @param element The element to extract the contents of.
     * @throws IllegalArgumentException if the given element argument is null
     * @throws XMLException if the specified element is not a text node.
     * @return the text contents of the specified element or an empty String if
     * the element had no contents.
     */
    public static String getElementTextValue(Element element)
        throws IllegalArgumentException, XMLException
    {
        if(element == null)
            throw new IllegalArgumentException("The specified element is null");

        NodeList nodelist = element.getChildNodes();
        if(nodelist.getLength() == 0)
            return "";

        if(nodelist.getLength() != 1 || nodelist.item(0).getNodeType() != Node.TEXT_NODE)
        {
            throw new XMLException("Not a text node: " + element);
        }
        else
        {
            return nodelist.item(0).getNodeValue().trim();
        }
    }

    /**
     * Extracts the text content of the element with the specified <code>name</code>
     * whose parent is <code>parent</code>
     * @param parent the parent of the element that we need the value of.
     * @param name the name of the element that we need the value of.
     * @throws XMLException if an error occurs while accessing XML content.
     * @return the text content of the element with the specified <code>name</code>
     * that is locate under <code>parent</code>
     */
    public static String getElementTextValue(Element parent, String name)
        throws XMLException
    {
        Element element = getChildElement(parent, name, true);
        if (element == null)
            return null;
        else
            return getElementTextValue(element);
    }

    /**
     * Returns the element with the specified <code>name</code> located under
     * <code>parent</code>.
     * @param parent the parent of the element to extract.
     * @param name the name of the element to extract.
     * @param assertUnique if true the method will throw an XMLException in the
     * case where multiple elements with the specifed <code>name</code> are
     * available. When false, the first element that has the specified <code>name</code>
     * will be returned.
     * @throws IllegalArgumentException if <code>parent</code> or <code>name</code>
     * are null or have an invalid value.
     * @throws XMLException if <code>assertUnique</code> is set to true and
     * multiple elements carrying the specified <code>name</code> are available.
     * @return Element the element with the specified <code>name</code> located
     * under <code>parent</code>.
     */
    public static Element getChildElement(Element parent, String name, boolean assertUnique)
        throws IllegalArgumentException, XMLException
    {
        Iterator elements = getAllChildElements(parent, name).iterator();
        if(elements.hasNext())
        {
            Element element = (Element)elements.next();
            if(assertUnique && elements.hasNext())
                throw new XMLException("Multiple \"" + name
                                       + "\" occurrences of are not allowed");
            else
                return element;
        }
        else
            //throw new XMLException("No \"" + name + "\" element was found.");
            return null;
    }

    /**
     * Returns the element with the specified <code>name</code> located under
     * <code>parent</code>.
     * @param parent the parent of the element to extract.
     * @param name the name of the element to extract.
     * @throws IllegalArgumentException if <code>parent</code> or <code>name</code>
     * are null or have an invalid value.
     * @throws XMLException if <code>assertUnique</code> is set to true and
     * multiple elements carrying the specified <code>name</code> are available.
     * @return Element the element with the specified <code>name</code> located
     * under <code>parent</code>.
     */
    public static Element getChildElement(Element parent, String name)
        throws IllegalArgumentException, XMLException
    {
        return getChildElement(parent, name, false);
    }

    /**
     * Returns a <code>java.util.ArrayList</code> containing all elements carrying
     * the specified <code>name</code> and located under the specified
     * <code>parent</code>.
     * @param parent the parent where the elements are located
     * @param name the name of the elements to extract.
     * @throws IllegalArgumentException if <code>parent</code> or <code>name</code>
     * have illegal values.
     * @return a <code>java.util.ArrayList</code> containing all elements carrying
     * the specified <code>name</code> and located under the specified
     * <code>parent</code>.
     */
    public static List getAllChildElements(Element parent, String name)
        throws IllegalArgumentException
    {
        if(parent == null)
            throw new IllegalArgumentException("The specified parent is null.");

        if(name == null || name.length() == 0)
            throw new IllegalArgumentException("Null or empty element name!");

        NodeList nodelist = parent.getChildNodes();
        ArrayList children = new ArrayList(nodelist.getLength());
        for(int i = 0; i < nodelist.getLength(); i++)
        {
            Node node = nodelist.item(i);
            if(node.getNodeType() == 1 && name.equals(((Element)node).getTagName()))
            {
                children.add((Element)node);
            }
        }

        return children;
    }
}