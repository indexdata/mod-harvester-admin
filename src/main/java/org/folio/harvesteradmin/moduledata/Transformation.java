package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import org.folio.harvesteradmin.moduledata.database.Tables;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transformation extends Entity {

    // Transformation record
    public record Record(UUID id, String name, boolean enabled, String description, String type) {
    }
    public Record record;

    // Data definitions
    private static final Tables TABLE = Tables.transformation;
    private static final Map<String, Field> FIELDS = new HashMap<>();
    public static final String ID = "ID", NAME = "NAME", TYPE = "TYPE", DESCRIPTION = "DESCRIPTION";
    static {
        FIELDS.put(ID,new Field("id", "id", PgColumn.Type.UUID, false, true, true));
        FIELDS.put(NAME,new Field("name", "name", PgColumn.Type.TEXT, false, true));
        FIELDS.put(DESCRIPTION, new Field("description", "description", PgColumn.Type.TEXT, true, true));
        FIELDS.put(TYPE, new Field("type", "type", PgColumn.Type.TEXT, true, true));
    }

    @Override
    public Tables table() {
        return TABLE;
    }

    @Override
    public Map<String, Field> fields() {
        return FIELDS;
    }

    public Entity fromJson(JsonObject json) {
        record = new Record(
                UUID.fromString(json.getString(jsonPropertyName(ID))),
                json.getString(jsonPropertyName(NAME)),
                true,
                json.getString(jsonPropertyName(TYPE)),
                json.getString(jsonPropertyName(DESCRIPTION)));
        return this;
    }

    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put(jsonPropertyName(ID), record.id());
        json.put(jsonPropertyName(NAME), record.name());
        json.put(jsonPropertyName(TYPE), record.type());
        json.put("enabled", record.enabled());
        json.put(jsonPropertyName(DESCRIPTION), record.description());
        return json;
    }


    @Override
    public RowMapper<Entity> getRowMapper() {
            return row -> {
                record = new Record(
                        row.getUUID(dbColumnName(ID)),
                        row.getString(dbColumnName(NAME)),
                        true,
                        row.getString(dbColumnName(TYPE)),
                        row.getString(dbColumnName(DESCRIPTION)));
                return this;
            };
    }

    @Override
    public TupleMapper<Entity> getTupleMapper() {
        return TupleMapper.mapper(
                step -> {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(dbColumnName(ID), record.id());
                    parameters.put(dbColumnName(NAME), record.name());
                    parameters.put(dbColumnName(TYPE), record.type());
                    parameters.put(dbColumnName(DESCRIPTION), record.description());
                    return parameters;
                });
    }

    @Override
    public String makeInsertTemplate(String schema) {
        return "INSERT INTO " + schema + "." + Tables.transformation
                + " ("
                + dbColumnName(ID) + ", "
                + dbColumnName(NAME) + ", "
                + dbColumnName(TYPE) + ", "
                + dbColumnName(DESCRIPTION)
                + ")"
                + " VALUES ("
                + "#{" + dbColumnName(ID) + "}, "
                + "#{" + dbColumnName(NAME) + "}, "
                + "#{" + dbColumnName(TYPE) + "}, "
                + "#{" + dbColumnName(DESCRIPTION) + "} "
                + ")";
    }

}
