#!/bin/bash
QUERY=$1

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

if [ -z QUERY ] 
  then
    curl -i -w '\n' --http1.1 "$protocol://$host/harvester-admin/${EP}?${QUERY}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
  else
    echo Using query ${QUERY}
    curl -i -w '\n' --http1.1 "$protocol://$host/harvester-admin/${EP}?${QUERY}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
fi
