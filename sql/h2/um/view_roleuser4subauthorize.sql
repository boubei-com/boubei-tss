
--用户拥有的能转授的角色,包括由组继承下来的角色
drop table view_roleuser4subauthorize cascade constraints;

create view view_roleuser4subauthorize as

    select u.id as userId, r.id as roleId
    from um_role r, um_roleuser ru, um_user u
    where r.id = ru.roleid and ru.userid = u.id
	  and u.disabled <> 1 and r.disabled <> 1  
          and sysdate between r.startdate and r.enddate
          and ru.strategyid is null

    union all
    
    select u.id as userId, r.id as roleId
    from um_group g, um_rolegroup rg, um_role r, um_groupuser gu, um_user u
    where g.id = rg.groupid and rg.roleid = r.id
	  and g.id = gu.groupid and gu.userid = u.id   
	  and u.disabled <> 1 and r.disabled <> 1  and g.disabled <> 1 
          and sysdate between r.startdate and r.enddate
          and rg.strategyid is null 