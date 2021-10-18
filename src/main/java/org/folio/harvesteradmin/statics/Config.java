package org.folio.harvesteradmin.statics;

import org.apache.logging.log4j.Level;


public class Config
{
    private static final String HARVESTER_HOST_ENV_VAR = "harvester.host";
    private static final String HARVESTER_PORT_ENV_VAR = "harvester.port";
    private static final String HARVESTER_PROTOCOL = "harvester.protocol";
    private static final String HARVESTER_BASIC_AUTH_USERNAME = "harvester.auth.basic.username";
    private static final String HARVESTER_BASIC_AUTH_PASSWORD = "harvester.auth.basic.password";
    private static final String FILTER_BY_TENANT = "acl_filter.by_tenant";
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

    public Config()
    {
        setServiceConfig();
        setHarvesterConfig();
    }

    private void setServiceConfig()
    {
        servicePort = Integer.parseInt( System.getProperty( SERVICE_PORT_SYS_PROP, SERVICE_PORT_DEFAULT ) );
    }

    public static boolean hasHarvesterPort()
    {
        return harvesterPort > 0;
    }

    public static boolean harvesterRequiresSsl()
    {
        return harvesterProtocol != null && harvesterProtocol.equalsIgnoreCase( "https" );
    }

    public static boolean hasBasicAuthForHarvester()
    {
        return basicAuthUsername != null && basicAuthPassword != null;
    }

    private void setHarvesterConfig()
    {
        harvesterHost = System.getenv( HARVESTER_HOST_ENV_VAR );
        harvesterProtocol = System.getenv().getOrDefault( HARVESTER_PROTOCOL, "http" );
        harvesterPort = Integer.parseInt( System.getenv().getOrDefault( System.getenv().get( HARVESTER_PORT_ENV_VAR ),
                ( harvesterProtocol.equals( "http" ) ? "80" : ( harvesterProtocol.equals(
                        "https" ) ? "443" : "-1" ) ) ) );
        basicAuthUsername = System.getenv().get( HARVESTER_BASIC_AUTH_USERNAME );
        basicAuthPassword = System.getenv().get( HARVESTER_BASIC_AUTH_PASSWORD );
        filterByTenant = !System.getenv().getOrDefault( FILTER_BY_TENANT, "true" ).equalsIgnoreCase( "false" );
    }


}
