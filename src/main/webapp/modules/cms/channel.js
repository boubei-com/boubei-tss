/* 后台响应数据节点名称 */
XML_MAIN_TREE    = "ChannelTree";
XML_ARTICLE_LIST = "ArticleList";
XML_CHANNEL_INFO = "ChannelInfo";

/* 默认唯一编号名前缀 */
CACHE_CHANNEL_DETAIL = "_channelDetail_id";
CACHE_SITE_DETAIL    = "_siteDetail_id";

/* XMLHTTP请求地址汇总 */
URL_SOURCE_TREE    = AUTH_PATH + "channel/list";
URL_SITE_DETAIL    = AUTH_PATH + "channel/detail/site/"; //{siteId}
URL_SAVE_SITE      = AUTH_PATH + "channel/site";
URL_CHANNEL_DETAIL = AUTH_PATH + "channel/detail/"; // {id}/{parentId}
URL_SAVE_CHANNEL   = AUTH_PATH + "channel";
URL_DEL_NODE       = AUTH_PATH + "channel/";
URL_MOVE_NODE      = AUTH_PATH + "channel/move/";    // {id}/{toParentId}
URL_SORT_NODE      = AUTH_PATH + "channel/sort/";    // {id}/{targetId}/{direction}
URL_STOP_NODE      = AUTH_PATH + "channel/disable/"; // {id}
URL_POPUP_TREE     = AUTH_PATH + "channel/list";
URL_GET_OPERATION  = AUTH_PATH + "channel/operations/";   // {resourceId}

URL_ARTICLE_LIST   = AUTH_PATH + "article/list/"; // {channelId}/{page}
URL_DEL_ARTICLE    = AUTH_PATH + "article/";
URL_MOVE_ARTICLE   = AUTH_PATH + "article/move/"; // {articleId}/{channelId}
URL_LOCK_ARTICLE   = AUTH_PATH + "article/list";
URL_SETTOP_ARTICLE = AUTH_PATH + "article/top/";  // {id}
URL_SEARCH_ARTICLE = AUTH_PATH + "article/query";  // {page}

URL_PUBLISH_PROGRESS = AUTH_PATH + "channel/publish/";   // {id}/{category}
URL_SYNC_PROGRESS    = AUTH_PATH + "channel/progress/";  // GET
URL_CONCEAL_PROGRESS = AUTH_PATH + "channel/progress/";  // DELETE

URL_CREATE_INDEX     = AUTH_PATH + "channel/index/";  // {siteId}/{increment}

if(IS_TEST) {
    URL_SOURCE_TREE    = "data/site_init.xml?";
    URL_SITE_DETAIL    = "data/siteDetail.xml?";
    URL_SAVE_SITE      = "data/_success.xml?";
    URL_CHANNEL_DETAIL = "data/channelDetail.xml?";
    URL_SAVE_CHANNEL   = "data/_success.xml?";
    URL_DEL_NODE       = "data/_success.xml?";
    URL_MOVE_NODE      = "data/_success.xml?";
    URL_SORT_NODE      = "data/_success.xml?";
    URL_STOP_NODE      = "data/_success.xml?";
    URL_POPUP_TREE     = "data/channelTree.xml"; 
    URL_GET_OPERATION  = "data/_operation.xml?";

    URL_ARTICLE_LIST   = "data/article_list.xml?";
    URL_DEL_ARTICLE    = "data/_success.xml?";
    URL_MOVE_ARTICLE   = "data/_success.xml?";
    URL_LOCK_ARTICLE   = "data/_success.xml?";
    URL_SETTOP_ARTICLE = "data/_success.xml?";
    URL_SEARCH_ARTICLE = "data/article_list.xml?";

    URL_PUBLISH_PROGRESS = "data/progress.xml?";
    URL_SYNC_PROGRESS    = "data/_success.xml?";
    URL_CONCEAL_PROGRESS = "data/_success.xml?";

    URL_CREATE_INDEX     = "data/_success.xml?";
}

