/* 后台响应数据节点名称 */
XML_MAIN_TREE = "MenuTree";
XML_MENU_INFO = "MenuInfo";

/* 默认唯一编号名前缀 */
CACHE_MENU_DETAIL = "_menu_";

/* XMLHTTP请求地址汇总 */
URL_SOURCE_TREE      = AUTH_PATH + "navigator/list";
URL_SOURCE_DETAIL    = AUTH_PATH + "navigator/";
URL_SOURCE_SAVE      = AUTH_PATH + "navigator";
URL_DELETE_NODE      = AUTH_PATH + "navigator/";
URL_SORT_NODE        = AUTH_PATH + "navigator/sort/";
URL_MOVE_NODE        = AUTH_PATH + "navigator/move/";
URL_STOP_NODE        = AUTH_PATH + "navigator/disable/";
URL_GET_OPERATION    = AUTH_PATH + "navigator/operations/";
URL_GET_PS_TREE      = AUTH_PATH + "navigator/pstree/";
URL_GET_CHANNEL_TREE = AUTH_PATH + "channel/list";
URL_FRESH_MENU_CACHE = AUTH_PATH + "navigator/cache/";
URL_GET_MENU_TREE    = AUTH_PATH + "navigator/tree/";
URL_EXPORT_MENU      = AUTH_PATH + "export/menu/";

if(IS_TEST) {
    URL_SOURCE_TREE      = "data/menu_tree.xml?";
    URL_SOURCE_DETAIL    = "data/menu_detail.xml?";
    URL_SOURCE_SAVE      = "data/_success.xml?";
    URL_DELETE_NODE      = "data/_success.xml?";
    URL_SORT_NODE        = "data/_success.xml?";
    URL_MOVE_NODE        = "data/_success.xml?";
    URL_STOP_NODE        = "data/_success.xml?";
    URL_GET_OPERATION    = "data/_operation.xml?";
    URL_GET_PS_TREE      = "data/structure_tree2.xml?";
    URL_GET_CHANNEL_TREE = "data/channel_tree.xml?";
    URL_FRESH_MENU_CACHE = "data/_success.xml?";
    URL_GET_MENU_TREE    = "data/menu_tree.xml?";
}

function init() {
    initMenus();
    initWorkSpace(false);
    initEvents();

    loadInitData();
}

