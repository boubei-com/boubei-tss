if( location.hostname == 'www.boubei.com' ) {
	$(function() {
	    var scriptNode = document.createElement("script");
	    scriptNode.src = location.protocol + "//s11.cnzz.com/z_stat.php?id=1256153120&web_id=1256153120";
	    scriptNode.async = false;
	    $('head').appendChild(scriptNode);
	});
}

/*********************** 系统配置 开始 **********************************/
 var
	IS_TEST = (location.protocol === 'file:'),

	FROMEWORK_CODE = "TSS",    /* 当前框架名 */
	APP_CODE       = "TSS";    /* 当前应用名 */

/*********************** 系统配置 END **********************************/
 var
	APPLICATION  = APP_CODE.toLowerCase(),
	CONTEXTPATH  = APPLICATION + "/",

	NO_AUTH_PATH = "/" + CONTEXTPATH,
	AUTH_PATH    = NO_AUTH_PATH + "auth/",
	URL_UPLOAD_FILE  = AUTH_PATH + "file/upload",	

	URL_CORE = IS_TEST ? "../../tools/tssJS/" : "/" + APPLICATION + "/tools/tssJS/",  // 界面核心包相对路径
	ICON  =  URL_CORE + "img/";


/*********************** 和工作区Workspace相关 的 公用函数 **********************************/

/*  常量定义 */
OPERATION_ADD        = "新建[$label]";
OPERATION_VIEW       = "查看[$label]";
OPERATION_DEL        = "删除[$label]";
OPERATION_EDIT 		 = "修改[$label]";
OPERATION_SEARCH 	 = "查询[$label]";
OPERATION_IMPORT 	 = "导入[$label]";
OPERATION_SETTING    = "设置[$label]";
OPERATION_PERMISSION = "设置[$label]权限";

/* Tab页切换延时 */
TIMEOUT_TAB_CHANGE = 200;

/* 默认新增节点ID */
DEFAULT_NEW_ID = "-10";

/* Grid、Tree等 */
XML_OPERATION = "Operation";
XML_PAGE_INFO = "PageInfo";
CACHE_TREE_NODE = "_treeNode_";
CACHE_MAIN_TREE = "_tree_";

var ws;
function initWorkSpace() {
	ws = new $.WorkSpace("ws");
	 
	var tr = ws.element.parentNode.parentNode;
	$1("ws").onTabCloseAll = function(event) { /* 隐藏tab页工作区 */
		$(tr).hide();
		$(tr.previousSibling).hide();  
	}
	$1("ws").onTabChange = function(event) { /* 显示tab页工作区 */
		$(tr).show();
		$(tr.previousSibling).show();  
	}
}

function closePalette() {
	$("#palette").hide().removeClass("opend");;
	$("#paletteOpen").show();
	$(".panel .header>td:nth-child(2)").hide();
	$(".panel .footer>td:nth-child(2)").hide();
}

function openPalette() {
	$("#palette").show().addClass("opend");
	$("#paletteOpen").hide();
	$(".panel .header>td:nth-child(2)").show();
	$(".panel .footer>td:nth-child(2)").show();
}

window.onresize = function() {
	var bodyWidth = document.body.offsetWidth;
	var bodyHeight = document.body.offsetHeight;

	$("#palette #tree").css("height", (bodyHeight - 23) + "px");
	$("#ws Tree").css("height", (bodyHeight - 103) + "px");	

	// 设置右边容易的最大宽度
	$(".panel .body td.groove").css("maxWidth", (bodyWidth - 250) + "px");
	$("#gridContainer").css("maxWidth", (bodyWidth - 250) + "px");
}
 
/* 事件绑定初始化 */
function initEvents() {
	/* 树节点查找 和 刷新 */
	$(".refreshTreeBT").addClass("icon-sync").title("刷新").click( function() { loadInitData(); } );
	$("#palette .icon-search").title("查找").click(searchTree);

	var searchKey = $1("searchKey");
	if( searchKey ) { searchKey.oninput = searchTree; }	

	window.onresize();

	/* 点击左栏控制按钮 */
	$("#palette").addClass("opend");
	$.leftbar(function(){
		if($("#palette").hasClass("opend")){
			closePalette();
		} else {
			openPalette();
		}
	});

	// 关闭页面自动注销
	$.Event.addEvent(window, "unload", function(ev) {
		ev = ev || window.event;
		if(ev.clientX > document.body.clientWidth && ev.clientY < 0 || ev.altKey) {
			logout();
		}
	});
}

