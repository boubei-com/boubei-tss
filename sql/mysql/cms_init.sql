drop table view_channel_resource;

CREATE VIEW view_channel_resource AS
select -1 as id, '全部' as name, 0 as parentId, '00001' as decode from dual
union
select id, name, parentId, decode from cms_channel;
    

create index idx_article_createTime on cms_article (createTime); 
create index idx_article_status on cms_article (status); 
create index idx_article_AUTHOR on cms_article (AUTHOR); 
create index idx_article_ISSUEDATE on cms_article (ISSUEDATE); 

create index idx_channel_name   on cms_channel (NAME);
create index idx_channel_DECODE on cms_channel (DECODE); 
