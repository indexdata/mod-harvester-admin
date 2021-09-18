/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map.Entry;

/**
 *
 * @author ne
 */
public class Json2Xml {

  private static final Logger logger = Logger.getLogger( "harvester-admin" );

  /**
   * Creates XML document from a JSON structure
   *
   * @param json structure to transform
   * @return XML document
   */
  public static Document recordJson2harvesterXml( String json ) throws DOMException, ParserConfigurationException
  {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    JsonObject jsonObject = new JsonObject( json );
    for ( Entry<String, Object> jsonProperty : jsonObject )
    {
      if ( jsonProperty.getValue() instanceof JsonObject )
      {
        Element element = doc.createElement( jsonProperty.getKey() );
        doc.appendChild( element );
        recurseIntoJsonObject( (JsonObject) jsonProperty.getValue(), doc, element );
      }
    }
    return doc;
  }

  /**
   * Recursively creates XML tree from JSON tree
   * <p>
   * Note: This method knows about Harvester record features, in particular that elements with an 'entityType' child
   * must be given an entity type attribute within an XMLSchema-instance name space.
   *
   * @param object The JSON object to traverse
   * @param doc    The XML document to add elements to
   * @param node   The XML element corresponding to the JSON object
   */
  public static void recurseIntoJsonObject (JsonObject object, Document doc, Element node)
  {
    for ( Entry<String, Object> jsonProperty : object )
    {
      if ( jsonProperty.getValue() instanceof String )
      {
        if ( jsonProperty.getKey().equals( "entityType" ) )
        {
          node.setAttributeNS( "http://www.w3.org/2001/XMLSchema-instance", "xsi:type",
                  jsonProperty.getValue().toString() );
        }
        else
        {
          Element el = doc.createElement( jsonProperty.getKey() );
          el.setTextContent( jsonProperty.getValue().toString() );
          node.appendChild( el );
        }
      }
      else if ( jsonProperty.getValue() instanceof JsonArray )
      {
        iterateJsonArray( jsonProperty.getKey(), (JsonArray) jsonProperty.getValue(), doc, node );
      }
      else if ( jsonProperty.getValue() instanceof JsonObject )
      {
        Element el = doc.createElement( jsonProperty.getKey() );
        node.appendChild( el );
        recurseIntoJsonObject( (JsonObject) jsonProperty.getValue(), doc, el );
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
  public static void iterateJsonArray (String arrayName, JsonArray array, Document doc, Element parent)
  {
    for ( Object element : array )
    {
      if ( element instanceof JsonObject )
      {
        Element el = doc.createElement( arrayName );
        parent.appendChild( el );
        recurseIntoJsonObject( (JsonObject) element, doc, el );
      }
      // Note: No support for JSON array of Strings
    }
  }

  /**
   * Create XML String from document DOM
   *
   * @param xmlDocument The XML document to be written to a String
   * @return XML String
   */
  public static String writeXmlDocumentToString( Document xmlDocument ) throws TransformerException
  {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    transformer = tf.newTransformer();
    StringWriter writer = new StringWriter();
    transformer.transform( new DOMSource( xmlDocument ), new StreamResult( writer ) );
    return writer.getBuffer().toString();
  }

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   */
  public static void main( String[] args ) throws TransformerException
  {
    try
    {
      Document doc = recordJson2harvesterXml( TestRecords.jsonSampleHarvestable() );
      System.out.println( writeXmlDocumentToString( doc ) );

      JsonObject jsonObject = Xml2Json.recordXml2Json( TestRecords.xmlSampleHarvestable() );
      if ( jsonObject != null )
      {
        Document doc2 = recordJson2harvesterXml( jsonObject.encodePrettily() );
        System.out.println( writeXmlDocumentToString( doc2 ) );
      }

    }
    catch ( DOMException | ParserConfigurationException e )
    {
      logger.error( e.getMessage() );
    }
  }


}
