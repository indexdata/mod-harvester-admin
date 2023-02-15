# mod-harvester-admin

## Purpose

Harvester Admin is an Okapi service that can be put in front of a legacy software product
called [localindices or Harvester](https://github.com/indexdata/localindices). Harvester Admin provides FOLIO based
access to control the workings of the Harvester.

The Harvester is a collection of modules that can read data from a variety of data sources like FTP servers, local file
systems, OAI-PMH servers and web pages, transform those data through highly configurable XSLT based transformation
pipelines and store the transformed data to storage systems like Solr databases or FOLIO Inventory. Its primary use is
harvesting of bibliographic records.

Harvest job definitions, scheduling, and transformation pipelines are configured in a MySQL database. The Harvester has
an XML based REST service that gives clients access to read and update the configurations and the Harvester then has a
JSF based admin UI for admins to create and update configurations through the REST API.

Mod-harvester-admin provides an alternative, FOLIO/JSON based interface to the configuration database that FOLIO clients 
(like a Stripes UI) can then use for managing harvest jobs.

Mod-harvester-admin is additionally able to persist the harvest job history, including the harvest configuration at the 
time the job was run as well as the Log4J logs for the job and structured reports regarding failed records.  

## Configuration

The module's access to the legacy harvester service is configured by the following environment variables:

* `harvester_protocol` -- the protocol by which to contact the legacy service. Default `http`; `https` is also supported.
* `harvester_host` -- the hostname or IP address on which the legacy service is running. Must be provided.
* `harvester_port` -- the port on which to contact the harvester host. Defaults to 80 if the protocol is `http`, 443 if it is `https`.
* `harvester_auth_basic_username` -- if provided, a username to submit as part of HTTP Basic authentication
* `harvester_auth_basic_password` -- if provided, a username to submit as part of HTTP Basic authentication. If neither username nor password is provided, then HTTP Basic authentication is not used at all.

In addition, if the `acl_filter_by_tenant` environment variable is set to the string value `"false"`, then tenant-filtering is turned off, and all tenants' harvesting jobs are available to any tenant. **Do not use this in production.**

## Provided Interfaces

Mod-harvester-admin provides two sets of APIs, firstly the administration APIs that are passed through to/from the legacy Harvester APIs, and 
secondly a number of APIs for retaining the harvest job history. 

The pass-through APIs are adapted to FOLIO conventions as much as possible but still differs somewhat from typical FOLIO back-end APIs. All legacy IDs (primary keys) are numeric for example, whereas FOLIO APIs typically use UUIDs for primary keys. Mod-harvester-admins own APIs for the job history use the FOLIO convention of UUIDs for identifiers. See more details regarding ID schemes below the API descriptions.

### APIs

#### Overview

In order to configure a harvest job from scratch through the API, following API interactions are required: 
1) Create the storage configuration to use for persisting harvested records.
2) Create the transformation steps that the job will use to transform incoming records to the desired format before storage.
3) Create the transformation pipeline. Possibly including the steps, otherwise do 4)
4) If steps where not assigned in 3) then assign transformation steps to the pipeline in the desired order of execution.
5) Finally, create the harvest job configuration referencing the newly created transformation pipeline and storage definition.

#### 1) Create storage definition

POST storage object to `/harvester-admin/storages`, for example a FOLIO inventory storage using Inventory Update's upsert-by-hrid API: 

```
{
  "name" : "FOLIO Inventory Storage",
  "type" : "inventoryStorage",
  "description" : "Inventory storage at localhost",
  "enabled" : "true",
  "json" : {
    "folioAuthPath" : "bl-users/login",
    "folioTenant" : "diku",
    "folioUsername" : "diku_admin",
    "folioPassword" : "admin",
    "inventoryUpsertPath" : "inventory-upsert-hrid",
    "inventoryBatchUpsertPath" : "inventory-batch-upsert-hrid",
    "logHistoryStoragePath" : "harvester-admin/harvestables/{id}/log/store"
  },
  "url" : "http://okapi:9130/",
}
``` 

This is an example of a Solr storage definition:

```
{
  "name" : "Solr @ localhost",
  "type" : "solrStorage",
  "description" : "Solr at localhost:8983",
  "enabled" : "true",
  "url" : "http://localhost:8983/solr/lui/"
}
```

Assuming the shell has `protocol`, `host`, `tenant` and `token` for a FOLIO session defined, a curl script to create the storage definition could be

