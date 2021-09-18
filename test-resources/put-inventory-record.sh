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
    echo "Usage: . ./put-inventory-record.sh [uuid] [json file name]"
    return || exit
fi
OPTS=`cat apis/inventory-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

curl -i -w '\n' --http1.1 -X PUT  "$protocol://$host/${EP}/${ID}" -d @${DATA} -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
