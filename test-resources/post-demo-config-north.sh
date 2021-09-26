#!/bin/bash
DATA=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/storages" -d "@./demo/storage-north-800001.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/oai-2-inst-north-800010.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/holdings-north-800011.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/locations-north-800012.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/steps" -d "@./demo/xml-2-json-north-800013.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/transformations" -d "@./demo/transformation-north-800015.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/harvestables" -d "@./demo/harvestable-north-800020.json" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json" 