```
curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name" : "FOLIO Inventory Storage",
  "type" : "inventoryStorage",
  "description" : "Inventory storage at localhost",
  "enabled" : "true",
  "json" : {
    "folioAuthPath" : "bl-users/login",
    "folioTenant" : "diku",
    "folioUsername" : "diku_admin",
    "folioPassword" : "admin",
    "inventoryUpsertPath" : "inventory-upsert-hrid",
    "inventoryBatchUpsertPath" : "inventory-batch-upsert-hrid",
    "logHistoryStoragePath" : "harvester-admin/harvestables/{id}/log/store"
  },
  "url" : "http://okapi:9130/"
}
' $protocol://$host/harvester-admin/storages
```

Response should be something like: 

```
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:9130/harvester-admin/storages/250767295269321
transfer-encoding: chunked

{
  "acl" : "diku",
  "bulkSize" : "1000",
  "currentStatus" : "NEW",
  "description" : "Inventory storage at localhost",
  "enabled" : "true",
  "id" : "250767295269321",
  "json" : {
    "folioAuthPath" : "bl-users/login",
    "folioTenant" : "diku",
    "folioUsername" : "diku_admin",
    "folioPassword" : "admin",
    "inventoryUpsertPath" : "inventory-upsert-hrid",
    "inventoryBatchUpsertPath" : "inventory-batch-upsert-hrid",
    "logHistoryStoragePath" : "harvester-admin/harvestables/{id}/log/store"
  },
  "name" : "FOLIO Inventory Storage",
  "url" : "http://okapi:9130/",
  "type" : "inventoryStorage"
}
```
#### 2) Create transformation steps

A pipeline can be composed of multiple steps and each step can be an XSLT transformation or a custom Java-based transformation.

POST step objects to `/harvester-admin/steps`. It can include the transformation XSLT which must be escaped to be embedded in JSON. Alternatively just the step metadata can be posted, followed by a PUT of the transformation script as an XSLT document. 

```
{
  "name": "Copy XML",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "outputFormat" : "XML",
  "type" : "XmlTransformStep"
}
```
Response: 
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:9130/harvester-admin/steps/563244948367770
transfer-encoding: chunked

{
  "acl" : "diku",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "name" : "Copy XML",
  "outputFormat" : "XML",
  "script" : "",
  "id" : "563244948367770",
  "testData" : "",
  "testOutput" : "",
  "type" : "XmlTransformStep"
}
```

Say the XSLT script has this content: 
```
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="node()|@*">
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
```

Then this XSLT can be PUT to `/harvester-admin/steps/{step-id}/script?name=Copy+XML`, here `/harvester-admin/steps/563244948367770/script`, and the result would be

GET /harvester-admin/steps/563244948367770: 
```
{
  "acl" : "diku",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "name" : "Copy XML",
  "outputFormat" : "XML",
  "script" : "<xsl:stylesheet version=\"1.0\"\n xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n    <xsl:template match=\"node()|@*\">\n      <xsl:copy>\n        <xsl:apply-templates select=\"node()|@*\"/>\n      </xsl:copy>\n    </xsl:template>\n</xsl:stylesheet>\n",
  "id" : "563244948367770",
  "testData" : "",
  "testOutput" : "",
  "type" : "XmlTransformStep"
}
```

The response on the GET above shows the stylesheet included in the step JSON, with quotes escaped and line feeds inserted. Sending this object instead would have had the same effect as sending first the step and then the script.

The following curl script would POST two steps and PUT the XSLTs. It uses `jq` to capture the IDs of the steps, which would not be necessary with the XSLT included in the step JSON.

```
curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name": "Copy XML",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "outputFormat" : "XML",
  "type" : "XmlTransformStep"
}
' $protocol://$host/harvester-admin/steps

STEP_ID_1=$(curl --silent \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" \
$protocol://$host/harvester-admin/steps?query=name=Copy+XML 2>&1 | jq -r -c '.transformationSteps[0].id')

curl -i -w '\n' --http1.1 -X PUT \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/xml" \
$protocol://$host/harvester-admin/steps/$STEP_ID_1/script?name=Copy+XML \
--data-binary '
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="node()|@*">
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
'

curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name": "Copy XML Again",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "outputFormat" : "XML",
  "type" : "XmlTransformStep"
}
' $protocol://$host/harvester-admin/steps

