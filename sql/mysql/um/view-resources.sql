
drop table view_group_resource;
drop table view_role_resource;

CREATE VIEW view_group_resource AS SELECT ID, parentId, name, decode FROM um_group;
CREATE VIEW view_role_resource AS SELECT ID, parentId, name, decode FROM um_role;

