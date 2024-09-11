package org.folio.harvesteradmin.legacydata.statics;

import java.lang.management.ManagementFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LegacyServiceConfig {
  private static final String HARVESTER_HOST_ENV_VAR = "harvester_host";
  private static final String HARVESTER_PORT_ENV_VAR = "harvester_port";
  private static final String HARVESTER_PROTOCOL = "harvester_protocol";
  private static final String HARVESTER_BASIC_AUTH_USERNAME = "harvester_auth_basic_username";
  private static final String HARVESTER_BASIC_AUTH_PASSWORD = "harvester_auth_basic_password";
  private static final String FILTER_BY_TENANT = "acl_filter_by_tenant";
  private static final String SERVICE_PORT_SYS_PROP = "port";
  private static final String SERVICE_PORT_DEFAULT = "8080";

  public static Level logLevel;
  public static int servicePort;
  public static int harvesterPort;
  public static String harvesterHost;
  public static String harvesterProtocol;
  public static String basicAuthUsername;
  public static String basicAuthPassword;
  public static boolean filterByTenant = true;
  private static final Logger logger = LogManager.getLogger("harvester-admin");

  /**
   * Constructor.
   */
  public LegacyServiceConfig() {
    setServiceConfig();
    if (! setHarvesterConfig()) {
      logger.error("There is a problem with the setup of Harvester: " + this);
    }
  }

  private void setServiceConfig() {
    servicePort = Integer.parseInt(System.getProperty(SERVICE_PORT_SYS_PROP, SERVICE_PORT_DEFAULT));
  }

  public static boolean hasHarvesterPort() {
    return harvesterPort > 0;
  }

  public static boolean harvesterRequiresSsl() {
    return harvesterProtocol != null && harvesterProtocol.equalsIgnoreCase("https");
  }

  public static boolean hasBasicAuthForHarvester() {
    return basicAuthUsername != null && basicAuthPassword != null;
  }

  private boolean setHarvesterConfig() {
    boolean configOk = true;
    harvesterHost = System.getenv(HARVESTER_HOST_ENV_VAR);
    if (harvesterHost == null || harvesterHost.isEmpty()) {
      logger.error("No Harvester specified in environment variables. "
          + "Environment variable 'harvester_host' is required for running "
          + "the harvester admin module.");
      configOk = false;
    }
    harvesterProtocol = System.getenv().getOrDefault(HARVESTER_PROTOCOL, "http");
    if (harvesterProtocol.equals("http")) {
      harvesterPort =
          Integer.parseInt(System.getenv().getOrDefault(HARVESTER_PORT_ENV_VAR, "80"));
    } else if (harvesterProtocol.equals("https")) {
      harvesterPort =
          Integer.parseInt(System.getenv().getOrDefault(HARVESTER_PORT_ENV_VAR, "443"));
    } else {
      logger.error(
          "Unrecognized protocol '" + harvesterProtocol + "', cannot connect to Harvester at "
              + harvesterHost + ": " + harvesterPort);
      configOk = false;
    }
    if (configOk) {
      logger.info("Attaching to Harvester at " + harvesterProtocol + "://" + harvesterHost + ":"
          + harvesterPort);
    }
    basicAuthUsername = System.getenv().get(HARVESTER_BASIC_AUTH_USERNAME);
    basicAuthPassword = System.getenv().get(HARVESTER_BASIC_AUTH_PASSWORD);
    if (hasBasicAuthForHarvester()) {
      logger.info("Using basic auth user " + basicAuthUsername);
    }
    filterByTenant =
        !System.getenv().getOrDefault(FILTER_BY_TENANT, "true")
            .equalsIgnoreCase("false");
    return configOk;
  }

  public String toString() {
    return ManagementFactory.getRuntimeMXBean().getName() + " on port " + servicePort
        + ", proxying " + harvesterProtocol + "://" + harvesterHost + ":" + harvesterPort;
  }

}
