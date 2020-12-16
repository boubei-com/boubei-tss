// 组移动只能在同域下移动、新增一个整枝更新组域的功能

    /* 后台响应数据节点名称 */
    XML_MAIN_TREE = "GroupTree";
    XML_USER_LIST = "SourceList";
 
    XML_USER_INFO = "UserInfo";
    XML_USER_TO_GROUP_TREE = "User2GroupTree";
    XML_USER_TO_GROUP_EXIST_TREE = "User2GroupExistTree";
    XML_USER_TO_ROLE_TREE = "User2RoleTree";
    XML_USER_TO_ROLE_EXIST_TREE = "User2RoleExistTree";

    XML_GROUP_INFO = "GroupInfo";
    XML_GROUP_TO_USER_TREE = "Group2UserTree";
    XML_GROUP_TO_USER_LIST_TREE = "Group2UserListTree";
    XML_GROUP_TO_USER_EXIST_TREE = "Group2UserExistTree";
    XML_GROUP_TO_ROLE_TREE = "Group2RoleTree";
    XML_GROUP_TO_ROLE_EXIST_TREE = "Group2RoleExistTree";
 
    XML_SEARCH_SUBAUTH = "SUBAUTH_RESULT";
    XML_SEARCH_ROLE = "ROLE_RESULT";
    XML_SEARCH_USER = "USER_RESULT";
 
    /* 默认唯一编号名前缀 */
    CACHE_GRID_ROW_DETAIL = "_row_id";
    CACHE_TREE_NODE_DETAIL = "_treeNode_id";
    CACHE_MAIN_TREE = "_tree_id";
 
    /* XMLHTTP请求地址汇总 */
    URL_INIT          = AUTH_PATH + "group/list";
    URL_POPUP_TREE    = AUTH_PATH + "group/list/";
    URL_USER_GRID     = AUTH_PATH + "user/list/";    // user/list/{groupId}/{page}
    URL_USER_DETAIL   = AUTH_PATH + "user/detail/";  // user/detail/{groupId}/{userId}
    URL_GROUP_DETAIL  = AUTH_PATH + "group/detail/"; // group/detail/{parentId}/{id}/{type}
    URL_SAVE_USER     = AUTH_PATH + "user";   // POST
    URL_SAVE_GROUP    = AUTH_PATH + "group";  // POST
    URL_DELETE_GROUP  = AUTH_PATH + "group/"; 
    URL_DEL_USER      = AUTH_PATH + "user/";
    URL_STOP_GROUP    = AUTH_PATH + "group/disable/"; 
    URL_SORT_GROUP    = AUTH_PATH + "group/sort/";
    URL_MOVE_NODE     = AUTH_PATH + "group/move/"; // {id}/{toGroupId}
    URL_STOP_USER     = AUTH_PATH + "user/disable/";
    URL_MOVE_USER     = AUTH_PATH + "user/move/";
    URL_GET_OPERATION = AUTH_PATH + "group/operations/"; 
    URL_GROUP_USERS   = AUTH_PATH + "group/users/";  // {groupId}
    URL_INIT_PASSWORD = AUTH_PATH + "user/initpwd/"; // user/initpwd/{groupId}/{userId}/{password}

    URL_SEARCH_USER   = AUTH_PATH + "user/search";
    URL_SEARCH_ROLE   = AUTH_PATH + "search/roles/";
    URL_SEARCH_SUBAUTH= AUTH_PATH + "search/subauth/";
    URL_SYNC_GROUP    = AUTH_PATH + "group/sync/";
    URL_SYNC_PROGRESS = AUTH_PATH + "group/progress/";  // {code} GET
    URL_CANCEL_SYNC   = AUTH_PATH + "group/progress/";  // {code} DELETE
    
    if(IS_TEST) {
        URL_INIT = "data/group_tree.xml?";
        URL_POPUP_TREE = "data/group_tree.xml?";
        URL_USER_GRID = "data/user_grid.xml?";
        URL_USER_DETAIL = "data/user_detail.xml?";
        URL_GROUP_DETAIL = "data/group_detail.xml?";
        URL_SAVE_USER = "data/_success.xml?";
        URL_SAVE_GROUP = "data/_success.xml?";
        URL_DELETE_GROUP = "data/_success.xml?";
        URL_STOP_GROUP = "data/_success.xml?";
        URL_SORT_GROUP = "data/_success.xml?";
        URL_MOVE_NODE  = "data/_success.xml?";
        URL_STOP_USER = "data/_success.xml?";
        URL_GROUP_USERS = "data/userlist.xml?";
        URL_DEL_USER = "data/_success.xml?";
        URL_INIT_PASSWORD = "data/_success.xml?";
        URL_GET_OPERATION = "data/operation.xml?";
        
        URL_SEARCH_USER    = "data/user_search.xml?";
        URL_SEARCH_ROLE    = "data/user_grid.xml?";
        URL_SEARCH_SUBAUTH = "data/user_grid.xml?";
        URL_SYNC_GROUP    = "data/_progress.xml?";
        URL_SYNC_PROGRESS = "data/_progress.xml?";
        URL_CANCEL_SYNC   = "data/_success.xml?";
    }
 
    function init() {
        initMenus();
        initWorkSpace();
        initEvents();

        loadInitData();

        bindTreeSearch("sk1", "page2Tree");
        bindTreeSearch("sk2", "page3Tree");
        bindTreeSearch("sk3", "page4Tree");
        bindTreeSearch("sk4", "page4Tree2");
    }

    function initMenus(){
        initTreeMenu();
        initGridMenu();
    }
    
    /* 是否为主用户组 */
    function isMainGroup(treeNode) {
        return (treeNode ? treeNode.getAttribute("groupType") : getTreeAttribute("groupType")) == "1";
    }
    
    /* 是否自注册用户组节点 */
    function isSelfRegisterGroup(id){
        if( id == null ) {
            var treeNode = $.T("tree").getActiveTreeNode();
            if( treeNode ) {
                id = treeNode.id;
            }            
        }
        return ("-7"==id );
    }
    
    function editable(includeRoot, excludeCustomer) {
        var isRoot = false, 
            treeNodeID = getTreeNodeId()
            isCustomerGroup = getTreeNodeName() === 'customer';  

        if(!includeRoot) {
            var rootList = $.T("tree").rootList;
            isRoot = rootList[0].id == treeNodeID;
        }
        return !isTreeRoot() && treeNodeID > 0 && getOperation("2") && !isRoot && (excludeCustomer || !isCustomerGroup);
    }

    function initTreeMenu(){
        var item1 = {
            label:"停用",
            callback:function() { stopOrStartTreeNode("1", URL_STOP_GROUP); },
            icon:"icon icon-triangle-down",
            visible:function(){return editable() && !isTreeNodeDisabled();}
        }
        var item2 = {
            label:"启用",
            callback:function() { stopOrStartTreeNode("0", URL_STOP_GROUP); },
            icon:"icon icon-triangle-up",
            visible:function(){return editable() && isTreeNodeDisabled();}
        }
        var item3 = {
            label:"编辑",
            callback:editGroupInfo,
            icon:"icon icon-pencil",
            visible:function() { return editable(true) || getTreeNodeId() == -8;}
        }
        var item4 = {
            label:"删除",
            callback:function() { delTreeNode(URL_DELETE_GROUP); },
            icon:"icon icon-x",
            visible:function() { return editable(); }
        }
        var item6 = {
            label:"新建用户组",
            callback:addNewGroup,
            visible:function(){ 
                var nodeId = getTreeNodeId();
                return !isSelfRegisterGroup() && (nodeId > 0 || nodeId == -8 || nodeId == -3) && getOperation("2") && getTreeNodeName() != 'customer'; 
            }
        }
        var item7 = {
            label:"新建用户",
            callback:addNewUser,
            icon:"icon icon-plus",
            visible:function(){ return !isTreeRoot() && isMainGroup() && editable(true, true); }
        }
        var item8 = {
            label:"浏览用户",
            callback:function() { showUserList(); },
            icon:"icon icon-list-ordered",
            visible:function(){ return !isTreeRoot() && getTreeNodeId() != -2 && getTreeNodeId() != -3 && getOperation("1"); }
        }
        var item9 = {
            label:"搜索用户...",
            callback:searchUser,
            icon:"icon icon-search",
            visible:function() { return !isTreeRoot() && isMainGroup() && getOperation("1"); }
        }
        var item11 = {
            label:"移动到...",
            callback:moveNodeTo,
            icon:"icon icon-arrow-right",          
            visible:function() {return editable();}
        }

        var subitem12_1 = {
            label:"初始化密码...",
            callback:resetPassword,
            icon:"icon icon-lock",
            visible:function() { return isMainGroup() && editable(true) || getTreeNodeId() == -8 || getTreeNodeId() == -9; }
        }
        var subitem12_2 = {
            label:"用户同步",
            icon:"icon icon-sync",
            callback:function() { syncGroup(); },
            visible:function() { return (isMainGroup() && editable(true) || getTreeNodeId() == -8 ) && getTreeAttribute("syncable") == "true"; }
        }
        var subitem12_4 = {
            label:"综合查询",
            visible:function() { return isMainGroup() && editable(true) || getTreeNodeId() == -8 || getTreeNodeId() == -9; }
        }
        var subitem12_4_1 = {
            label:"用户角色",
            callback:generalSearchRole
        }
        var subitem12_4_2 = {
            label:"用户转授",
            callback:generalSearchSubauth
        }

        var submenu12_4 = new $.Menu();
        submenu12_4.addItem(subitem12_4_1);
        submenu12_4.addItem(subitem12_4_2);
        subitem12_4.submenu = submenu12_4;
 
        var menu1 = new $.Menu();
        menu1.addItem(item3);
        menu1.addItem(item1);
        menu1.addItem(item2);
        menu1.addItem(item4);
        menu1.addItem(item11);
        menu1.addItem(item6);
        menu1.addSeparator();
        menu1.addItem(item7);
        menu1.addItem(item8);
        menu1.addItem(item9);
        menu1.addSeparator();
        menu1.addItem(subitem12_1);
        menu1.addItem(subitem12_2);
        menu1.addItem(subitem12_4);

        if(userCode === 'Admin') {
            menu1.addItem(createPermissionMenuItem("1"));
        }

        $1("tree").contextmenu = menu1;
    }
 
    function initGridMenu() {
        var item1 = {
            label:"停用",
            callback:function() { stopOrStartUser("1"); },
            icon:"icon icon-triangle-down", 
            visible:function() { return getUserOperation("2") && "0" == getUserState(); }
        }
        var item2 = {
            label:"启用",
            callback:function() { stopOrStartUser("0"); },
            icon:"icon icon-triangle-up", 
            visible:function() { return getUserOperation("2") && "1" == getUserState(); }
        }
        var item3 = {
            label:"编辑",
            callback:editUserInfo,
            icon:"icon icon-pencil", 
            visible:function() { return getUserOperation("2") || getGroupOperation(); }
        }
        var item5 = {
            label:"移动到其它组",
            callback:move2otherGroup,
            icon:"icon icon-arrow-right", 
            visible:function() { return getUserOperation("2"); }
        }
        var item4 = {
            label:"删除",
            callback: function() { delelteUser(); },
            icon:"icon icon-x", 
            visible:function() { return getUserOperation("2"); }
        }
        var item5 = {
            label:"完全删除",
            callback: function() { deepDelelteUser(); },
            visible:function() { return userCode === 'Admin'; }
        }
        /* 登录过的用户不能被删除，只能被停用。
           防止域管理员把域下用户删除，导致删除用户创建的数据表记录无法被查询到，甚至会可能被其它域下后期注册的同名用户吸走了） */

        var menu1 = new $.Menu();
        menu1.addItem(item1);
        menu1.addItem(item2);
        menu1.addItem(item3);
        menu1.addItem(item4);
        menu1.addItem(item5);
 
        $1("grid").contextmenu = menu1;

        $1("grid").onRightClickRow = function() {
            $1("grid").contextmenu.show(event.clientX, event.clientY);
        }   
    }

    function delelteUser() {
        var grid = $.G("grid");
        var userName  = grid.getColumnValue("userName");

        $.confirm("您确定要删除用户【" +userName+ "】吗？", "删除确认", function(){
            var grid = $.G("grid");
            var userID  = grid.getColumnValue("id");
            var groupId = grid.getColumnValue("groupId");
            if( userID ) {
                $.ajax({
                    url : URL_DEL_USER + groupId + "/" + userID,
                    method : "DELETE",
                    waiting: true, 
                    onsuccess : function() { 
                        grid.deleteSelectedRow();
                    }
                }); 
            }
        });
    }

    function deepDelelteUser() {
        var grid = $.G("grid");
        var userName  = grid.getColumnValue("userName");

        $.confirm("您确定要完全删除用户【" +userName+ "】吗？", "删除确认", function(){
            var grid = $.G("grid");
            var userID  = grid.getColumnValue("id");
            var groupId = grid.getColumnValue("groupId");
            if( userID ) {
                $.ajax({
                    url : URL_DEL_USER + "deeprm/" + groupId + "/" + userID,
                    method : "DELETE",
                    waiting: true, 
                    ondata : function() { 
                        grid.deleteSelectedRow();
                    }
                }); 
            }
        });
    }

    function removeBatch() {
        var grid = tssJS.G("grid");
        var ids  = grid.getCheckedRowsValue("id");
        var groupIds = grid.getCheckedRowsValue("groupId");
        if(!ids || ids.length == 0) {
            return alert("您没有选中任何用户记录，请勾选后再进行批量删除。");
        }
        tssJS.confirm("您确定要批量删除选中用户吗？", "批量删除确认", function(){
            ids.each(function(i, userID){
                var groupId = groupIds[i];
                $.ajax({
                    url : URL_DEL_USER + groupId + "/" + userID,
                    method : "DELETE",
                    headers : { "noAlert": true },
                    waiting: true, 
                    onsuccess : function() { 
                        if( i == ids.length - 1 ) {
                            showUserList(groupId);
                        }
                    }
                }); 
            });
        });
    }
 
    function loadInitData(defaultOpenGroup) {
        var onresult = function(){
            var groupTreeNode = this.getNodeValue(XML_MAIN_TREE);
            $.cache.XmlDatas[CACHE_MAIN_TREE] = groupTreeNode;
            var tree = $.T("tree", groupTreeNode);
            var rootList = tree.rootList;
            if(rootList.length) {
                var rootId = rootList[0].id;
                if(rootId == '-2') {
                    rootId = -7;
                }

                defaultOpenGroup = defaultOpenGroup || rootId;
                showUserList(defaultOpenGroup);

                var defaultOpenNode = tree.getTreeNodeById( defaultOpenGroup );
                defaultOpenNode && getTreeOperation(defaultOpenNode); // 获取对此节点的权限信息
            }

            tree.onTreeNodeActived = function(ev){ onTreeNodeActived(ev); }
            tree.onTreeNodeDoubleClick = function(ev){
                var treeNode = getActiveTreeNode();
                getTreeOperation(treeNode, function(_operation) {
                    if(treeNode.id != -2) { // 防止浏览到Admin和匿名用户
                        showUserList();
                    }
                });
            }
            tree.onTreeNodeMoved = function(ev){ sort(ev); }
            tree.onTreeNodeRightClick = function(ev){ onTreeNodeRightClick(ev, true); }
        }
        
        $.ajax({url : URL_INIT, method : "GET", onresult : onresult, waiting: true});
    }
 
    function sort(ev) {
        var movedNode  = ev.dragNode;
        var movedNodeID = movedNode.id;     
        if("-2" == movedNodeID || "-3" == movedNodeID ) {
            alert("不能移动此节点!");
            return;
        }

        sortTreeNode(URL_SORT_GROUP, ev);
    }

    function moveNodeTo() {
        var tree = $.T("tree");
        var treeNode = tree.getActiveTreeNode();
        var id  = treeNode.id;
        var pId = treeNode.parent.id;
        var groupType = treeNode.getAttribute("groupType");

        var params = {id:id, parentID: pId};
        popupTree(URL_POPUP_TREE + groupType, "GroupTree", params, function(target) {
            if(target.id == -1 || target.id == -7) {
                return alert("不能移动到根目录或自注册用户组下面");
            }
            moveTreeNode(tree, id, target.id);
        });
    }

    function move2otherGroup() {
        var userId  = $.G("grid").getColumnValue("id");   
        var groupId = $.G("grid").getColumnValue("groupId");  

        popupTree(URL_INIT, "GroupTree", {}, function(target) {
            if(target.id == groupId) {
                return alert("用户已在此目标组织下");
            }
            $.post(URL_MOVE_USER + userId + "/" + target.id, {}, function(result) {
                showUserList(groupId);
            });
        });
    }

    function moveBatch() {
        var grid = tssJS.G("grid");
        var ids  = grid.getCheckedRowsValue("id");
        if(!ids || ids.length == 0) {
            return alert("您没有选中任何用户记录，请勾选后再进行批量移动。");
        }

        popupTree(URL_INIT, "GroupTree", {}, function(target) {
            var count = 0;
            ids.each(function(i, userId){
                $.post(URL_MOVE_USER + userId + "/" + target.id, {}, function(result) {
                    count++;
                    if (count == ids.length) {
                        showUserList( target.id );
                    }
                });
            });
        });
    }
 
    /* 初始化密码  */
    function resetPassword(){
        var treeNode = $.T("tree").getActiveTreeNode();
        $.prompt("请输入新密码", "初始化【" + treeNode.name + "】的密码", function(value) {
            if ( $.isNullOrEmpty(value) ) return alert("密码不能为空。");
            
            $.ajax({
                url : URL_INIT_PASSWORD + treeNode.id + "/0", 
                params : {"password": value.trim()},
                waiting: true
            });
        });
    }
 
    function editGroupInfo(newGroupID) { 
        var isAddGroup = (DEFAULT_NEW_ID == newGroupID);
    
        var treeNode = $.T("tree").getActiveTreeNode();
        var treeID   = isAddGroup ? newGroupID : treeNode.id;
        var treeName = isAddGroup ? "用户组" : treeNode.name;      
        var parentID = isAddGroup ? treeNode.id : treeNode.attrs.parentId;       
        var groupType = treeNode.getAttribute("groupType");

        var phases = [];
        phases[0] = {page:"page1",label:"基本信息"};
        if( isMainGroup() ) { 
            phases[1] = {page:"page3",label:"分配角色"};
        } else {
            phases[1] = {page:"page4",label:"用户列表"};
            phases[2] = {page:"page3",label:"分配角色"};
        }       
        
        var callback = {};
        callback.onTabChange = function() {
            setTimeout(function() {
                loadGroupDetailData(treeID, parentID, groupType);
            }, TIMEOUT_TAB_CHANGE);

             $1("ws").style.display = "block";
        };
        callback.onTabClose = onTabClose;
        
        var inf = {};
        inf.defaultPage = "page1";
        inf.callback = callback;
        inf.label = (isAddGroup ? OPERATION_ADD : OPERATION_EDIT).replace(/\$label/i, treeName);
        inf.SID = CACHE_TREE_NODE_DETAIL + treeID;
                    
        inf.phases = phases;
        var tab = ws.open(inf);         
    }
 
    function addNewGroup() {
        editGroupInfo(DEFAULT_NEW_ID);
    }
      
    /*
     *  树节点数据详细信息加载数据
     *  参数： string:treeID               树节点id
                string:parentID             父节点id
     */
    function loadGroupDetailData(treeID, parentID, groupType) {
        var request = new $.HttpRequest();
        request.url = URL_GROUP_DETAIL + parentID + "/" + treeID + "/" + groupType;
        request.onresult = function(){
            var groupInfoNode = this.getNodeValue(XML_GROUP_INFO);
            var group2UserTreeNode = $.cache.XmlDatas[CACHE_MAIN_TREE].cloneNode(true);
            var group2UserGridNode = this.getNodeValue(XML_GROUP_TO_USER_EXIST_TREE);
            var group2RoleTreeNode = this.getNodeValue(XML_GROUP_TO_ROLE_TREE);
            var group2RoleGridNode = this.getNodeValue(XML_GROUP_TO_ROLE_EXIST_TREE);
 
            $.cache.XmlDatas[treeID + "." + XML_GROUP_INFO] = groupInfoNode;
            disableTreeNodes(group2RoleTreeNode, "treeNode[isGroup='1']");
                
            var page1Form = $.F("page1Form", groupInfoNode);
            attachReminder(page1Form.box.id, page1Form);
            if( treeID != -8) {
                var el1 = $1("syncConfig"), el2 = $1("fromGroupId");
                el1 && $(el1.parentNode.parentNode).hide();
                el2 && $(el2.parentNode.parentNode).hide()
            }
 
            var page3Tree  = $.T("page3Tree",  group2RoleTreeNode);
            var page3Tree2 = $.T("page3Tree2", group2RoleGridNode);
 
            if( !isMainGroup() ) { // 辅助用户组
                var page4Tree3 = $.T("page4Tree3", group2UserGridNode);
                var page4Tree  = $.T("page4Tree",  group2UserTreeNode);
                
                page4Tree.onTreeNodeDoubleClick = function(ev) {
                    var treeNode = page4Tree.getActiveTreeNode();
                    $.ajax({
                        url : URL_GROUP_USERS + treeNode.id,
                        onresult : function() { 
                            var sourceListNode = this.getNodeValue(XML_GROUP_TO_USER_LIST_TREE);
                            $.T("page4Tree2", sourceListNode);
                        }
                    }); 
                }               
            }
            
            // 设置翻页按钮显示状态
            $1("page4BtPrev").style.display = "";
            $1("page3BtPrev").style.display = "";
            $1("page1BtNext").style.display = "";
            $1("page4BtNext").style.display = "";

            // 设置保存按钮操作
            $1("page1BtSave").onclick = $1("page4BtSave").onclick = $1("page3BtSave").onclick = function(){
                saveGroup(treeID, parentID, groupType);
            }

            // 设置添加按钮操作
            $1("page3BtAdd").onclick = function() {
                addTreeNode(page3Tree, page3Tree2);
            }

            // 设置添加按钮操作
            $1("page4BtAdd").onclick = function(){
                addTreeNode($.T("page4Tree2"), page4Tree3);
            }

            // 设置删除按钮操作
            $1("page3BtDel").onclick = function(){
                 removeTreeNode(page3Tree2);
            }
            $1("page4BtDel").onclick = function(){
                 removeTreeNode(page4Tree3);
            }
        }
        request.send();
    }
 
    /* 保存用户组 */
    function saveGroup(treeID, parentID, groupType){
        var page1Form = $.F("page1Form");
        if( !page1Form.checkForm() ) {
            ws.switchToPhase("page1");
            return;
        }

        var request = new $.HttpRequest();
        request.url = URL_SAVE_GROUP;
 
        //用户组基本信息
        var groupInfoNode = $.cache.XmlDatas[treeID + "." + XML_GROUP_INFO];
        var groupInfoDataNode = groupInfoNode.querySelector("data");
        request.setFormContent(groupInfoDataNode);

        // 用户组对用户
        if( !isMainGroup() ) {
            var group2UserIDs = $.T("page4Tree3").getAllNodeIds();
            request.addParam(XML_GROUP_TO_USER_EXIST_TREE, group2UserIDs.join(","));
        }

        // 用户组对角色
        var group2RoleIDs = $.T("page3Tree2").getAllNodeIds();
        request.addParam(XML_GROUP_TO_ROLE_EXIST_TREE, group2RoleIDs.join(","));
        
        // 同步按钮状态
        syncButton([$1("page1BtSave"), $1("page4BtSave"), $1("page3BtSave")], request);

        request.onresult = function() {
            afterSaveTreeNode.call(this, treeID, parentID);
        }
        request.onsuccess = function() {
            afterSaveTreeNode(treeID, page1Form);
        }
        request.send();
    }
    
    /* 同步用户组 */
    function syncGroup() {
        var treeNode = $.T("tree").getActiveTreeNode();
        var treeNodeID = treeNode.id;

        var onresult = function() {
            var data = this.getNodeValue("ProgressInfo");
            var progress = new $.Progress(URL_SYNC_PROGRESS, data, URL_CANCEL_SYNC);

            // 完成同步后，重新加载树，打开同步节点，并显示其下用户列表
            progress.oncomplete = function() {
                loadInitData(treeNodeID); 
                treeNode.openNode();                
            }
            progress.start();
        }

        $.ajax({url : URL_SYNC_GROUP + treeNodeID, onresult : onresult});
    }
    
    function getUserOperation(code) {
        var rowID   = $.G("grid").getColumnValue("id");   
        if(parseInt(rowID) < 0) return;
        
        var groupId   = $.G("grid").getColumnValue("groupId");  
        var groupNode = $.T("tree").getTreeNodeById(groupId);
        var _operation = groupNode.getAttribute("_operation");
        return checkOperation(code, _operation);
    }

    function getGroupOperation() {
        var groupId = $.G("grid").getColumnValue("groupId");   
        var groupNode = $.T("tree").getTreeNodeById(groupId);
        if( groupNode.getAttribute("_operation") ) return;

        $.ajax({
            url : URL_GET_OPERATION + groupId,
            onresult : function() {
                _operation = this.getNodeValue(XML_OPERATION);
                groupNode.setAttribute("_operation", _operation);
            }
        }); 
    }
 
    /* 显示用户列表 */
    function showUserList(groupId) {
        groupId = groupId || getTreeNodeId();

        var groupNode = $.T("tree").getTreeNodeById( groupId );
        var _operation = groupNode.getAttribute("_operation");
        var opts = (_operation||'').split(",");

        var groupName = groupNode.name;
        var groupType = groupNode.getAttribute("groupType");
        
        if( groupId > 0 && groupType == "1" && opts.contains("2") ) {
            $("#x1").attr("data-group", groupId);
            $("#gridTitle .tssbutton").show();
        } else {
            $("#gridTitle .tssbutton").hide();
            $("#btn_account, #btn_mv").show();
        }
        $("#x1").text(groupName);

        $.showGrid(URL_USER_GRID + groupId, XML_USER_LIST, editUserInfo);
    }
 
    function addNewUser(topBtn) {
        var groupId;
        if( topBtn === true ) {
            groupId = $("#x1").attr("data-group");
            if( !groupId ) return;
        } else {
            groupId = getActiveTreeNode().id;
        }
        loadUserInfo(OPERATION_ADD, DEFAULT_NEW_ID, "用户", groupId);        
    }
 
    function editUserInfo() {
        var rowID   = $.G("grid").getColumnValue("id");   
        var rowName = $.G("grid").getColumnValue("userName");  
        var groupId = $.G("grid").getColumnValue("groupId");   
        if( getUserOperation("2") ) {
            loadUserInfo(OPERATION_EDIT, rowID, rowName, groupId);
        } else {
            var groupNode = $.T("tree").getTreeNodeById(groupId);
            if( groupNode.getAttribute("_operation") ) return;

            $.ajax({
                url : URL_GET_OPERATION + groupId,
                onresult : function() {
                    _operation = this.getNodeValue(XML_OPERATION);
                    groupNode.setAttribute("_operation", _operation);

                    if ( getUserOperation("2") ) {
                        loadUserInfo(OPERATION_EDIT, rowID, rowName, groupId);
                    }
                }
            }); 
        }
    }
    
    function loadUserInfo(operationName, rowID, rowName, groupId) {
        var phases = [];
        phases[0] = {page:"page1", label:"基本信息"};
        phases[1] = {page:"page2", label:"所属组织"};
        phases[2] = {page:"page3", label:"分配角色"};

        var callback = {};
        callback.onTabChange = function() {
            setTimeout(function() {
                loadUserDetailData(rowID, groupId);
            }, TIMEOUT_TAB_CHANGE);

            $1("ws").style.display = "block";
        };
        callback.onTabClose = onTabClose;

        var inf = {};
        inf.label = operationName.replace(/\$label/i, rowName || "用户");
        inf.SID = CACHE_GRID_ROW_DETAIL + rowID;
        inf.defaultPage = "page1";
        inf.phases = phases;
        inf.callback = callback;
        ws.open(inf);
    }

    var onTabClose = function() {
        if( ws.noTabOpend() ) {
             $1("ws").style.display = "none";
        }      
    }
 
    function loadUserDetailData(userID, groupId) {
        var currentGroup;
        var request = new $.HttpRequest();
        request.url = URL_USER_DETAIL + (groupId || 0) + "/" + userID;
        request.onresult = function(){
            var userInfoNode = this.getNodeValue(XML_USER_INFO);
            var user2GroupExistTreeNode = this.getNodeValue(XML_USER_TO_GROUP_EXIST_TREE);
            var user2GroupTreeNode = $.cache.XmlDatas[CACHE_MAIN_TREE].cloneNode(true);
            var user2RoleTreeNode = this.getNodeValue(XML_USER_TO_ROLE_TREE);
            var user2RoleGridNode = this.getNodeValue(XML_USER_TO_ROLE_EXIST_TREE);
            
            // 过滤掉 系统级用户组 和 角色组
            disableTreeNodes(user2GroupTreeNode, "treeNode[id='-2']");
            disableTreeNodes(user2GroupTreeNode, "treeNode[id='-3']");
            disableTreeNodes(user2RoleTreeNode,  "treeNode[isGroup='1']");
 
            $.cache.XmlDatas[userID + "." + XML_USER_INFO] = userInfoNode;
            
            var page1Form = $.F("page1Form", userInfoNode);
            attachReminder(page1Form.box.id, page1Form);

            if( !$("#loginName").value() ) {
                page1Form.setFieldEditable("loginName", "true"); 
            }
            if( userCode != 'Admin' ) {
                page1Form.setFieldEditable("authToken", "false"); 
            }
            
            var page3Tree  = $.T("page3Tree",  user2RoleTreeNode);
            var page3Tree2 = $.T("page3Tree2", user2RoleGridNode);
            var page2Tree  = $.T("page2Tree",  user2GroupTreeNode);
            var page2Tree2 = $.T("page2Tree2", user2GroupExistTreeNode);
            
            page2Tree2.groupType = "1"; // 标记当前page2Tree2是主(辅助)用户组

            // 设置翻页按钮显示状态
            $1("page2BtPrev").style.display = "";
            $1("page3BtPrev").style.display = "";
            $1("page1BtNext").style.display = "";
            $1("page2BtNext").style.display = "";

            //设置保存按钮操作
            $1("page1BtSave").onclick = $1("page2BtSave").onclick = $1("page3BtSave").onclick = function(){
                saveUser(userID, groupId);
            }

            // 设置添加按钮操作
            $1("page2BtAdd").onclick = function(){
                var mainGroupCount = 0;
                var selectedNodes = page2Tree.getCheckedNodes(false);
                selectedNodes.each(function(i, curNode) {
                    var groupType = curNode.getAttribute("groupType");
                    if( groupType == "1" ) {
                        mainGroupCount ++;
                    }
                });
                if(mainGroupCount > 1){
                    $(page2Tree.el).notice("一个用户只能属于一个主用户组，请选择唯一的主用户组。");
                    return;
                }

                addTreeNode(page2Tree, page2Tree2, function(treeNode){
                    var result = {
                        "error":false,
                        "message":"",
                        "stop":true
                    };
                    var groupType = treeNode.getAttribute("groupType");
                    if( groupType == "1" ) {
                        // 先主动移除当前主用户组，如果有的话
                        currentGroup = currentGroup || groupId; 

                        var currentGroupNode = page2Tree2.getTreeNodeById(currentGroup);
                        if( currentGroupNode && currentGroupNode.getAttribute("groupType") == '1') {
                            page2Tree2.removeTreeNode(currentGroupNode);
                        }

                        if(hasSameAttributeTreeNode(page2Tree2, "groupType", groupType)){
                            result.error = true;
                            result.message = "一个用户只能属于一个主用户组，请先移除当前的主用户组。";
                            result.stop = true;
                            return result;
                        }

                        currentGroup = treeNode.id;
                    }

                    return result;
                });
            }
            $1("page3BtAdd").onclick = function() {
                addTreeNode(page3Tree, page3Tree2, function(treeNode) {
                    var result = {
                        "error": false,
                        "message": "",
                        "stop": true
                    };
                    if( treeNode.getAttribute("isGroup") == "1"){
                        result.error = true;
                        result.message = null;
                        result.stop = false;
                    }
                    return result;
                });
            }

            // 设置删除按钮操作
            $1("page2BtDel").onclick = function(){
                removeTreeNode(page2Tree2);
            }
            $1("page3BtDel").onclick = function(){
                removeTreeNode(page3Tree2);
            }
        }
        request.send();
    }

    function saveUser(userID, groupId){
        var page1Form = $.F("page1Form");
        if( !page1Form.checkForm() ) {
            ws.switchToPhase("page1");
            return;
        }

        // 校验用户对组page2Tree2数据有效性
        var page2Tree2 = $.T("page2Tree2");
        if( !hasSameAttributeTreeNode(page2Tree2, 'groupType', '1') ) {
            ws.switchToPhase("page2");
           
            $(page2Tree2.el).notice("至少要属于一个主用户组，请选择。");
            return;
        }

        var request = new $.HttpRequest();
        request.url = URL_SAVE_USER;
 
        // 用户基本信息
        var userInfoNode = $.cache.XmlDatas[userID + "." + XML_USER_INFO];
        var userInfoDataNode = userInfoNode.querySelector("data");
        request.setFormContent(userInfoDataNode);
 
        //用户对用户组
        var user2GroupIDs = $.T("page2Tree2").getAllNodeIds();
        request.addParam(XML_USER_TO_GROUP_EXIST_TREE, user2GroupIDs.join(","));

        // 主用户组id
        var mainGroupId;
        page2Tree2.getAllNodes().each(function(i, node){
            if(node.getAttribute('groupType') == '1') {
                mainGroupId = node.id;
            }
        });
        request.addParam("mainGroupId", mainGroupId);

        //用户对角色
        var user2RoleIDs = $.T("page3Tree2").getAllNodeIds();
        request.addParam(XML_USER_TO_ROLE_EXIST_TREE, user2RoleIDs.join(","));
        
        //同步按钮状态
        syncButton([$1("page1BtSave"), $1("page2BtSave"), $1("page3BtSave")], request);

        request.onsuccess = function(){
            detachReminder(page1Form.box.id);

            ws.closeActiveTab();

            // 如果当前grid显示为此用户所在组，则刷新grid
            var gridGroupId = $.G("grid").getColumnValue("groupId");
            showUserList(groupId || gridGroupId || -7);
        }
        request.send();
    }
 
    /* 获取用户状态 */
    function getUserState(){
        return getUserAttr("disabled"); 
    }

    function getUserAttr(attr){
        return $.G("grid").getColumnValue(attr); 
    }
 
    function stopOrStartUser(state) {
        var userID  = $.G("grid").getColumnValue("id");
        var groupId = $.G("grid").getColumnValue("groupId");  
        if(userID == null) return;

        $.ajax({
            url : URL_STOP_USER + groupId + "/" + userID + "/" + state,
            waiting: true,
            onsuccess : function() {  // 移动树节点                  
                // 成功后设置状态
                $.G("grid").modifySelectedRow("disabled", state);
                $.G("grid").modifySelectedRow("icon", "images/user_" + state + ".gif");
                
                if (state == "0") { // 启用组
                    var treeNode = $.T("tree").getTreeNodeById(groupId);
                    if(treeNode) {
                        refreshTreeNodeState(treeNode, "0");
                    }
                }
            }
        });
    }  

    function assignAccount() {
        $.openIframePanel("aa", "分配账号", 1028, 600, "/tss/more/pay/account.html?assignAccount=true", true);
    }
 
    function searchUser(){
        var treeNode = $.T("tree").getActiveTreeNode();
        var treeID   = treeNode.id;
        var treeName = treeNode.name;
        
        $.prompt("请输入查询条件", "查询【" + treeName + "】下用户", function(value) {
            if ( $.isNullOrEmpty(value) ) return alert("条件不能为空。");
            
            var params = {"groupId": treeID, "searchStr": value};
            $("#x1").text( treeName + " / " + value ).attr("data-group", null);
            $("#gridTitle .tssbutton").hide();
            $("#btn_account, #btn_mv").show();

            $.showGrid(URL_SEARCH_USER, XML_USER_LIST, editUserInfo, "grid", 1, params);
        });
    }
 
    /* 综合查询(用户角色查询) */
    function generalSearchRole(){
        var treeNode = getActiveTreeNode();
        var title = "查看【" + treeNode.name +"】组下用户的角色信息";
        var url = URL_SEARCH_ROLE + treeNode.id;
        popupGrid(url, XML_SEARCH_ROLE, title);
    }
    
    /* 综合查询(用户转授查询) */
    function generalSearchSubauth() {
        var treeNode = getActiveTreeNode();
        var title = "查看组【" + treeNode.name +"】下用户的转授角色信息";
        var url = URL_SEARCH_SUBAUTH + treeNode.id;
        popupGrid(url, XML_SEARCH_SUBAUTH, title);
    }
 
    window.onload = init;