function searchTree() {
	var key = $("#searchKey").value();
	key && $.T('tree').searchNode(key);
}

function bindTreeSearch(inputElId, treeId) {
	var inputEl = $1(inputElId);
	var $btn = $("button", inputEl.parentNode);
	$btn.addClass("tssbutton small blue").click( function(){
		var key = $(inputEl).value();
		key && $.T(treeId).searchNode(key);
	} );

	inputEl.oninput = function() {
		var key = $(this).value();
		key && $.T(treeId).searchNode(key);
	}
}

function openActiveTreeNode(callback) {	
	var tree = $.T('tree');
	if( tree && tree.getActiveTreeNode() ) {
		callback = callback || tree.onTreeNodeDoubleClick || tree.onTreeNodeActived;
		callback && callback();
    }
}
 
function onTreeNodeActived(ev) { }

function onTreeNodeRightClick(ev, carePermission, treeName) {
	var menu = $1(treeName || "tree").contextmenu;
	if(menu == null) {
		return;
	}

	if( carePermission ) {
        var treeNode = ev.treeNode;
        getTreeOperation(treeNode, function(_operation) {
			menu.show(ev.clientX, ev.clientY);
        });
	}
	else {
		menu.show(ev.clientX, ev.clientY);
	}
}

function getSourceOperation(sourceId, callback) {
	$.ajax({
		url : URL_GET_OPERATION + sourceId,
		onresult : function() {
			var _operation = this.getNodeValue(XML_OPERATION);
			callback && callback(_operation);
		}
	});			  
}

/*
 *	获取对树节点的操作权限
 *	参数：	treeNode:treeNode       树节点
			function:callback       回调函数
 */
function getTreeOperation(treeNode, callback, url) {
	url = url || URL_GET_OPERATION;
	var _operation = treeNode.getAttribute("_operation");

	var treeId = treeNode.id;
	if(treeId == "_root") {
		treeId = 0;
	}
	
	// 如果节点上还没有_operation属性，则发请求从后台获取信息
	if( $.isNullOrEmpty(_operation) ) { 
		$.ajax({
			url : url + treeId,
			onresult : function() {
				_operation = this.getNodeValue(XML_OPERATION);
				treeNode.setAttribute("_operation", _operation);

				if ( callback ) {
					callback(_operation);
				}
			}
		});			
	} 
	else {
		if ( callback ) {
			callback(_operation);
		}
	}    
}
	
/*
 *	检测右键菜单项是否可见
 *	参数：	string:code     操作码
 */
function getOperation(code, treeName) {
	var flag = false;
	var treeNode = getActiveTreeNode(treeName);
	if( treeNode ) {
		var _operation = treeNode.getAttribute("_operation");

		var opts = code.split(",");
		opts.each(function(i, opt){
			if( checkOperation(opt, _operation) ) {
				flag = true;
			}
		});
	}
	return flag;
}

/*
 *	检测操作权限
 *	参数：	string:code             操作码
			string:_operation       权限
 */
function checkOperation(code, _operation) {
	var flag = false;
	if( "string" == typeof(code) && "string" == typeof(_operation) ) {
		var reg = new RegExp("(^" + code + ",)|(^" + code + "$)|(," + code + ",)|(," + code + "$)", "gi");
		flag = reg.test(_operation);
	}
	return flag;
}

var globalValiable = {};  // 用来存放传递给iframe页面的信息

/* 授予角色 */
function setRole2Permission(resourceType, rootId) {
    var treeNode = getActiveTreeNode();
    globalValiable = {};
    globalValiable.roleId = treeNode.id == '_root' ? (rootId || "0") : treeNode.id;
    globalValiable.resourceType = resourceType;
    globalValiable.applicationId = "tss";
    globalValiable.isRole2Resource = "0";
    var title = "把【" + treeNode.name + "】作为资源授予角色";

    $.openIframePanel("permissionPanel", title, 850, 520, "../um/setpermission.html");
    $("#permissionPanel").find("h2").html(title);
}

