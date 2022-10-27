package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import java.util.UUID;

public abstract class Entity {

  JsonObject json;
  protected UUID uuid;

  public Entity(JsonObject json) {
    this.json = json;
    uuid = UUID.randomUUID();
  }

  public Entity() {
  }

  public UUID getId() {
    return uuid;
  }
}
