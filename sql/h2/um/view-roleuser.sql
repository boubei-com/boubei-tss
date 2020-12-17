--用户拥有的所有角色, 包括由组继承下来的角色
drop table view_roleuser cascade constraints;

create view view_roleuser as
--普通的用户对角色
select u.id as userId, r.id as roleId
    from um_role r, um_roleuser ru, um_user u
    where r.id = ru.roleid and ru.userid = u.id
	  and u.disabled <> 1 and  r.disabled <> 1  
          and sysdate between r.startdate and r.enddate
          and ru.strategyid is null    
union all
--转授的用户对角色    
select u.id as userId, r.id as roleId
    from um_role r, um_roleuser ru, um_user u, um_sub_authorize s
    where r.id = ru.roleid and ru.userid = u.id
	  and u.disabled <> 1 and r.disabled <> 1  
          and sysdate between r.startdate and r.enddate
          and s.id = ru.strategyid and sysdate between s.startdate and s.enddate and s.disabled <> 1           
union all
--普通的组对角色   
select u.id as userId, r.id as roleId
    from um_group g, um_rolegroup rg, um_role r, um_groupuser gu, um_user u
    where g.id = rg.groupid and rg.roleid = r.id and g.id = gu.groupid and gu.userid = u.id 
	  and u.disabled <> 1 and r.disabled <> 1  and g.disabled <> 1 
          and sysdate between r.startdate and r.enddate
          and rg.strategyid is null          
union all
--转授的组对角色     
select u.id as userId, r.id as roleId
    from um_group g, um_rolegroup rg, um_role r, um_groupuser gu, um_user u, um_sub_authorize s
    where g.id = rg.groupid and rg.roleid = r.id and g.id = gu.groupid and gu.userid = u.id 
	  and u.disabled <> 1 and r.disabled <> 1  and g.disabled <> 1 
          and sysdate between r.startdate and r.enddate
          and s.id = rg.strategyid and sysdate between s.startdate and s.enddate and s.disabled <> 1 