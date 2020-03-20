/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author ne
 */
public class Json2Xml {


  private static final Logger logger = LoggerFactory.getLogger("harvester-admin");

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   * @param args
   */
  public static void main (String[] args) {

    try {
      Document doc = recordJson2xml(TestRecords.jsonSampleHarvestable());
      System.out.println(writeXmlDocumentToString(doc));

      JsonObject jsonObject = Xml2Json.recordXml2Json(TestRecords.xmlSampleHarvestable());
      Document doc2 = recordJson2xml(jsonObject.encodePrettily());
      System.out.println(writeXmlDocumentToString(doc2));

    } catch (DOMException | ParserConfigurationException e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Creates XML document from a JSON structure
   *
   * @param json structure to transform
   * @return XML document
   * @throws DOMException
   * @throws ParserConfigurationException
   */
  public static Document recordJson2xml(String json) throws DOMException, ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    JsonObject jsonObject = new JsonObject(json);
    Iterator<Entry<String,Object>> iter = jsonObject.iterator();
    while (iter.hasNext())
    {
      Entry<String,Object> prop = iter.next();
      if (prop.getValue() instanceof JsonObject) {
        Element element = doc.createElement(prop.getKey());
        doc.appendChild(element);
        recurseIntoJsonObject((JsonObject)prop.getValue(), doc, element);
      }
    }
    return doc;
  }

  /**
   * Recursively creates XML tree from JSON tree
   *
   * Note: This method knows about Harvester record features, in particular that
   * elements with an 'entityType' child must be given an entity type attribute
   * within an XMLSchema-instance name space.
   *
   * @param object
   * @param doc
   * @param node
   */
  public static void recurseIntoJsonObject (JsonObject object, Document doc, Element node) {
    Iterator<Entry<String,Object>> iter = object.iterator();
    while (iter.hasNext())
    {
      Entry<String,Object> prop = iter.next();
      if (prop.getValue() instanceof String) {
        if (prop.getKey().equals("entityType")) {
          node.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", prop.getValue().toString());
        } else {
          Element el = doc.createElement(prop.getKey());
          el.setTextContent(prop.getValue().toString());
          node.appendChild(el);
        }
      } else if (prop.getValue() instanceof JsonArray) {
        iterateJsonArray(prop.getKey(), (JsonArray) prop.getValue(), doc, node);
      } else if (prop.getValue() instanceof JsonObject) {
        Element el = doc.createElement(prop.getKey());
        node.appendChild(el);
        recurseIntoJsonObject((JsonObject)prop.getValue(), doc, el);
      }
    }
  }

  /**
   * Loops a JsonArray and recurses into each element of the array
   *
   * Note: This method assumes that individual elements from a JSON array
   * can be given the array name for tag names
   *
   * @param arrayName All elements are given the arrayName of the array
   * @param array JSON array to transform to XML
   * @param doc The owner document
   * @param parent The parent element
   */
  public static void iterateJsonArray (String arrayName, JsonArray array, Document doc, Element parent) {
    Iterator iter = array.iterator();
    while (iter.hasNext()) {
      Object element = iter.next();
      if (element instanceof JsonObject) {
        Element el = doc.createElement(arrayName);
        parent.appendChild(el);
        recurseIntoJsonObject((JsonObject)element, doc, el);
      }
      // Note: No support for JSON array of Strings
    }
  }

  /**
   * Create XML String from document DOM
   * @param xmlDocument
   * @return XML String
   */
  public static String writeXmlDocumentToString(Document xmlDocument) {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    try {
        transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();
        return xmlString;
    } catch (TransformerException e) {
        e.printStackTrace();
    }
    return null;
  }

}
