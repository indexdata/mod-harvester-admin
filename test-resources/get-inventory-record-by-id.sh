#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

ID=$1

if [ -z $token ]
  then
    echo "Can't find okapi token-- Make sure to run a login script."
    return || exit 
fi
OPTS=`cat $SCRIPT_DIR/apis/inventory-endpoints`;

# echo "Choose an endpoint..."
select EP in $OPTS
do
  break
done

curl -i -w '\n' --http1.1  "$protocol://$host/${EP}/${ID}" -H "x-okapi-tenant: $tenant" -H "x-okapi-token: $token"