function createPermissionMenuItem(resourceType, operation) {
	return {
        label:"授予角色",
        icon:"icon icon-organization",
        callback:function() { 
            setRole2Permission(resourceType); 
        },   
        visible:function() { return getOperation(operation || "2"); }
    };
}

/* request请求期间，同步按钮禁止/允许状态 */
function syncButton(btObjs, request) {
	btObjs.each(function(i, btEl){
		btEl.disabled = true;
	});

	request.ondata = request.onexception = function() {
		btObjs.each(function(i, btEl){
			btEl.disabled = false;
		});
	}
}

/* 组件资源管理 */
function fileManage(params, title) {
    $.openIframePanel("fileManagerPanel", title, 440, 388, "../portal/filemanager.html?" + params, true);
}

/* 创建导入Div */
function createImportDiv(remark, checkFileWrong, importUrl, callback) {
	var importDiv = $1("importDiv");
	if( importDiv == null ) {
		importDiv = $.createElement("div", null, "importDiv");    
		document.body.appendChild(importDiv);

		var str = [];
		str[str.length] = "<form id='importForm' method='post' target='fileUpload' enctype='multipart/form-data'>";
		str[str.length] = "	 <div class='fileUpload'> <input type='file' name='file' id='sourceFile' multiple='multiple' onchange=\"$('#importDiv h2').html(this.value)\" /> </div> ";
		str[str.length] = "	 <input type='button' id='importBt' value='确定导入' class='tssbutton blue'/> ";
		str[str.length] = "</form>";
		str[str.length] = "<iframe style='width:0; height:0;' name='fileUpload'></iframe>";

		$(importDiv).panel(remark, str.join("\r\n"), false);
		$(importDiv).css("height", "300px").center();
	} else {
		$("#sourceFile").value("");
		$('#importDiv h2').text(" - " + remark);
	}

	// 每次 importUrl 可能不一样，比如导入门户组件时。不能缓存
	$("#importBt").click( function() {
		var fileValue = $1("sourceFile").value;
		if( !fileValue ) {
			 return $("#importDiv h2").notice("请选择导入文件!");				 
		}

		var index  = fileValue.lastIndexOf(".");
		var subfix = fileValue.substring(index).toLowerCase();
		if( checkFileWrong && checkFileWrong(subfix) ) {
		   return $("#importDiv h2").notice(remark);
		}

		var form = $1("importForm");
		form.action = importUrl;
		form.submit();

		callback && callback();

		$(importDiv).hide();
	} );

	$1("sourceFile").click();
	return importDiv;
}

 /* 创建导出用iframe */
function createExportFrame() {
	var frameName = "exportFrame";
	if( $1(frameName) == null ) {
		var exportDiv = $.createElement("div"); 
		$(exportDiv).hide().html("<iframe id='" + frameName + "' style='display:none'></iframe>");
		document.body.appendChild(exportDiv);
	}
	return frameName;
}
 
(function() {
	if(window.dialogArguments && window.dialogArguments.title) {
		document.write("<title>" + window.dialogArguments.title + new Array(100).join("　") + "</title>");
	}

	/* 禁止鼠标右键 */
	document.oncontextmenu = function(ev) {
		ev = ev || window.event;
		var srcElement = $.Event.getSrcElement(ev);
		var tagName = srcElement.tagName.toLowerCase();
		if("input" != tagName && "textarea" != tagName) {
			$.Event.cancel(ev);            
		}
	}

	window._alert = window.alert;
	window.alert = $.alert;

	// 离开提醒
	window.onbeforeunload = function() {
		var count = reminder.count;
		if(count > 0) {            
			return "当前有 <" + count + "> 项修改未保存，您确定要离开吗？";
		}
	}

})();


var Reminder = function() {
	this.items = {};   // 提醒项
	this.count = 0;

	this.add = function(id) {
		if( null == this.items[id] ) {
			this.items[id] = true;
			this.count ++;
		}
	}

	// ws里关闭tab页的时，可以自定义onTabClose事件，接触Tab页上的提醒
	this.del = function(id) {
		if(  this.items[id] ) {
			delete this.item[id];
			this.count --;
		}
	}

	/* 取消提醒 */
	this.reset = function() {
		this.items = {};   // 提醒项
		this.count = 0;
	}
};

var reminder = new Reminder();

