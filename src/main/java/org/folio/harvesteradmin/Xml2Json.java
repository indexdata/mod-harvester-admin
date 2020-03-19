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
public class Xml2Json {

  /**
   * main is meant for troubleshooting the transformation or testing changes to it.
   * @param args
   */
  public static void main (String[] args) {
    String xml1 =
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
      "        <name>Harvest Job A</name>\n" +
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
      "        <name>Harvest Job B</name>\n" +
      "        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
      "        <storageUrl>http://localhost:9130/</storageUrl>\n" +
      "    </harvestableBrief>\n" +
      "</harvestables>";

    String xml2 =
            "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"yes\\\"?>\\n\" +\n" +
      "\"<harvestable uri=\\\"http://localhost:8080/harvester/records/harvestables/2005\\\">\\n\" +\n" +
      "\"    <oaiPmh>\\n\" +\n" +
      "\"        <allowErrors>false</allowErrors>\\n\" +\n" +
      "\"        <cacheEnabled>false</cacheEnabled>\\n\" +\n" +
      "\"        <constantFields></constantFields>\\n\" +\n" +
      "\"        <contactNotes></contactNotes>\\n\" +\n" +
      "\"        <currentStatus>NEW</currentStatus>\\n\" +\n" +
      "\"        <description></description>\\n\" +\n" +
      "\"        <diskRun>false</diskRun>\\n\" +\n" +
      "\"        <enabled>false</enabled>\\n\" +\n" +
      "\"        <harvestImmediately>false</harvestImmediately>\\n\" +\n" +
      "\"        <id>2005</id>\\n\" +\n" +
      "\"        <initiallyHarvested>2019-11-21T13:01:05Z</initiallyHarvested>\\n\" +\n" +
      "\"        <json>{&#xD;\\n\" +\n" +
      "\" \\\"folioAuthPath\\\": \\\"bl-users/login\\\",&#xD;\\n\" +\n" +
      "\" \\\"folioTenant\\\": \\\"diku\\\",&#xD;\\n\" +\n" +
      "\" \\\"folioUsername\\\": \\\"diku_admin\\\",&#xD;\\n\" +\n" +
      "\" \\\"folioPassword\\\": \\\"admin\\\",&#xD;\\n\" +\n" +
      "\" \\\"instanceStoragePath\\\": \\\"instance-storage-match/instances\\\",&#xD;\\n\" +\n" +
      "\" \\\"holdingsStoragePath\\\": \\\"holdings-storage/holdings\\\",&#xD;\\n\" +\n" +
      "\" \\\"itemStoragePath\\\": \\\"item-storage/items\\\"&#xD;\\n\" +\n" +
      "\"}</json>\\n\" +\n" +
      "\"        <lastUpdated>2019-01-01T19:10:04Z</lastUpdated>\\n\" +\n" +
      "\"        <laxParsing>false</laxParsing>\\n\" +\n" +
      "\"        <logLevel>INFO</logLevel>\\n\" +\n" +
      "\"        <mailAddress></mailAddress>\\n\" +\n" +
      "\"        <mailLevel>WARN</mailLevel>\\n\" +\n" +
      "\"        <managedBy></managedBy>\\n\" +\n" +
      "\"        <name>SI, Millersville, physicals 6</name>\\n\" +\n" +
      "\"        <openAccess>false</openAccess>\\n\" +\n" +
      "\"        <overwrite>false</overwrite>\\n\" +\n" +
      "\"        <retryCount>2</retryCount>\\n\" +\n" +
      "\"        <retryWait>60</retryWait>\\n\" +\n" +
      "\"        <scheduleString>10 10 10 6 *</scheduleString>\\n\" +\n" +
      "\"        <serviceProvider>Millersville</serviceProvider>\\n\" +\n" +
      "\"        <storage xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:type=\\\"inventoryStorageEntity\\\">\\n\" +\n" +
      "\"            <bulkSize>1000</bulkSize>\\n\" +\n" +
      "\"            <currentStatus>TODO</currentStatus>\\n\" +\n" +
      "\"            <description>FOLIO</description>\\n\" +\n" +
      "\"            <enabled>true</enabled>\\n\" +\n" +
      "\"            <id>204</id>\\n\" +\n" +
      "\"            <idAsString>204</idAsString>\\n\" +\n" +
      "\"            <name>FOLIO @ localhost</name>\\n\" +\n" +
      "\"            <retryCount>2</retryCount>\\n\" +\n" +
      "\"            <retryWait>60</retryWait>\\n\" +\n" +
      "\"            <timeout>60</timeout>\\n\" +\n" +
      "\"            <url>http://10.0.2.2:9130/</url>\\n\" +\n" +
      "\"        </storage>\\n\" +\n" +
      "\"        <storeOriginal>false</storeOriginal>\\n\" +\n" +
      "\"        <technicalNotes></technicalNotes>\\n\" +\n" +
      "\"        <timeout>300</timeout>\\n\" +\n" +
      "\"        <transformation xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:type=\\\"basicTransformation\\\">\\n\" +\n" +
      "\"            <description>MARC21 to FOLIO Inventory, Temple</description>\\n\" +\n" +
      "\"            <enabled>true</enabled>\\n\" +\n" +
      "\"            <name>MARC21 to FOLIO Inventory, Temple</name>\\n\" +\n" +
      "\"            <parallel>false</parallel>\\n\" +\n" +
      "\"            <stepAssociations>\\n\" +\n" +
      "\"                <id>5002</id>\\n\" +\n" +
      "\"                <position>1</position>\\n\" +\n" +
      "\"                <step xsi:type=\\\"xmlTransformationStep\\\">\\n\" +\n" +
      "\"                    <description>MARC21 XML to FOLIO Instance XML</description>\\n\" +\n" +
      "\"                    <inputFormat>XML</inputFormat>\\n\" +\n" +
      "\"                    <name>MARC21 to Instance XML</name>\\n\" +\n" +
      "\"                    <outputFormat>XML</outputFormat>\\n\" +\n" +
      "\"                    <script>&lt;?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?&gt;&#xD;\\n\" +\n" +
      "\"&lt;xsl:stylesheet&#xD;\\n\" +\n" +
      "\"    version=\\\"1.0\\\"&#xD;\\n\" +\n" +
      "\"    xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\"&#xD;\\n\" +\n" +
      "\"    xmlns:marc=\\\"http://www.loc.gov/MARC21/slim\\\"&#xD;\\n\" +\n" +
      "\"    xmlns:oai20=\\\"http://www.openarchives.org/OAI/2.0/\\\"&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:import href=\\\"map-relator-to-contributor-type.xsl\\\"/&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:output indent=\\\"yes\\\" method=\\\"xml\\\" version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"/&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"&lt;!-- Extract metadata from MARC21/USMARC&#xD;\\n\" +\n" +
      "\"      http://www.loc.gov/marc/bibliographic/ecbdhome.html&#xD;\\n\" +\n" +
      "\"--&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"/\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;collection&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:apply-templates /&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/collection&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"//oai20:header[@status='deleted']\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;record status=\\\"deleted\\\"&gt;&#xD;\\n\" +\n" +
      "\"      &lt;status&gt;deleted&lt;/status&gt;&#xD;\\n\" +\n" +
      "\"      &lt;identifier&gt;&lt;xsl:value-of select=\\\"oai20:identifier\\\"/&gt;&lt;/identifier&gt;&#xD;\\n\" +\n" +
      "\"      &lt;identifierTypeIdHere/&gt;&#xD;\\n\" +\n" +
      "\"      &lt;permanentLocationIdHere/&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/record&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"//marc:record\\\"&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"    &lt;record&gt;&#xD;\\n\" +\n" +
      "\"      &lt;source&gt;MARC&lt;/source&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Instance type ID (resource type) --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;instanceTypeId&gt;&#xD;\\n\" +\n" +
      "\"        &lt;!-- UUIDs for resource types --&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='a'\\\"&gt;6312d172-f0cf-40f6-b27d-9fa8feaf332f&lt;/xsl:when&gt; &lt;!-- language material : text --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='c'\\\"&gt;497b5090-3da2-486c-b57f-de5bb3c2e26d&lt;/xsl:when&gt; &lt;!-- notated music : notated music --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='d'\\\"&gt;497b5090-3da2-486c-b57f-de5bb3c2e26d&lt;/xsl:when&gt; &lt;!-- manuscript notated music : notated music -&gt; notated music --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='e'\\\"&gt;526aa04d-9289-4511-8866-349299592c18&lt;/xsl:when&gt; &lt;!-- cartographic material : cartographic image --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='f'\\\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- other --&gt; &lt;!-- manuscript cartographic material : ? --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='g'\\\"&gt;535e3160-763a-42f9-b0c0-d8ed7df6e2a2&lt;/xsl:when&gt; &lt;!-- projected image : still image --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='i'\\\"&gt;9bce18bd-45bf-4949-8fa8-63163e4b7d7f&lt;/xsl:when&gt; &lt;!-- nonmusical sound recording : sounds --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='j'\\\"&gt;3be24c14-3551-4180-9292-26a786649c8b&lt;/xsl:when&gt; &lt;!-- musical sound recording : performed music --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='k'\\\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- other --&gt; &lt;!-- two-dimensional nonprojectable graphic : ?--&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='m'\\\"&gt;df5dddff-9c30-4507-8b82-119ff972d4d7&lt;/xsl:when&gt; &lt;!-- computer file : computer dataset --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='o'\\\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- kit : other --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='p'\\\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- mixed material : other --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='r'\\\"&gt;c1e95c2b-4efc-48cf-9e71-edb622cf0c22&lt;/xsl:when&gt; &lt;!-- three-dimensional artifact or naturally occurring object : three-dimensional form --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:when test=\\\"substring(marc:leader,7,1)='t'\\\"&gt;6312d172-f0cf-40f6-b27d-9fa8feaf332f&lt;/xsl:when&gt; &lt;!-- manuscript language material : text --&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:otherwise&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:otherwise&gt;                             &lt;!--  : other --&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/instanceTypeId&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Identifiers --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='010' or @tag='020' or @tag='022' or @tag='024' or @tag='028' or @tag='035' or @tag='074']&#xD;\\n\" +\n" +
      "\"                   or marc:controlfield[@tag='001']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;identifiers&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:for-each select=\\\"marc:controlfield[@tag='001']\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;i&gt;&#xD;\\n\" +\n" +
      "\"              &lt;value&gt;&lt;xsl:value-of select=\\\".\\\"/&gt;&lt;/value&gt;&#xD;\\n\" +\n" +
      "\"              &lt;!-- A subsequent library specific transformation (style sheet)&#xD;\\n\" +\n" +
      "\"                   must replace this tag with the actual identifierTypeId for&#xD;\\n\" +\n" +
      "\"                   the record identifer type of the given library --&gt;&#xD;\\n\" +\n" +
      "\"              &lt;identifierTypeIdHere/&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:for-each select=\\\"marc:datafield[@tag='001' or @tag='010' or @tag='020' or @tag='022' or @tag='024' or @tag='028' or @tag='035' or @tag='074']\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;i&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='010'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;c858e4f2-2b6b-4385-842b-60732ee14abb&lt;/identifierTypeId&gt; &lt;!-- LCCN --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='020'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;8261054f-be78-422d-bd51-4ed9f33c3422&lt;/identifierTypeId&gt; &lt;!-- ISBN --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='022'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;913300b2-03ed-469a-8179-c1092c991227&lt;/identifierTypeId&gt; &lt;!-- ISSN --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='024'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;2e8b3b6c-0e7d-4e48-bca2-b0b23b376af5&lt;/identifierTypeId&gt; &lt;!-- Other standard identifier --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='028'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;b5d8cdc4-9441-487c-90cf-0c7ec97728eb&lt;/identifierTypeId&gt; &lt;!-- Publisher number --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='035'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;7e591197-f335-4afb-bc6d-a6d76ca3bace&lt;/identifierTypeId&gt; &lt;!-- System control number --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:when test=\\\"current()[@tag='074'] and marc:subfield[@code='a']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;value&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/value&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;identifierTypeId&gt;351ebc1c-3aae-4825-8765-c6d50dbf011f&lt;/identifierTypeId&gt; &lt;!-- GPO item number --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/identifiers&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Classifications --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='050' or @tag='060' or @tag='080' or @tag='082' or @tag='086' or @tag='090']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;classifications&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:for-each select=\\\"marc:datafield[@tag='050' or @tag='060' or @tag='080' or @tag='082' or @tag='086' or @tag='090']\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"current()[@tag='050']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:for-each select=\\\"marc:subfield[@code='a' or @code='b']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:if test=\\\"position() &gt; 1\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:text&gt;; &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:value-of select=\\\".\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;/classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationTypeId&gt;ce176ace-a53e-4b4d-aa89-725ed7b2edac&lt;/classificationTypeId&gt; &lt;!-- LC, Library of Congress --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"current()[@tag='082']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:for-each select=\\\"marc:subfield[@code='a' or @code='b']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:if test=\\\"position() &gt; 1\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:text&gt;; &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:value-of select=\\\".\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;/classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationTypeId&gt;42471af9-7d25-4f3a-bf78-60d29dcf463b&lt;/classificationTypeId&gt; &lt;!-- Dewey --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"current()[@tag='086']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;/classificationNumber&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;classificationTypeId&gt;9075b5f8-7d97-49e1-a431-73fdd468d476&lt;/classificationTypeId&gt; &lt;!-- SUDOC --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/classifications&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- title --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:for-each select=\\\"marc:datafield[@tag='245']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;title&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='a']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:if test=\\\"marc:subfield[@code='b']\\\"&gt;&#xD;\\n\" +\n" +
      "\"           &lt;xsl:text&gt; : &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='b']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:if test=\\\"marc:subfield[@code='h']\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:text&gt; &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='h']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/title&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;matchKey&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:for-each select=\\\"marc:datafield[@tag='245']\\\"&gt;&#xD;\\n\" +\n" +
      "\"          &lt;title&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='a']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/title&gt;&#xD;\\n\" +\n" +
      "\"          &lt;remainder-of-title&gt;&#xD;\\n\" +\n" +
      "\"           &lt;xsl:text&gt; : &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='b']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/remainder-of-title&gt;&#xD;\\n\" +\n" +
      "\"          &lt;medium&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='h']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/medium&gt;&#xD;\\n\" +\n" +
      "\"          &lt;!-- Only fields that are actually included in&#xD;\\n\" +\n" +
      "\"               the instance somewhere - for example in 'title' -&#xD;\\n\" +\n" +
      "\"               should be included as 'matchKey' elements lest&#xD;\\n\" +\n" +
      "\"               the instance \\\"magically\\\" splits on \\\"invisible\\\"&#xD;\\n\" +\n" +
      "\"               properties.&#xD;\\n\" +\n" +
      "\"          &lt;name-of-part-section-of-work&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:value-of select=\\\"marc:subfield[@code='p']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/name-of-part-section-of-work&gt;&#xD;\\n\" +\n" +
      "\"          &lt;number-of-part-section-of-work&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:value-of select=\\\"marc:subfield[@code='n']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/number-of-part-section-of-work&gt;&#xD;\\n\" +\n" +
      "\"          &lt;inclusive-dates&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:value-of select=\\\"marc:subfield[@code='f']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/inclusive-dates&gt; --&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/matchKey&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Contributors --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='100' or @tag='110' or @tag='111' or @tag='700' or @tag='710' or @tag='711']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;contributors&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:for-each select=\\\"marc:datafield[@tag='100' or @tag='110' or @tag='111' or @tag='700' or @tag='710' or @tag='711']\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                &lt;name&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:for-each select=\\\"marc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='f' or @code='g' or @code='j' or @code='k' or @code='l' or @code='n' or @code='p' or @code='q' or @code='t' or @code='u']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:if test=\\\"position() &gt; 1\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:text&gt;, &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:with-param  name=\\\"input\\\" select=\\\".\\\" /&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-.&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/name&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"@tag='100' or @tag='700'\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;contributorNameTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe2a&lt;/contributorNameTypeId&gt; &lt;!-- personal name --&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:if test=\\\"@tag='100'\\\"&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;primary&gt;true&lt;/primary&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"@tag='110' or @tag='710'\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;contributorNameTypeId&gt;2e48e713-17f3-4c13-a9f8-23845bb210aa&lt;/contributorNameTypeId&gt; &lt;!-- corporate name --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:when test=\\\"@tag='111' or @tage='711'\\\"&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;contributorNameTypeId&gt;e8b311a6-3b21-43f2-a269-dd9310cb2d0a&lt;/contributorNameTypeId&gt; &lt;!-- meeting name --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;contributorNameTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe2a&lt;/contributorNameTypeId&gt; &lt;!-- personal name --&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:if test=\\\"marc:subfield[@code='e' or @code='4']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;contributorTypeId&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:call-template name=\\\"map-relator\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/contributorTypeId&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/contributors&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Editions --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='250']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;editions&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:for-each select=\\\"marc:datafield[@tag='250']\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;i&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:if test=\\\"marc:subfield[@code='b']\\\"&gt;; &lt;xsl:value-of select=\\\"marc:subfield[@code='b']\\\"/&gt;&lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/editions&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Publication --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:when test=\\\"marc:datafield[@tag='260' or @tag='264']\\\"&gt;&#xD;\\n\" +\n" +
      "\"          &lt;publication&gt;&#xD;\\n\" +\n" +
      "\"            &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:for-each select=\\\"marc:datafield[@tag='260' or @tag='264']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;publisher&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='b']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/publisher&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;place&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/place&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;dateOfPublication&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;xsl:value-of select=\\\"marc:subfield[@code='c']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/dateOfPublication&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/publication&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"          &lt;publication&gt;&#xD;\\n\" +\n" +
      "\"            &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"              &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                &lt;dateOfPublication&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:value-of select=\\\"substring(marc:controlfield[@tag='008'],8,4)\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/dateOfPublication&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/publication&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- physicalDescriptions --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='300']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;physicalDescriptions&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:for-each select=\\\"marc:datafield[@tag='300']\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:with-param  name=\\\"input\\\" select=\\\"marc:subfield[@code='a']\\\" /&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/physicalDescriptions&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- Subjects --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:if test=\\\"marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;subjects&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:for-each select=\\\"marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']\\\"&gt;&#xD;\\n\" +\n" +
      "\"            &lt;i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:for-each select=\\\"marc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='f' or @code='g' or @code='j' or @code='k' or @code='l' or @code='n' or @code='p' or @code='q' or @code='t' or @code='u' or @code='v' or @code='z']\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:if test=\\\"position() &gt; 1\\\"&gt;&#xD;\\n\" +\n" +
      "\"                &lt;xsl:text&gt;--&lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"              &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:with-param  name=\\\"input\\\" select=\\\".\\\" /&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;xsl:with-param  name=\\\"characters\\\"&gt;,-.&lt;/xsl:with-param&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/subjects&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"      &lt;!-- holdings and items, MARC fields to be processed by subsequent transformations or be removed before FOLIO --&gt;&#xD;\\n\" +\n" +
      "\"      &lt;passthrough&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:for-each select=\\\"marc:datafield[@tag='852' or @tag='900' or @tag='954' or @tag='995']\\\"&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:copy-of select=\\\".\\\"/&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/passthrough&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/record&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"text()\\\"/&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:param name=\\\"input\\\" /&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:param name=\\\"characters\\\"/&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:variable name=\\\"lastcharacter\\\" select=\\\"substring($input,string-length($input))\\\" /&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:when test=\\\"$characters and $lastcharacter and contains($characters, $lastcharacter)\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:call-template name=\\\"remove-characters-last\\\"&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:with-param  name=\\\"input\\\" select=\\\"substring($input,1, string-length($input)-1)\\\" /&gt;&#xD;\\n\" +\n" +
      "\"          &lt;xsl:with-param  name=\\\"characters\\\" select=\\\"$characters\\\" /&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/xsl:call-template&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"        &lt;xsl:value-of select=\\\"$input\\\"/&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"&lt;/xsl:stylesheet&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"</script>\\n\" +\n" +
      "\"                    <id>4004</id>\\n\" +\n" +
      "\"                </step>\\n\" +\n" +
      "\"                <transformation>3004</transformation>\\n\" +\n" +
      "\"            </stepAssociations>\\n\" +\n" +
      "\"            <stepAssociations>\\n\" +\n" +
      "\"                <id>5005</id>\\n\" +\n" +
      "\"                <position>2</position>\\n\" +\n" +
      "\"                <step xsi:type=\\\"xmlTransformationStep\\\">\\n\" +\n" +
      "\"                    <description>Holdings and Items, Temple</description>\\n\" +\n" +
      "\"                    <inputFormat>XML</inputFormat>\\n\" +\n" +
      "\"                    <name>Holdings and Items, Temple</name>\\n\" +\n" +
      "\"                    <outputFormat>XML</outputFormat>\\n\" +\n" +
      "\"                    <script>&lt;?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?&gt;&#xD;\\n\" +\n" +
      "\"&lt;xsl:stylesheet&#xD;\\n\" +\n" +
      "\"  version=\\\"1.0\\\"&#xD;\\n\" +\n" +
      "\"  xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\"&#xD;\\n\" +\n" +
      "\"  xmlns:marc=\\\"http://www.loc.gov/MARC21/slim\\\"&#xD;\\n\" +\n" +
      "\"  &gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:strip-space elements=\\\"*\\\"/&gt;&#xD;\\n\" +\n" +
      "\"  &lt;xsl:output indent=\\\"yes\\\" method=\\\"xml\\\" version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"/&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"@* | node()\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:copy&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:apply-templates select=\\\"@* | node()\\\"/&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/xsl:copy&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"passthrough\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:when test=\\\"marc:datafield[@tag='852']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"           &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"             &lt;xsl:for-each select=\\\"marc:datafield[@tag='852']\\\"&gt;&#xD;\\n\" +\n" +
      "\"               &lt;xsl:variable name=\\\"holdingsId\\\" select=\\\"marc:subfield[@code='8']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"               &lt;xsl:variable name=\\\"holdingPos\\\" select=\\\"position()\\\"/&gt;&#xD;\\n\" +\n" +
      "\"               &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;formerIds&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                       &lt;xsl:value-of select=\\\"marc:subfield[@code='8']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;/formerIds&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;permanentLocationIdHere&gt;&lt;xsl:value-of select=\\\"marc:subfield[@code='b']\\\"/&gt;&lt;/permanentLocationIdHere&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;callNumber&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;xsl:for-each select=\\\"marc:subfield[@code='h']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;xsl:if test=\\\"position() &gt; 1\\\"&gt;&#xD;\\n\" +\n" +
      "\"                       &lt;xsl:text&gt; &lt;/xsl:text&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;xsl:value-of select=\\\".\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;/callNumber&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;items&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;xsl:for-each select=\\\"../marc:datafield[@tag='954']\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:if test=\\\"position() = $holdingPos\\\"&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;itemIdentifier&gt;&#xD;\\n\" +\n" +
      "\"                            &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;/itemIdentifier&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;barcode&gt;&#xD;\\n\" +\n" +
      "\"                            &lt;xsl:value-of select=\\\"marc:subfield[@code='b']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;/barcode&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;permanentLoanTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe27&lt;/permanentLoanTypeId&gt;                    &lt;!-- Can circulate --&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;materialTypeId&gt;&#xD;\\n\" +\n" +
      "\"                            &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                              &lt;xsl:when test=\\\"marc:subfield[@code='d']='BOOK'\\\"&gt;1a54b431-2e4f-452d-9cae-9cee66c9a892&lt;/xsl:when&gt; &lt;!-- Book --&gt;&#xD;\\n\" +\n" +
      "\"                              &lt;xsl:otherwise&gt;71fbd940-1027-40a6-8a48-49b44d795e46&lt;/xsl:otherwise&gt;                              &lt;!-- Unspecified --&gt;&#xD;\\n\" +\n" +
      "\"                            &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;/materialTypeId&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;status&gt;&#xD;\\n\" +\n" +
      "\"                            &lt;name&gt;Unknown&lt;/name&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;/status&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;/xsl:if&gt;&#xD;\\n\" +\n" +
      "\"                     &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"                   &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"                 &lt;/items&gt;&#xD;\\n\" +\n" +
      "\"               &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"             &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"           &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:when test=\\\"marc:datafield[@tag='954']\\\"&gt;&#xD;\\n\" +\n" +
      "\"        &lt;holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"            &lt;xsl:for-each select=\\\"marc:datafield[@tag='954']\\\"&gt;&#xD;\\n\" +\n" +
      "\"              &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                &lt;!-- No \\\"852\\\" tag (no holdings record), use ID of item as holdingsRecord ID as well --&gt;&#xD;\\n\" +\n" +
      "\"                &lt;formerIds&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;                      &#xD;\\n\" +\n" +
      "\"                    &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/formerIds&gt;&#xD;\\n\" +\n" +
      "\"                &lt;permanentLocationIdHere /&gt;&#xD;\\n\" +\n" +
      "\"                &lt;items&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;i&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;itemIdentifier&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:value-of select=\\\"marc:subfield[@code='a']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/itemIdentifier&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;barcode&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:value-of select=\\\"marc:subfield[@code='b']\\\"/&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/barcode&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;permanentLoanTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe27&lt;/permanentLoanTypeId&gt;                      &lt;!-- Can circulate --&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;materialTypeId&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;xsl:when test=\\\"marc:subfield[@code='d']='BOOK'\\\"&gt;1a54b431-2e4f-452d-9cae-9cee66c9a892&lt;/xsl:when&gt; &lt;!-- Book --&gt;&#xD;\\n\" +\n" +
      "\"                          &lt;xsl:otherwise&gt;71fbd940-1027-40a6-8a48-49b44d795e46&lt;/xsl:otherwise&gt;                              &lt;!-- Unspecified --&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/materialTypeId&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;status&gt;&#xD;\\n\" +\n" +
      "\"                        &lt;name&gt;Unknown&lt;/name&gt;&#xD;\\n\" +\n" +
      "\"                      &lt;/status&gt;&#xD;\\n\" +\n" +
      "\"                    &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"                  &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"                &lt;/items&gt;&#xD;\\n\" +\n" +
      "\"              &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/xsl:for-each&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:when&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"        &lt;holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"          &lt;arr&gt;&#xD;\\n\" +\n" +
      "\"            &lt;i&gt;&#xD;\\n\" +\n" +
      "\"              &lt;permanentLocationIdHere /&gt;&#xD;\\n\" +\n" +
      "\"            &lt;/i&gt;&#xD;\\n\" +\n" +
      "\"          &lt;/arr&gt;&#xD;\\n\" +\n" +
      "\"        &lt;/holdingsRecords&gt;&#xD;\\n\" +\n" +
      "\"      &lt;/xsl:otherwise&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/xsl:choose&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&lt;/xsl:stylesheet&gt;</script>\\n\" +\n" +
      "\"                    <id>4007</id>                    \\n\" +\n" +
      "\"                </step>\\n\" +\n" +
      "\"                <transformation>3004</transformation>\\n\" +\n" +
      "\"            </stepAssociations>\\n\" +\n" +
      "\"            <stepAssociations>\\n\" +\n" +
      "\"                <id>5013</id>\\n\" +\n" +
      "\"                <position>3</position>\\n\" +\n" +
      "\"                <step xsi:type=\\\"xmlTransformationStep\\\">\\n\" +\n" +
      "\"                    <description>Maps locations, record identifier type</description>\\n\" +\n" +
      "\"                    <inputFormat>XML</inputFormat>\\n\" +\n" +
      "\"                    <name>Library codes, Temple</name>\\n\" +\n" +
      "\"                    <outputFormat>XML</outputFormat>\\n\" +\n" +
      "\"                    <script>&lt;?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" ?&gt;&#xD;\\n\" +\n" +
      "\"&lt;xsl:stylesheet version=\\\"1.0\\\" xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\"&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"@* | node()\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;xsl:copy&gt;&#xD;\\n\" +\n" +
      "\"      &lt;xsl:apply-templates select=\\\"@* | node()\\\"/&gt;&#xD;\\n\" +\n" +
      "\"    &lt;/xsl:copy&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;!-- Map legacy code for the library/institution to a FOLIO resource identifier&#xD;\\n\" +\n" +
      "\"       type UUID. Used for qualifying a local record identifier with the library&#xD;\\n\" +\n" +
      "\"       it originated from in context of a shared index setup where the Instance&#xD;\\n\" +\n" +
      "\"       represents bib records from multiple libraries.&#xD;\\n\" +\n" +
      "\"  --&gt;&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"//identifierTypeIdHere\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;identifierTypeId&gt;17bb9b44-0063-44cc-8f1a-ccbb6188060b&lt;/identifierTypeId&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"  &lt;!-- Map legacy location code to a FOLIO location UUID --&gt;&#xD;\\n\" +\n" +
      "\"  &lt;xsl:template match=\\\"//permanentLocationIdHere\\\"&gt;&#xD;\\n\" +\n" +
      "\"    &lt;permanentLocationId&gt;87038e41-0990-49ea-abd9-1ad00a786e45&lt;/permanentLocationId&gt; &lt;!-- Temple --&gt;&#xD;\\n\" +\n" +
      "\"  &lt;/xsl:template&gt;&#xD;\\n\" +\n" +
      "\"&#xD;\\n\" +\n" +
      "\"&lt;/xsl:stylesheet&gt;&#xD;\\n\" +\n" +
      "\"</script>\\n\" +\n" +
      "\"                    <id>4011</id>\\n\" +\n" +
      "\"                    \\n\" +\n" +
      "\"                </step>\\n\" +\n" +
      "\"                <transformation>3004</transformation>\\n\" +\n" +
      "\"            </stepAssociations>\\n\" +\n" +
      "\"            <stepAssociations>\\n\" +\n" +
      "\"                <id>5008</id>\\n\" +\n" +
      "\"                <position>6</position>\\n\" +\n" +
      "\"                <step xsi:type=\\\"customTransformationStep\\\">\\n\" +\n" +
      "\"                    <customClass>com.indexdata.masterkey.localindices.harvest.messaging.InstanceXmlToInstanceJsonTransformerRouter</customClass>\\n\" +\n" +
      "\"                    <description>FOLIO Instance XML to JSON</description>\\n\" +\n" +
      "\"                    <enabled>true</enabled>\\n\" +\n" +
      "\"                    <inputFormat>XML</inputFormat>\\n\" +\n" +
      "\"                    <name>Instance XML to JSON</name>\\n\" +\n" +
      "\"                    <outputFormat>JSON</outputFormat>\\n\" +\n" +
      "\"                    <script></script>\\n\" +\n" +
      "\"                    <id>4003</id>\\n\" +\n" +
      "\"                    <testData></testData>\\n\" +\n" +
      "\"                    <testOutput></testOutput>\\n\" +\n" +
      "\"                    <type>custom</type>\\n\" +\n" +
      "\"                </step>\\n\" +\n" +
      "\"                <transformation>3004</transformation>\\n\" +\n" +
      "\"            </stepAssociations>\\n\" +\n" +
      "\"            <id>3004</id>\\n\" +\n" +
      "\"        </transformation>\\n\" +\n" +
      "\"        <usedBy></usedBy>\\n\" +\n" +
      "\"        <clearRtOnError>false</clearRtOnError>\\n\" +\n" +
      "\"        <dateFormat>yyyy-MM-dd'T'hh:mm:ss'Z'</dateFormat>\\n\" +\n" +
      "\"        <keepPartial>true</keepPartial>\\n\" +\n" +
      "\"        <metadataPrefix>marc21</metadataPrefix>\\n\" +\n" +
      "\"        <oaiSetName>IndexDataHoldItemPhysicalTitles</oaiSetName>\\n\" +\n" +
      "\"        <resumptionToken></resumptionToken>\\n\" +\n" +
      "\"        <url>https://na01.alma.exlibrisgroup.com/view/oai/01SSHELCO_MILLRSVL/request</url>\\n\" +\n" +
      "\"    </oaiPmh>\\n\" +\n" +
      "\"    <id>2005</id>\\n\" +\n" +
      "\"</harvestable>\\n\";\n";

    Document doc = XMLStringToXMLDocument(xml1);
    System.out.println(recordSetXml2json(xml1).encodePrettily());
  }

