#!/bin/bash
DATA=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $DATA ]
  then
    echo "Usage: . ./post-admin-record.sh [json file name]"
    return || exit
fi
OPTS=`cat apis/harvester-admin-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

curl -i -w '\n' --http1.1 -X POST  "$protocol://$host/harvester-admin/${EP}" -d @${DATA} -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
