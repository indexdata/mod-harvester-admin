package org.folio.harvesteradmin.legacydata.dataconverters;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.StringWriter;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class JsonToHarvesterXml {

  private static final Logger logger = LogManager.getLogger("harvester-admin");

  /**
   * Embeds incoming JSON in two levels of outer objects,
   * see {@link #wrapJson(JsonObject, String)} and converts the
   * result to XML.
   *
   * @param json         Incoming JSON
   * @param rootProperty The top-level property to embed the JSON in
   * @return wrapped JSON converted to an XML string
   */
  public static String convertToHarvesterRecord(JsonObject json, String rootProperty, String tenant)
      throws ParserConfigurationException, TransformerException {
    json.put("acl", tenant);
    JsonObject wrapped = wrapJson(json, rootProperty);
    Document doc = recordJsonToHarvesterXml(wrapped);
    return writeXmlDocumentToString(doc);
  }

  /**
   * Creates XML document from a JSON structure.
   *
   * @param json structure to transform
   * @return XML document
   */
  private static Document recordJsonToHarvesterXml(JsonObject json)
      throws DOMException, ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    for (Entry<String, Object> jsonProperty : json) {
      if (jsonProperty.getValue() instanceof JsonObject) {
        Element element = doc.createElement(jsonProperty.getKey());
        doc.appendChild(element);
        recurseIntoJsonObject((JsonObject) jsonProperty.getValue(), doc, element);
      }
    }
    return doc;
  }

  /**
   * Recursively creates XML tree from JSON tree.
   *
   * <p>Note: This method knows about Harvester record features,
   * in particular that elements with an 'entityType' child
   * must be given an entity type attribute within an XMLSchema-instance name space,
   * and that fields named 'json'
   * should be stored as JSON strings in the XML.
   *
   * @param object The JSON object to traverse
   * @param doc    The XML document to add elements to
   * @param node   The XML element corresponding to the JSON object
   */
  private static void recurseIntoJsonObject(JsonObject object, Document doc, Element node) {
    for (Entry<String, Object> jsonProperty : object) {
      if (jsonProperty.getValue() instanceof String) {
        if (jsonProperty.getKey().equals("entityType")) {
          node.setAttributeNS(
              "http://www.w3.org/2001/XMLSchema-instance",
              "xsi:type",
              jsonProperty.getValue().toString());
        } else {
          Element el = doc.createElement(jsonProperty.getKey());
          el.setTextContent(jsonProperty.getValue().toString());
          node.appendChild(el);
        }
      } else if (jsonProperty.getValue() instanceof JsonArray) {
        iterateJsonArray(jsonProperty.getKey(), (JsonArray) jsonProperty.getValue(), doc, node);
      } else if (jsonProperty.getValue() instanceof JsonObject) {
        Element el = doc.createElement(jsonProperty.getKey());
        node.appendChild(el);
        if (jsonProperty.getKey().equals("json")) {
          el.setTextContent(((JsonObject) jsonProperty.getValue()).encodePrettily());
        } else {
          recurseIntoJsonObject((JsonObject) jsonProperty.getValue(), doc, el);
        }
      }
    }
  }

  /**
   * Loops a JsonArray and recurses into each element of the array.
   *
   * <p>Note: This method assumes that individual elements from a JSON array
   * can be given the array name for tag names.
   *
   * @param arrayName All elements are given the arrayName of the array
   * @param array     JSON array to transform to XML
   * @param doc       The owner document
   * @param parent    The parent element
   */
  private static void iterateJsonArray(String arrayName, JsonArray array, Document doc,
                                       Element parent) {
    for (Object element : array) {
      if (element instanceof JsonObject) {
        Element el = doc.createElement(arrayName);
        parent.appendChild(el);
        recurseIntoJsonObject((JsonObject) element, doc, el);
      }
      // Note: No support for JSON array of Strings
    }
  }

  /**
   * Create XML String from document DOM.
   *
   * @param xmlDocument The XML document to be written to a String
   * @return XML String
   */
  private static String writeXmlDocumentToString(Document xmlDocument) throws TransformerException {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    transformer = tf.newTransformer();
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
    return writer.getBuffer().toString();
  }

  /**
   * Create XML String from document node.
   */
  public static String writeXmlNodeToString(Node node) throws TransformerException {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(node), new StreamResult(writer));
    return writer.getBuffer().toString();
  }


  /**
   * Takes incoming JSON and embeds it in two levels of root objects to comply
   * with the Harvester schema.<br/> <br/>
   * For example, the storage entity JSON <br/>
   * <pre>
   * {
   *    "type": "solrStorage",
   *    "id": "10001",
   *    "name": "Local SOLR",
   *    etc
   * }
   * </pre>
   * becomes<br/>
   * <pre>
   * {
   *   "storage": {
   *     "solrStorage": {
   *        "id": "10001",
   *        "name": "Local SOLR",
   *        etc
   *     },
   *     "id": "10001"
   *   }
   * }
   * </pre>
   *
   * @param json         Incoming JSON
   * @param rootProperty The top level property to wrap the incoming JSON in
   * @return The wrapped JSON
   */
  private static JsonObject wrapJson(JsonObject json, String rootProperty) {
    final JsonObject wrappedEntity = new JsonObject();

    String type = json.getString("type");
    json.remove("type");
    String id = json.getString("id");

    JsonObject innerEntity = new JsonObject();
    innerEntity.put(type, json.copy());
    if (id != null) {
      innerEntity.put("id", id);
    }

    wrappedEntity.put(rootProperty, innerEntity);
    return wrappedEntity;
  }

  /*
   * main is meant for troubleshooting the transformation or testing changes to it.
   */
  /*
  public static void main( String[] args ) throws TransformerException
  {
    try
    {
      Document doc = recordJsonToHarvesterXml( new JsonObject(
       TestRecords.jsonSampleHarvestable() ) );
      System.out.println( writeXmlDocumentToString( doc ) );

      JsonObject jsonObject = HarvesterXml2Json.convertRecordToJson(
      TestRecords.xmlSampleHarvestable() );
      if ( jsonObject != null )
      {
        Document doc2 = recordJsonToHarvesterXml( jsonObject );
        System.out.println( writeXmlDocumentToString( doc2 ) );
      }

    }
    catch ( DOMException | ParserConfigurationException e )
    {
      logger.error( e.getMessage() );
    }
  }
  */

}
