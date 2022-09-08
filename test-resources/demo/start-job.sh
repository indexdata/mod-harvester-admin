#!/bin/bash
ID=$1

if [ -z "$token" ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z "$ID" ]
  then
    echo "Usage: . ./start-job.sh [harvestable id]"
    return || exit
fi

# shellcheck disable=SC2154
curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/harvester-admin/jobs/run/$ID"  -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
