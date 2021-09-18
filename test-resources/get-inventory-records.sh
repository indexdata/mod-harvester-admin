#!/bin/bash
QUERY=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
OPTS=`cat apis/inventory-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

if [ -z QUERY ] 
  then
    curl -i -w '\n' --http1.1 "$protocol://$host/${EP}?${QUERY}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
  else
    curl -i -w '\n' --http1.1 "$protocol://$host/${EP}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
fi