function init() { 
    initMenus();
    initEvents();

    loadInitData();
}

function isSite() {
    return getTreeAttribute("isSite") == "1";
}
function isChannel() {
    return getTreeAttribute("isSite") == "0";
}

function initMenus() { 
    var item1 = {
        label:"新建站点",
        icon:"icon icon-plus",
        callback:addNewSite,
        visible:function() { return isTreeRoot() && getOperation("1");}
    }
    var item2 = {
        label:"新建栏目",
        icon:"icon icon-plus",
        callback:addNewChannel,
        visible:function() { return !isTreeRoot() && getOperation("2");}
    }
    var item3 = {
        label:"新建文章",
        icon:"icon icon-file-text",
        callback:function() { addNewArticle(); },
        visible:function() { return isChannel() && getOperation("3");}
    }

    var submenu4 = new $.Menu(); // 发布
    var item4 = {
        label:"发布",
        icon:"icon icon-sign-out",
        visible:function() { return !isTreeRoot() && getOperation("4");},
        submenu:submenu4
    }
    var subitem4a = {
        label:"增量发布",
        icon:"icon icon-plus",
        callback:function() { 
            publishArticle(1);
        }
    }
    var subitem4b = {
        label:"完全发布",
        icon:"icon icon-sync",
        callback:function() { 
            $.confirm("你确定要执行【完全发布】吗？完全发布后栏目下所有文章的发布日期将都被更新成最新日期，建议优先使用【增量发布】", "确认发布", 
                function() { publishArticle(2); },
                function() { return;}
            );
        }
    }
    submenu4.addItem(subitem4a);
    submenu4.addItem(subitem4b);

    var item5 = {
        label:"编辑",
        callback:editTreeNode,
        icon:"icon icon-pencil",
        visible:function() { return !isTreeRoot() && getOperation("5");}
    }
    var item6 = {
        label:"删除",
        callback: function() { delTreeNode(URL_DEL_NODE) },
        icon:"icon icon-x",
        visible:function() { return !isTreeRoot() && getOperation("6");}
    }
    var item7 = { 
        label:"启用",
        callback: function() { stopOrStartTreeNode("0"); },
        icon:"icon icon-triangle-up",
        visible:function() { return isTreeNodeDisabled() && !isTreeRoot() && getOperation("7");}
    }
    var item8 = {
        label:"停用",
        callback: function() { stopOrStartTreeNode("1"); },
        icon:"icon icon-triangle-down",
        visible:function() { return !isTreeNodeDisabled() && !isTreeRoot() && getOperation("7");}
    }
    var item9 = {
        label:"移动到...",
        callback:moveChannelTo,
        icon:"icon icon-arrow-right",
        visible:function() { return isChannel() && getOperation("6");}
    }
    var item10 = {
        label:"浏览文章",
        callback: function() { showArticleList(); },
        icon:"icon icon-list-unordered",
        visible:function() { return isChannel() && getOperation("1");}
    }        
    var item12 = {
        label:"搜索文章",
        callback: searchArticles,
        icon:"icon icon-search",
        visible:function() { return getOperation("1"); }
    } 

    var submenu5 = new $.Menu(); // 即时建立索引
    var item11 = {
        label:"即时建索引",
        icon:"icon icon-clock",
        visible:function() { return isSite() && getOperation("4");},
        submenu:submenu5
    }
    var subitem5a = {
        label:"增量索引",
        icon:"icon icon-plus",
        callback:function() { 
            createLuceneIndex(1);
        }
    }
    var subitem5b = {
        label:"重建索引",
        icon:"icon icon-sync",
        callback:function() { 
            createLuceneIndex(0);
        }
    }
    submenu5.addItem(subitem5a);
    submenu5.addItem(subitem5b);

    var menu1 = new $.Menu();
    menu1.addItem(item1);
    menu1.addItem(item2);
    menu1.addItem(item3);
    menu1.addItem(item5);
    menu1.addItem(item7);
    menu1.addItem(item8);
    menu1.addItem(item9);
    menu1.addItem(item6);
    menu1.addSeparator();
    menu1.addItem(item4);
    menu1.addItem(item10);
    menu1.addItem(item11);
    menu1.addItem(item12);
    
    menu1.addItem(createPermissionMenuItem("3"));

    $1("tree").contextmenu = menu1;

    initGridMenu();
}

