drop table um_roleusermapping;
create table um_roleusermapping(roleId INTEGER not null, userId INTEGER not null, primary key (roleId, userId));

-- 将匿名角色信息插入，否则匿名访问的时候匿名用户没有匿名角色的权限，因为匿名访问不会自动插入角色
insert into um_roleusermapping(ROLEID, USERID) values (-10000, -10000);
commit;