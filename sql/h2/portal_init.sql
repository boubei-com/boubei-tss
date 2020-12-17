-- 门户结构资源视图
drop table view_portal_resource cascade constraints;
CREATE VIEW view_portal_resource AS
	SELECT 0 as id, 'root' as name, -1 as parentId, '00001' as decode FROM dual
	UNION
	SELECT id, name, parentId, decode FROM portal_structure;

-- 菜单资源视图
drop table view_navigator_resource cascade constraints;
CREATE VIEW view_navigator_resource AS
	SELECT 0 as id, 'root' as name, -1 as parentId, '00001' as decode FROM dual
	union
	select id, name, parentid, decode from portal_navigator;