function loadInitData() { 
    $.ajax({
        url : URL_SOURCE_TREE,
        onresult : function() { 
            var tree = $.T("tree", this.getNodeValue(XML_MAIN_TREE)); 
            tree.onTreeNodeMoved = function(ev) { sortTreeNode(URL_SORT_NODE, ev); }
            tree.onTreeNodeRightClick = function(ev) { onTreeNodeRightClick(ev, true); }
            tree.onTreeNodeDoubleClick = function(ev) {
                var treeNode = getActiveTreeNode();
                getTreeOperation(treeNode, function(_operation) {
                    if("_root" == treeNode.id )  return;

                    var canShowArticleList = checkOperation("1", _operation);                
                    if( isChannel() && canShowArticleList) { 
                        showArticleList();
                    } 
                    else {
                        var canEdit = checkOperation("5", _operation);
                        if( canEdit ) {
                            editTreeNode();                    
                        }
                    }            
                }); 
            }

            // 自动打开文章创建编辑器
            var channel = $.Query.get("channel");
            var title = $.Query.get("title");
            if(channel && title) {
                addNewArticle(channel, title);
                tree.setActiveTreeNode(channel);

                var treeNode = getActiveTreeNode();
                getTreeOperation(treeNode, function(_operation) { });
            }
        }
    });
}

function editTreeNode() { 
    if( isChannel() ) { 
        editChannelInfo();
    } else {
        editSiteInfo();
    }
}

function addNewSite() { 
    loadSiteDetailData(DEFAULT_NEW_ID);
}

function editSiteInfo() { 
    loadSiteDetailData(getTreeNodeId());
}

function loadSiteDetailData(treeID) { 
    $.ajax({
        url : URL_SITE_DETAIL + treeID,
        onresult : function() { 
            var siteInfoNode = this.getNodeValue(XML_CHANNEL_INFO);
            $.cache.XmlDatas[treeID] = siteInfoNode;

            showChannelForm();
            $.F("channelForm", siteInfoNode);

            // 设置保存按钮操作
            $1("channelFormSave").onclick = function() { 
                saveSite(treeID);
            }
        }
    });
}

function showChannelForm() {
    var $panel = $("#channelFormDiv").show(true);
    var html = '<div id="channelForm"></div>' +
        '<input type="button" class="tssbutton medium blue" value="保 存" id="channelFormSave"/>';
    $panel.html("").panel("编辑栏目【" + getTreeNodeName() + "】", html);
}

function saveSite(treeID) {
    var xform = $.F("channelForm");
    if( !xform.checkForm() ) return;

    var request = new $.HttpRequest();
    request.url = URL_SAVE_SITE;

    var siteInfoNode = $.cache.XmlDatas[treeID];
    var dataNode = siteInfoNode.querySelector("data");  
    request.setFormContent(dataNode);

    syncButton([ $1("channelFormSave")], request);

    request.onresult = function() { 
        afterSaveTreeNode.call(this, treeID, "_root");
        $("#channelFormDiv").hide();
    }
    request.onsuccess = function() { 
        afterSaveTreeNode(treeID, xform);
        $("#channelFormDiv").hide();
    }
    request.send();
}

function addNewChannel() { 
    var parentID  = getTreeNodeId();
    var channelID = DEFAULT_NEW_ID;
    loadChannelDetailData(channelID, parentID);
}

function editChannelInfo() { 
    loadChannelDetailData(getTreeNodeId());
}

