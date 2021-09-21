package org.folio.harvesteradmin;

import org.apache.logging.log4j.Level;


public class Config
{
    private static final String HARVESTER_HOST_ENV_VAR = "harvester_host";
    private static final String HARVESTER_PORT_ENV_VAR = "harvester_port";
    private static final String SERVICE_PORT_SYS_PROP = "port";
    private static final String SERVICE_PORT_DEFAULT = "8080";
    private static final String LOG_LEVEL_SYS_PROP = "logLevel";

    public static Level logLevel;
    public static int servicePort;
    public static int harvesterPort;
    public static String harvesterHost;

    public Config()
    {
        setServiceConfig();
        setHarvesterConfig();
    }

    private void setServiceConfig()
    {
        servicePort = Integer.parseInt( System.getProperty( SERVICE_PORT_SYS_PROP, SERVICE_PORT_DEFAULT ) );
    }

    private void setHarvesterConfig()
    {
        harvesterHost = System.getenv( HARVESTER_HOST_ENV_VAR );
        harvesterPort = Integer.parseInt( System.getenv( HARVESTER_PORT_ENV_VAR ) );
    }

}
