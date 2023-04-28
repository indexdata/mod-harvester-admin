# It seems the M4 file was removed in commit 63ccb575c9d26275c8e4b1b8f6c6abbd1ab81ac4
#
# harvester-admin-0.1.yaml: harvester-admin-0.1.yaml.m4
# 	rm -rf $@
# 	m4 $? > $@
# 	chmod 444 $@

lint: schemalint examplelint apilint

schemalint:
	ajv compile -s schemas/harvestableXmlBulkPostPut.json
	ajv compile -s schemas/harvestableOaiPmhPostPut.json
	# ajv doesn't seem to be able to handle $ref at all, so we can't validate the uber-schema
	# ajv compile -s schemas/harvestablePostPut.json

examplelint:
	ajv test -s schemas/harvestableBrief.json -d example/harvestableBrief.json --valid
	ajv test -s schemas/harvestableXmlBulkPostPut.json -d example/xmlBulk.json --valid
	ajv test -s schemas/harvestableOaiPmhPostPut.json -d example/oaiPmh.json --valid
	ajv test -s schemas/harvestableConnectorPostPut.json -d example/connector.json --valid
	ajv test -s schemas/harvestableStatusPostPut.json -d example/status.json --valid

apilint: harvest-admin-1.0.yaml
	api_lint.py -t OAS -d .

doc:
	api_doc.py -o doc -t OAS -d .