function initMenus() {
    var item1 = {
        label:"新建菜单",
        icon:"icon icon-plus",
        callback: function() { addNewMenu("1", "菜单"); },
        visible: function() {return "1"==getTreeNodeType() && getOperation("2");}
    }
    var item2 = {
        label:"删除",
        callback: function() { delTreeNode() },
        icon:"icon icon-x",
        visible: function() {return getOperation("2");}
    }
    var item3 = {
        label:"编辑",
        callback:editTreeNode,
        icon:"icon icon-pencil",
        visible: function() {return getOperation("2");}
    }
    var item4 = {
        label:"移动到...",
        callback:moveTo,
        icon:"icon icon-arrow-right",
        visible: function() {return "1" != getTreeNodeType() && getOperation("2");}
    }
    var item6 = {
        label:"刷新菜单缓存",
        callback:flushMenuCache,
        visible: function() {return "1" == getTreeNodeType() && getOperation("2");}
    }
    var item7 = {
        label:"启用",
        callback: function() { stopOrStartTreeNode("0"); },
        icon:"icon icon-triangle-up",
        visible:function() {return isTreeNodeDisabled();}
    }
    var item8 = {
        label:"停用",
        callback: function() { stopOrStartTreeNode("1"); },
        icon:"icon icon-triangle-down",
        visible:function() {return !isTreeNodeDisabled();}
    }

    var item12 = {
        label:"新建普通链接",
        callback: function() { addNewMenu("4", "普通链接"); },
        visible: function() {return getOperation("2");}
    }
    var item13 = {
        label:"新建按钮",
        callback: function() { addNewMenu("2", "按钮"); },
        visible: function() {return getOperation("2");}
    }
    var item5 = {
        label:"新建门户导航",
        visible: function() {return getOperation("2") && !$.isNullOrEmpty(getTreeAttribute("portalId"));}
    }

    var submenu = new $.Menu();
    subItem3 = {
        label:"门户内部链接",
        callback: function() { addNewMenu("3", "内部链接"); }
    }
    subItem6 = {
        label:"定制脚本跳转",
        callback: function() { addNewMenu("6", "脚本跳转"); }
    }
    subItem7 = {
        label:"CMS栏目链接",
        callback: function() { addNewMenu("7", "栏目链接"); }
    }

    submenu.addItem(subItem3);
    submenu.addItem(subItem7);
    submenu.addItem(subItem6);
    item5.submenu = submenu;

    var menu1 = new $.Menu();
    menu1.addItem(item3);
    menu1.addItem(item6);
    menu1.addItem(item4);
    menu1.addItem(item7);
    menu1.addItem(item2);
    menu1.addItem(item8);
    menu1.addSeparator();
    menu1.addItem(item1);
    menu1.addItem(item5);
    menu1.addItem(item12);
    menu1.addItem(item13);

    var item14 = {
        label: "导出菜单列表",
        callback: exportMenu,
        visible: function() { return getOperation("2"); }
    }
    var item15 = {
        label: "导入菜单列表",
        callback: importMenu,
        visible: function() { return getOperation("2"); }
    }
    menu1.addSeparator();
    menu1.addItem(item14);
    menu1.addItem(item15);

    menu1.addItem(createPermissionMenuItem("5"));

    $1("tree").contextmenu = menu1;
}

function getTreeNodeType() {
    return getTreeAttribute("type");
}

function loadInitData() {
    $.ajax({
        url: URL_SOURCE_TREE,
        onresult: function() {
            var tree = $.T("tree", this.getNodeValue(XML_MAIN_TREE));
            tree.onTreeNodeMoved = function(ev) { sortTreeNode(URL_SORT_NODE, ev); }
            tree.onTreeNodeRightClick = function(ev) { onTreeNodeRightClick(ev, true); }
            tree.onTreeNodeDoubleClick = function(ev) { 
                var treeNode = getActiveTreeNode();
                getTreeOperation(treeNode, function(_operation) {
                    var hasPermission = checkOperation("2", _operation);
                    if( hasPermission ) {
                        editTreeNode();            
                    }
                });
            }
        }
    });
}

function addNewMenu(type, typeName) {
    var treeName = typeName || ("菜单" + type);
    var treeID = DEFAULT_NEW_ID;
    
    var tree = $.T("tree");
    var treeNode = tree.getActiveTreeNode();
    var parentId = treeNode.id;
    var portalId = treeNode.getAttribute("portalId");

    var callback = {};
    callback.onTabChange = function() {
        setTimeout(function() {
            loadMenuDetailData(treeID, type, parentId, portalId);
        },TIMEOUT_TAB_CHANGE);
    };
    callback.onTabClose = function() {
        delete $.cache.XmlDatas[DEFAULT_NEW_ID];
    };

    var inf = {};
    inf.defaultPage = "page1";
    inf.label = OPERATION_ADD.replace(/\$label/i, treeName);
    inf.callback = callback;
    inf.SID = CACHE_MENU_DETAIL + type + treeID;
    ws.open(inf);
}

function editTreeNode() {
    var tree = $.T("tree");
    var treeNode = tree.getActiveTreeNode();
    var treeID = treeNode.id;
    var treeName = treeNode.name;

    var callback = {};
    callback.onTabChange = function() {
        setTimeout(function() {
            loadMenuDetailData(treeID);
        }, TIMEOUT_TAB_CHANGE);
    };

    var inf = {};            
    inf.label = OPERATION_EDIT.replace(/\$label/i, treeName);
    inf.SID = CACHE_MENU_DETAIL + treeID;
    inf.defaultPage = "page1";
    inf.callback = callback;
    ws.open(inf);
}

