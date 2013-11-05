/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Winston Prakash
 *
 ******************************************************************************
 */
package hudson.util;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utilities for XML Manipulation
 *
 * @author Winston Prakash
 */
public class XmlUtils {

    public static Document parseXmlFile(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);

        Document doc = factory.newDocumentBuilder().parse(xmlFile);
        return doc;
    }

    public static boolean hasElement(Document inDoc, String elementName) {
        NodeList list = inDoc.getElementsByTagName(elementName);
        return (list != null) && (list.getLength() > 0);
    }

    public static void moveElement(Document fromDoc, Document toDoc, Element root, String elementName) {
        NodeList list = fromDoc.getElementsByTagName(elementName);
        if ((list != null) && (list.getLength() > 0)) {
            Element element = (Element) list.item(0);
            Node node = toDoc.importNode(element, true);
            root.appendChild(node);
            element.getParentNode().removeChild(element);
        }
    }

    public static void deleteElement(Document fromDoc, String elementName) {
        NodeList list = fromDoc.getElementsByTagName(elementName);
        if ((list != null) && (list.getLength() > 0)) {
            Element element = (Element) list.item(0);
            element.getParentNode().removeChild(element);
        }
    }

    public static void writeXmlFile(Document doc, File file) throws TransformerConfigurationException, TransformerException {
        Source source = new DOMSource(doc);
        Result result = new StreamResult(file);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
    }
}
