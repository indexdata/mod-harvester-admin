#!/bin/bash
DATA=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $DATA ]
  then
    echo "Usage: . ./start-job.sh [json file name]"
    return || exit
fi

curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/harvester-admin/jobs/run" -d @${DATA} -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
