/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

/**
 * Sample records for testing {@link Json2Xml} and {@link Xml2Json}
 * @author ne
 */
public class TestRecords {

  public static String jsonSampleHarvestable() {
    return "{\n" +
          "  \"harvestable\" : {\n" +
          "    \"xmlBulk\" : {\n" +
          "      \"allowErrors\" : \"false\",\n" +
          "      \"cacheEnabled\" : \"false\",\n" +
          "      \"contactNotes\" : \"\",\n" +
          "      \"currentStatus\" : \"OK\",\n" +
          "      \"description\" : \"\",\n" +
          "      \"diskRun\" : \"false\",\n" +
          "      \"enabled\" : \"true\",\n" +
          "      \"harvestImmediately\" : \"false\",\n" +
          "      \"id\" : \"9999\",\n" +
          "      \"initiallyHarvested\" : \"2011-12-28T21:22:32Z\",\n" +
          "      \"json\" : \"\",\n" +
          "      \"lastHarvestFinished\" : \"2015-06-10T10:32:11Z\",\n" +
          "      \"lastHarvestStarted\" : \"2015-06-10T10:10:00Z\",\n" +
          "      \"lastUpdated\" : \"2014-09-24T08:33:27Z\",\n" +
          "      \"laxParsing\" : \"false\",\n" +
          "      \"logLevel\" : \"DEBUG\",\n" +
          "      \"mailAddress\" : \"\",\n" +
          "      \"mailLevel\" : \"WARN\",\n" +
          "      \"name\" : \"Test data\",\n" +
          "      \"openAccess\" : \"true\",\n" +
          "      \"overwrite\" : \"true\",\n" +
          "      \"retryCount\" : \"2\",\n" +
          "      \"retryWait\" : \"60\",\n" +
          "      \"scheduleString\" : \"10 10 10 6 *\",\n" +
          "      \"serviceProvider\" : \"Index Data\",\n" +
          "      \"storage\" : {\n" +
          "        \"entityType\" : \"solrStorageEntity\",\n" +
          "        \"bulkSize\" : \"1000\",\n" +
          "        \"currentStatus\" : \"TODO\",\n" +
          "        \"description\" : \"Solr at localhost:8983\",\n" +
          "        \"enabled\" : \"true\",\n" +
          "        \"id\" : \"103\",\n" +
          "        \"idAsString\" : \"103\",\n" +
          "        \"name\" : \"PUT Solr @ localhost\",\n" +
          "        \"retryCount\" : \"2\",\n" +
          "        \"retryWait\" : \"60\",\n" +
          "        \"timeout\" : \"60\",\n" +
          "        \"url\" : \"http://localhost:8983/solr/lui/\"\n" +
          "      },\n" +
          "      \"storeOriginal\" : \"false\",\n" +
          "      \"technicalNotes\" : \"\",\n" +
          "      \"timeout\" : \"60\",\n" +
          "      \"transformation\" : {\n" +
          "        \"entityType\" : \"basicTransformation\",\n" +
          "        \"description\" : \"\",\n" +
          "        \"enabled\" : \"true\",\n" +
          "        \"name\" : \"OAI-PMH(DC) to PZ, simple\",\n" +
          "        \"stepAssociations\" : [ {\n" +
          "          \"id\" : \"5252\",\n" +
          "          \"position\" : \"1\",\n" +
          "          \"step\" : {\n" +
          "            \"entityType\" : \"xmlTransformationStep\",\n" +
          "            \"customClass\" : \"PZ\",\n" +
          "            \"description\" : \"Missing non-protocol filtering \",\n" +
          "            \"enabled\" : \"true\",\n" +
          "            \"inputFormat\" : \"\",\n" +
          "            \"name\" : \"OAIPMH-DC to PZ\",\n" +
          "            \"outputFormat\" : \"\",\n" +
          "            \"script\" : \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\r\\n<!--\\r\\n\\r\\n    This stylesheet expects oai/dc records\\r\\n-->\\r\\n<xsl:stylesheet\\r\\n    version=\\\"1.0\\\"\\r\\n    xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\"\\r\\n    xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\"\\r\\n    xmlns:oai=\\\"http://www.openarchives.org/OAI/2.0/\\\"\\r\\n    xmlns:dc=\\\"http://purl.org/dc/elements/1.1/\\\"\\r\\n    xmlns:dcterms=\\\"http://purl.org/dc/terms/\\\">\\r\\n\\r\\n  <xsl:output indent=\\\"yes\\\"\\r\\n              method=\\\"xml\\\"\\r\\n              version=\\\"1.0\\\"\\r\\n              encoding=\\\"UTF-8\\\"/>\\r\\n\\r\\n  <xsl:template match=\\\"/oai:OAI-PMH\\\">\\r\\n    <xsl:apply-templates/>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"oai:ListRecords\\\">\\r\\n    <pz:collection>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:collection>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"oai:record\\\">\\r\\n    <pz:record>\\r\\n      <pz:metadata type=\\\"id\\\">\\r\\n        <xsl:value-of select=\\\"oai:header/oai:identifier\\\"/>\\r\\n      </pz:metadata>\\r\\n      <xsl:if test=\\\"oai:header[@status='deleted']\\\">\\r\\n <pz:metadata type=\\\"record_status\\\">deleted</pz:metadata>\\r\\n      </xsl:if>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:record>\\r\\n  </xsl:template>\\r\\n    \\r\\n  <xsl:template match=\\\"oai:metadata/*\\\">\\r\\n    <xsl:for-each select=\\\"dc:title\\\">\\r\\n      <pz:metadata type=\\\"title\\\">\\r\\n        <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:date\\\">\\r\\n      <pz:metadata type=\\\"date\\\">\\r\\n <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:subject\\\">\\r\\n      <pz:metadata type=\\\"subject\\\">\\r\\n <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:creator\\\">\\r\\n      <pz:metadata type=\\\"author\\\">\\r\\n        <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:description\\\">\\r\\n      <pz:metadata type=\\\"description\\\">\\r\\n <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:identifier\\\">\\r\\n      <pz:metadata type=\\\"electronic-url\\\">\\r\\n <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n    <xsl:for-each select=\\\"dc:type\\\">\\r\\n      <pz:metadata type=\\\"medium\\\">\\r\\n <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n      \\r\\n    <xsl:for-each select=\\\"dcterms:bibliographicCitation\\\">\\r\\n      <pz:metadata type=\\\"citation\\\">\\r\\n        <xsl:value-of select=\\\".\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:for-each>\\r\\n\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"text()\\\"/>\\r\\n\\r\\n</xsl:stylesheet>\",\n" +
          "            \"id\" : \"10\",\n" +
          "            \"testData\" : \"OAI-PMH(DC)\",\n" +
          "            \"type\" : \"\"\n" +
          "          },\n" +
          "          \"transformation\" : \"5203\"\n" +
          "        }, {\n" +
          "          \"id\" : \"5255\",\n" +
          "          \"position\" : \"2\",\n" +
          "          \"step\" : {\n" +
          "            \"entityType\" : \"xmlTransformationStep\",\n" +
          "            \"customClass\" : \"PZ\",\n" +
          "            \"description\" : \"Medium cleanup\",\n" +
          "            \"inputFormat\" : \"\",\n" +
          "            \"name\" : \"Medium Normalization (PZ)\",\n" +
          "            \"outputFormat\" : \"\",\n" +
          "            \"script\" : \"<?xml version=\\\"1.0\\\"?>\\r\\n<xsl:stylesheet version=\\\"1.0\\\" xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\" \\r\\n    xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\" \\r\\n    xmlns:tmarc=\\\"http://www.indexdata.com/turbomarc\\\">\\r\\n\\r\\n  <xsl:output indent=\\\"yes\\\" method=\\\"xml\\\" version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" />\\r\\n  <xsl:template  match=\\\"/\\\">\\r\\n    <pz:collection>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:collection>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"pz:record\\\">\\r\\n    <pz:record>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:record>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"pz:metadata[@type='medium']\\\">\\r\\n    <pz:metadata type='medium'>\\r\\n      <xsl:choose>\\r\\n        <xsl:when \\r\\n     test=\\\". = 'article' or . = 'audio-visual' or . = 'article (electronic)' or \\r\\n    . = 'book' or . = 'book (electronic)' or \\r\\n     . = 'electronicresource' or . = 'electronic' or\\r\\n     . = 'journal (electronic)' or . = 'journal' or \\r\\n     . = 'map (electronic)' or . = 'map' or \\r\\n     . = 'microform' or . = 'music-score' or . = 'music-score (electronic)' or \\r\\n    . = 'newspaper' or . = 'newspaper (electronic)' or\\r\\n    . = 'recording' or . = 'recording (electronic)' or . = 'recording-cd' or . = 'recording-cassette' or . = 'recording-vinyl' or \\r\\n    . = 'soundrecording' or \\r\\n    . = 'thesis (electronic)' or . = 'thesis' or\\r\\n    . = 'video-dvd' or . = 'video-vhs' or . = 'video (electronic)' or . = 'video' or . = 'videorecording' or . = 'video' or . = 'video-blu-ray'  or\\r\\n     . = 'web' or . = 'web (eletronic)' or\\r\\n     . = 'realia' or . = 'picture' or \\r\\n     . = 'other' \\r\\n    \\\">\\r\\n          <xsl:value-of select=\\\".\\\"/>\\r\\n        </xsl:when>\\r\\n        <!-- Correct some typical differences -->\\r\\n        <xsl:when test=\\\". = 'sound recording(dc)'\\\"> \\r\\n            <xsl:text>recording-cd</xsl:text>\\r\\n        </xsl:when>         \\r\\n        <xsl:when test=\\\". = 'videorecording(dvd)'\\\">\\r\\n            <xsl:text>video-dvd</xsl:text>\\r\\n        </xsl:when>         \\r\\n        <xsl:when \\r\\n         test=\\\". = 'electronic resource' or . = 'electronisk resours' \\\" >\\r\\n          <xsl:text>electronicresource</xsl:text>\\r\\n        </xsl:when>\\r\\n        <xsl:otherwise>\\r\\n          <xsl:text>other</xsl:text>\\r\\n        </xsl:otherwise>\\r\\n      </xsl:choose>\\r\\n    </pz:metadata>\\r\\n  </xsl:template>\\r\\n  \\r\\n  <xsl:template match=\\\"pz:metadata\\\"> \\r\\n    <xsl:copy-of select=\\\".\\\" />\\r\\n  </xsl:template>\\r\\n  \\r\\n  <xsl:template match=\\\"text()\\\" />\\r\\n</xsl:stylesheet>\\r\\n\",\n" +
          "            \"id\" : \"4055\",\n" +
          "            \"testData\" : \"PZ\",\n" +
          "            \"type\" : \"\"\n" +
          "          },\n" +
          "          \"transformation\" : \"5203\"\n" +
          "        }, {\n" +
          "          \"id\" : \"5253\",\n" +
          "          \"position\" : \"3\",\n" +
          "          \"step\" : {\n" +
          "            \"entityType\" : \"xmlTransformationStep\",\n" +
          "            \"customClass\" : \"PZ\",\n" +
          "            \"description\" : \"Map to one\",\n" +
          "            \"inputFormat\" : \"\",\n" +
          "            \"name\" : \"Anonymous normalization\",\n" +
          "            \"outputFormat\" : \"\",\n" +
          "            \"script\" : \"<?xml version=\\\"1.0\\\"?>\\r\\n<xsl:stylesheet version=\\\"1.0\\\" xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\" \\r\\n   xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\" \\r\\n    xmlns:tmarc=\\\"http://www.indexdata.com/turbomarc\\\">\\r\\n\\r\\n  <xsl:output indent=\\\"yes\\\" method=\\\"xml\\\" version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" />\\r\\n  <xsl:template  match=\\\"/\\\">\\r\\n    <pz:collection>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:collection>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"pz:record\\\">\\r\\n    <pz:record>\\r\\n      <xsl:apply-templates/>\\r\\n    </pz:record>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"pz:metadata[@type='author']\\\">\\r\\n    <pz:metadata type='author'>\\r\\n      <xsl:choose>\\r\\n        <xsl:when \\r\\n     test=\\\". = 'ANON'   or . = 'Anon'   or . = 'anon'   or \\r\\n     . = 'ANON)'  or . = 'Anon)'  or . = 'anon)'  or \\r\\n    . = 'ANON.)' or . = 'Anon.)' or . = 'anon.)' or\\r\\n                 . = 'ANON]'  or . = 'Anon]'  or . = 'anon]'  or \\r\\n    . = 'ANON.]' or . = 'Anon.]' or . = 'anon.]' or \\r\\n    . = 'Anon.,' or . = 'anon). / anon' or\\r\\n    . = 'ANONYMOUS' or . = 'anonymous' or \\r\\n    . = 'Anonymous (editor)' or . = 'Anonymous Editor' or \\r\\n    . = 'anonymous]' or . = 'Anonymous]' or . = 'ANONYMOUS]' or\\r\\n     . = 'ANONYMOUS)' or . = 'Anonymous)' or  . = 'anonymous)'  \\r\\n\\r\\n     \\\">\\r\\n          <xsl:text>Anonymous</xsl:text>\\r\\n        </xsl:when>\\r\\n        <xsl:otherwise>\\r\\n          <xsl:value-of select=\\\".\\\"/>\\r\\n        </xsl:otherwise>\\r\\n      </xsl:choose>\\r\\n    </pz:metadata>\\r\\n  </xsl:template>\\r\\n  \\r\\n  <xsl:template match=\\\"pz:metadata\\\"> \\r\\n    <xsl:copy-of select=\\\".\\\" />\\r\\n  </xsl:template>\\r\\n  \\r\\n  <xsl:template match=\\\"text()\\\" />\\r\\n</xsl:stylesheet>\\r\\n\",\n" +
          "            \"id\" : \"4402\",\n" +
          "            \"testData\" : \"<records xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\">\\r\\n  <pz:record>\\r\\n    <pz:metadata type=\\\"author\\\">ANON</pz:metadata>\\r\\n  </pz:record>\\r\\n</records>\",\n" +
          "            \"testOutput\" : \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\r\\n<pz:collection xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\" xmlns:tmarc=\\\"http://www.indexdata.com/turbomarc\\\">\\r\\n<pz:record>\\r\\n<pz:metadata type=\\\"author\\\">Anonymous</pz:metadata>\\r\\n</pz:record>\\r\\n</pz:collection>\\r\\n\",\n" +
          "            \"type\" : \"XSLT\"\n" +
          "          },\n" +
          "          \"transformation\" : \"5203\"\n" +
          "        }, {\n" +
          "          \"id\" : \"5254\",\n" +
          "          \"position\" : \"4\",\n" +
          "          \"step\" : {\n" +
          "            \"entityType\" : \"xmlTransformationStep\",\n" +
          "            \"customClass\" : \"PZ\",\n" +
          "            \"description\" : \"Removing date from Author\",\n" +
          "            \"inputFormat\" : \"\",\n" +
          "            \"name\" : \"Author - clean date at end \",\n" +
          "            \"outputFormat\" : \"\",\n" +
          "            \"script\" : \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" ?>\\r\\n<xsl:stylesheet version=\\\"2.0\\\" xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\" \\r\\n   xmlns:pz=\\\"http://www.indexdata.com/pazpar2/1.0\\\">\\r\\n  <xsl:template match=\\\"@* | node()\\\">\\r\\n    <xsl:copy>\\r\\n      <xsl:apply-templates select=\\\"@* | node()\\\"/>\\r\\n    </xsl:copy>\\r\\n  </xsl:template>\\r\\n\\r\\n  <xsl:template match=\\\"pz:metadata[@type='author']\\\">\\r\\n    <xsl:variable name=\\\"author\\\" select=\\\"replace(string(.), ', [0-9]{4}\\\\-[0-9]{4}$', '')\\\" />\\r\\n    <xsl:if test=\\\"$author and $author != '' \\\">\\r\\n      <pz:metadata type=\\\"author\\\">\\r\\n  <xsl:value-of select=\\\"$author\\\"/>\\r\\n      </pz:metadata>\\r\\n    </xsl:if>\\r\\n  </xsl:template>\\r\\n</xsl:stylesheet>\",\n" +
          "            \"id\" : \"4159\",\n" +
          "            \"testData\" : \"PZ\",\n" +
          "            \"type\" : \"\"\n" +
          "          },\n" +
          "          \"transformation\" : \"5203\"\n" +
          "        } ],\n" +
          "        \"id\" : \"5203\"\n" +
          "      },\n" +
          "      \"allowCondReq\" : \"false\",\n" +
          "      \"expectedSchema\" : \"\",\n" +
          "      \"outputSchema\" : \"\",\n" +
          "      \"passiveMode\" : \"false\",\n" +
          "      \"recurse\" : \"false\",\n" +
          "      \"splitAt\" : \"1\",\n" +
          "      \"splitSize\" : \"1000\",\n" +
          "      \"url\" : \"http://localhost:8080/test/oai_dc.xml\"\n" +
          "    },\n" +
          "    \"id\" : \"9999\"\n" +
          "  }\n" +
          "}";
  }

