#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

DATA=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/storages" -d "@${SCRIPT_DIR}/storage-north-800001.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@${SCRIPT_DIR}/oai-2-inst-north-800010.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@${SCRIPT_DIR}/holdings-north-800011.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@${SCRIPT_DIR}/locations-north-800012.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@${SCRIPT_DIR}/xml-2-json-north-800013.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/transformations" -d "@${SCRIPT_DIR}/transformation-north-800015.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/harvestables" -d "@${SCRIPT_DIR}/harvestable-north-800020.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

