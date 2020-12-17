-- 用户组资源视图
drop table view_group_resource cascade constraints;
CREATE VIEW view_group_resource AS SELECT ID, parentId, name, decode FROM  um_group;

-- 角色资源视图
drop table view_role_resource cascade constraints;
CREATE VIEW view_role_resource AS SELECT ID, parentId, name, decode FROM  um_role;
