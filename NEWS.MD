## 0.4.0-SNAPSHOT

* Adds full-text query capability to PG text fields (MODHAADM-51)
* Fixes bug in sorting logic (MODHAADM-52)
* Adds previousJob.message to queryable fields (MODHAADM-50)
* Adds logLine.line, logLine.jobLabel to queryable fields (MODHAADM-48) 

## 0.3.9-SNAPSHOT

* Bugfix: enables CQL operators for text fields (MODHAADM-47)
* Declares and validates UUIDs in API paths (in OpenApi)  (MODHAADM-46)
* Improves error responses for exceptions in the APIs 

## 0.3.8-SNAPSHOT

* Supports both JSON and plain text responses for `./previous-jobs/{id}/log` (MODHAADM-45)

## 0.3.7-SNAPSHOT

* Supports paging (offset,limit) for legacy logs, failed records (MODHAADM-42)

## 0.3.6-SNAPSHOT

* Changes property names to camelCase, failed-records (MODHAADM-41)

## 0.3.5-SNAPSHOT

* Fixes declared content type for two APIs (MODHAADM-40)

## 0.3.4-SNAPSHOT

* Provides below OAI-PMH and XML bulk specific APIs (MODHAADM-34)
* POST /harvester-admin/harvestables/xml-bulk (MODHAADM-34)
* POST /harvester-admin/harvestables/oai-pmh (MODHAADM-34)
* PUT /harvester-admin/harvestables/xml-bulk/{id} (MODHAADM-34)
* PUT /harvester-admin/harvestables/oai-pmh/{id} (MODHAADM-34)
* Multiple schema fixes, particularly for OAI-PMH (MODHAADM-34)

## 0.3.3-SNAPSHOT

* Aligns POM version with build tag.

## 0.3.2-SNAPSHOT

* Exposes port 8081 in Docker container (was 8080)

## 0.3.1-SNAPSHOT

* Requires Okapi 5.0.0 (without -SNAPSHOT)

## 0.3.0-SNAPSHOT

* Log history: Override Harvester's status, start-date (MODHAADM-30)
* Bug-fix: Grant access to failed-records even with acl enabled (MODHAADM-33)
* Bug-fix: Better error handling for requests with bad references (MODHAADM-31)

## 0.2.9-SNAPSHOT

* Allows `harvestable.storageBatchLimit` (MODHAADM-29)
* Provides walk-through of harvest configuration in README (MODHAADM-22)

## 0.2.6-SNAPSHOT

* Enforces unique names when no "id" in POSTs (MODHAADM-26)
* Supports config names in references in POSTS (MODHAADM-25 etc)
* Bug-fixes, schema improvement and fixes, documentation

## 0.2.5-SNAPSHOT

* Fixes NPE when storing logs by GET
* Fixes error handling when `step` not found in GET script
* Omits `testData`, `testOutput` from step in nested display 

## 0.2.4-SNAPSHOT re-tag due to timeout in release-maven workflow

* Enables retries for registry login

## 0.2.3-SNAPSHOT (not a release)

* Applies second line of validations for complex schema rules (MODHAADM-23)
* Accepts harvestables of type `xmlBulk` too (MODHAADM-23)
* Provides 15 digit random numbers for primary keys (MODHAADM-21)

## 0.1.0 2022-07-19

* First provisional release
* Provides interface `harvester-admin` `0.1`

