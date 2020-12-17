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


create index idx_wfstatus_1 on dm_workflow_status (tableId);
create index idx_wfstatus_2 on dm_workflow_status (itemId); 
create index idx_wfstatus_3 on dm_workflow_status (nextProcessor);
create index idx_wfstatus_4 on dm_workflow_status (currentStatus);

create index idx_wflog_1 on dm_workflow_log (tableId);
create index idx_wflog_2 on dm_workflow_log (itemId); 
