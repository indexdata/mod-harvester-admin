#!/bin/bash
ID=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
OPTS=`cat apis/harvester-admin-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

curl -i -v -w '\n' --http1.1 -X DELETE  "$protocol://$host/harvester-admin/${EP}/${ID}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
