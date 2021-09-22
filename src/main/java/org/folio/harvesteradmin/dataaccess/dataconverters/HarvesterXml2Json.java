/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin.dataaccess.dataconverters;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.TestRecords;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Transforms Harvester XML to JSON following FOLIO conventions as closely as possible.
 * <p>
 * Note: This class knows about Harvester records specifics, in particular that there is an element named
 * 'stepAssociations', which must be treated as a repeatable element -- even if there's just one (or no) occurrence of
 * it.
 *
 * @author ne
 */
public class HarvesterXml2Json
{

  private static final Logger logger = LogManager.getLogger( "harvester-admin" );


  /**
   * Creates JSON object from XML String containing non-repeatable elements
   *
   * @param xml The XML to convert
   * @return JsonObject created from the provided XML
   */
  public static JsonObject convertRecordToJson( String xml )
  {
    try
    {
      Document doc = XMLStringToXMLDocument( xml );
      return convertRecordToJson( doc );
    }
    catch ( IOException | ParserConfigurationException | SAXException e )
    {
      logger.error( "Couldn't parse string [" + xml + "] as XML document: " + e.getMessage() );
    }
    return new JsonObject();
  }

  /**
   * Create JSON object from XML String of an element known to contain 0, 1 or more repeatable elements.
   * <p>
   * Note: This would not work for an element containing children that were a mix of repeatable and none repeatable
   * elements or an element containing multiple different repeatable elements (none of this in the Harvester APIs
   * currently)
   *
   * @param xml Harvester XML output
   * @return JSON representation of Harvester XML
   */
  public static JsonObject convertRecordSetToJson( String xml )
  {
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

  private static JsonObject convertRecordToJson( Document doc )
  {
    JsonObject outgoingJsonObject = null;
    if ( doc != null )
    {
      stripWhiteSpaceNodes( doc );
      JsonObject jsonObject = recurseIntoNode( doc );
      outgoingJsonObject = new JsonObject();
      Optional<Map.Entry<String, Object>> outerRootObject = getRootObject( jsonObject );
      if ( outerRootObject.isPresent() )
      {
        Optional<Map.Entry<String, Object>> innerRootObject = getRootObject(
                (JsonObject) outerRootObject.get().getValue() );
        if ( innerRootObject.isPresent() )
        {
          outgoingJsonObject = (JsonObject) innerRootObject.get().getValue();
          outgoingJsonObject.put( "type", innerRootObject.get().getKey() );
          outgoingJsonObject.remove( "idAsString" );
        }
      }
    }
    return outgoingJsonObject;
  }

  private static Optional<Map.Entry<String, Object>> getRootObject( JsonObject json )
  {
    return json.stream().filter( entry -> entry.getValue() instanceof JsonObject ).findFirst();
  }

  /**
   * Creates JSON array of JSON objects from a Node known to contain repeatable elements
   *
   * @param records An XML node with repeatable elements
   * @return XML elements as JSON array
   */
  private static JsonArray xmlRecords2jsonArray( Node records )
  {
    JsonArray jsonArray = new JsonArray();
    for ( Node record : iterable( records ) )
    {
      jsonArray.add( recurseIntoNode( record ) );
    }
    return jsonArray;
  }

  /**
   * Creates a JSON object from an XML element; recursively if necessary The code relies on knowledge about the names
   * of XML elements that are repeatable in the Harvester WS API - there has always been only one such field,
   * 'stepAssociations'. Also, the code expects XML elements named 'json' to contain strings that can be passed on as
   * JSON objects (if JSON parsing fails the content will be passed on as is).
   *
   * @param node XML element to create JsonObject for
   * @return XML element as JSON object
   */
  private static JsonObject recurseIntoNode (Node node) {
    JsonObject json = new JsonObject();
    boolean isChildEntity = false;
    if (node.hasAttributes() && node.getAttributes().getNamedItem("xsi:type") != null) {
      String entityType = node.getAttributes().getNamedItem("xsi:type").getTextContent();
      json.put( "entityType", entityType );
      isChildEntity = true;
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
      } else
      {
        if ( child.getNodeName().equals( "json" ) )
        {
          try
          {
            if ( !child.getTextContent().isEmpty() )
            {
              json.put( child.getNodeName(), new JsonObject( child.getTextContent() ) );
            }
          }
          catch ( DecodeException de )
          {
            logger.error( "Could not parse content of 'json' field as JSON: " + de.getMessage() );
            json.put( child.getNodeName(), child.getTextContent() );
          }
        }
        else if ( isChildEntity && child.getNodeName().equals( "script" ) )
        {
          json.put( child.getNodeName(),
                  ( child.getTextContent().isEmpty() ? "" : "<scripts omitted from nested displays>" ) );
        }
        else
        {
          json.put( child.getNodeName(), child.getTextContent() );
        }
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
  private static Document XMLStringToXMLDocument( String xmlString ) throws IOException, ParserConfigurationException, SAXException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse( new InputSource( new StringReader( xmlString ) ) );
  }

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   *
   * @param args Arguments ignored
   */
  public static void main( String[] args )
  {

    System.out.println( convertRecordSetToJson( TestRecords.xmlSampleHarvestables() ).encodePrettily() );
    System.out.println( convertRecordToJson( TestRecords.xmlSampleHarvestable() ).encodePrettily() );
  }

}
