package org.folio.harvesteradmin.dataaccess.dataconverters;

import static org.folio.harvesteradmin.dataaccess.dataconverters.JsonToHarvesterXml.writeXmlNodeToString;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class HarvesterXml2JsonFailedRecords extends HarvesterXml2Json {

  /**
   * Converts a failed-record XML to JSON.
   */
  public static JsonObject convertRecordToJson(Document doc) {
    if (doc != null) {
      stripWhiteSpaceNodes(doc);
      return recurseIntoNode(doc);
    } else {
      logger.error("XML-to-JSON converter for failed-record received a null XML document.");
      return new JsonObject();
    }
  }

  protected static JsonObject recurseIntoNode(Node node) {
    JsonObject json = new JsonObject();
    for (Node child : iterable(node)) {
      if (hasChildElements(child)) {
        if (child.getNodeName().equals("record-errors")) {
          if (!json.containsKey("record-errors")) {
            json.put("record-errors", new JsonArray());
          }
          json.getJsonArray("record-errors").add(recurseIntoNode(child));
        } else if (child.getNodeName().equals("original")) {
          try {
            json.put("original", writeXmlNodeToString(child));
          } catch (TransformerException te) {
            json.put("original", te.getMessage());
          }
        } else {
          json.put(child.getNodeName(), recurseIntoNode(child));
        }
      } else {
        if (contentIsJson(child)) {
          json.put(child.getNodeName(), new JsonObject(child.getTextContent()));
        } else {
          json.put(child.getNodeName(), child.getTextContent());
        }
      }
    }
    return json;
  }

  protected static boolean contentIsJson(Node node) {
    if (node.getTextContent().isEmpty()) {
      return false;
    } else {
      String content = node.getTextContent().trim();
      if (!content.startsWith("{") || !content.endsWith("}")) {
        return false;
      } else {
        try {
          new JsonObject(node.getTextContent());
          return true;
        } catch (DecodeException de) {
          return false;
        }
      }
    }
  }


}
