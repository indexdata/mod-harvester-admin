package org.folio.harvesteradmin;

import org.apache.log4j.Level;

import java.util.Optional;


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
        setLogLevel();
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

    private void setLogLevel()
    {
        Optional<String> levelKey = System.getProperties().keySet().stream().map( key -> key.toString() ).filter(
                key -> key.equalsIgnoreCase( LOG_LEVEL_SYS_PROP ) ).findFirst();
        if ( levelKey.isPresent() )
        {
            switch ( System.getProperty( levelKey.get() ) )
            {
                case "DEBUG":
                    logLevel = Level.DEBUG;
                    break;
                case "ERROR":
                    logLevel = Level.ERROR;
                    break;
                case "WARN":
                    logLevel = Level.WARN;
                    break;
                case "TRACE":
                    logLevel = Level.TRACE;
                    break;
                default:
                    logLevel = Level.INFO;
            }
        }
        else
        {
            logLevel = Level.INFO;
        }
    }

}
