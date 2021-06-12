SQL_ID
======

Computes the Oracle SQL_ID of a JDBC query string.

There are three different sources from which a SQL_ID can be computed:

* From a JDBC query string with positional placeholders for bind variables, eg `"SELECT * FROM dual WHERE dummy = ?"`
* From a native query string with named placeholders for bind variables, eg `"SELECT * FROM dual WHERE dummy = :val"`
* From a `SQLException`

Usage
-----

```xml
<dependency>
  <groupId>com.github.marschall</groupId>
  <artifactId>sqlid</artifactId>
  <version>1.0.0</version>
</dependency>
```

If you already have a native query string with :name as placeholders you can simply use

```java
SqlId.compute("SELECT * FROM dual WHERE dummy = :1 ");
```

If you want more convenience `SqlIdLookup` takes care of converting form JDBC query strings to native query strings and also performs caching.


```java
SqlIdLookup lookup = new SqlIdLookup(dataSource, 128);
String sqlId = lookup.getSqlIdOfJdbcString("SELECT * FROM dual WHERE dummy = ?");
```

