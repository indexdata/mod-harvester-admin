# mod-harvester-admin

## Purpose

Harvester Admin is an Okapi service that can be put in front of a legacy software product
called [localindices or Harvester](https://github.com/indexdata/localindices). Harvester Admin provides FOLIO based
access to control the workings of the Harvester.

The Harvester is a collection of modules that can read data from a variety of data sources like FTP servers, local file
systems, OAI-PMH servers and web pages, transform those data through highly configurable XSLT based transformation
pipelines and store the transformed data to storage systems like Solr databases or FOLIO Inventory. It's primary use is
harvesting of bibliographic records.

Harvest job definitions, scheduling, and transformation pipelines are configured in a MySQL database. The Harvester has
an XML based REST service that gives clients access to read and update the configurations and the Harvester then has a
JSF based admin UI for admins to create and update configurations through the REST API.

Mod-harvester-admin provides an alternative, FOLIO/JSON based interface to the configuration database that FOLIO clients
can then use for managing harvest jobs.

## Provided Interfaces

### /harvester-admin/harvestables

Create, update and delete harvest job configurations. A job configuration includes scheduling, definition of the record
source to harvest from, transformation pipeline to use, and storage engine to persist the resulting records in.

### /harvester-admin/storages

Create, update and delete definitions of storage engines, for example the server location and APIs of a FOLIO Inventory.

### /harvester-admin/transformations

Create, update and delete record transformation pipelines.

### /harvester-admin/steps

Create, update, and delete transformation steps. A pipeline can be composed of multiple steps and each step can be an
XSLT transformation or a custom Java-based transformation.

### /harvester-admin/tsas

TSA stands for transformation-step-association, and the API can be used to create, update, and delete associations of
specific transformation steps to a pipeline.