/* 给xform等添加离开提醒 */
function attachReminder(id, form) {
	if( form ) {
		form.ondatachange = function(ev) {
			reminder.add(ev.id); // 数据有变化时才添加离开提醒
		}
	}
	else {
		reminder.add(id);
	}
}

function detachReminder(id) {
	reminder.reset();
} 

/*********************** Tree 的 公用函数 **********************************/

function getActiveTreeNode(treeName) {
	return $.T(treeName || "tree").getActiveTreeNode();
}

function getTreeAttribute(name, treeName) {
	var treeNode = getActiveTreeNode(treeName);
	if( treeNode ) {
		return treeNode.getAttribute(name);
	}
	return null;   
}

function getTreeNodeId() {
	return getTreeAttribute("id");
}

function getTreeNodeName() {
	return getTreeAttribute("name");
}

function isTreeNodeDisabled() {
	return getTreeAttribute("disabled") == "1";
}

function isTreeRoot() {
	return getTreeNodeId() == "_root";
}

function hasSameAttributeTreeNode(tree, attr, value) {
	var result = false;
	tree.getAllNodes().each(function(i, node) {
		if(node.getAttribute(attr) == value) {
			result = true;
		}
	});
	return result;
}

/*
 *	修改树节点属性
 *	参数：  string:id               树节点id
			string:attrName         属性名
			string:attrValue        属性值
 */
function modifyTreeNode(id, attrName, attrValue, treeName) {
	var tree = $.T(treeName || "tree");
	var treeNode = tree.getTreeNodeById(id);
	if( treeNode ) {
		treeNode.attrs[attrName] = attrValue;
		if(attrName == "name") {
			treeNode.name = treeNode.li.a.title = attrValue;
			$(treeNode.li.a).html(attrValue);
		}
	}
}

/* 添加子节点 */
function appendTreeNode(id, xmlNode, treeName) {
	var tree = $.T(treeName || "tree");
	var treeNode = tree.getTreeNodeById(id);
	if( treeNode && xmlNode ) {
		tree.addTreeNode(xmlNode, treeNode);
	}
}

/* 获取树全部节点id数组 */
function getTreeNodeIds(xmlNode) {
	var idArray = [];
	$.each(xmlNode.querySelectorAll("treeNode>treeNode"), function(i, curNode){
	  	var id = curNode.getAttribute("id");
		if( id ) {
			idArray.push(id);
		}
	});
	return idArray;
}

function afterSaveTreeNode(treeID, X) {
	if(X.getData) { // 更新树节点名称
		var name = X.getData("name");
		modifyTreeNode(treeID, "name", name); 
	} 
	else { // 往资源树上动态添加新增的节点
		var xmlNode = this.getResponseXML().querySelector("treeNode");
		appendTreeNode(X, xmlNode);  
	}

	ws && ws.closeActiveTab(); // 关闭资源tab页，以免重复保存的时候报乐观锁。界面上缓存的lockVersion值没有及时更新

    delete $.cache.XmlDatas[treeID]; // 清除缓存
    detachReminder(treeID); // 解除提醒
}

/* 根据条件将部分树节点设置为不可选状态 */
function disableTreeNodes(treeXML, selector) {
	var nodeLsit = treeXML.querySelectorAll(selector);
	for(var i = 0; i < nodeLsit.length; i++) {
		nodeLsit[i].setAttribute("disabled", "1");
	}
}
 			
/* 删除树选中节点 */
function removeTreeNode(tree, excludeIds) {
	excludeIds = excludeIds || ["_root"];

	tree.getCheckedNodes().each(function(i, node){
		if( !excludeIds.contains(node.id) ) {
			tree.removeTreeNode(node);
		}
	});
}

/*
 *	将树选中节点添加到另一树中(注：过滤重复id节点，并且结果树只有一层结构)
 *	参数：	Element:fromTree         树控件
			Element:toTree           树控件
			Function:checkFunction   检测单个节点是否允许添加
 */