function loadChannelDetailData(treeID, parentID) { 
    $.ajax({
        url : URL_CHANNEL_DETAIL + treeID + "/" + (parentID || 0),
        onresult : function() { 
            var channelInfoNode = this.getNodeValue(XML_CHANNEL_INFO);
            $.cache.XmlDatas[treeID] = channelInfoNode;

            showChannelForm();
            var xform = $.F("channelForm", channelInfoNode);

            // 设置保存按钮操作
            $1("channelFormSave").onclick = function() { 
                saveChannel(treeID, parentID);
            }
        }
    });
}

function saveChannel(treeID, parentID) { 
    var xform = $.F("channelForm");
    if( !xform.checkForm() ) return;

    var request = new $.HttpRequest();
    request.url = URL_SAVE_CHANNEL;

    var channelInfoNode = $.cache.XmlDatas[treeID];
    var dataNode = channelInfoNode.querySelector("data");
    request.setFormContent(dataNode);

    syncButton([$1("channelFormSave")], request);

    request.onresult = function() { 
        afterSaveTreeNode.call(this, treeID, parentID);
        $("#channelFormDiv").hide();
    }
    request.onsuccess = function() {
        afterSaveTreeNode(treeID, xform);
        $("#channelFormDiv").hide();
    }
    request.send();
}

function moveChannelTo() { 
    var tree = $.T("tree");
    var treeNode = tree.getActiveTreeNode();
    var id  = treeNode.id;
    var pId = treeNode.parent.id;

    var params = {id:id, parentID: pId};
    popupTree(URL_POPUP_TREE, "ChannelTree", params, function(target) {
        if(target.id == "_root") {
            return alert("栏目不能移动到根节点下。");
        }
        moveTreeNode(tree, id, target.id, URL_MOVE_NODE);
    });
}

function moveArticleTo() { 
    var articleId = getArticleAttribute("id");;
    popupTree(URL_POPUP_TREE, "ChannelTree", {}, function(target) {
         $.ajax({
            url: URL_MOVE_ARTICLE + articleId + "/" + target.id,
            onsuccess: function() {
                var channelId = getTreeNodeId();
                showArticleList(channelId); // 刷新当前栏目文章列表
            }
        });
    });
}

var globalValiable = {}; // 用来存放传递给iframe页面的信息
function addNewArticle(channelId, title) { 
    globalValiable.channelId = channelId || getTreeNodeId();
    globalValiable.articleId = null;

    $("#grid").hide();
    $("#articleFrame").show();
    $1("articleFrame").setAttribute("src", "article.html" + (title ? "?title="+title : "") );
}

function editArticleInfo() { 
    var canEdit = getGridOperation("5");
    if( !canEdit ) return;            

    globalValiable.articleId = getArticleAttribute("id");
    globalValiable.channelId = getArticleAttribute("channel.id");

    $("#grid").hide();
    $("#articleFrame").show();
    $1("articleFrame").setAttribute("src", "article.html");
}

function delArticle() { 
    $.confirm("您确定要删除该文章吗？", "删除确认", function(){
        var articleId = getArticleAttribute("id");
        $.ajax({
            url : URL_DEL_ARTICLE + articleId,
            method: "DELETE",
            onsuccess : function() {            
                var channelId = getTreeNodeId();
                showArticleList(channelId); 
            }
        });
    });
} 

/* 获取文章属性 */
function getArticleAttribute(name) { 
    return $.G("grid").getColumnValue(name);
}

/* 置顶文章 */
function setTopArticle(isTop) { 
    var articleId = getArticleAttribute("id");
    $.ajax({
        url : URL_SETTOP_ARTICLE + articleId,
        onsuccess : function() {            
            var channelId = getTreeNodeId();
            showArticleList(channelId); 
        }
    });
}

/*
 *  检测用户列表右键菜单项是否可见
 *  参数： string:code     操作码
 *  返回值：是够有权限
 */
