drop table view_report_resource;

CREATE VIEW view_report_resource AS
SELECT 0 as id, 'root' as name, -1 as parentId, '00001' as decode FROM dual
UNION
SELECT id, name, parentId, decode FROM dm_report;


drop table view_record_resource;

CREATE VIEW view_record_resource AS
SELECT 0 as id, 'root' as name, -1 as parentId, '00001' as decode FROM dual
UNION
SELECT id, name, parentId, decode FROM dm_record;