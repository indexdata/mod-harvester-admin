/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Transforms multi-record XML from the Harvester to arrays of JSON records
 * following FOLIO conventions as closely as possible.
 * @author ne
 */
public class Records2JsonArray {

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   * @param args
   */
  public static void main (String[] args) {
    String xml =
"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
"<harvestables count=\"9\" max=\"100\" uri=\"http://localhost:8080/harvester/records/harvestables/\" start=\"0\">\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/9998/\">\n" +
"        <currentStatus>NEW</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>9998</id>\n" +
"        <jobClass>HarvestConnectorResource</jobClass>\n" +
"        <lastHarvestFinished>2017-07-21T15:51:25Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2017-07-21T15:53:56Z</lastHarvestStarted>\n" +
"        <lastUpdated>2017-07-21T15:58:26Z</lastUpdated>\n" +
"        <name>Europeana API (harvest)</name>\n" +
"        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://localhost:8983/solr/lui/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/10008/\">\n" +
"        <amountHarvested>0</amountHarvested>\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>10008</id>\n" +
"        <jobClass>XmlBulkResource</jobClass>\n" +
"        <lastHarvestFinished>2020-03-13T18:19:37Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2020-03-13T18:19:33Z</lastHarvestStarted>\n" +
"        <lastUpdated>2020-03-13T18:19:32Z</lastUpdated>\n" +
"        <message></message>\n" +
"        <name>GBV (0046)</name>\n" +
"        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://esxh-9.gbv.de:9130/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"</harvestables>";

    Document doc = XMLStringToXMLDocument(xml);
    System.out.println(doc.getDocumentElement().getNodeName());
    System.out.println(doc.getDocumentElement().getChildNodes().getLength());
    System.out.println(doc.getDocumentElement().getFirstChild().getChildNodes().getLength());
    System.out.println(xml2json(xml).encodePrettily());
  }

  /**
   * Create JSON object from String of XML
   * @param xml
   * @return
   */
  public static JsonObject xml2json(String xml) {
    JsonObject jsonObject = new JsonObject();
    Document doc = XMLStringToXMLDocument(xml);
    stripWhiteSpaceNodes(doc);
    Node records = doc.getDocumentElement();
    jsonObject.put(doc.getDocumentElement().getNodeName(),xmlRecords2jsonArray(records));
    jsonObject.put("totalRecords", Integer.parseInt(records.getAttributes().getNamedItem("count").getTextContent()));
    return jsonObject;
  }

  /**
   * Create DOM from String of XML
   * @param xmlString
   * @return XML as DOM
   */
  private static Document XMLStringToXMLDocument(String xmlString)
  {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
      return doc;
    } catch (IOException | ParserConfigurationException | SAXException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Creates JSON array of JSON objects from a list of XML records
   * @param records
   * @return XML elements as JSON array
   */
  private static JsonArray xmlRecords2jsonArray (Node records) {
    JsonArray jsonArray = new JsonArray();
    for (Node record : iterable(records)) {
      jsonArray.add(node2json(record));
    }
    return jsonArray;
  }

  /**
   * Creates a JSON object from an XML element; recursively if necessary
   * @param node
   * @return XML element as JSON object
   */
  private static JsonObject node2json (Node node) {
    JsonObject json = new JsonObject();
    for (Node child : iterable(node)) {
      if (hasChildElements(child)) {
        json.put(child.getNodeName(), node2json(child));
      } else {
        json.put(child.getNodeName(), child.getTextContent());
      }
    }
    return json;
  }

  /**
   * Determines in an XML element has child element (that needs to be recursed)
   * @param node
   * @return true if the XML element contains other XML elements
   */
  private static boolean hasChildElements(Node node) {
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      for (Node child : iterable(node)) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Remove whitespace text nodes (indentation) between elements
   * @param node
   */
  private static void stripWhiteSpaceNodes(Node node) {
    // Clean up whitespace text nodes between elements
    List<Node> whiteSpaceNodes = new ArrayList();
    findWhiteSpaceNodes(node, whiteSpaceNodes);
    for (Node nodeToDelete : whiteSpaceNodes) {
      nodeToDelete.getParentNode().removeChild(nodeToDelete);
    }
  }

  /**
   * Recursively finds whitespace text nodes between elements and adds them to the list
   * @param node the element to find whitespace in (at arbitrary depth)
   * @param whiteSpaceNodes adds text nodes to the list as they are found
   */
   private static void findWhiteSpaceNodes (Node node, List<Node> whiteSpaceNodes) {
    for (Node child : iterable(node)) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        findWhiteSpaceNodes(child, whiteSpaceNodes);
      } else if (child.getTextContent().matches("\\s+")) {
        whiteSpaceNodes.add(child);
      }
    }
  }

  /**
   * Creates an Iterable for a nodeList
   * @param nodeList
   * @return Iterable over XML nodes
   */
  private static Iterable<Node> iterable(final NodeList nodeList) {
    return () -> new Iterator<Node>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
          return index < nodeList.getLength();
      }

      @Override
      public Node next() {
          if (!hasNext())
              throw new NoSuchElementException();
          return nodeList.item(index++);
      }
    };
  }

  /**
   * Creates an Iterable for the childNodes of node
   * @param node
   * @return Iterable over XML nodes
   */
  private static Iterable<Node> iterable(Node node) {
    return iterable(node.getChildNodes());
  }


}
