https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md#running-oracle-database-enterprise-and-standard-edition-2-in-a-docker-container
https://docs.oracle.com/en/database/oracle/oracle-database/19/comsc/installing-sample-schemas.html#GUID-CB945E4C-D08A-4B26-A12D-3D6D688467EA
https://docs.oracle.com/en/database/oracle/oracle-database/19/comsc/database-sample-schemas.pdf
https://infohub.delltechnologies.com/l/oracle-in-docker-containers-managed-by-kubernetes-1/step-5-import-sample-schemas-from-github-7

```
docker exec -ti jdbc-oracle-sqlid /bin/bash
$ORACLE_HOME/demo/schema/human_resources
sqlplus sys/<your password>@//localhost:1521/ORCLCDB as sysdba
alter session set container=ORCLPDB1;
@?/demo/schema/human_resources/hr_main.sql
GRANT SELECT_CATALOG_ROLE TO HR CONTAINER=CURRENT;

users
temp
$ORACLE_HOME/demo/schema/log/
```