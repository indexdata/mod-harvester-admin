package org.folio.harvesteradmin.service.fileimport.transformation;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class InventoryXmlToInventoryJson {

    public static final Logger logger = LogManager.getLogger("InventoryXmlToInventoryJson");

    public static JsonObject convert(String xmlStr)  {
        JsonObject genericJson = parseXmlToJson(xmlStr);
        JsonObject inventoryJson = new JsonObject();
        makeCollection(inventoryJson, genericJson);
        return inventoryJson;
    }

    public static JsonObject parseXmlToJson(String xmlStr)  {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            XMLToJSONHandler handler = new XMLToJSONHandler();
            saxParser.parse(new InputSource(new StringReader(xmlStr)), handler);
            return new JsonObject(handler.getData());
        } catch (ParserConfigurationException | SAXException e) {
            logger.error("Error parsing XML to JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // SAX parser, XML-to-JSON.
    public static class XMLToJSONHandler extends DefaultHandler {
        private final Stack<Map<String, Object>> stack = new Stack<>();
        private Map<String, Object> currentData = new HashMap<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            Map<String, Object> element = new HashMap<>();
            element.put("name", qName);
            if (!stack.isEmpty()) {
                stack.peek().computeIfAbsent("children", k -> new ArrayList<Map<String, Object>>());
                ((List<Map<String, Object>>) stack.peek().get("children")).add(element);
            }
            stack.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            //if (stack.size()>1) {
            if (!stack.isEmpty()) {
                currentData = stack.pop();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            String content = new String(ch, start, length).trim();
            if (!content.isEmpty()) {
                content = content.replace("&", "&amp;");
                content = content.replace("<", "&lt;");
                content = content.replace(">", "&gt;");
                stack.peek().put("text", content);
            }
        }

        public Map<String, Object> getData() {
            return currentData;
        }
    }

    private static void makeCollection(JsonObject toJson, JsonObject intermediateJson) {
        toJson.put("inventoryRecordSets", new JsonArray());
        if (intermediateJson.containsKey("children")) {
            JsonArray fromRecords = intermediateJson.getJsonArray("children");
            for (Object fromRecord : fromRecords ) {
                toJson.getJsonArray("inventoryRecordSets").add(makeInventoryJsonObject(new JsonObject(), (JsonObject)fromRecord));
            }
        }
    }
    private static JsonObject makeInventoryJsonObject(JsonObject toJson, JsonObject intermediateJson) {
        String propertyName = intermediateJson.getString("name");
        if (intermediateJson.containsKey("children")) {
            JsonArray childProperties = intermediateJson.getJsonArray("children");
            if (childProperties.getJsonObject(0).getString("name").equals("arr")) {
                JsonArray toArray = new JsonArray();
                toJson.put(intermediateJson.getString("name"), toArray);
                makeInventoryJsonArray(toArray, childProperties);
            } else {
                if (propertyName.equals("record")) {
                    for (Object childProperty : childProperties) {
                        makeInventoryJsonObject(toJson, (JsonObject) childProperty);
                    }
                } else {
                    JsonObject toProperty = new JsonObject();
                    toJson.put(propertyName, toProperty);
                    for (Object childProperty : childProperties) {
                        makeInventoryJsonObject(toProperty, (JsonObject) childProperty);
                    }
                }
            }
        } else if (intermediateJson.containsKey("text")) {
            toJson.put(propertyName, intermediateJson.getString("text"));
        }
        return toJson;
    }

    private static void makeInventoryJsonArray(JsonArray toJsonArray, JsonArray intermediateJsonArray) {
        for (Object o : intermediateJsonArray) {
            JsonObject element = (JsonObject) o;
            if (element.containsKey("children")) {
                for (Object child : element.getJsonArray("children")) {
                    if (((JsonObject) child).containsKey("children")) {
                        // array of objects
                        JsonObject arrayElement = new JsonObject();
                        JsonArray children = ((JsonObject) child).getJsonArray("children");
                        for (Object prop : children) {
                            makeInventoryJsonObject(arrayElement, (JsonObject) prop);
                        }
                        toJsonArray.add(arrayElement);
                    } else if (((JsonObject) child).containsKey("text")) {
                        // array of strings
                        toJsonArray.add(((JsonObject) child).getString("text"));
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String xmlStr = "<collection>\n" +
                "   <record>\n" +
                "      <processing>\n" +
                "         <holdingsRecord>\n" +
                "            <retainOmittedRecord>\n" +
                "               <ifField>hrid</ifField>\n" +
                "               <matchesPattern>\\D+.*</matchesPattern>\n" +
                "            </retainOmittedRecord>\n" +
                "            <retainExistingValues>\n" +
                "               <forOmittedProperties>true</forOmittedProperties>\n" +
                "            </retainExistingValues>\n" +
                "            <statisticalCoding>\n" +
                "               <arr>\n" +
                "                  <i>\n" +
                "                     <if>deleteSkipped</if>\n" +
                "                     <becauseOf>HOLDINGS_RECORD_PATTERN_MATCH</becauseOf>\n" +
                "                     <setCode>ac9bae48-d14c-4414-919a-292d539f9967</setCode>\n" +
                "                  </i>\n" +
                "                  <i>\n" +
                "                     <if>deleteSkipped</if>\n" +
                "                     <becauseOf>ITEM_PATTERN_MATCH</becauseOf>\n" +
                "                     <setCode>970b8b4e-ee88-4037-b954-a10ee75340f0</setCode>\n" +
                "                  </i>\n" +
                "               </arr>\n" +
                "            </statisticalCoding>\n" +
                "         </holdingsRecord>\n" +
                "         <item>\n" +
                "            <retainOmittedRecord>\n" +
                "               <ifField>hrid</ifField>\n" +
                "               <matchesPattern>\\D+.*</matchesPattern>\n" +
                "            </retainOmittedRecord>\n" +
                "            <retainExistingValues>\n" +
                "               <forOmittedProperties>true</forOmittedProperties>\n" +
                "            </retainExistingValues>\n" +
                "            <statisticalCoding>\n" +
                "               <arr>\n" +
                "                  <i>\n" +
                "                     <if>deleteSkipped</if>\n" +
                "                     <becauseOf>ITEM_STATUS</becauseOf>\n" +
                "                     <setCode>e7b3071c-8cc0-48cc-9cd0-dfc82c4e4602</setCode>\n" +
                "                  </i>\n" +
                "               </arr>\n" +
                "            </statisticalCoding>\n" +
                "            <status>\n" +
                "               <policy>overwrite</policy>\n" +
                "               <ifStatusWas>\n" +
                "                  <arr>\n" +
                "                     <i>\n" +
                "                        <name>On order</name>\n" +
                "                     </i>\n" +
                "                     <i>\n" +
                "                        <name>Available</name>\n" +
                "                     </i>\n" +
                "                     <i>\n" +
                "                        <name>Intellectual item</name>\n" +
                "                     </i>\n" +
                "                     <i>\n" +
                "                        <name>Missing</name>\n" +
                "                     </i>\n" +
                "                     <i>\n" +
                "                        <name>Restricted</name>\n" +
                "                     </i>\n" +
                "                     <i>\n" +
                "                        <name>Unknown</name>\n" +
                "                     </i>\n" +
                "                  </arr>\n" +
                "               </ifStatusWas>\n" +
                "            </status>\n" +
                "         </item>\n" +
                "      </processing>\n" +
                "      \n" +
                "      <instance>\n" +
                "         <source>K10plus</source>\n" +
                "         <hrid>359314724</hrid>\n" +
                "         <modeOfIssuanceId>9d18a02f-5897-4c31-9106-c9abb5c7ae8b</modeOfIssuanceId>\n" +
                "         <instanceTypeId>6312d172-f0cf-40f6-b27d-9fa8feaf332f</instanceTypeId>\n" +
                "         <instanceFormatIds>\n" +
                "            <arr>\n" +
                "               <i>8d511d33-5e85-4c5d-9bce-6e3c9cd0c324</i>\n" +
                "            </arr>\n" +
                "         </instanceFormatIds>\n" +
                "         <identifiers>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <value>359314724</value>\n" +
                "                  <identifierTypeId>1d5cb40c-508f-451b-8952-87c92be4255a</identifierTypeId>\n" +
                "               </i>\n" +
                "               <i>\n" +
                "                  <value>51698851</value>\n" +
                "                  <identifierTypeId>439bfbae-75bc-4f74-9fc7-b2a2d47ce3ef</identifierTypeId>\n" +
                "               </i>\n" +
                "               <i>\n" +
                "                  <value>GBV: 359314724</value>\n" +
                "                  <identifierTypeId>8e33c1be-e2c4-43ac-a975-8fb50f71137a</identifierTypeId>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </identifiers>\n" +
                "         <title>Describing a class of global attractors via symbol sequences / Jörg Härterich; Matthias Wolfrum</title>\n" +
                "         <indexTitle>Describing a class of global attractors via symbol sequences Jörg Härterich Matthias Wolfrum</indexTitle>\n" +
                "         <alternativeTitles>\n" +
                "            <arr/>\n" +
                "         </alternativeTitles>\n" +
                "         <contributors>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <name>Härterich, Jörg</name>\n" +
                "                  <contributorNameTypeId>2b94c631-fca9-4892-a730-03ee529ffe2a</contributorNameTypeId>\n" +
                "                  <primary>true</primary>\n" +
                "               </i>\n" +
                "               <i>\n" +
                "                  <name>Wolfrum, Matthias</name>\n" +
                "                  <contributorNameTypeId>2b94c631-fca9-4892-a730-03ee529ffe2a</contributorNameTypeId>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </contributors>\n" +
                "         <publication>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <place>Berlin</place>\n" +
                "                  <publisher>WIAS</publisher>\n" +
                "                  <dateOfPublication>2002</dateOfPublication>\n" +
                "                  <role>Publisher</role>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </publication>\n" +
                "         <electronicAccess>\n" +
                "            <arr/>\n" +
                "         </electronicAccess>\n" +
                "         <notes>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <note>Erscheint auch als Online-Ausgabe: Describing a class of global attractors via symbol sequences / Härterich, Jörg. - Berlin : Weierstraß-Institut für Angewandte Analysis und Stochastik (WIAS), 2002</note>\n" +
                "                  <instanceNoteTypeId>1d51e8b2-dee7-43f5-983c-a40757b9cdfa</instanceNoteTypeId>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </notes>\n" +
                "         <natureOfContentTermIds>\n" +
                "            <arr>\n" +
                "               <i>4570a93e-ddb6-4200-8e8b-283c8f5c9bfa</i>\n" +
                "            </arr>\n" +
                "         </natureOfContentTermIds>\n" +
                "         <languages>\n" +
                "            <arr>\n" +
                "               <i>eng</i>\n" +
                "            </arr>\n" +
                "         </languages>\n" +
                "         <series>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <value>Preprint ; 746</value>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </series>\n" +
                "         <physicalDescriptions>\n" +
                "            <arr>\n" +
                "               <i>26 S</i>\n" +
                "            </arr>\n" +
                "         </physicalDescriptions>\n" +
                "         <administrativeNotes>\n" +
                "            <arr>\n" +
                "               <i>Aau (0500: Bibliografische Gattung)</i>\n" +
                "               <i>08.02.24, 00:59 (0210: Datum der letzten Änderung)</i>\n" +
                "            </arr>\n" +
                "         </administrativeNotes>\n" +
                "      </instance>\n" +
                "      <instanceRelations>\n" +
                "         <parentInstances>\n" +
                "            <arr>\n" +
                "               <i>\n" +
                "                  <instanceIdentifier>\n" +
                "                     <hrid>51613356X</hrid>\n" +
                "                  </instanceIdentifier>\n" +
                "                  <provisionalInstance>\n" +
                "                     <title>Preprint / Weierstraß-Institut für Angewandte Analysis und Stochastik</title>\n" +
                "                     <instanceTypeId>6312d172-f0cf-40f6-b27d-9fa8feaf332f</instanceTypeId>\n" +
                "                     <source>K10plus</source>\n" +
                "                  </provisionalInstance>\n" +
                "                  <instanceRelationshipTypeId>30773a27-b485-4dab-aeb6-b8c04fa3cb17</instanceRelationshipTypeId>\n" +
                "               </i>\n" +
                "            </arr>\n" +
                "         </parentInstances>\n" +
                "      </instanceRelations>\n" +
                "      <holdingsRecords>\n" +
                "         <arr>\n" +
                "            <i>\n" +
                "               <hrid>586366237</hrid>\n" +
                "               <permanentLocationId>78091d58-5057-4f5d-bbcc-ca07eafd8cc1</permanentLocationId>\n" +
                "               <callNumber>K 2003 B 324</callNumber>\n" +
                "               <callNumberPrefix/>\n" +
                "               <holdingsTypeId>0c422f92-0f4d-4d32-8cbe-390ebc33a3e5</holdingsTypeId>\n" +
                "               <holdingsStatements/>\n" +
                "               <sourceId>fa687f33-aab5-4119-b0ad-05afe8de4d92</sourceId>\n" +
                "               <administrativeNotes>\n" +
                "                  <arr>\n" +
                "                     <i>27-12-10, 18:16 (7903: Datum und Uhrzeit der letzten Änderung)</i>\n" +
                "                  </arr>\n" +
                "               </administrativeNotes>\n" +
                "               <discoverySuppress>false</discoverySuppress>\n" +
                "               <notes>\n" +
                "                  <arr/>\n" +
                "               </notes>\n" +
                "               <electronicAccess>\n" +
                "                  <arr/>\n" +
                "               </electronicAccess>\n" +
                "               <illPolicyId/>\n" +
                "               <items>\n" +
                "                  <arr>\n" +
                "                     <i>\n" +
                "                        <hrid>586366237</hrid>\n" +
                "                        <materialTypeId>1a54b431-2e4f-452d-9cae-9cee66c9a892</materialTypeId>\n" +
                "                        <permanentLoanTypeId>a8cacfd0-b284-47ae-b5fa-235d768411e7</permanentLoanTypeId>\n" +
                "                        <status>\n" +
                "                           <name>Available</name>\n" +
                "                        </status>\n" +
                "                        <barcode>7$213905248</barcode>\n" +
                "                        <copyNumber/>\n" +
                "                        <volume/>\n" +
                "                        <chronology/>\n" +
                "                        <enumeration/>\n" +
                "                        <descriptionOfPieces/>\n" +
                "                        <accessionNumber/>\n" +
                "                        <discoverySuppress>false</discoverySuppress>\n" +
                "                     </i>\n" +
                "                  </arr>\n" +
                "               </items>\n" +
                "            </i>\n" +
                "         </arr>\n" +
                "      </holdingsRecords>\n" +
                "   </record>\n" +
                "</collection>\n";
        String jsonStr = convert(xmlStr).encodePrettily();
        System.out.println(jsonStr);
    }
}



