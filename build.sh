#!/bin/sh
mvn install:install-file -Dfile=libs/FastAsyncWorldEdit-API.jar -DgroupId=boy0001 -DartifactId=FastAsyncWorldEdit-API  -Dversion=3.5.1 -Dpackaging=jar
mvn install:install-file -Dfile=libs/FastAsyncWorldEdit-bukkit18-3.5.1-39acae0.jar -DgroupId=boy0001 -DartifactId=FastAsyncWorldEdit-bukkit  -Dversion=3.5.1 -Dpackaging=jar
mvn package assembly:single
