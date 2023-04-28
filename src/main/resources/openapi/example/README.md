# JSON example files for mod-harvester-admin schemas

The files in this directory are provided for the use of `make schemalint` in the parent directory, which validates each of them against the appropriate schema.

* `harvestableBrief.json` -- a single record from the response to a search for harvestables.

The remaining files represent full harvestable records of the fuor different types, and are taken from HTTP responses obtained when fetching real harvestable objects, with the following modifications:

* `xmlBulk.json` -- Fields `retryCount` and `retryWait` removed, as the UI does in its `cooked2raw` function.
* `oaiPmh.json` -- No changes
* `connector.json` -- No changes
* `status.json` -- No changes

