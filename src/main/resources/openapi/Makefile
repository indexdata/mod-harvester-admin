harvester-admin-0.1.yaml: harvester-admin-0.1.yaml.m4
	rm -rf $@
	m4 $? > $@
	chmod 444 $@

# Ensure folio-tools/api-lint/api_lint.py is on $PATH
lint:
	api_lint.py -t OAS -d .

# To install: sudo npm -g install ajv-cli
schemalint:
	ajv compile -s "schema/*.json"

