SELECT /*+ ignored_hint1 ignored_hint2 */ /*+ full(x) */ *
FROM dual;

SELECT /*+ ignored_hint3 */ /*+ full(x) */ *
FROM dual;

SELECT /*+ full(x) */ *
FROM dual;

SELECT sql_id, sql_text
FROM v$sqltext
WHERE sql_text like ('%ignored' || '%hint%');

SELECT XMLQuery('/other_xml/hint_usage/q/h[@st = "PE" or @st = "UR" or @st = "NU" or @st = "EU"]/x/text()' PASSING other_xml RETURNING CONTENT NULL ON EMPTY), other_xml, sql_id
FROM (
SELECT xmltype(other_xml) other_xml, sql_id
FROM v$sql_plan
WHERE sql_id = '03vtzr5z0cvgq'
  AND other_xml IS NOT NULL);

SELECt *
FROM v$sql_hint;

DESC v$sql_hint;

SELECT p.sql_id, XMLCast(h.column_value AS VARCHAR2(64)) hint
FROM v$sql_plan p,
    -- v$sqltext t,
    XMLtable('/other_xml/hint_usage/q/h[@st = "PE" or @st = "UR" or @st = "NU" or @st = "EU"]/x/text()' PASSING xmltype(p.other_xml)) h
WHERE p.sql_id IN ('03vtzr5z0cvgq', 'd5zv9sj1j13bg')
  -- AND p.sql_id = t.sql_id
  AND p.other_xml IS NOT NULL;

SELECT *
FROM dba_hist_sql_plan
WHERE sql_id = '03vtzr5z0cvgq' and other_xml is not null;


SELECT v.sql_id, v.hint, v.error, t.sql_fulltext
  FROM (
    SELECT DISTINCT p.sql_id, h.hint, h.error 
      FROM v$sql_plan p,
           XMLtable('/other_xml/hint_usage/q/(t|h)[@st = "PE" or @st = "UR" or @st = "NU" or @st = "EU"]'
             PASSING xmltype(p.other_xml)
             COLUMNS
               HINT VARCHAR2(64) PATH '//x/text()',
               ERROR VARCHAR2(2) PATH '@st') h
     WHERE p.other_xml LIKE '%hint_usage%'
       AND p.parent_id = 0
       AND p.position = 1) v
  JOIN v$sqlarea t ON (v.sql_id = t.sql_id)
UNION ALL
SELECT v.sql_id, v.hint, v.error, t.sql_text
  FROM (
    SELECT DISTINCT p.sql_id, h.hint, h.error 
      FROM dba_hist_sql_plan p,
           XMLtable('/other_xml/hint_usage/q/(t|h)[@st = "PE" or @st = "UR" or @st = "NU" or @st = "EU"]'
           PASSING xmltype(p.other_xml)
           COLUMNS
             HINT VARCHAR2(64) PATH '//x/text()',
             ERROR VARCHAR2(2) PATH '@st') h
     WHERE p.other_xml LIKE '%hint_usage%'
       AND p.parent_id = 0
       AND p.position = 1) v
  JOIN dba_hist_sqltext t ON (v.sql_id = t.sql_id)
ORDER BY sql_id;
  
  