#!/bin/bash
DATA=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/storages" -d "@./demo/storage-diku-900001.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/oai-2-inst-diku-900010.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/holdings-diku-900011.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/locations-diku-900012.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/xml-2-json-diku-900013.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/transformations" -d "@./demo/transformation-diku-900015.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/harvestables" -d "@./demo/harvestable-diku-900020.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

