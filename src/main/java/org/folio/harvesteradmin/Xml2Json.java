/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Transforms Harvester XML to JSON following FOLIO conventions as closely as possible.
 * <p>
 * Note: This class knows about Harvester records specifics, in particular that there is an element named
 * 'stepAssociations', which must be treated as a repeatable element -- even if there's just one (or no) occurrence of
 * it.
 *
 * @author ne
 */
public class Xml2Json {

  private static final Logger logger = Logger.getLogger( "harvester-admin");

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   * @param args Arguments ignored
   */
  public static void main (String[] args) {
    System.out.println( recordSetXml2json( TestRecords.xmlSampleHarvestables() ).encodePrettily() );
    System.out.println( recordXml2Json( TestRecords.xmlSampleHarvestable() ).encodePrettily() );
  }

  /**
   * Create JSON object from XML String of an element known to contain 0, 1
   * or more repeatable elements.
   *
   * Note: This would not work for an element containing children that were
   * a mix of repeatable and none repeatable elements or an element containing
   * multiple different repeatable elements (none of this in the Harvester APIs currently)
   *
   * @param xml Harvester XML output
   * @return JSON representation of Harvester XML
   */
  public static JsonObject recordSetXml2json(String xml) {
    JsonObject jsonObject = new JsonObject();
    try
    {
      Document doc = XMLStringToXMLDocument( xml );
      if ( doc != null )
      {
        stripWhiteSpaceNodes( doc );
        Node records = doc.getDocumentElement();
        jsonObject.put( doc.getDocumentElement().getNodeName(), xmlRecords2jsonArray( records ) );
        int recordCount = Integer.parseInt( records.getAttributes().getNamedItem( "count" ).getTextContent() );
        jsonObject.put( "totalRecords", recordCount );
      }
    }
    catch ( IOException | ParserConfigurationException | SAXException e )
    {
      logger.error( "Couldn't parse string [" + xml + "] as XML document: " + e.getMessage() );
    }
    return jsonObject;
  }

  /**
   * Creates JSON object from XML String containing non-repeatable elements
   *
   * @param xml The XML to convert
   * @return JsonObject created from the provided XML
   */
  public static JsonObject recordXml2Json( String xml )
  {
    try
    {
      Document doc = XMLStringToXMLDocument( xml );
      return recordXml2Json( doc );
    }
    catch ( IOException | ParserConfigurationException | SAXException e )
    {
      logger.error( "Couldn't parse string [" + xml + "] as XML document: " + e.getMessage() );
    }
    return null;
  }

  public static JsonObject recordXml2Json( Document doc )
  {
    JsonObject jsonObject = null;
    if ( doc != null )
    {
      stripWhiteSpaceNodes( doc );
      jsonObject = recurseIntoNode( doc );
    }
    return jsonObject;
  }

  /**
   * Creates JSON array of JSON objects from a Node known to contain repeatable
   * elements
   * @param records An XML node with repeatable elements
   * @return XML elements as JSON array
   */
  private static JsonArray xmlRecords2jsonArray (Node records) {
    JsonArray jsonArray = new JsonArray();
    for (Node record : iterable(records)) {
      jsonArray.add(recurseIntoNode(record));
    }
    return jsonArray;
  }

  /**
   * Creates a JSON object from an XML element;
   * recursively if necessary
   * The code relies on knowledge about the names of XML elements
   * that are repeatable in the Harvester WS API - it has always been
   * only one, 'stepAssociations'.
   *
   * @param node XML element to create JsonObject for
   * @return XML element as JSON object
   */
  private static JsonObject recurseIntoNode (Node node) {
    JsonObject json = new JsonObject();
    if (node.hasAttributes() && node.getAttributes().getNamedItem("xsi:type") != null) {
      String entityType = node.getAttributes().getNamedItem("xsi:type").getTextContent();
      json.put("entityType", entityType);
    }
    for (Node child : iterable(node)) {
      if (hasChildElements(child)) {
        if (child.getNodeName().equals("stepAssociations")) {
          if (!json.containsKey("stepAssociations")) {
            json.put("stepAssociations", new JsonArray());
          }
          json.getJsonArray("stepAssociations").add(recurseIntoNode(child));
        } else {
          json.put(child.getNodeName(), recurseIntoNode(child));
        }
      } else {
        json.put(child.getNodeName(), child.getTextContent());
      }
    }
    return json;
  }

  /**
   * Determines if an XML element has child elements
   * @param node element to expect for child elements
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
   * @param node XML node to strip between-elements whitespace from
   */
  private static void stripWhiteSpaceNodes(Node node) {
    // Clean up whitespace text nodes between elements
    List<Node> whiteSpaceNodes = new ArrayList<>();
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
   * @param nodeList List of XML nodes to iterate over
   * @return Iterable over XML nodes
   */
  private static Iterable<Node> iterable(final NodeList nodeList) {
    return () -> new Iterator<>()
    {
      private int index = 0;

      @Override
      public boolean hasNext()
      {
        return index < nodeList.getLength();
      }

      @Override
      public Node next()
      {
        if ( !hasNext())
              throw new NoSuchElementException();
          return nodeList.item(index++);
      }
    };
  }

  /**
   * Creates an Iterable for the childNodes of node
   * @param node The node whose children to iterate
   * @return Iterable over XML nodes
   */
  private static Iterable<Node> iterable(Node node )
  {
    return iterable( node.getChildNodes() );
  }

  /**
   * Create DOM from String of XML
   *
   * @param xmlString String to build XML DOM from
   * @return XML as DOM or null if an exception occurred
   */
  public static Document XMLStringToXMLDocument( String xmlString ) throws IOException, ParserConfigurationException, SAXException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse( new InputSource( new StringReader( xmlString)));
  }

}