function addTreeNode(fromTree, toTree, checkFunction) {	
	var reload = false;
	var selectedNodes = fromTree.getCheckedNodes(false);	

	var _break = false;
	selectedNodes.each(function(i, curNode) {
		var curNode = selectedNodes[i];
		if( _break || !curNode.isEnable() ) {
			return;  // 过滤不可选择的节点
		}

		if( checkFunction ) {
			var result = checkFunction(curNode);
			if( result && result.error ) {
				if( result.message ) {
					$(toTree.el).notice(result.message); // 显示错误信息
				}
				if( result.stop ) {
					_break = true;
				}
				return;
			}
		}

		var theSameNode = toTree.el.querySelector("li[nodeId='" + curNode.id + "']");
		if("_root" != curNode.id && theSameNode == null) {
			var parent = toTree.getTreeNodeById("_root");
			if( parent ) {
				toTree.addTreeNode(curNode.attrs, parent);
			}
		}
	});
}

// 删除选中树节点
function delTreeNode(url, treeName) {
	var tree = $.T(treeName || "tree");
	var treeNode = tree.getActiveTreeNode();

	$.confirm("您确定要删除节点【" +treeNode.name+ "】吗？", "删除确认", function(){
		$.ajax({
			url : (url || URL_DELETE_NODE) + treeNode.id,
			method : "DELETE",
			onsuccess : function() { 
				tree.removeTreeNode(treeNode);
				tree.setActiveTreeNode(treeNode.parent.id);
			}
		});	
	});
}

/*
 *	停用启用节点
 *	参数：	url      请求地址
			state    状态
 */
function stopOrStartTreeNode(state, url, treeName) {	
	var tree = $.T(treeName || "tree");
	var treeNode = tree.getActiveTreeNode();
	
	if( state == "1" ) {
		$.confirm("您确定要停用该节点【" +treeNode.name+ "】吗？", "停用确认", callback);
	}
	else {
		callback();
	}
		
	function callback() {
		$.ajax({
			url : (url || URL_STOP_NODE) + treeNode.id + "/" + state,
			onsuccess : function() { 
				// 刷新父子树节点停用启用状态: 启用上溯，停用下溯
				refreshTreeNodeState(treeNode, state);
		
				if("1" == state) {
	 				treeNode.children.each(function(i, child){
	 					refreshTreeNodeState(child, state);
	 				});
				} else if ("0" == state) {
					var parent = treeNode.parent;
					while( parent && parent.id != "_root") {
						refreshTreeNodeState(parent, state);
						parent = parent.parent;
					}            
				}
			}
		});
	}
}

function refreshTreeNodeState(treeNode, state) {
	treeNode.setAttribute("disabled", state);

	var iconPath = treeNode.getAttribute("icon");
	if(iconPath) {
		iconPath = iconPath.replace( /_[0,1].gif/gi, "_" + state + ".gif"); 
		iconPath = iconPath.replace( /_[0,1].png/gi, "_" + state + ".png"); 
	
		treeNode.setAttribute("icon", iconPath);
		treeNode.li.selfIcon.css("backgroundImage", "url(" + iconPath + ")");
	}
}

// 对同层的树节点进行排序
function sortTreeNode(url, ev) {
	var dragNode = ev.dragNode;
	var destNode = ev.destNode;
	
	if(dragNode.id === destNode.id) return;
 
	$.ajax({
		url : url + dragNode.id + "/" + destNode.id + "/-1",
		onsuccess : function() { 
			ev.ownTree.sortTreeNode(dragNode, destNode);
		}
	});
}

// 移动树节点
function moveTreeNode(tree, id, targetId, url) {
	if(id == targetId) return;
	
	$.ajax({
		url : (url || URL_MOVE_NODE) + id + "/" + targetId,
		onsuccess : function() {  				
			var treeNode = tree.getTreeNodeById(id);
			var parent   = tree.getTreeNodeById(targetId == '0' ? '_root' : targetId);
			tree.moveTreeNode(treeNode, parent); // 移动树节点	

			// 父节点停用则下溯
			if( !parent.isEnable() ) {
				refreshTreeNodeState(treeNode, "1");
			}

			treeNode.setAttribute("_operation", null); // 清除树节点操作权限
		}
	});
}

/*********************** 和UM相关 的 公用函数 **********************************/
// 用户权限信息
var userCode, 
	userName, 
	userDomain,
	userGroups = [], 
	userRoles = [],
	userRoleNames = [];

