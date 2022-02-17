harvester-admin-0.1.yaml: harvester-admin-0.1.yaml.m4
	rm -rf $@
	m4 $? > $@
	chmod 444 $@

lint: schemalint apilint

schemalint:
	ajv compile -s "schema/*.json"

apilint: harvester-admin-0.1.yaml
	api_lint.py -t OAS -d .

doc:
	api_doc.py -o doc -t OAS -d .

