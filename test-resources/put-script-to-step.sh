#!/bin/bash
ID=$1
NAME=$2
DATA=$3

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $DATA ]
  then
    echo "Usage: . ./put-admin-record.sh [id] [step name (possibly with truncation)]  [xslt file name]"
    return || exit
fi


curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/harvester-admin/steps/${ID}/script?name=${NAME}" --data-binary @${DATA} -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/xml"
