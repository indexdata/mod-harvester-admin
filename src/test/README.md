# Testing notes, mod-harvester-admin

A majority of the module's unit tests depend on access to an installed Harvester (rather than relying on a mock-up of the legacy
Harvester API). When doing a `mvn install`, with no further options, only the few tests that do not involve Harvester will be run. 

All tests, including Harvester tests, can be run with 
`mvn install -P harvesterTests`

The Surefire plug-in configuration in the POM configures environment variables for a local Harvester, like host and port, to define the
access, similar to what would be defined in the module's deployment descriptor, so these POM configurations can be modified if 
the Harvester to use runs at another address. 