  /**
   * Create JSON object from String of XML
   * @param xml
   * @return
   */
  public static JsonObject recordSetXml2json(String xml) {
    JsonObject jsonObject = new JsonObject();
    Document doc = XMLStringToXMLDocument(xml);
    stripWhiteSpaceNodes(doc);
    Node records = doc.getDocumentElement();
    jsonObject.put(doc.getDocumentElement().getNodeName(),xmlRecords2jsonArray(records));
    jsonObject.put("totalRecords", Integer.parseInt(records.getAttributes().getNamedItem("count").getTextContent()));
    return jsonObject;
  }

  /**
   * Creates JSON object from a single XML record
   * @param xml
   * @return
   */
  public static JsonObject recordXml2Json (String xml) {
    JsonObject jsonObject = new JsonObject();
    Document doc = XMLStringToXMLDocument(xml);
    stripWhiteSpaceNodes(doc);
    jsonObject = node2json(doc);
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
   * Creates a JSON object from an XML element;
   * recursively if necessary
   * The code relies on knowledge about the names of XML elements
   * that are repeatable in the Harvester WS API - it has always been
   * only one, 'stepAssociations'.
   *
   * @param node XML element to create JsonObject for
   * @return XML element as JSON object
   */
  private static JsonObject node2json (Node node) {
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
          json.getJsonArray("stepAssociations").add(node2json(child));
        } else {
          json.put(child.getNodeName(), node2json(child));
        }
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
