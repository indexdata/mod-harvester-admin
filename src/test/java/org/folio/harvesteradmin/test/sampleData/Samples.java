package org.folio.harvesteradmin.test.sampleData;
import io.vertx.core.json.JsonObject;

public class Samples {
  public static final int SAMPLES_ID_PREFIX = 9631;

  /**
   * Obtains an ID starting with {{SAMPLES_ID_PREFIX}}.
   * The prefix is used for cleaning up sample records when the unit test completes.
   */
  public static int sampleId(int id) {
    return (SAMPLES_ID_PREFIX * 100000) + id;
  }

  public static final int BASE_STORAGE_ID = 9999;
  public static final int BASE_TRANSFORMATION_ID = 9999;

  public static JsonObject BASE_STORAGE_JSON = new JsonObject(
      "{\n"
          + "  \"id\" : \"" + sampleId(BASE_STORAGE_ID) + "\",\n"
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
          + "  \"id\" : \"" + sampleId(BASE_STORAGE_ID) + "\",\n"
          + "  \"description\" : \"Test\",\n"
          + "  \"enabled\" : \"true\",\n"
          + "  \"type\" : \"basicTransformation\"\n"
          + "}\n"
  );


}
