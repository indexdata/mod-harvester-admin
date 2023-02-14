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
3) Create the transformation pipeline.
4) Assign transformation steps to the pipeline in the desired order of execution.
5) Finally, create the harvest job configuration referencing the newly created transformation pipeline and storage definition.

#### 1) Create storage definition

POST storage object to `/harvester-admin/storages`, for example a FOLIO inventory storage using Inventory Update's upsert-by-hrid API: 

```
{
  "name" : "FOLIO Inventory Storage",
  "type" : "inventoryStorage"
  "description" : "Inventory storage at localhost",
  "enabled" : "true",
  "json" : {
    "folioAuthPath" : "bl-users/login",
    "folioTenant" : "diku",
    "folioUsername" : "diku_admin",
    "folioPassword" : "admin",
    "inventoryUpsertPath" : "inventory-upsert-hrid",
    "inventoryBatchUpsertPath" : "inventory-batch-upsert-hrid",
    "logHistoryStoragePath" : "/harvester-admin/harvestables/{id}/log/store"
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

#### 2) Create transformation steps

A pipeline can be composed of multiple steps and each step can be an XSLT transformation or a custom Java-based transformation.

POST step object to `/harvester-admin/steps`. It can include the transformation XSLT which must be escaped to be embedded in JSON. Alternatively just the step metadata can be posted, followed by a PUT of the transformation script as a XSLT document. 

````
{
  "name": "Copy XML",
  "description" : "Minimal step",
  "inputFormat" : "XML",
  "outputFormat" : "XML",
  "type" : "XmlTransformStep"
}
````
Response: 
````
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
````

Say the XSLT script has this content: 
````
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="node()|@*">
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
````

Then this XSLT can be PUT to `/harvester-admin/steps/{step-id}/script?name=Copy+XML`, here `/harvester-admin/steps/563244948367770/script`, and the result would be

GET /harvester-admin/steps/563244948367770: 
````
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
````

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
The response should be something like
````
{
  "acl" : "diku",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "name" : "A transformation",
  "parallel" : "false",
  "stepAssociations" : [ {
    "id" : "733436313031356",
    "position" : "1",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step",
      "inputFormat" : "XML",
      "name" : "Copy XML",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "563244948367770",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "638697013731967"
  }, {
    "id" : "904615174851830",
    "position" : "2",
    "step" : {
      "entityType" : "xmlTransformationStep",
      "acl" : "diku",
      "description" : "Minimal step 2",
      "inputFormat" : "XML",
      "name" : "Copy XML again",
      "outputFormat" : "XML",
      "script" : "<'script' omitted from nested displays>",
      "id" : "550979590351830",
      "testData" : "",
      "testOutput" : ""
    },
    "transformation" : "638697013731967"
  } ],
  "id" : "638697013731967",
  "type" : "basicTransformation"
}
````
Note that a PUT of a composite transformation entity will NOT consider the embedded steps. That is, a PUT would update basic information like name and description of the pipeline but would not change, add or remove steps from the transformation. Embedded steps are in other words ignored in the PUT, and changes to the sequence of steps must be performed through `/harvester-admin/tsas`.

#### 4) Assign transformation steps to the pipeline in the desired order of execution.

In the example above, the steps where included with the POST of the pipeline, but alternatively the steps could be added one by one.

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
Response would be:
````
Location: http://localhost:9130/harvester-admin/transformations/516309045363828
{
  "acl" : "diku",
  "description" : "transformation pipeline with two XSLT steps",
  "enabled" : "true",
  "name" : "A transformation - example 2",
  "parallel" : "false",
  "id" : "516309045363828",
  "type" : "basicTransformation"
}
````

Then the steps can be added subsequently: 


#### /harvester-admin/tsas

Pass-through API to attach steps to transformation pipelines. TSA stands for transformation-step-association, and the API can be used to create, update, and delete associations of specific transformation steps to a pipeline.



#### /harvester-admin/harvestables

Pass-through API to create, update and delete harvest job configurations. A job configuration includes scheduling, definition of the record
source to harvest from, and the IDs of the transformation pipeline to use and the storage engine to persist the resulting records in.

The minimal harvestable JSON object that the service will accept would contain the `name` for the job, the `type` (`oaiPmh` or `xmlBulk`), whether the job is enabled to run as scheduled (`enabled`. This boolean would not affect whether the job can be started on demand), whether the job should run immediately `harvestImmediately`, what `storage` and what `transformation` pipeline to use, and the `url` it should fetch data from. If the job is of type `oaiPmh` the object should also contain the properties `oaiSetName` and `metadataPrefix`:

```
{
  "name": "my harvest job",
  "type": "xmlBulk",
  "harvestImmediately": "false",
  "enabled": "false",
  "url": "http://localhost:8080/marc-xml-files/",
  "transformation": {
    "id": "642863552135242"
  },
  "storage": {
    "id": "203985902042325"
  }
}
```

When retrieving a harvestable through the API, the definitions of transformation and storage will be (almost) fully expanded as embedded objects. In order to support retrieval of a harvestable from one service for import to another, the fully expanded harvestable can be posted to `/harvester-admin/harvestables` just like the minimal harvestable, but all the detailed properties of the transformation and the storage (apart from the IDs and the entity types) would then be ignored.  

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

When posting configuration objects to the Harvester through mod-harvester-admin, objects will be assigned a 15-digit random number for its ID if no ID is provided in the posted JSON. Currently, the APIs allow the client to set ID in a POST, and it can be set to any numeric ID. However, a large random number is adviced because multiple FOLIO tenants may be accessing the same legacy Harvester, and would thus write to the same primary key index for each object type whereas each tenant may only be able see the records for that tenant through the API. The tenant cannot see which IDs are already taken by some other tenant, only that a collision occurred. This is most likely only a concern when posting data through the API directly, outside of a UI. A UI will probably depend entirely on the API generating the IDs internally.

The module has a convenience API `/harvester-admin/generate-ids` that will generate and return as plain text a 15-digit random number using the same logic as the module uses internally for creating primary keys. These IDs might be used to define the IDs client side before POSTing. Up to hundred IDs can be generated at a time: `/harvester-admin/generate-ids?count=100`.    

As mentioned, the new APIs on top of the module's own storage use the standard FOLIO identifier scheme of UUIDs, which will ensure uniqueness at any time (as long as no generated ID is reused of course).
