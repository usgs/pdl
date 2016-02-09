Product Distribution Layer (PDL)
================================
[![Build Status](https://travis-ci.org/usgs/pdl.svg?branch=master)](https://travis-ci.org/usgs/pdl)


PDL distributes many types of information. It:
- integrates with existing systems, Cross-platform command line and Java APIs.
- is loosely coupled, producers and consumers may communicate directly or via hubs.
- uses push distribution.
- uses digital signatures to provide authentication and integrity.


## Download latest release
  - http://ehppdl1.cr.usgs.gov/ProductClient.zip
  - or, only the latest JAR file http://ehppdl1.cr.usgs.gov/ProductClient/ProductClient.jar

## User Guide
  - http://ehppdl1.cr.usgs.gov/userguide/
  - http://ehppdl2.wr.usgs.gov/userguide/

## Examples
  - http://github.com/jmfee-usgs/pdl-client-examples

## Building or Developing

This is a java project with an ant build file.  To build the project, from the project directory run:
```
ant jar
```

This project includes Eclipse project files, but using Eclipse is not required.

Some unit tests depend on
- internet connection
- SSH access to localhost with out a password (via a key).