STEP_ID_2=$(curl --silent \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" \
$protocol://$host/harvester-admin/steps?query=name=Copy+XML+Again 2>&1 | jq -r -c '.transformationSteps[0].id')

curl -i -w '\n' --http1.1 -X PUT \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/xml" \
$protocol://$host/harvester-admin/steps/$STEP_ID_2/script?name=Copy+XML+Again --data-binary '
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="node()|@*">
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
'
```


#### 3) Create the transformation pipeline

The transformation pipeline can be created in two ways: Either create a minimal transformation object through `/harvester-admin/transformations` and subsequently assign steps to it through `/harvester-admin/tsas` _or_ post a composite transformation object with all the steps already embedded to `/harvester-admin/transformations`.

The option to POST a composite transformation object is a convenience when exporting/GETting the composite from one service and importing/POSTing it to another.

```
{
  "name" : "A transformation",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "parallel" : "false",
  "stepAssociations" : [ {
      "position" : "1",
      "step": {
        "name" : "Copy XML"
      }
    }, {
      "position" : "2",
      "step": {
        "name" : "Copy XML again"
      }
    }
  ],
  "type" : "basicTransformation"
}
```

This curl script should create the transformation pipeline: 

```
curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name" : "A transformation",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "parallel" : "false",
  "stepAssociations" : [ {
      "position" : "1",
      "step": {
        "name" : "Copy XML"
      }
    }, {
      "position" : "2",
      "step": {
        "name" : "Copy XML again"
      }
    }
  ],
  "type" : "basicTransformation"
}
' $protocol://$host/harvester-admin/transformations
```

The response should be something like
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:9130/harvester-admin/transformations/313200596498197
transfer-encoding: chunked

{
  "acl" : "diku",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "name" : "A transformation",
  "parallel" : "false",
  "stepAssociations" : [ {
    "id" : "464431555010095",
    "position" : "1",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "Copy XML",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "873913596204614",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "313200596498197"
  }, {
    "id" : "773419215379837",
    "position" : "2",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "Copy XML Again",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "194799646936121",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "313200596498197"
  } ],
  "id" : "313200596498197",
  "type" : "basicTransformation"
}
```

#### 4) Assign transformation steps to the pipeline in the desired order of execution.

In the example above, the steps where included with the POST of the pipeline but alternatively the steps could be added one by one using the `/harvester-admin/tsas` (transformation-step-associations) endpoint.

Say, the transformation POST body was instead: 
```
{
  "name" : "A transformation",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "parallel" : "false"
  "type" : "basicTransformation"
}
```
then the steps could be added through tsas afterwards. Likewise for the transformation pipeline that was created with two steps. The following scripts will add a third step in second position. 

Create the new step:
```
curl -i -w '\n' \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name": "New Second Step",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "outputFormat" : "XML",
  "type" : "XmlTransformStep"
}
' $protocol://$host/harvester-admin/steps

STEP_ID=$(curl --silent \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" \
$protocol://$host/harvester-admin/steps?query=name=Copy+XML 2>&1 | jq -r -c '.transformationSteps[0].id')

curl -i -w '\n' --http1.1 -X PUT \
-H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/xml" \
$protocol://$host/harvester-admin/steps/$STEP_ID/script?name=New+Second+Step \
--data-binary '
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="node()|@*">
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
'
```

Insert it in second position:
```
curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "position" : "2",
  "step" : {
    "name" : "New Second Step"
  },
  "transformationName": "A transformation"
}
' $protocol://$host/harvester-admin/tsas
```

The resulting transformation pipeline would be something like:
```
{
  "acl" : "diku",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "name" : "A transformation",
  "parallel" : "false",
  "stepAssociations" : [ {
    "id" : "713325705981288",
    "position" : "1",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "Copy XML",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "926226260562339",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "770158869141425"
  }, {
    "id" : "570064169942393",
    "position" : "2",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "New Second Step",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "600615164767587",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "770158869141425"
  }, {
    "id" : "373552657153015",
    "position" : "3",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "Copy XML Again",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "823703256263144",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "770158869141425"
  } ],
  "id" : "770158869141425",
  "type" : "basicTransformation"
}
```

In order to fix the description of this transformation, take the JSON response above, change the description to "transformation pipeline with three XSLT steps" and PUT it to `/harvester-admin/transformations/770158869141425`. When doing that, the contents of each step, like the XSLT, is ignored, whereas the PUT would change the number and positions of steps if the PUT JSON changed any of that.

#### 5) Create the harvest job configuration

Once the required storage definition and transformation pipeline are configured, a harvest job can be created that will use them.