var userHas = tssJS.Cookie.decode("userHas");
if(userHas) {
    userGroups = userHas[0];
    userRoles  = userHas[1];
    userRoleNames = userHas[11];
    userCode   = userHas[3];
    userName   = userHas[4];
    userDomain = userHas[12];
    userRoleNames.each(function(i, item){
        userRoles.push(item);
    });
}

function logout() {
	$.ajax({
		url : URL_CORE + "../logout.in",
		method : "GET",
		onsuccess : function() { 
			$.Cookie.del("token", "/" + CONTEXTPATH);
			$.Cookie.del("token", "/" + APPLICATION);
			$.Cookie.del("token", "");
			$.Cookie.del("token", "/");
			location.href = URL_CORE + "../login.html";
		}
	});
}

/* 检查密码强度 */
function checkPasswordSecurityLevel(formObj, url, password, loginName, el) {
	$.ajax({
		url : url,
		method : "POST",
		headers: {"appCode": FROMEWORK_CODE},
		params : {"password": password, "loginName": loginName}, 
		onresult : function() {
			var securityLevel = this.getNodeValue(XML_SECURITY_LEVEL);
			var errorInfo = {
				0: "您输入的密码安全等级太低，请重新输入",
				1: "您输入的密码安全等级为低，只能保障基本安全",
				2: "您输入的密码安全等级为中，较安全",
				3: "您输入的密码安全等级为高，很安全"
			};
			if( securityLevel < 2) {
				formObj.showCustomErrorInfo(el || "password", errorInfo[securityLevel]);
			}
			formObj.securityLevel = securityLevel;
		}
	});
}

/*********************** 临时 公用函数 **********************************/
Element.attachResize = function(element, type) {
	 $(element).resize(type);
}

Element.moveable = function(element, handle) {
	$(element).drag(handle);
}

/* 
 * 弹出选择树.
 * popupTree('/tss/auth/role/list', 'RoleTree', {'_default': '12'}, function(d){} );
 * popupTree('/tss/auth/service/roles/tree', 'RoleTree', {'treeType':'multi', '_default': '8,54'}, function(d){} );
 */
function popupTree(url, nodeName, params, callback, method) {
	removeDialog();
	params = params || {};
	var treeType = params.treeType||'single';

	var boxName = "popupTree";
	var el = $.createElement("div", "popupItem");
	el.innerHTML = '<h2>- ' + (params._title || '点击选择树节点') + 
		'<span class="search"> <input id="sksk"/><button class="tssbutton orange small">查找</button> </span> </h2>' +
		'<Tree id="' + boxName + '" treeType="' +treeType+ '"><div class="loading"></div></Tree>' + 
	    '<div class="bts">' + 
	       '<input type="button" value="确定" class="tssbutton blue small b1" >' + 
       	   '<input type="button" value="关闭" class="tssbutton blue small b2" >' +  
	    '</div>';
	document.body.appendChild(el);
	$(el).addClass("dialog").css("width", "420px").css("height", "360px").center(420, 360).css("zIndex", "999");

	$(".bts .b2", el).click(removeDialog);

	$(".search input", el)[0].oninput = function(){
		var key = $(this).value();
		key && $.T(boxName).searchNode(key);
	}
	$(".search button", el).click(function(){
		var key = $(".search input", el).value();
		key && $.T(boxName).searchNode(key);
	});

	var _default = params._default;
	delete params._default;
	delete params._title;
	delete params.treeType;

	$.ajax({
		url: url,
		params: params,
		method: method,
		onresult : function() { 
			$.showWaitingLayer();

			var tree = $.T(boxName, this.getNodeValue(nodeName));
			if(_default) {
				if(treeType == 'multi') {
					tree.setCheckValues(_default);
				} else {
					tree.setActiveTreeNode(_default);
				}
			}

			tree.onTreeNodeDoubleClick = function(ev) {
				doCallback();
			}

			function doCallback(){
				var result = treeType == 'multi' ? tree.getCheckedIds() : getActiveTreeNode(boxName);
				if( result ) {
					removeDialog();
					callback(result);
				}
			}

			$(".bts .b1", el).click(doCallback);
		}
	});
}