  public static String xmlSampleHarvestable() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
          "<harvestable uri=\"http://localhost:8080/harvester/records/harvestables/2005\">\n" +
          "    <oaiPmh>\n" +
          "        <allowErrors>false</allowErrors>\n" +
          "        <cacheEnabled>false</cacheEnabled>\n" +
          "        <constantFields></constantFields>\n" +
          "        <contactNotes></contactNotes>\n" +
          "        <currentStatus>NEW</currentStatus>\n" +
          "        <description></description>\n" +
          "        <diskRun>false</diskRun>\n" +
          "        <enabled>false</enabled>\n" +
          "        <harvestImmediately>false</harvestImmediately>\n" +
          "        <id>2005</id>\n" +
          "        <initiallyHarvested>2019-11-21T13:01:05Z</initiallyHarvested>\n" +
          "        <json>{&#xD;\n" +
          " \"folioAuthPath\": \"bl-users/login\",&#xD;\n" +
          " \"folioTenant\": \"diku\",&#xD;\n" +
          " \"folioUsername\": \"diku_admin\",&#xD;\n" +
          " \"folioPassword\": \"admin\",&#xD;\n" +
          " \"instanceStoragePath\": \"instance-storage-match/instances\",&#xD;\n" +
          " \"holdingsStoragePath\": \"holdings-storage/holdings\",&#xD;\n" +
          " \"itemStoragePath\": \"item-storage/items\"&#xD;\n" +
          "}</json>\n" +
          "        <lastUpdated>2019-01-01T19:10:04Z</lastUpdated>\n" +
          "        <laxParsing>false</laxParsing>\n" +
          "        <logLevel>INFO</logLevel>\n" +
          "        <mailAddress></mailAddress>\n" +
          "        <mailLevel>WARN</mailLevel>\n" +
          "        <managedBy></managedBy>\n" +
          "        <name>SI, Millersville, physicals 6</name>\n" +
          "        <openAccess>false</openAccess>\n" +
          "        <overwrite>false</overwrite>\n" +
          "        <retryCount>2</retryCount>\n" +
          "        <retryWait>60</retryWait>\n" +
          "        <scheduleString>10 10 10 6 *</scheduleString>\n" +
          "        <serviceProvider>Millersville</serviceProvider>\n" +
          "        <storage xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"inventoryStorageEntity\">\n" +
          "            <bulkSize>1000</bulkSize>\n" +
          "            <currentStatus>TODO</currentStatus>\n" +
          "            <description>FOLIO</description>\n" +
          "            <enabled>true</enabled>\n" +
          "            <id>204</id>\n" +
          "            <idAsString>204</idAsString>\n" +
          "            <name>FOLIO @ localhost</name>\n" +
          "            <retryCount>2</retryCount>\n" +
          "            <retryWait>60</retryWait>\n" +
          "            <timeout>60</timeout>\n" +
          "            <url>http://10.0.2.2:9130/</url>\n" +
          "        </storage>\n" +
          "        <storeOriginal>false</storeOriginal>\n" +
          "        <technicalNotes></technicalNotes>\n" +
          "        <timeout>300</timeout>\n" +
          "        <transformation xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"basicTransformation\">\n" +
          "            <description>MARC21 to FOLIO Inventory, Temple</description>\n" +
          "            <enabled>true</enabled>\n" +
          "            <name>MARC21 to FOLIO Inventory, Temple</name>\n" +
          "            <parallel>false</parallel>\n" +
          "            <stepAssociations>\n" +
          "                <id>5002</id>\n" +
          "                <position>1</position>\n" +
          "                <step xsi:type=\"xmlTransformationStep\">\n" +
          "                    <description>MARC21 XML to FOLIO Instance XML</description>\n" +
          "                    <inputFormat>XML</inputFormat>\n" +
          "                    <name>MARC21 to Instance XML</name>\n" +
          "                    <outputFormat>XML</outputFormat>\n" +
          "                    <script>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;&#xD;\n" +
          "&lt;xsl:stylesheet&#xD;\n" +
          "    version=\"1.0\"&#xD;\n" +
          "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"&#xD;\n" +
          "    xmlns:marc=\"http://www.loc.gov/MARC21/slim\"&#xD;\n" +
          "    xmlns:oai20=\"http://www.openarchives.org/OAI/2.0/\"&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:import href=\"map-relator-to-contributor-type.xsl\"/&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:output indent=\"yes\" method=\"xml\" version=\"1.0\" encoding=\"UTF-8\"/&gt;&#xD;\n" +
          "&#xD;\n" +
          "&lt;!-- Extract metadata from MARC21/USMARC&#xD;\n" +
          "      http://www.loc.gov/marc/bibliographic/ecbdhome.html&#xD;\n" +
          "--&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"/\"&gt;&#xD;\n" +
          "    &lt;collection&gt;&#xD;\n" +
          "      &lt;xsl:apply-templates /&gt;&#xD;\n" +
          "    &lt;/collection&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"//oai20:header[@status='deleted']\"&gt;&#xD;\n" +
          "    &lt;record status=\"deleted\"&gt;&#xD;\n" +
          "      &lt;status&gt;deleted&lt;/status&gt;&#xD;\n" +
          "      &lt;identifier&gt;&lt;xsl:value-of select=\"oai20:identifier\"/&gt;&lt;/identifier&gt;&#xD;\n" +
          "      &lt;identifierTypeIdHere/&gt;&#xD;\n" +
          "      &lt;permanentLocationIdHere/&gt;&#xD;\n" +
          "    &lt;/record&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"//marc:record\"&gt;&#xD;\n" +
          "&#xD;\n" +
          "    &lt;record&gt;&#xD;\n" +
          "      &lt;source&gt;MARC&lt;/source&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Instance type ID (resource type) --&gt;&#xD;\n" +
          "      &lt;instanceTypeId&gt;&#xD;\n" +
          "        &lt;!-- UUIDs for resource types --&gt;&#xD;\n" +
          "        &lt;xsl:choose&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='a'\"&gt;6312d172-f0cf-40f6-b27d-9fa8feaf332f&lt;/xsl:when&gt; &lt;!-- language material : text --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='c'\"&gt;497b5090-3da2-486c-b57f-de5bb3c2e26d&lt;/xsl:when&gt; &lt;!-- notated music : notated music --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='d'\"&gt;497b5090-3da2-486c-b57f-de5bb3c2e26d&lt;/xsl:when&gt; &lt;!-- manuscript notated music : notated music -&gt; notated music --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='e'\"&gt;526aa04d-9289-4511-8866-349299592c18&lt;/xsl:when&gt; &lt;!-- cartographic material : cartographic image --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='f'\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- other --&gt; &lt;!-- manuscript cartographic material : ? --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='g'\"&gt;535e3160-763a-42f9-b0c0-d8ed7df6e2a2&lt;/xsl:when&gt; &lt;!-- projected image : still image --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='i'\"&gt;9bce18bd-45bf-4949-8fa8-63163e4b7d7f&lt;/xsl:when&gt; &lt;!-- nonmusical sound recording : sounds --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='j'\"&gt;3be24c14-3551-4180-9292-26a786649c8b&lt;/xsl:when&gt; &lt;!-- musical sound recording : performed music --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='k'\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- other --&gt; &lt;!-- two-dimensional nonprojectable graphic : ?--&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='m'\"&gt;df5dddff-9c30-4507-8b82-119ff972d4d7&lt;/xsl:when&gt; &lt;!-- computer file : computer dataset --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='o'\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- kit : other --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='p'\"&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:when&gt; &lt;!-- mixed material : other --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='r'\"&gt;c1e95c2b-4efc-48cf-9e71-edb622cf0c22&lt;/xsl:when&gt; &lt;!-- three-dimensional artifact or naturally occurring object : three-dimensional form --&gt;&#xD;\n" +
          "          &lt;xsl:when test=\"substring(marc:leader,7,1)='t'\"&gt;6312d172-f0cf-40f6-b27d-9fa8feaf332f&lt;/xsl:when&gt; &lt;!-- manuscript language material : text --&gt;&#xD;\n" +
          "          &lt;xsl:otherwise&gt;a2c91e87-6bab-44d6-8adb-1fd02481fc4f&lt;/xsl:otherwise&gt;                             &lt;!--  : other --&gt;&#xD;\n" +
          "        &lt;/xsl:choose&gt;&#xD;\n" +
          "      &lt;/instanceTypeId&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Identifiers --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='010' or @tag='020' or @tag='022' or @tag='024' or @tag='028' or @tag='035' or @tag='074']&#xD;\n" +
          "                   or marc:controlfield[@tag='001']\"&gt;&#xD;\n" +
          "        &lt;identifiers&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "          &lt;xsl:for-each select=\"marc:controlfield[@tag='001']\"&gt;&#xD;\n" +
          "            &lt;i&gt;&#xD;\n" +
          "              &lt;value&gt;&lt;xsl:value-of select=\".\"/&gt;&lt;/value&gt;&#xD;\n" +
          "              &lt;!-- A subsequent library specific transformation (style sheet)&#xD;\n" +
          "                   must replace this tag with the actual identifierTypeId for&#xD;\n" +
          "                   the record identifer type of the given library --&gt;&#xD;\n" +
          "              &lt;identifierTypeIdHere/&gt;&#xD;\n" +
          "            &lt;/i&gt;&#xD;\n" +
          "          &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;xsl:for-each select=\"marc:datafield[@tag='001' or @tag='010' or @tag='020' or @tag='022' or @tag='024' or @tag='028' or @tag='035' or @tag='074']\"&gt;&#xD;\n" +
          "            &lt;i&gt;&#xD;\n" +
          "              &lt;xsl:choose&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='010'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;c858e4f2-2b6b-4385-842b-60732ee14abb&lt;/identifierTypeId&gt; &lt;!-- LCCN --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='020'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;8261054f-be78-422d-bd51-4ed9f33c3422&lt;/identifierTypeId&gt; &lt;!-- ISBN --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='022'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;913300b2-03ed-469a-8179-c1092c991227&lt;/identifierTypeId&gt; &lt;!-- ISSN --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='024'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;2e8b3b6c-0e7d-4e48-bca2-b0b23b376af5&lt;/identifierTypeId&gt; &lt;!-- Other standard identifier --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='028'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;b5d8cdc4-9441-487c-90cf-0c7ec97728eb&lt;/identifierTypeId&gt; &lt;!-- Publisher number --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='035'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;7e591197-f335-4afb-bc6d-a6d76ca3bace&lt;/identifierTypeId&gt; &lt;!-- System control number --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;xsl:when test=\"current()[@tag='074'] and marc:subfield[@code='a']\"&gt;&#xD;\n" +
          "                  &lt;value&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/value&gt;&#xD;\n" +
          "                  &lt;identifierTypeId&gt;351ebc1c-3aae-4825-8765-c6d50dbf011f&lt;/identifierTypeId&gt; &lt;!-- GPO item number --&gt;&#xD;\n" +
          "                &lt;/xsl:when&gt;&#xD;\n" +
          "              &lt;/xsl:choose&gt;&#xD;\n" +
          "            &lt;/i&gt;&#xD;\n" +
          "          &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/identifiers&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Classifications --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='050' or @tag='060' or @tag='080' or @tag='082' or @tag='086' or @tag='090']\"&gt;&#xD;\n" +
          "        &lt;classifications&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "            &lt;xsl:for-each select=\"marc:datafield[@tag='050' or @tag='060' or @tag='080' or @tag='082' or @tag='086' or @tag='090']\"&gt;&#xD;\n" +
          "              &lt;i&gt;&#xD;\n" +
          "                &lt;xsl:choose&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"current()[@tag='050']\"&gt;&#xD;\n" +
          "                    &lt;classificationNumber&gt;&#xD;\n" +
          "                      &lt;xsl:for-each select=\"marc:subfield[@code='a' or @code='b']\"&gt;&#xD;\n" +
          "                        &lt;xsl:if test=\"position() &gt; 1\"&gt;&#xD;\n" +
          "                        &lt;xsl:text&gt;; &lt;/xsl:text&gt;&#xD;\n" +
          "                      &lt;/xsl:if&gt;&#xD;\n" +
          "                      &lt;xsl:value-of select=\".\"/&gt;&#xD;\n" +
          "                      &lt;/xsl:for-each&gt;&#xD;\n" +
          "                    &lt;/classificationNumber&gt;&#xD;\n" +
          "                    &lt;classificationTypeId&gt;ce176ace-a53e-4b4d-aa89-725ed7b2edac&lt;/classificationTypeId&gt; &lt;!-- LC, Library of Congress --&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"current()[@tag='082']\"&gt;&#xD;\n" +
          "                    &lt;classificationNumber&gt;&#xD;\n" +
          "                      &lt;xsl:for-each select=\"marc:subfield[@code='a' or @code='b']\"&gt;&#xD;\n" +
          "                        &lt;xsl:if test=\"position() &gt; 1\"&gt;&#xD;\n" +
          "                        &lt;xsl:text&gt;; &lt;/xsl:text&gt;&#xD;\n" +
          "                      &lt;/xsl:if&gt;&#xD;\n" +
          "                      &lt;xsl:value-of select=\".\"/&gt;&#xD;\n" +
          "                      &lt;/xsl:for-each&gt;&#xD;\n" +
          "                    &lt;/classificationNumber&gt;&#xD;\n" +
          "                    &lt;classificationTypeId&gt;42471af9-7d25-4f3a-bf78-60d29dcf463b&lt;/classificationTypeId&gt; &lt;!-- Dewey --&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"current()[@tag='086']\"&gt;&#xD;\n" +
          "                    &lt;classificationNumber&gt;&#xD;\n" +
          "                      &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                    &lt;/classificationNumber&gt;&#xD;\n" +
          "                    &lt;classificationTypeId&gt;9075b5f8-7d97-49e1-a431-73fdd468d476&lt;/classificationTypeId&gt; &lt;!-- SUDOC --&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                &lt;/xsl:choose&gt;&#xD;\n" +
          "              &lt;/i&gt;&#xD;\n" +
          "            &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/classifications&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- title --&gt;&#xD;\n" +
          "      &lt;xsl:for-each select=\"marc:datafield[@tag='245']\"&gt;&#xD;\n" +
          "        &lt;title&gt;&#xD;\n" +
          "          &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "            &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='a']\" /&gt;&#xD;\n" +
          "            &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "          &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;xsl:if test=\"marc:subfield[@code='b']\"&gt;&#xD;\n" +
          "           &lt;xsl:text&gt; : &lt;/xsl:text&gt;&#xD;\n" +
          "            &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='b']\" /&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "            &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;/xsl:if&gt;&#xD;\n" +
          "          &lt;xsl:if test=\"marc:subfield[@code='h']\"&gt;&#xD;\n" +
          "            &lt;xsl:text&gt; &lt;/xsl:text&gt;&#xD;\n" +
          "            &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='h']\" /&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "            &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;/xsl:if&gt;&#xD;\n" +
          "        &lt;/title&gt;&#xD;\n" +
          "      &lt;/xsl:for-each&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;matchKey&gt;&#xD;\n" +
          "        &lt;xsl:for-each select=\"marc:datafield[@tag='245']\"&gt;&#xD;\n" +
          "          &lt;title&gt;&#xD;\n" +
          "            &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='a']\" /&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "            &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;/title&gt;&#xD;\n" +
          "          &lt;remainder-of-title&gt;&#xD;\n" +
          "           &lt;xsl:text&gt; : &lt;/xsl:text&gt;&#xD;\n" +
          "            &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='b']\" /&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "            &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;/remainder-of-title&gt;&#xD;\n" +
          "          &lt;medium&gt;&#xD;\n" +
          "            &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='h']\" /&gt;&#xD;\n" +
          "              &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "            &lt;/xsl:call-template&gt;&#xD;\n" +
          "          &lt;/medium&gt;&#xD;\n" +
          "          &lt;!-- Only fields that are actually included in&#xD;\n" +
          "               the instance somewhere - for example in 'title' -&#xD;\n" +
          "               should be included as 'matchKey' elements lest&#xD;\n" +
          "               the instance \"magically\" splits on \"invisible\"&#xD;\n" +
          "               properties.&#xD;\n" +
          "          &lt;name-of-part-section-of-work&gt;&#xD;\n" +
          "            &lt;xsl:value-of select=\"marc:subfield[@code='p']\" /&gt;&#xD;\n" +
          "          &lt;/name-of-part-section-of-work&gt;&#xD;\n" +
          "          &lt;number-of-part-section-of-work&gt;&#xD;\n" +
          "            &lt;xsl:value-of select=\"marc:subfield[@code='n']\" /&gt;&#xD;\n" +
          "          &lt;/number-of-part-section-of-work&gt;&#xD;\n" +
          "          &lt;inclusive-dates&gt;&#xD;\n" +
          "            &lt;xsl:value-of select=\"marc:subfield[@code='f']\" /&gt;&#xD;\n" +
          "          &lt;/inclusive-dates&gt; --&gt;&#xD;\n" +
          "        &lt;/xsl:for-each&gt;&#xD;\n" +
          "      &lt;/matchKey&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Contributors --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='100' or @tag='110' or @tag='111' or @tag='700' or @tag='710' or @tag='711']\"&gt;&#xD;\n" +
          "        &lt;contributors&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "            &lt;xsl:for-each select=\"marc:datafield[@tag='100' or @tag='110' or @tag='111' or @tag='700' or @tag='710' or @tag='711']\"&gt;&#xD;\n" +
          "              &lt;i&gt;&#xD;\n" +
          "                &lt;name&gt;&#xD;\n" +
          "                &lt;xsl:for-each select=\"marc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='f' or @code='g' or @code='j' or @code='k' or @code='l' or @code='n' or @code='p' or @code='q' or @code='t' or @code='u']\"&gt;&#xD;\n" +
          "                  &lt;xsl:if test=\"position() &gt; 1\"&gt;&#xD;\n" +
          "                    &lt;xsl:text&gt;, &lt;/xsl:text&gt;&#xD;\n" +
          "                  &lt;/xsl:if&gt;&#xD;\n" +
          "                  &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "                    &lt;xsl:with-param  name=\"input\" select=\".\" /&gt;&#xD;\n" +
          "                    &lt;xsl:with-param  name=\"characters\"&gt;,-.&lt;/xsl:with-param&gt;&#xD;\n" +
          "                  &lt;/xsl:call-template&gt;&#xD;\n" +
          "                &lt;/xsl:for-each&gt;&#xD;\n" +
          "                &lt;/name&gt;&#xD;\n" +
          "                &lt;xsl:choose&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"@tag='100' or @tag='700'\"&gt;&#xD;\n" +
          "                    &lt;contributorNameTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe2a&lt;/contributorNameTypeId&gt; &lt;!-- personal name --&gt;&#xD;\n" +
          "                    &lt;xsl:if test=\"@tag='100'\"&gt;&#xD;\n" +
          "                      &lt;primary&gt;true&lt;/primary&gt;&#xD;\n" +
          "                    &lt;/xsl:if&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"@tag='110' or @tag='710'\"&gt;&#xD;\n" +
          "                    &lt;contributorNameTypeId&gt;2e48e713-17f3-4c13-a9f8-23845bb210aa&lt;/contributorNameTypeId&gt; &lt;!-- corporate name --&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                  &lt;xsl:when test=\"@tag='111' or @tage='711'\"&gt;&#xD;\n" +
          "                    &lt;contributorNameTypeId&gt;e8b311a6-3b21-43f2-a269-dd9310cb2d0a&lt;/contributorNameTypeId&gt; &lt;!-- meeting name --&gt;&#xD;\n" +
          "                  &lt;/xsl:when&gt;&#xD;\n" +
          "                  &lt;xsl:otherwise&gt;&#xD;\n" +
          "                    &lt;contributorNameTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe2a&lt;/contributorNameTypeId&gt; &lt;!-- personal name --&gt;&#xD;\n" +
          "                  &lt;/xsl:otherwise&gt;&#xD;\n" +
          "                &lt;/xsl:choose&gt;&#xD;\n" +
          "                &lt;xsl:if test=\"marc:subfield[@code='e' or @code='4']\"&gt;&#xD;\n" +
          "                  &lt;contributorTypeId&gt;&#xD;\n" +
          "                    &lt;xsl:call-template name=\"map-relator\"/&gt;&#xD;\n" +
          "                  &lt;/contributorTypeId&gt;&#xD;\n" +
          "                &lt;/xsl:if&gt;&#xD;\n" +
          "              &lt;/i&gt;&#xD;\n" +
          "            &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/contributors&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Editions --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='250']\"&gt;&#xD;\n" +
          "        &lt;editions&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "          &lt;xsl:for-each select=\"marc:datafield[@tag='250']\"&gt;&#xD;\n" +
          "            &lt;i&gt;&#xD;\n" +
          "              &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "              &lt;xsl:if test=\"marc:subfield[@code='b']\"&gt;; &lt;xsl:value-of select=\"marc:subfield[@code='b']\"/&gt;&lt;/xsl:if&gt;&#xD;\n" +
          "            &lt;/i&gt;&#xD;\n" +
          "          &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/editions&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Publication --&gt;&#xD;\n" +
          "      &lt;xsl:choose&gt;&#xD;\n" +
          "        &lt;xsl:when test=\"marc:datafield[@tag='260' or @tag='264']\"&gt;&#xD;\n" +
          "          &lt;publication&gt;&#xD;\n" +
          "            &lt;arr&gt;&#xD;\n" +
          "              &lt;xsl:for-each select=\"marc:datafield[@tag='260' or @tag='264']\"&gt;&#xD;\n" +
          "                &lt;i&gt;&#xD;\n" +
          "                  &lt;publisher&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='b']\"/&gt;&#xD;\n" +
          "                  &lt;/publisher&gt;&#xD;\n" +
          "                  &lt;place&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                  &lt;/place&gt;&#xD;\n" +
          "                  &lt;dateOfPublication&gt;&#xD;\n" +
          "                    &lt;xsl:value-of select=\"marc:subfield[@code='c']\"/&gt;&#xD;\n" +
          "                  &lt;/dateOfPublication&gt;&#xD;\n" +
          "                &lt;/i&gt;&#xD;\n" +
          "              &lt;/xsl:for-each&gt;&#xD;\n" +
          "            &lt;/arr&gt;&#xD;\n" +
          "          &lt;/publication&gt;&#xD;\n" +
          "        &lt;/xsl:when&gt;&#xD;\n" +
          "        &lt;xsl:otherwise&gt;&#xD;\n" +
          "          &lt;publication&gt;&#xD;\n" +
          "            &lt;arr&gt;&#xD;\n" +
          "              &lt;i&gt;&#xD;\n" +
          "                &lt;dateOfPublication&gt;&#xD;\n" +
          "                  &lt;xsl:value-of select=\"substring(marc:controlfield[@tag='008'],8,4)\"/&gt;&#xD;\n" +
          "                &lt;/dateOfPublication&gt;&#xD;\n" +
          "              &lt;/i&gt;&#xD;\n" +
          "            &lt;/arr&gt;&#xD;\n" +
          "          &lt;/publication&gt;&#xD;\n" +
          "        &lt;/xsl:otherwise&gt;&#xD;\n" +
          "      &lt;/xsl:choose&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- physicalDescriptions --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='300']\"&gt;&#xD;\n" +
          "        &lt;physicalDescriptions&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "            &lt;xsl:for-each select=\"marc:datafield[@tag='300']\"&gt;&#xD;\n" +
          "              &lt;i&gt;&#xD;\n" +
          "                &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "                  &lt;xsl:with-param  name=\"input\" select=\"marc:subfield[@code='a']\" /&gt;&#xD;\n" +
          "                  &lt;xsl:with-param  name=\"characters\"&gt;,-./ :;&lt;/xsl:with-param&gt;&#xD;\n" +
          "                &lt;/xsl:call-template&gt;&#xD;\n" +
          "              &lt;/i&gt;&#xD;\n" +
          "            &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/physicalDescriptions&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- Subjects --&gt;&#xD;\n" +
          "      &lt;xsl:if test=\"marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']\"&gt;&#xD;\n" +
          "        &lt;subjects&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "          &lt;xsl:for-each select=\"marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']\"&gt;&#xD;\n" +
          "            &lt;i&gt;&#xD;\n" +
          "            &lt;xsl:for-each select=\"marc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='f' or @code='g' or @code='j' or @code='k' or @code='l' or @code='n' or @code='p' or @code='q' or @code='t' or @code='u' or @code='v' or @code='z']\"&gt;&#xD;\n" +
          "              &lt;xsl:if test=\"position() &gt; 1\"&gt;&#xD;\n" +
          "                &lt;xsl:text&gt;--&lt;/xsl:text&gt;&#xD;\n" +
          "              &lt;/xsl:if&gt;&#xD;\n" +
          "              &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "                  &lt;xsl:with-param  name=\"input\" select=\".\" /&gt;&#xD;\n" +
          "                  &lt;xsl:with-param  name=\"characters\"&gt;,-.&lt;/xsl:with-param&gt;&#xD;\n" +
          "                &lt;/xsl:call-template&gt;&#xD;\n" +
          "            &lt;/xsl:for-each&gt;&#xD;\n" +
          "            &lt;/i&gt;&#xD;\n" +
          "          &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/subjects&gt;&#xD;\n" +
          "      &lt;/xsl:if&gt;&#xD;\n" +
          "&#xD;\n" +
          "      &lt;!-- holdings and items, MARC fields to be processed by subsequent transformations or be removed before FOLIO --&gt;&#xD;\n" +
          "      &lt;passthrough&gt;&#xD;\n" +
          "        &lt;xsl:for-each select=\"marc:datafield[@tag='852' or @tag='900' or @tag='954' or @tag='995']\"&gt;&#xD;\n" +
          "          &lt;xsl:copy-of select=\".\"/&gt;&#xD;\n" +
          "        &lt;/xsl:for-each&gt;&#xD;\n" +
          "      &lt;/passthrough&gt;&#xD;\n" +
          "    &lt;/record&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"text()\"/&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "    &lt;xsl:param name=\"input\" /&gt;&#xD;\n" +
          "    &lt;xsl:param name=\"characters\"/&gt;&#xD;\n" +
          "    &lt;xsl:variable name=\"lastcharacter\" select=\"substring($input,string-length($input))\" /&gt;&#xD;\n" +
          "    &lt;xsl:choose&gt;&#xD;\n" +
          "      &lt;xsl:when test=\"$characters and $lastcharacter and contains($characters, $lastcharacter)\"&gt;&#xD;\n" +
          "        &lt;xsl:call-template name=\"remove-characters-last\"&gt;&#xD;\n" +
          "          &lt;xsl:with-param  name=\"input\" select=\"substring($input,1, string-length($input)-1)\" /&gt;&#xD;\n" +
          "          &lt;xsl:with-param  name=\"characters\" select=\"$characters\" /&gt;&#xD;\n" +
          "        &lt;/xsl:call-template&gt;&#xD;\n" +
          "      &lt;/xsl:when&gt;&#xD;\n" +
          "      &lt;xsl:otherwise&gt;&#xD;\n" +
          "        &lt;xsl:value-of select=\"$input\"/&gt;&#xD;\n" +
          "      &lt;/xsl:otherwise&gt;&#xD;\n" +
          "    &lt;/xsl:choose&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "&lt;/xsl:stylesheet&gt;&#xD;\n" +
          "&#xD;\n" +
          "</script>\n" +
          "                    <id>4004</id>\n" +
          "                </step>\n" +
          "                <transformation>3004</transformation>\n" +
          "            </stepAssociations>\n" +
          "            <stepAssociations>\n" +
          "                <id>5005</id>\n" +
          "                <position>2</position>\n" +
          "                <step xsi:type=\"xmlTransformationStep\">\n" +
          "                    <description>Holdings and Items, Temple</description>\n" +
          "                    <inputFormat>XML</inputFormat>\n" +
          "                    <name>Holdings and Items, Temple</name>\n" +
          "                    <outputFormat>XML</outputFormat>\n" +
          "                    <script>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;&#xD;\n" +
          "&lt;xsl:stylesheet&#xD;\n" +
          "  version=\"1.0\"&#xD;\n" +
          "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"&#xD;\n" +
          "  xmlns:marc=\"http://www.loc.gov/MARC21/slim\"&#xD;\n" +
          "  &gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:strip-space elements=\"*\"/&gt;&#xD;\n" +
          "  &lt;xsl:output indent=\"yes\" method=\"xml\" version=\"1.0\" encoding=\"UTF-8\"/&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"@* | node()\"&gt;&#xD;\n" +
          "    &lt;xsl:copy&gt;&#xD;\n" +
          "      &lt;xsl:apply-templates select=\"@* | node()\"/&gt;&#xD;\n" +
          "    &lt;/xsl:copy&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"passthrough\"&gt;&#xD;\n" +
          "    &lt;xsl:choose&gt;&#xD;\n" +
          "      &lt;xsl:when test=\"marc:datafield[@tag='852']\"&gt;&#xD;\n" +
          "        &lt;holdingsRecords&gt;&#xD;\n" +
          "           &lt;arr&gt;&#xD;\n" +
          "             &lt;xsl:for-each select=\"marc:datafield[@tag='852']\"&gt;&#xD;\n" +
          "               &lt;xsl:variable name=\"holdingsId\" select=\"marc:subfield[@code='8']\"/&gt;&#xD;\n" +
          "               &lt;xsl:variable name=\"holdingPos\" select=\"position()\"/&gt;&#xD;\n" +
          "               &lt;i&gt;&#xD;\n" +
          "                 &lt;formerIds&gt;&#xD;\n" +
          "                   &lt;arr&gt;&#xD;\n" +
          "                     &lt;i&gt;&#xD;\n" +
          "                       &lt;xsl:value-of select=\"marc:subfield[@code='8']\"/&gt;&#xD;\n" +
          "                     &lt;/i&gt;&#xD;\n" +
          "                   &lt;/arr&gt;&#xD;\n" +
          "                 &lt;/formerIds&gt;&#xD;\n" +
          "                 &lt;permanentLocationIdHere&gt;&lt;xsl:value-of select=\"marc:subfield[@code='b']\"/&gt;&lt;/permanentLocationIdHere&gt;&#xD;\n" +
          "                 &lt;callNumber&gt;&#xD;\n" +
          "                   &lt;xsl:for-each select=\"marc:subfield[@code='h']\"&gt;&#xD;\n" +
          "                     &lt;xsl:if test=\"position() &gt; 1\"&gt;&#xD;\n" +
          "                       &lt;xsl:text&gt; &lt;/xsl:text&gt;&#xD;\n" +
          "                     &lt;/xsl:if&gt;&#xD;\n" +
          "                     &lt;xsl:value-of select=\".\"/&gt;&#xD;\n" +
          "                   &lt;/xsl:for-each&gt;&#xD;\n" +
          "                 &lt;/callNumber&gt;&#xD;\n" +
          "                 &lt;items&gt;&#xD;\n" +
          "                   &lt;arr&gt;&#xD;\n" +
          "                     &lt;xsl:for-each select=\"../marc:datafield[@tag='954']\"&gt;&#xD;\n" +
          "                        &lt;xsl:if test=\"position() = $holdingPos\"&gt;&#xD;\n" +
          "                        &lt;i&gt;&#xD;\n" +
          "                          &lt;itemIdentifier&gt;&#xD;\n" +
          "                            &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                          &lt;/itemIdentifier&gt;&#xD;\n" +
          "                          &lt;barcode&gt;&#xD;\n" +
          "                            &lt;xsl:value-of select=\"marc:subfield[@code='b']\"/&gt;&#xD;\n" +
          "                          &lt;/barcode&gt;&#xD;\n" +
          "                          &lt;permanentLoanTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe27&lt;/permanentLoanTypeId&gt;                    &lt;!-- Can circulate --&gt;&#xD;\n" +
          "                          &lt;materialTypeId&gt;&#xD;\n" +
          "                            &lt;xsl:choose&gt;&#xD;\n" +
          "                              &lt;xsl:when test=\"marc:subfield[@code='d']='BOOK'\"&gt;1a54b431-2e4f-452d-9cae-9cee66c9a892&lt;/xsl:when&gt; &lt;!-- Book --&gt;&#xD;\n" +
          "                              &lt;xsl:otherwise&gt;71fbd940-1027-40a6-8a48-49b44d795e46&lt;/xsl:otherwise&gt;                              &lt;!-- Unspecified --&gt;&#xD;\n" +
          "                            &lt;/xsl:choose&gt;&#xD;\n" +
          "                          &lt;/materialTypeId&gt;&#xD;\n" +
          "                          &lt;status&gt;&#xD;\n" +
          "                            &lt;name&gt;Unknown&lt;/name&gt;&#xD;\n" +
          "                          &lt;/status&gt;&#xD;\n" +
          "                        &lt;/i&gt;&#xD;\n" +
          "                        &lt;/xsl:if&gt;&#xD;\n" +
          "                     &lt;/xsl:for-each&gt;&#xD;\n" +
          "                   &lt;/arr&gt;&#xD;\n" +
          "                 &lt;/items&gt;&#xD;\n" +
          "               &lt;/i&gt;&#xD;\n" +
          "             &lt;/xsl:for-each&gt;&#xD;\n" +
          "           &lt;/arr&gt;&#xD;\n" +
          "        &lt;/holdingsRecords&gt;&#xD;\n" +
          "      &lt;/xsl:when&gt;&#xD;\n" +
          "      &lt;xsl:when test=\"marc:datafield[@tag='954']\"&gt;&#xD;\n" +
          "        &lt;holdingsRecords&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "            &lt;xsl:for-each select=\"marc:datafield[@tag='954']\"&gt;&#xD;\n" +
          "              &lt;i&gt;&#xD;\n" +
          "                &lt;!-- No \"852\" tag (no holdings record), use ID of item as holdingsRecord ID as well --&gt;&#xD;\n" +
          "                &lt;formerIds&gt;&#xD;\n" +
          "                  &lt;arr&gt;&#xD;\n" +
          "                    &lt;i&gt;&#xD;\n" +
          "                      &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;                      &#xD;\n" +
          "                    &lt;/i&gt;&#xD;\n" +
          "                  &lt;/arr&gt;&#xD;\n" +
          "                &lt;/formerIds&gt;&#xD;\n" +
          "                &lt;permanentLocationIdHere /&gt;&#xD;\n" +
          "                &lt;items&gt;&#xD;\n" +
          "                  &lt;arr&gt;&#xD;\n" +
          "                    &lt;i&gt;&#xD;\n" +
          "                      &lt;itemIdentifier&gt;&#xD;\n" +
          "                        &lt;xsl:value-of select=\"marc:subfield[@code='a']\"/&gt;&#xD;\n" +
          "                      &lt;/itemIdentifier&gt;&#xD;\n" +
          "                      &lt;barcode&gt;&#xD;\n" +
          "                        &lt;xsl:value-of select=\"marc:subfield[@code='b']\"/&gt;&#xD;\n" +
          "                      &lt;/barcode&gt;&#xD;\n" +
          "                      &lt;permanentLoanTypeId&gt;2b94c631-fca9-4892-a730-03ee529ffe27&lt;/permanentLoanTypeId&gt;                      &lt;!-- Can circulate --&gt;&#xD;\n" +
          "                      &lt;materialTypeId&gt;&#xD;\n" +
          "                        &lt;xsl:choose&gt;&#xD;\n" +
          "                          &lt;xsl:when test=\"marc:subfield[@code='d']='BOOK'\"&gt;1a54b431-2e4f-452d-9cae-9cee66c9a892&lt;/xsl:when&gt; &lt;!-- Book --&gt;&#xD;\n" +
          "                          &lt;xsl:otherwise&gt;71fbd940-1027-40a6-8a48-49b44d795e46&lt;/xsl:otherwise&gt;                              &lt;!-- Unspecified --&gt;&#xD;\n" +
          "                        &lt;/xsl:choose&gt;&#xD;\n" +
          "                      &lt;/materialTypeId&gt;&#xD;\n" +
          "                      &lt;status&gt;&#xD;\n" +
          "                        &lt;name&gt;Unknown&lt;/name&gt;&#xD;\n" +
          "                      &lt;/status&gt;&#xD;\n" +
          "                    &lt;/i&gt;&#xD;\n" +
          "                  &lt;/arr&gt;&#xD;\n" +
          "                &lt;/items&gt;&#xD;\n" +
          "              &lt;/i&gt;&#xD;\n" +
          "            &lt;/xsl:for-each&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/holdingsRecords&gt;&#xD;\n" +
          "      &lt;/xsl:when&gt;&#xD;\n" +
          "      &lt;xsl:otherwise&gt;&#xD;\n" +
          "        &lt;holdingsRecords&gt;&#xD;\n" +
          "          &lt;arr&gt;&#xD;\n" +
          "            &lt;i&gt;&#xD;\n" +
          "              &lt;permanentLocationIdHere /&gt;&#xD;\n" +
          "            &lt;/i&gt;&#xD;\n" +
          "          &lt;/arr&gt;&#xD;\n" +
          "        &lt;/holdingsRecords&gt;&#xD;\n" +
          "      &lt;/xsl:otherwise&gt;&#xD;\n" +
          "    &lt;/xsl:choose&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&lt;/xsl:stylesheet&gt;</script>\n" +
          "                    <id>4007</id>                    \n" +
          "                </step>\n" +
          "                <transformation>3004</transformation>\n" +
          "            </stepAssociations>\n" +
          "            <stepAssociations>\n" +
          "                <id>5013</id>\n" +
          "                <position>3</position>\n" +
          "                <step xsi:type=\"xmlTransformationStep\">\n" +
          "                    <description>Maps locations, record identifier type</description>\n" +
          "                    <inputFormat>XML</inputFormat>\n" +
          "                    <name>Library codes, Temple</name>\n" +
          "                    <outputFormat>XML</outputFormat>\n" +
          "                    <script>&lt;?xml version=\"1.0\" encoding=\"UTF-8\" ?&gt;&#xD;\n" +
          "&lt;xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;xsl:template match=\"@* | node()\"&gt;&#xD;\n" +
          "    &lt;xsl:copy&gt;&#xD;\n" +
          "      &lt;xsl:apply-templates select=\"@* | node()\"/&gt;&#xD;\n" +
          "    &lt;/xsl:copy&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;!-- Map legacy code for the library/institution to a FOLIO resource identifier&#xD;\n" +
          "       type UUID. Used for qualifying a local record identifier with the library&#xD;\n" +
          "       it originated from in context of a shared index setup where the Instance&#xD;\n" +
          "       represents bib records from multiple libraries.&#xD;\n" +
          "  --&gt;&#xD;\n" +
          "  &lt;xsl:template match=\"//identifierTypeIdHere\"&gt;&#xD;\n" +
          "    &lt;identifierTypeId&gt;17bb9b44-0063-44cc-8f1a-ccbb6188060b&lt;/identifierTypeId&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "  &lt;!-- Map legacy location code to a FOLIO location UUID --&gt;&#xD;\n" +
          "  &lt;xsl:template match=\"//permanentLocationIdHere\"&gt;&#xD;\n" +
          "    &lt;permanentLocationId&gt;87038e41-0990-49ea-abd9-1ad00a786e45&lt;/permanentLocationId&gt; &lt;!-- Temple --&gt;&#xD;\n" +
          "  &lt;/xsl:template&gt;&#xD;\n" +
          "&#xD;\n" +
          "&lt;/xsl:stylesheet&gt;&#xD;\n" +
          "</script>\n" +
          "                    <id>4011</id>\n" +
          "                    \n" +
          "                </step>\n" +
          "                <transformation>3004</transformation>\n" +
          "            </stepAssociations>\n" +
          "            <stepAssociations>\n" +
          "                <id>5008</id>\n" +
          "                <position>6</position>\n" +
          "                <step xsi:type=\"customTransformationStep\">\n" +
          "                    <customClass>com.indexdata.masterkey.localindices.harvest.messaging.InstanceXmlToInstanceJsonTransformerRouter</customClass>\n" +
          "                    <description>FOLIO Instance XML to JSON</description>\n" +
          "                    <enabled>true</enabled>\n" +
          "                    <inputFormat>XML</inputFormat>\n" +
          "                    <name>Instance XML to JSON</name>\n" +
          "                    <outputFormat>JSON</outputFormat>\n" +
          "                    <script></script>\n" +
          "                    <id>4003</id>\n" +
          "                    <testData></testData>\n" +
          "                    <testOutput></testOutput>\n" +
          "                    <type>custom</type>\n" +
          "                </step>\n" +
          "                <transformation>3004</transformation>\n" +
          "            </stepAssociations>\n" +
          "            <id>3004</id>\n" +
          "        </transformation>\n" +
          "        <usedBy></usedBy>\n" +
          "        <clearRtOnError>false</clearRtOnError>\n" +
          "        <dateFormat>yyyy-MM-dd'T'hh:mm:ss'Z'</dateFormat>\n" +
          "        <keepPartial>true</keepPartial>\n" +
          "        <metadataPrefix>marc21</metadataPrefix>\n" +
          "        <oaiSetName>IndexDataHoldItemPhysicalTitles</oaiSetName>\n" +
          "        <resumptionToken></resumptionToken>\n" +
          "        <url>https://na01.alma.exlibrisgroup.com/view/oai/01SSHELCO_MILLRSVL/request</url>\n" +
          "    </oaiPmh>\n" +
          "    <id>2005</id>\n" +
          "</harvestable>\n";
  }

  public static String xmlSampleHarvestables() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<harvestables count=\"2\" max=\"100\" uri=\"http://localhost:8080/harvester/records/harvestables/\" start=\"0\">\n" + "    <harvestableBrief uri=\"http://localhost:8080/harvester/records/harvestables/9998/\">\n" + "        <currentStatus>NEW</currentStatus>\n" + "        <enabled>false</enabled>\n" + "        <id>9998</id>\n" + "        <jobClass>HarvestConnectorResource</jobClass>\n" + "        <lastHarvestFinished>2017-07-21T15:51:25Z</lastHarvestFinished>\n" + "        <lastHarvestStarted>2017-07-21T15:53:56Z</lastHarvestStarted>\n" + "        <lastUpdated>2017-07-21T15:58:26Z</lastUpdated>\n" + "        <name>Harvest Job A</name>\n" + "        <nextHarvestSchedule>2020-03-19T00:00:00Z</nextHarvestSchedule>\n" +
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
  }
}
