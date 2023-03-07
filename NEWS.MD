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

