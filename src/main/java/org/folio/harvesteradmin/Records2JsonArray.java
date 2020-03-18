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
 *
 * @author ne
 */
public class Records2JsonArray {
  
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
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/10003/\">\n" +
"        <amountHarvested>0</amountHarvested>\n" +
"        <currentStatus>NEW</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>10003</id>\n" +
"        <jobClass>XmlBulkResource</jobClass>\n" +
"        <lastHarvestFinished>2020-03-16T15:29:23Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2020-03-16T15:29:20Z</lastHarvestStarted>\n" +
"        <lastUpdated>2020-03-16T15:43:39Z</lastUpdated>\n" +
"        <name>GBV test</name>\n" +
"        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://10.0.2.2:9130/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/9996/\">\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>true</enabled>\n" +
"        <id>9996</id>\n" +
"        <jobClass>XmlBulkResource</jobClass>\n" +
"        <lastHarvestFinished>2015-06-10T10:32:11Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2015-06-10T10:10:00Z</lastHarvestStarted>\n" +
"        <lastUpdated>2014-09-24T08:33:27Z</lastUpdated>\n" +
"        <name>HTTPS test</name>\n" +
"        <nextHarvestSchedule>2020-06-10T10:10:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://localhost:8983/solr/lui/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/9997/\">\n" +
"        <amountHarvested>100</amountHarvested>\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>9997</id>\n" +
"        <jobClass>OaiPmhResource</jobClass>\n" +
"        <lastHarvestFinished>2016-07-22T15:47:40Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2016-07-22T15:46:28Z</lastHarvestStarted>\n" +
"        <lastUpdated>2016-07-22T15:46:25Z</lastUpdated>\n" +
"        <message>Stop requested after 100 records</message>\n" +
"        <name>NASA Technical Reports</name>\n" +
"        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://localhost:8983/solr/lui/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/2005/\">\n" +
"        <currentStatus>NEW</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>2005</id>\n" +
"        <jobClass>OaiPmhResource</jobClass>\n" +
"        <lastUpdated>2019-01-01T19:10:04Z</lastUpdated>\n" +
"        <name>SI, Millersville, physicals</name>\n" +
"        <nextHarvestSchedule>2020-06-10T10:10:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://10.0.2.2:9130/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/2004/\">\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>2004</id>\n" +
"        <jobClass>OaiPmhResource</jobClass>\n" +
"        <lastUpdated>2019-01-01T01:01:01Z</lastUpdated>\n" +
"        <name>SI, Temple, rapid_print_books</name>\n" +
"        <nextHarvestSchedule>2020-06-10T10:10:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://10.0.2.2:9130/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/2006/\">\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>false</enabled>\n" +
"        <id>2006</id>\n" +
"        <jobClass>OaiPmhResource</jobClass>\n" +
"        <lastUpdated>2019-01-01T14:18:46Z</lastUpdated>\n" +
"        <name>SI, Villanova, Main Stacks</name>\n" +
"        <nextHarvestSchedule>2020-06-10T10:10:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://10.0.2.2:9130/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/9999/\">\n" +
"        <currentStatus>OK</currentStatus>\n" +
"        <enabled>true</enabled>\n" +
"        <id>9999</id>\n" +
"        <jobClass>XmlBulkResource</jobClass>\n" +
"        <lastHarvestFinished>2015-06-10T10:32:11Z</lastHarvestFinished>\n" +
"        <lastHarvestStarted>2015-06-10T10:10:00Z</lastHarvestStarted>\n" +
"        <lastUpdated>2014-09-24T08:33:27Z</lastUpdated>\n" +
"        <name>Test data</name>\n" +
"        <nextHarvestSchedule>2020-06-10T10:10:00Z</nextHarvestSchedule>\n" +
"        <storageUrl>http://localhost:8983/solr/lui/</storageUrl>\n" +
"    </harvestableBrief>\n" +
"</harvestables>";
            
    Document doc = XMLStringToXMLDocument(xml);
    System.out.println(doc.getDocumentElement().getNodeName());
    System.out.println(doc.getDocumentElement().getChildNodes().getLength());
    System.out.println(doc.getDocumentElement().getFirstChild().getChildNodes().getLength());
    System.out.println(xml2json(xml).encodePrettily());
  }
    
  public static JsonObject xml2json(String xml) {
    JsonObject jsonObject = new JsonObject();
    Document doc = XMLStringToXMLDocument(xml);
    stripWhiteSpaceNodes(doc);
    Node records = doc.getDocumentElement();
    jsonObject.put(doc.getDocumentElement().getNodeName(),xmlRecords2jsonArray(records));
    jsonObject.put("totalRecords", Integer.parseInt(records.getAttributes().getNamedItem("count").getTextContent()));
    return jsonObject;
  }

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
  
  private static JsonArray xmlRecords2jsonArray (Node records) {
    JsonArray jsonArray = new JsonArray();
    for (Node record : iterable(records)) {
      jsonArray.add(node2json(record));
    }
    return jsonArray;
  }
    
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
   * @return
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
   * @return
   */
  private static Iterable<Node> iterable(Node node) {
    return iterable(node.getChildNodes());
  }

  
}
