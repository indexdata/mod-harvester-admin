package org.folio.harvesteradmin.test.sampleData;
import io.vertx.core.json.JsonObject;
import org.folio.harvesteradmin.test.SampleId;

public class Samples {

  public static final SampleId BASE_STORAGE_ID = new SampleId(9999);
  public static final SampleId BASE_TRANSFORMATION_ID = new SampleId(9999);
  public static final SampleId SAMPLE_STEP_ID = new SampleId(9999);
  public static JsonObject BASE_STORAGE_JSON = new JsonObject(
      "{\n"
          + "  \"id\" : \"" + BASE_STORAGE_ID.fullId() + "\",\n"
          + "  \"name\": \"BASE_STORAGE\",\n"
          + "  \"description\" : \"Test storage definition\",\n"
          + "  \"type\" : \"inventoryStorage\",\n"
          + "  \"url\" : \"http://10.0.2.2:9130/\",\n"
          + "  \"enabled\" : \"true\",\n"
          + "  \"json\" : {\n"
          + "    \"folioAuthPath\" : \"bl-users/login\",\n"
          + "    \"folioTenant\" : \"diku\",\n"
          + "    \"folioUsername\" : \"diku_admin\",\n"
          + "    \"folioPassword\" : \"admin\",\n"
          + "    \"inventoryUpsertPath\" : \"inventory-upsert-hrid\",\n"
          + "    \"inventoryBatchUpsertPath\" : \"inventory-batch-upsert-hrid\"\n"
          + "  }\n"
          + "}"
  );
  public static JsonObject BASE_TRANSFORMATION_JSON = new JsonObject(
      "{\n"
          + "  \"name\" : \"BASE_TRANSFORMATION\",\n"
          + "  \"id\" : \"" + BASE_STORAGE_ID.fullId() + "\",\n"
          + "  \"description\" : \"Test\",\n"
          + "  \"enabled\" : \"true\",\n"
          + "  \"type\" : \"basicTransformation\"\n"
          + "}\n"
  );
  public static JsonObject SAMPLE_STEP =
      new JsonObject(
          "{\n"
              + "  \"id\" : \"" + SAMPLE_STEP_ID + "\",   \n"
              + "  \"name\" : \"Test step\",\n"
              + "  \"description\" : \"Sample record\",\n"
              + "  \"inputFormat\" : \"XML\",\n"
              + "  \"outputFormat\" : \"XML\",\n"
              + "  \"script\" : \"\",\n"
              + "  \"testData\" : \"\",\n"
              + "  \"testOutput\" : \"\",\n"
              + "  \"type\" : \"XmlTransformStep\"\n"
              + "}\n"
      );
  public static String SAMPLE_SCRIPT =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
          + "                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" version=\"1.0\" >\n"
          + "    \n"
          + "  <xsl:output method=\"xml\" indent=\"yes\" />\n"
          + "  <xsl:template match=\"/\">\n"
          + "    <collection>\n"
          + "      <xsl:apply-templates />\n"
          + "      </collection>\n"
          + "    </xsl:template>\n"
          + "    <xsl:template match=\"feed\">\n"
          + "            <xsl:apply-templates />\n"
          + "    </xsl:template>\n"
          + "    <xsl:template match=\"doc\">\n"
          + "      <pz:record>\n"
          + " <pz:metadata type=\"id\">\n"
          + "   <xsl:value-of select=\"substring-after(url,'http://')\" />\n"
          + "    </pz:metadata>\n"
          + "  <pz:metadata type=\"title\">\n"
          + "    <xsl:value-of select=\"substring-after(title, 'Wikipedia: ')\" />\n"
          + "   </pz:metadata>\n"
          + "  <!-- <field name=\"title-abbreviated\"> <xsl:value-of select=\"substring-before"
          + "(substring-after(concat(title, \n"
          + "       ' ('), 'Wikipedia: '), ' (')\"/> </field> -->\n"
          + "  <!-- <field name=\"date\"> <xsl:value-of select=\"date\" /> </field> -->\n"
          + "  <pz:metadata type=\"description\">\n"
          + "    <xsl:value-of select=\"abstract\" />\n"
          + "    </pz:metadata>\n"
          + "  <pz:metadata type=\"electronic-url\">\n"
          + "   <xsl:value-of select=\"url\" />\n"
          + "   </pz:metadata>\n"
          + "  </pz:record>\n"
          + "      </xsl:template>\n"
          + "</xsl:stylesheet>\n";
  
}
