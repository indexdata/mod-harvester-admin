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

Mod-harvester-admin provides two sets of APIs, firstly the administration APIs that are passed through from the legacy Harvester APIs, and 
secondly a number of APIs for retaining the harvest job history. The second group of APIs are specific for the module and not found in the legacy Harvester.

The pass-through APIs are adapted to FOLIO conventions as much as possible but still differs somewhat from typical FOLIO back-end APIs. All legacy IDs (primary keys) are numeric for example, whereas FOLIO APIs typically use UUIDs for primary keys. Mod-harvester-admins own APIs for the job history use UUIDs for identifiers. See more details regarding ID schemes below the API descriptions.

### APIs

#### /harvester-admin/harvestables

Pass-through API to create, update and delete harvest job configurations. A job configuration includes scheduling, definition of the record
source to harvest from, transformation pipeline to use, and storage engine to persist the resulting records in.

#### /harvester-admin/harvestables/{harvestable-id}/log

Pass-through API to return the most recent log-file for the harvestable in plain text.

#### /harvester-admin/harvestables/{harvestable-id}/failed-records

Pass-through API to return list of structure error reports for indidvidual records 

#### /harvester-admin/storages

Pass-through API to create, update and delete definitions of storage engines, for example the server location and APIs of a FOLIO Inventory.

#### /harvester-admin/transformations

Pass-through API to create, update and delete record transformation pipelines.

#### /harvester-admin/steps

Pass-through API to create, update, and delete transformation steps. A pipeline can be composed of multiple steps and each step can be an
XSLT transformation or a custom Java-based transformation.

#### /harvester-admin/tsas

Pass-through API to attach steps to transformation pipelines. TSA stands for transformation-step-association, and the API can be used to create, update, and delete associations of specific transformation steps to a pipeline.

#### /harvester-admin/job/run/{id}

Convenience end-point to start a harvest job with the given harvest configuration ID. It is equivalent to PUTting a harvestable object with `harvestImmediately` set to true and `lastUpdated` set to now. 

#### /harvester-admin/job/stop/{id}

Convenience end-point to stop a running harvest job. It is equivalent to PUTting a harvestable object with `lastUpdated` set to now for a running job. 

#### /harvester-admin/

### IDs for primary keys

When posting configuration objects to the Harvester through mod-harvester-admin, objects will be assigned a 15-digit random number for its ID if no ID is provided in the posted JSON. Currently, the APIs allow the client to set ID in a POST, and it can be set to any numeric ID. However, a large random number is adviced because multiple FOLIO tenants may be accessing the same legacy Harvester, and would thus write to the same primary key index for each object type whereas each tenant may only be able see the records for that tenant through the API. The tenant cannot see which IDs are already taken by some other tenant, only that a collision occurred. This is most likely only a concern when posting data through the API directly, outside of a UI. A UI will probably depend entirely on the API generating the IDs internally.

The module has a convenience API `/harvester-admin/generate-ids` that will generate and return as plain text a 15-digit random number using the same logic as the module uses internally for creating primary keys. These IDs might be used to define the IDs client side before POSTing. Up to hundred IDs can be generated at a time: `/harvester-admin/generate-ids?count=100`.    

As mentioned, the new APIs on top of the module's own storage use the standard FOLIO identifier scheme of UUIDs, which will ensure uniqueness at any time (as long as no generated ID is reused of course).
