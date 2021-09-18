#!/bin/bash
ID=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
if [ -z $DATA ]
  then
    echo "Usage: . ./delete-inventory-record.sh [uuid]"
    return || exit
fi
OPTS=`cat apis/inventory-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

curl -i -v -w '\n' --http1.1 -X DELETE  "$protocol://$host/${EP}/${ID}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token" -H "Content-Type: application/json"
