#!/bin/bash
ID=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $ID ]
  then
    echo "Usage: . ./stop-job.sh [harvestable ID]"
    return || exit
fi

curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/harvester-admin/jobs/stop/$ID"  -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