The minimal harvestable JSON object that the service will accept would contain the `name` for the job, the `type` (`oaiPmh` or `xmlBulk`), whether the job is enabled to run as scheduled (`enabled`; this boolean would not affect whether the job can be started on demand), whether the job should run immediately `harvestImmediately`, what `storage` and what `transformation` pipeline to use, and the `url` it should fetch data from. If the job is of type `oaiPmh` the object should also contain the properties `oaiSetName` and `metadataPrefix`.

In the POST, the transformation and the storage can be referenced by name or by ID. If both are present, the ID will be used and the name ignored. 

```
{
  "name": "My Harvest Job",
  "type": "xmlBulk",
  "harvestImmediately": "false",
  "enabled": "false",
  "url": "http://localhost:8080/test/marc-xml-files/",
  "transformation": {
    "name": "A transformation"
  },
  "storage": {
    "name": "FOLIO Inventory Storage"
  }
}
```

This curl script should create the job configuration:

```
curl -i -w '\n' -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" \
-d '
{
  "name": "My Harvest Job",
  "type": "xmlBulk",
  "harvestImmediately": "false",
  "enabled": "false",
  "url": "http://localhost:8080/test/marc-xml-files/",
  "transformation": {
    "name": "A transformation"
  },
  "storage": {
    "name": "FOLIO Inventory Storage"
  }
}
' $protocol://$host/harvester-admin/harvestables
```

The response should be something like:

```
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:9130/harvester-admin/harvestables/930117057636685
transfer-encoding: chunked

{
  "acl" : "diku",
  "allowErrors" : "false",
  "cacheEnabled" : "false",
  "currentStatus" : "NEW",
  "diskRun" : "false",
  "enabled" : "false",
  "failedRecordsLogging" : "CLEAN_DIRECTORY",
  "harvestImmediately" : "false",
  "id" : "734110994458146",
  "lastUpdated" : "2023-02-14T23:04:12.755Z",
  "laxParsing" : "false",
  "logLevel" : "INFO",
  "mailLevel" : "WARN",
  "maxSavedFailedRecordsPerRun" : "100",
  "maxSavedFailedRecordsTotal" : "1000",
  "name" : "My Harvest Job",
  "openAccess" : "false",
  "overwrite" : "false",
  "retryCount" : "2",
  "retryWait" : "60",
  "storage" : {
    "entityType" : "inventoryStorageEntity",
    "acl" : "diku",
    "bulkSize" : "1000",
    "currentStatus" : "NEW",
    "description" : "Inventory storage at localhost",
    "enabled" : "true",
    "id" : "634323009732878",
    "idAsString" : "634323009732878",
    "json" : {
      "folioAuthPath" : "bl-users/login",
      "folioTenant" : "diku",
      "folioUsername" : "diku_admin",
      "folioPassword" : "admin",
      "inventoryUpsertPath" : "inventory-upsert-hrid",
      "inventoryBatchUpsertPath" : "inventory-batch-upsert-hrid",
      "logHistoryStoragePath" : "harvester-admin/harvestables/{id}/log/store"
    },
    "name" : "FOLIO Inventory Storage",
    "url" : "http://okapi:9130/"
  },
  "storeOriginal" : "false",
  "timeout" : "300",
  "transformation" : {
    "entityType" : "basicTransformation",
    "acl" : "diku",
    "description" : "transformation pipeline with three XSLT steps",
    "enabled" : "true",
    "name" : "A transformation",
    "parallel" : "false",
    "stepAssociations" : [ {
      "id" : "713325705981288",
      "position" : "1",
      "step" : {
        "entityType" : "xmlTransformationStep",
        "acl" : "diku",
        "description" : "Minimal step",
        "inputFormat" : "XML",
        "name" : "Copy XML",
        "outputFormat" : "XML",
        "script" : "<'script' omitted from nested displays>",
        "id" : "926226260562339",
        "testData" : "",
        "testOutput" : ""
      },
      "transformation" : "770158869141425"
    }, {
      "id" : "570064169942393",
      "position" : "2",
      "step" : {
        "entityType" : "xmlTransformationStep",
        "acl" : "diku",
        "description" : "Minimal step",
        "inputFormat" : "XML",
        "name" : "New Second Step",
        "outputFormat" : "XML",
        "script" : "<'script' omitted from nested displays>",
        "id" : "600615164767587",
        "testData" : "",
        "testOutput" : ""
      },
      "transformation" : "770158869141425"
    }, {
      "id" : "373552657153015",
      "position" : "3",
      "step" : {
        "entityType" : "xmlTransformationStep",
        "acl" : "diku",
        "description" : "Minimal step",
        "inputFormat" : "XML",
        "name" : "Copy XML Again",
        "outputFormat" : "XML",
        "script" : "<'script' omitted from nested displays>",
        "id" : "823703256263144",
        "testData" : "",
        "testOutput" : ""
      },
      "transformation" : "770158869141425"
    } ],
    "id" : "770158869141425"
  },
  "allowCondReq" : "false",
  "passiveMode" : "false",
  "recurse" : "false",
  "url" : "http://localhost:8080/test/marc-xml-files/",
  "type" : "xmlBulk"
}

```

