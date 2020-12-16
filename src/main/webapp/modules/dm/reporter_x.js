/* 后台响应数据节点名称 */
XML_SOURCE_TREE = "SourceTree";
XML_REPORT_DATA = "ReportData";
XML_SOURCE_INFO = "SourceInfo";

PAGESIZE = 100;

/* XMLHTTP请求地址汇总 */
URL_REPORT_DATA    = NO_AUTH_PATH + "data/";
URL_REPORT_JSON    = NO_AUTH_PATH + "data/json/";
URL_REPORT_EXPORT  = NO_AUTH_PATH + "data/export/";

// 获取导出分离机器的配置，如果有的话, eg: http://10.45.10.96:8080/tss/data/export/
 getParam('report_export_url', function(url) {
    if( url ) {
        URL_REPORT_EXPORT  = url + URL_REPORT_EXPORT;
    }
 });

if(IS_TEST) {
    URL_REPORT_DATA    = "data/report_data.xml?";
    URL_REPORT_JSON    = "data/report_data.json?";
    URL_REPORT_EXPORT  = "data/_success.xml?";  
}

function getServiceUrl(reportId, displayUri) {
    $.Query.init(displayUri);
    var url = $.Query.get("service") || (URL_REPORT_JSON + reportId); // 优先使用展示地址里指定的模板地址
    $.Query.init();

    return url;
}

function genQueryForm(title, paramDefine) {
    var $panel = $("#searchFormDiv");
    $panel.show(true);
    $panel.html("").panel("【" + title + "】查询条件", '<div id="searchForm"></div>');

    var buttonBox = [];
    buttonBox[buttonBox.length] = "<TR>";
    buttonBox[buttonBox.length] = "  <TD colspan='2' height='46'><div class='buttonBox'>";
    buttonBox[buttonBox.length] = "     <a class='tssbutton small white btSearch' id='btSearch'>查 询</a> - ";
    buttonBox[buttonBox.length] = "     <a class='tssbutton small white btDownload' id='btDownload'>查询并导出</a>";
    buttonBox[buttonBox.length] = "  </div></TD>";
    buttonBox[buttonBox.length] = "</TR>";

    var searchForm = $.json2Form("searchForm", paramDefine, buttonBox.join(""));
    $.cache.XmlDatas["searchFormXML"] = searchForm.template.sourceXML;

    return searchForm;
}

function getParams() {
    var searchFormXML = $.cache.XmlDatas["searchFormXML"];
    var dataNode = searchFormXML.querySelector("data");

    var params = {};
    var nodes = dataNode.querySelectorAll("row *");
    for(var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        params[node.nodeName] = $.XML.getText(node);
    }

    $("#searchFormDiv").hide();
    return params;
}

function searchGridReport(reportId, params) {        
    var request = new $.HttpRequest();
    request.url = URL_REPORT_DATA + reportId + "/1/" + PAGESIZE;
    request.waiting = true;
    request.params = params;
    
    request.onresult = function() {
        var grid = $.G("grid", this.getNodeValue(XML_REPORT_DATA)); 
        var gridToolBar = $1("gridToolBar");

        var pageListNode = this.getNodeValue(XML_PAGE_INFO);        
        var callback = function(page) {
            request.url = URL_REPORT_DATA + reportId + "/" + page + "/" + PAGESIZE;
            request.onresult = function() {
                $.G("grid", this.getNodeValue(XML_REPORT_DATA)); 
            }               
            request.send();
        };

        $.initGridToolBar(gridToolBar, pageListNode, callback);
        
        grid.el.onScrollToBottom = function () {            
            var currentPage = gridToolBar.getCurrentPage();
            if(currentPage <= gridToolBar.getTotalPages() ) {
                var nextPage = parseInt(currentPage) + 1; 
                request.url = URL_REPORT_DATA + reportId + "/" + nextPage + "/" + PAGESIZE;
                request.onresult = function() {
                    $.G("grid").load(this.getNodeValue(XML_REPORT_DATA), true);
                    $.initGridToolBar(gridToolBar, this.getNodeValue(XML_PAGE_INFO), callback);
                }               
                request.send();
            }
        }

        $(".btDownload").click( function () {
            exportReport(reportId, params);
        });
    }
    request.send();
} 

function exportReport(reportId, params) {       
    var queryString = "?";
    $.each(params, function(key, value) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += (key + "=" + value);
    });

    // 因为导出服务器可能是独立的，发导出请求时需要带上在查询服务器上的登录信息
    var user = $.Cookie.getValue("iUserName");
    if(user) {
        if( queryString.length > 1 ) {
            queryString += "&";
        }
        queryString += "iUser=" + user;
    }
    
    // 为防止一次性查询出太多数据导致OOM，限制每次最多只能导出10万行，超出则提示进行分批查询
    $("#downloadFrame").attr( "src", encodeURI(URL_REPORT_EXPORT + reportId + "/1/100000" + queryString) );
}

// ------------------------------------------------- 多级下拉选择联动 ------------------------------------------------
/*
 *  多级下拉选择联动  getNextLevelOption(2, 1090, 1)
 *  参数： nextParam    下一级联动参数的序号（1->n）或 code
        serviceID       下一级联动的service地址             
        serviceParam    service接收的参数code
        curParamVal     当前联动参数字段的值, 自动获取到填入(xform控件)
 */
function getNextLevelOption(nextParam, serviceID, serviceParam, curParamVal) {
    if(nextParam == null || serviceID == null || serviceParam == null || $.isNullOrEmpty(curParamVal)) return;

    var paramELId = isInt(nextParam)    ? "param" + nextParam : nextParam;
    var paramName = isInt(serviceParam) ? "param" + serviceParam : serviceParam;
    
    // serviceID maybe is ID of report, maybe a serviceUrl
    var url = isInt(serviceID) ? '/tss/data/json/' + serviceID : serviceID;

    $.getNextLevelOption($.F("searchForm"), paramName, curParamVal, url, paramELId);
}

function isInt(x) {
    return /^-?[1-9]+[0-9]*]*$/.test(x)
}