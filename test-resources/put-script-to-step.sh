#!/bin/bash
ID=$1
DATA=$2

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $DATA ]
  then
    echo "Usage: . ./put-admin-record.sh [id] [xslt file name]"
    return || exit
fi


curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/harvester-admin/steps/${ID}/script" --data-binary @${DATA} -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/xml"