function popupForm(url, nodeName, params, callback, title, init) {
	removeDialog();

	var boxName = "popupForm";
	var el = $.createElement("div", "popupItem");
	el.innerHTML = '<h2>- ' + title + '：</h2>' +
		'<div id="' + boxName + '"><div class="loading"></div></div>' + 
	    '<div class="bts">' + 
	       '<input type="button" value="确 定" class="b1 tssbutton blue small">' + 
       	   '<input type="button" value="关 闭" class="b2 tssbutton blue small">' +  
	    '</div>';
	document.body.appendChild(el);
	$(el).addClass("dialog").center(300, 300).css("zIndex", "999");

	$(".bts .b2", el).click(removeDialog);

	params = params || {};

	$.ajax({
		url : url,
		method : "GET",
		onresult : function() {
			$.showWaitingLayer();

			var formXML = this.getNodeValue(nodeName);
			var rowNode = formXML.querySelector("data row");
			if(rowNode == null) {
				rowNode = $.XML.toNode("<row/>");
				formXML.querySelector("data").appendChild(rowNode);
			}
			$.each(params, function(key, value) {
				$.XML.setCDATA(rowNode, key, value);
			});

			$.cache.XmlDatas[nodeName] = formXML;
			var xform = $.F(boxName, formXML);
			init &&  init(xform);

			$(".bts .b1", el).click(function(){
				var condition = {};       
		        var formXML = $.cache.XmlDatas[nodeName];
	            var nodes = formXML.querySelectorAll("data row *");
	            $.each(nodes, function(i, node){
	                condition[node.nodeName] = $.XML.getText(node);
	            });

	            if( callback(condition) !== false ) {
	            	removeDialog();
	            }
			});
		}
	});
}

function removeDialog() {
	if($(".popupItem").length > 0) {
		$(".popupItem").each(function(i, el){
			$.removeNode(el);
			$.hideWaitingLayer();
		}); 
	}
}

function popupGrid(url, nodeName, title, params) {
	removeDialog();
	var boxName = "popupGrid";
	var el = $.createElement("div", "popupItem");
	el.innerHTML = '<h2>- ' + title + '</h2>' +
		'<Grid id="' + boxName + '"><div class="loading"></div></Grid>' + 
	    '<div class="bts">' + 
       	   '<input type="button" value="关闭" class="tssbutton blue small"/>' +  
	    '</div>';
	document.body.appendChild(el);
	$(el).addClass("dialog").css("width", "600px").css("height", "auto").center(600, 400);
	$("#" + boxName, el).css("minHeight", "200px").css("maxHeight", "400px");

	$(".bts .tssbutton", el).click(removeDialog);

	$.ajax({
		url: url,
		params: params || {},
		onresult: function() {
			$.G(boxName, this.getNodeValue(nodeName));
		}
	});
}

function popupGroupTree(callback) {
	var url = AUTH_PATH + "group/visibleList";
	if(IS_TEST) {
		url = "data/group_tree.xml";
	}
	popupTree(url, "GroupTree", {}, callback)
}

// 获取系统参数模块的配置信息
function getParam(key, callback) {
	$.getJSON(NO_AUTH_PATH + "param/json/simple/" + key, {}, 
        function(result) {
        	var val;
            if( result && result.length  && result[0] ) {
                val = result[0];
            }
            callback && callback(val);
        }, "GET");
}

// 发送邮件，支持html标签
function email(receivers, title, content) {
	$.post(AUTH_PATH + "message/email2", {"receivers": receivers, "title": title, "content": content});
}

function sendMessage(receivers, title, content, category, level, callback) {
	var params = {"receivers": receivers, "title": title, "content": content, "category": category, "level": level};
	$.post(AUTH_PATH + "message", params, callback);
}

function listMessages(callback) {
	$.getJSON(
		AUTH_PATH + "message/list", {},
		function() {
			var messages = this.getResponseJSON();
			callback && callback(messages);
		}, 
		"GET"
	);
}

function getNewMessageNum(callback) {
	$.getJSON(
		AUTH_PATH + "message/num", {},
		function() {
			var num = this.getResponseJSON();
			callback && callback(num);
		}, 
		"GET"
	);
}

function checkUploadFile(fileValue) {
	var blacklist = ['php','php3','php5','phtml','asp','aspx','jsp','cfc','pl','bat','exe','dll','reg','cgi'];
    var flag = false;
    blacklist.each(function(i, x){
        if( fileValue.indexOf("." + x) > 0 ) {
            flag = true;
        }
    });
    return flag;
}
