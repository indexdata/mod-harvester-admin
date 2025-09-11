###
Start localindices for running the HarvesterIntegrationTest:

    docker compose -f localindices.yml up -d


### Log in to localindices in mysql

    mysql -P 3306 -u localidxadm -plocalidxadmpass localindices
To reference localhost from within the container, for example accessing FOLIO auth and inventory:
  Most Docker Engine installations will represent the host as 172.17.0.1 on the default docker0 bridge network.
  You can check your own IP by running this command on your host:
  `ip addr show docker0`
  So, Okapi is at:   172.17.0.1:9130  and the storage "url" should thus be http://172.17.0.1:9130

Copy files to test directory in docker container for harvesting
   docker cp <file> harvester:/var/lib/jetty/webapps/test/

A file copied there can be harvested using the url http://localhost:8080/test/<file> in the harvestable definition

Make separate source file directory for testing:

   docker exec -u root -it harvester /bin/bash -c "mkdir /var/lib/jetty/webapps/test/separate-dir"
