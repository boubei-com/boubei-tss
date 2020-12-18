DROP table IF EXISTS `view_group_resource`;
DROP table IF EXISTS `view_navigator_resource`;
DROP table IF EXISTS `view_record_resource`;
DROP table IF EXISTS `view_role_resource`;
DROP table IF EXISTS `view_report_resource`;
DROP table IF EXISTS `view_roleuser`;
DROP table IF EXISTS `view_roleuser4subauthorize`;

--
-- Final view structure for view `view_group_resource`
--

/*!50001 DROP VIEW IF EXISTS `view_group_resource`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_group_resource` AS select `um_group`.`id` AS `ID`,`um_group`.`parentId` AS `parentId`,`um_group`.`name` AS `name`,`um_group`.`decode` AS `decode` from `um_group` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_navigator_resource`
--

/*!50001 DROP VIEW IF EXISTS `view_navigator_resource`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_navigator_resource` AS select 0 AS `id`,'root' AS `name`,-(1) AS `parentId`,'00001' AS `decode` union select `portal_navigator`.`id` AS `id`,`portal_navigator`.`name` AS `name`,`portal_navigator`.`parentId` AS `parentid`,`portal_navigator`.`decode` AS `decode` from `portal_navigator` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;


--
-- Final view structure for view `view_record_resource`
--

/*!50001 DROP VIEW IF EXISTS `view_record_resource`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_record_resource` AS select 0 AS `id`,'root' AS `name`,-(1) AS `parentId`,'00001' AS `decode` union select `dm_record`.`id` AS `id`,`dm_record`.`name` AS `name`,`dm_record`.`parentId` AS `parentId`,`dm_record`.`decode` AS `decode` from `dm_record` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_report_resource`
--

/*!50001 DROP VIEW IF EXISTS `view_report_resource`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_report_resource` AS select 0 AS `id`,'root' AS `name`,-(1) AS `parentId`,'00001' AS `decode` union select `dm_report`.`id` AS `id`,`dm_report`.`name` AS `name`,`dm_report`.`parentId` AS `parentId`,`dm_report`.`decode` AS `decode` from `dm_report` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_role_resource`
--

/*!50001 DROP VIEW IF EXISTS `view_role_resource`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_role_resource` AS select `um_role`.`id` AS `ID`,`um_role`.`parentId` AS `parentId`,`um_role`.`name` AS `name`,`um_role`.`decode` AS `decode` from `um_role` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_roleuser`
--

/*!50001 DROP VIEW IF EXISTS `view_roleuser`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_roleuser` AS select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from ((`um_role` `r` join `um_roleuser` `ru`) join `um_user` `u`) where ((`r`.`id` = `ru`.`roleId`) and (`ru`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and isnull(`ru`.`strategyId`)) union all select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from (((`um_role` `r` join `um_roleuser` `ru`) join `um_user` `u`) join `um_sub_authorize` `s`) where ((`r`.`id` = `ru`.`roleId`) and (`ru`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and (`s`.`id` = `ru`.`strategyId`) and (sysdate() between `s`.`startDate` and `s`.`endDate`) and (`s`.`disabled` <> 1)) union all select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from ((((`um_group` `g` join `um_rolegroup` `rg`) join `um_role` `r`) join `um_groupuser` `gu`) join `um_user` `u`) where ((`g`.`id` = `rg`.`groupId`) and (`rg`.`roleId` = `r`.`id`) and (`g`.`id` = `gu`.`groupId`) and (`gu`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (`g`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and isnull(`rg`.`strategyId`)) union all select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from (((((`um_group` `g` join `um_rolegroup` `rg`) join `um_role` `r`) join `um_groupuser` `gu`) join `um_user` `u`) join `um_sub_authorize` `s`) where ((`g`.`id` = `rg`.`groupId`) and (`rg`.`roleId` = `r`.`id`) and (`g`.`id` = `gu`.`groupId`) and (`gu`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (`g`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and (`s`.`id` = `rg`.`strategyId`) and (sysdate() between `s`.`startDate` and `s`.`endDate`) and (`s`.`disabled` <> 1)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_roleuser4subauthorize`
--

/*!50001 DROP VIEW IF EXISTS `view_roleuser4subauthorize`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE VIEW `view_roleuser4subauthorize` AS select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from ((`um_role` `r` join `um_roleuser` `ru`) join `um_user` `u`) where ((`r`.`id` = `ru`.`roleId`) and (`ru`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and isnull(`ru`.`strategyId`)) union all select `u`.`id` AS `userId`,`r`.`id` AS `roleId` from ((((`um_group` `g` join `um_rolegroup` `rg`) join `um_role` `r`) join `um_groupuser` `gu`) join `um_user` `u`) where ((`g`.`id` = `rg`.`groupId`) and (`rg`.`roleId` = `r`.`id`) and (`g`.`id` = `gu`.`groupId`) and (`gu`.`userId` = `u`.`id`) and (`u`.`disabled` <> 1) and (`r`.`disabled` <> 1) and (`g`.`disabled` <> 1) and (sysdate() between `r`.`startDate` and `r`.`endDate`) and isnull(`rg`.`strategyId`)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;


-- Dump completed on 2016-02-26  9:14:50
