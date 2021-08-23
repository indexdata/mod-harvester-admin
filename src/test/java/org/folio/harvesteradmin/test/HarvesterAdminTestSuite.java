package org.folio.harvesteradmin.test;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith( VertxUnitRunner.class )
public class HarvesterAdminTestSuite
{
    private final Logger logger = LoggerFactory.getLogger( "HarvesterAdminTestSuite" );

    //Vertx vertx;

    public HarvesterAdminTestSuite()
    {
    }

    @Test
    public void dummy( TestContext testContext )
    {
        logger.info( "Running dummy test method" );
    }

}