function getGridOperation(code) {
    var flag = false;
    var channelId = getArticleAttribute("channel.id");
    if( channelId ) {
        var channelNode = $.T("tree").getTreeNodeById(channelId);
        var _operation = channelNode.getAttribute("_operation");
        flag = checkOperation(code, _operation);
    }
    return flag;
}

/*
 *  发布文章
 *  参数： string:type     完全发布“2”; 增量发布“1”
 */
function publishArticle(category) { 
    var channelId = getTreeNodeId();
    $.ajax({
        url : URL_PUBLISH_PROGRESS + channelId + "/" + category,
        onresult : function() {             
            var data = this.getNodeValue("ProgressInfo");
            var progress = new $.Progress(URL_SYNC_PROGRESS, data, URL_CONCEAL_PROGRESS);
            progress.oncomplete = function() { 
                showArticleList(channelId); // 发布完成：刷新grid
            }
            progress.start();
        }
    });
}

function createLuceneIndex(increment) {
    var siteId = getTreeNodeId();
    $.ajax({
        url : URL_CREATE_INDEX + siteId + "/" + increment,
        onresult : function() {             
            var data = this.getNodeValue("ProgressInfo");
            var progress = new $.Progress(URL_SYNC_PROGRESS, data, URL_CONCEAL_PROGRESS);
            progress.oncomplete = function() { 
                alert("索引创建完成。"); 
            }
            progress.start();
        }
    });
}

function initGridMenu() { 
    var item1 = {
        label:"编辑",
        callback:editArticleInfo,
        icon:"icon icon-pencil",
        visible:function() { return getGridOperation("5");}
    }
    var item2 = {
        label:"删除",
        callback:delArticle,
        icon:"icon icon-x",
        visible:function() { return getGridOperation("5");}
    }
    var item3 = {
        label:"移动到...",
        callback:moveArticleTo,
        icon:"icon icon-arrow-right",
        visible:function() { return getGridOperation("5");}
    }
    var item4 = {
        label:"置顶",
        callback:function() { 
            setTopArticle("1");
        },
        icon:"icon icon-cloud-upload",
        visible:function() { return "1" != getArticleAttribute("isTop") && getGridOperation("5");}
    }
    var item5 = {
        label:"解除置顶",
        callback:function() { 
            setTopArticle("0");
        },
        icon:"icon icon-cloud-download",
        visible:function() { return "1" == getArticleAttribute("isTop") && getGridOperation("5");}
    }

    var menu1 = new $.Menu();
    menu1.addItem(item1);
    menu1.addItem(item2);
    menu1.addItem(item3);
    menu1.addItem(item4);
    menu1.addItem(item5);

    $1("grid").contextmenu = menu1;
}

/* 显示文章列表 */
function showArticleList(channelId) {
    $("#grid").show();
    $("#articleFrame").hide();

    channelId = channelId || getTreeNodeId();
    $.showGrid(URL_ARTICLE_LIST + channelId, XML_ARTICLE_LIST, editArticleInfo);
}   

function searchArticles(){
    $("#grid").show();
    $("#articleFrame").hide();

    var treeNode = $.T("tree").getActiveTreeNode();
    var treeID   = treeNode.id;
    var treeName = treeNode.name;

    popupForm("SearchArticle.xml", "SearchArticle", {}, function(condition) {
        condition.channelId = treeID;
        $.showGrid(URL_SEARCH_ARTICLE, XML_ARTICLE_LIST, editArticleInfo, "grid", 1, condition);

    }, "搜索【" + treeName + "】下所有文章");
}

function showComments(articleId) {
    tssJS.get("/tss/auth/article/" + articleId + "/comment?anonymous=true", {}, 
        function(commonts) {
            var html = "";
            commonts.each(function(i, item){
                html += '<div class="cmt-author"><span>' + item[0] + '</span><time class="time">' + item[2] + '</time></div>' +
                        '<div class="cmt-summay">' + item[1] + '</div>';
                
            });
            tssJS.alert(html||'暂无评论');
    });
}

window.onload = init;

