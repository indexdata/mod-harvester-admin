#!/bin/bash
ID=$1
CURL_OPTIONS=$2

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi


if [ "$CURL_OPTIONS" = "" ]; then
  curl -i -w '\n' --http1.1  "$protocol://$host/harvester-admin/steps/${ID}/script" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
else
  curl -s --http1.1  "$protocol://$host/harvester-admin/steps/${ID}/script" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" $CURL_OPTIONS 
fi
