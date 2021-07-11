#!/bin/bash
# https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md#running-oracle-database-enterprise-and-standard-edition-2-in-a-docker-container
# https://docs.oracle.com/en/database/oracle/oracle-database/19/comsc/installing-sample-schemas.html#GUID-CB945E4C-D08A-4B26-A12D-3D6D688467EA
# sqlplus sys/<your password>@//localhost:1521/<your SID> as sysdba
# alter session set "_ORACLE_SCRIPT"=true;
DIRECTORY=`dirname $0`
DIRECTORY=$(realpath $DIRECTORY)

docker run --name jdbc-oracle-sqlid \
 -p 1521:1521 -p 5500:5500 \
 --shm-size=1g \
 -v ${DIRECTORY}/sql:/docker-entrypoint-initdb.d/setup \
 -d oracle/database:19.3.0-se2
