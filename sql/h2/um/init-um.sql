truncate table um_group;

insert into um_group (ID, NAME, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-1, 'root',     0, 0,  0, 1, 1, '00001', 0);

insert into um_group (ID, NAME, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-2, 'Main-Group',     0, 1,  -1, 1, 2, '0000100001', 0);

insert into um_group (ID, NAME, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-3, 'Assistant-Group',   0, 2,  -1, 2, 2, '0000100002', 0);

insert into um_group (ID, NAME, DOMAIN, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-7, 'Self-Register-Group', 'Self', 0, 1, -2, 1, 3, '000010000100001', 0);

insert into um_group (ID, NAME, DOMAIN, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-8, 'Domain-Group', 'Business', 0, 1, -2, 2, 3, '000010000100002', 0);

insert into um_group (ID, NAME, DOMAIN, DISABLED, GROUPTYPE, PARENTID, SEQNO, LEVELNO, DECODE, lockVersion)
values (-9, 'Dev-Group', 'Dev', 0, 1, -2, 3, 3, '000010000100003', 0);

commit;


truncate table um_role;

insert into um_role (ID, ISGROUP, PARENTID, NAME, SEQNO, DISABLED, LEVELNO, DECODE, lockVersion)
values (-6, 1, 0, 'root', 1, 0, 1, '00001', 0);

insert into um_role (ID, STARTDATE, ENDDATE, ISGROUP, PARENTID, NAME, SEQNO, DISABLED, LEVELNO, DECODE, lockVersion)
values (-1,     SYSDATE, SYSDATE + 365*50, 0, -6, 'Admin', 1, 0, 2, '0000100001', 0);

insert into um_role (ID, STARTDATE, ENDDATE, ISGROUP, PARENTID, NAME, SEQNO, DISABLED, LEVELNO, DECODE, lockVersion)
values (-10000, SYSDATE, SYSDATE + 365*50, 0, -6, 'ANONYMOUS',   2, 0, 2, '0000100002', 0);

insert into um_role (ID, STARTDATE, ENDDATE, ISGROUP, PARENTID, NAME, SEQNO, DISABLED, LEVELNO, DECODE, lockVersion)
values (-8, SYSDATE, SYSDATE + 365*50, 0, -6, 'DomainAdmin',   3, 0, 2, '0000100003', 0);

insert into um_role (ID, STARTDATE, ENDDATE, ISGROUP, PARENTID, NAME, SEQNO, DISABLED, LEVELNO, DECODE, lockVersion)
values (-9, SYSDATE, SYSDATE + 365*50, 0, -6, 'Developer',  4, 0, 2, '0000100004', 0);


commit;

-- truncate table um_user;
delete from um_user;

--系统管理员ID=-1，初始化密码为123456
insert into um_user (ID, DISABLED, ACCOUNTLIFE, AUTHMETHOD, LOGINNAME, PASSWORD, USERNAME, lockVersion, email)
values (-1, 0, SYSDATE + 365*50, 'com.boubei.tss.um.sso.UMPasswordIdentifier', 'Admin', 'E5E0A2593A3AE4C038081D5F113CEC78', 'Admin', 0, 'lovejava@163.com');

--匿名用户ID=-10000
insert into um_user (ID, DISABLED, ACCOUNTLIFE, AUTHMETHOD, LOGINNAME, PASSWORD, USERNAME, lockVersion)
values (-10000, 0, SYSDATE + 365*50, null, 'ANONYMOUS', null, 'ANONYMOUS', 0);
commit;


truncate table um_groupUser;

-- 将系统管理员和匿名用户卦到主用户组下
insert into um_groupuser (ID, GROUPID, USERID) values (-1, -2, -1);
insert into um_groupuser (ID, GROUPID, USERID) values (-2, -2, -10000);
commit;

truncate table um_roleuser;

insert into um_roleuser (ID, ROLEID, USERID) values (0, -1, -1);
commit;

-- 将匿名角色信息插入，否则匿名访问的时候匿名用户没有匿名角色的权限，因为匿名访问不会自动插入角色
truncate table um_roleusermapping;
insert into um_roleusermapping(ROLEID, USERID) values (-10000, -10000);
commit;