function loadMenuDetailData(treeID, type, parentId, portalId) {
    var params = {};
    if(type)     params.type = type;
    if(parentId) params.parentId = parentId;
    if(portalId) params.portalId = portalId;

    delete $.cache.XmlDatas[DEFAULT_NEW_ID];
    var menuInfoNode = $.cache.XmlDatas[treeID];
    if(menuInfoNode) {
        return initForm();
    }

    $.ajax({
        url: URL_SOURCE_DETAIL + treeID,
        params: params,
        onresult: function() {
            menuInfoNode = this.getNodeValue(XML_MENU_INFO);
            $.cache.XmlDatas[treeID] = menuInfoNode;
            initForm();
        }
    });

    function initForm() {
        $.F("page1Form", menuInfoNode);
        $("#page1BtSave").click(function() {
            saveMenu(treeID, parentId);
        });
    }
}

function saveMenu(treeID, parentId) {
    var page1Form = $.F("page1Form");
    if( !page1Form.checkForm() )  return;

    var request = new $.HttpRequest();
    request.url = URL_SOURCE_SAVE;

    var menuInfoNode = $.cache.XmlDatas[treeID];
    var dataNode = menuInfoNode.querySelector("data");
    request.setFormContent(dataNode);

    syncButton([$1("page1BtSave")], request); // 同步按钮状态

    request.onresult = function() {
        afterSaveTreeNode.call(this, treeID, parentId);
    }
    request.onsuccess = function() {
        afterSaveTreeNode(treeID, page1Form);
    }
    request.send();
}

/*
 *  弹出窗口选择显示内容
 *  参数： string:contentName      xform列名
            string:contentId        xform列名
            string:type             弹出窗口显示数据类型
 */
function getContent(contentName, contentId, type) {
    var page1Form = $.F("page1Form");
    var portalId = page1Form.getData("portalId");
    if( portalId == null ) return;

    var url = URL_GET_PS_TREE + portalId + "/" + type;
    popupTree(url, "StructureTree", {}, function(target){
        page1Form.updateDataExternal(contentId, target.id);
        page1Form.updateDataExternal(contentName, target.name);
    });
}

/*  选择菜单对应的栏目ID  */
function getChannel() {
    var url = URL_GET_CHANNEL_TREE;
    popupTree(url, "ChannelTree", {}, function(target){
        var page1Form = $.F("page1Form");
        page1Form.updateDataExternal('name', target.name);
        page1Form.updateDataExternal('url', "${common.articleListUrl}&channelId=" + target.id);
        page1Form.updateDataExternal('description', "本菜单项对应栏目为：" + target.name);
    });
}

function moveTo() {
    var tree = $.T("tree");
    var treeNode = tree.getActiveTreeNode();
    var id = treeNode.id;

    var url = URL_GET_MENU_TREE + id;
    popupTree(url, "MenuTree", {}, function(target){
        moveTreeNode(tree, id, target.id)
    });
}

/* 刷新菜单缓存  */
function flushMenuCache() {
    $.ajax({
        url: URL_FRESH_MENU_CACHE + getTreeNodeId()
    });
}

function exportMenu() {
    var url = URL_EXPORT_MENU + getTreeNodeId();
    var frameName = createExportFrame();
    $1(frameName).setAttribute("src", url);
}

function importMenu() {
    function checkFileWrong(subfix) {
        return subfix != ".json";
    }

    var url = URL_UPLOAD_FILE + "?groupId=" + getTreeNodeId();
    url += "&afterUploadClass=com.boubei.tss.dm.ext.ImportMenu";
    var importDiv = createImportDiv("只支持json文件格式导入", checkFileWrong, url);
    $(importDiv).show().center();
}

window.onload = init;
