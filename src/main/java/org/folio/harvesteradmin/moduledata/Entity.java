package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import java.util.UUID;

public abstract class Entity {

  JsonObject json;
  UUID uuid;

  public Entity(JsonObject json) {
    this.json = json;
    uuid = UUID.randomUUID();
  }

  public UUID getId() {
    return uuid;
  }
}