If the configuration details are otherwise correct, this should complete the configurations required to run the harvest job. 

### Harvester logs and log history

#### /harvester-admin/harvestables/{harvestable-id}/log

Pass-through API to return the most recent log-file for the harvestable in plain text.

#### /harvester-admin/harvestables/{harvestable-id}/failed-records

Pass-through API to return list of structured error reports for individual records, that failed to load partially or fully.

#### /harvester-admin/harvestables/{harvestable-id}/failed-records/{record-number}

Pass-through API to retrieve a structured error report for an individual record that failed to load partially or fully.

#### /harvester-admin/harvestables/{id}/log/store

End-point for requesting that mod-harvester-admin stores a current harvest configuration with the most recent logs and record error reports. It should generally be invoked by Harvester, which would do it right after a job has completed (if the harvester is configured for storing the log history to mod-harvester-admin). 

The API has two versions, GET and POST. Harvester needs the POST version. Other clients _could_ use the GET version but there is no need to if Harvester has already requested that the logs be pulled, using the POST.

##### `GET /harvester-admin/harvestables/{id}/log/store` 
Requests that mod-harvester-admin pulls the current job configuration plus the logs and failed records, if any, for a completed job, and stores them in mod-harvester-admin's log history. 

##### `POST /harvester-admin/harvestables/{id}/log/store`
The same as GET, except the POST body contains corrective data for the job's status message and finish time.

The POST version is needed due to a time-lag in Harvester's internal persistence of a completed job's status. The status is only written to Harvester's database some time after the job finishes, so the GET will potentially retrieve an outdated status if invoked immediately after the completion of the job; the POSTed data will correct the stale data as necessary.    



#### /harvester-admin/job/run/{id}

Convenience end-point to start a harvest job with the given harvest configuration ID. It is equivalent to PUTting a harvestable object with `harvestImmediately` set to true and `lastUpdated` set to now. 

#### /harvester-admin/job/stop/{id}

Convenience end-point to stop a running harvest job. It is equivalent to PUTting a harvestable object with `lastUpdated` set to now for a running job. 

#### /harvester-admin/previous-jobs

End-point that will get a list of previous jobs as stored in mod-harvester-admin through `/harvester-admin/harvestables/{id}/log/store` as described  further above. This is a module native API; it will contain UUIDs for primary keys for example. 

#### /harvester-admin/previous-jobs/{job run's uuid}/log

Gets the log for a job in job history. If the job run is the most recent and the harvestable was not updated since the run, then this log would be the same as the log retrieved through `/harvester-admin/harvestables/{harvestable-id}/log`

#### /harvester-admin/previous-jobs/{job run's uuid}/failed-records

Gets collection of error reports for failed records for a job in the job history.



### IDs for primary keys

When posting configuration objects to the Harvester through mod-harvester-admin, objects will be assigned a 15-digit random number for its ID if no ID is provided in the posted JSON. Currently, the APIs allow the client to set ID in a POST, and it can be set to any numeric ID. However, a large random number is advised because multiple FOLIO tenants may be accessing the same legacy Harvester, and would thus write to the same primary key index for each object type whereas each tenant may only be able to see the records for that tenant through the API. The tenant cannot see which IDs are already taken by some other tenant, only that a collision occurred. This is most likely only a concern when posting data through the API directly, outside a UI. A UI will probably depend entirely on the API generating the IDs internally.

The module has a convenience API `/harvester-admin/generate-ids` that will generate and return as plain text a 15-digit random number using the same logic as the module uses internally for creating primary keys. These IDs might be used to define the IDs client side before POSTing. Up to a hundred IDs can be generated at a time: `/harvester-admin/generate-ids?count=100`.    

As mentioned, the new APIs on top of the module's own storage use the standard FOLIO identifier scheme of UUIDs, which will ensure uniqueness at any time (as long as no generated ID is reused of course).
