FROMEWORK_CODE = "TSS";
APP_CODE    = "TSS";
APPLICATION = APP_CODE.toLowerCase();
CONTEXTPATH = APPLICATION + "/";

// 获取系统参数模块的配置信息
function getParam(key, callback) {
    $.getJSON("/tss/param/json/simple/" + key, {}, 
        function(result) {
            var val;
            if( result && result.length  && result[0] ) {
                val = result[0];
            }
            callback && callback(val);
        }, "GET", false);
}

function initUserInfo(callback) {
    $.ajax({
        url : "/tss/auth/user/operatorInfo",
        method : "POST",
        onresult : function() {
            var userName = this.getNodeValue("name");
            $("#userInfo").html(userName || '个人信息');

            callback();
        }
    });
}

function su() {
    $.prompt("请输入您要切换的账号", "切换账号", function(value) {
        if ( $.isNullOrEmpty(value) ) return alert("账号不能为空");
        $.ajax({
            url: "/tss/si/su?uName=Admin&uSign=1", // 装作API CALL，避免踢人
            method: "PUT",
            waiting: true,
            params: {"sessionId": $.Cookie.getValue("JSESSIONID"), "target": value},
            ondata: function() { 
                $.alert("账号切换完成，请刷新页面.");
            }
        });
    }); 
}

function logout() {
    $.ajax({
        url : "/tss/logout.in",
        method : "GET",
        onsuccess : function() { 
            $.Cookie.del("token", "");
            $.Cookie.del("token", "/");
            $.Cookie.del("token", "/tss");
            $.Cookie.del("token", "/tss/");
            location.href = "/tss/login.html";
        }
    });
}

function dbsx() {
  $.get("/tss/auth/message/num", {}, function(num) {
      if(num > 0) {
        $.tssTip("最近三天，您的站内信箱里有<b> " + num + " </b>条新消息，<a href='javascript:void(0)' onclick='openMsgPage()'>点击查看。</a>");
      }
    }
  );
}

/* 禁止鼠标右键 
(function() {
    document.oncontextmenu = function(ev) {
        ev = ev || window.event;
        var srcElement = $.Event.getSrcElement(ev);
        var tagName = srcElement.tagName.toLowerCase();
        if("input" != tagName && "textarea" != tagName) {
            $.Event.cancel(ev);            
        }
    }
})();
*/

function openUrl(url, dialog) {
    if (url == "#") return;

    var id = "if" + tssJS.hashCode(url);
    if( dialog ) { // 弹窗打开
        window.open("/tss/" + url);
        return;
    }

    // 在框架页里打开
    var $iframe = $("#" + id);
    if( $iframe.length == 0 ) {
        var iframe = $.createElement("iframe", "tssIFrame", id);
        $iframe = $(iframe);
        $iframe.attr("frameborder", "0").attr("scrolling", "auto");
        $(document.body).appendChild(iframe);

        resizeIFrame();
        $iframe.attr("src", url);
    } 
    else { // 打开内嵌页的leftBar，如果有的话
        var sonWindow = $iframe[0].contentWindow;
        sonWindow.openPalette && sonWindow.openPalette();
    }

    $(".tssIFrame").addClass("hidden").removeClass("open").css("background", "white");
    $iframe.removeClass("hidden").addClass("open");
}

// report_portlet.html  的 feedback()方法调用到这里
function feedback(module) {
    openUrl( encodeURI('modules/dm/recorder.html?rctable=x_feedback' + (module ? "&udf="+module : "")), true, "建议反馈" );
}

function manageJob() {
    openUrl( encodeURI('modules/dm/recorder.html?rctable=component_job_def') );
}

function manageETLTask() {
    openUrl( encodeURI('modules/dm/recorder.html?rctable=dm_etl_task') );
}

function manageToken() {
    openUrl( encodeURI('modules/dm/recorder.html?rctable=um_user_token') );
}

function releaseModule() {
    openUrl( encodeURI('modules/dm/recorder.html?rctable=cloud_module_def') );
}

function fixUserInfo() {
    $.openIframePanel("p1", "个人信息", 760, 320, "modules/um/_userInfo.htm", true);
    tssJS.getJSON("/tss/auth/user/has?refreshFlag=true", {}, function(info) { }, "GET");
}

function openMsgPage() {
    openUrl('modules/um/message.html');
}

function changePasswd() {
    $.openIframePanel("p2", "", 440, 360, "modules/um/_password.htm", true);
}

// 监听IFrame的click事件
var IframeOnClick = {  
    resolution: 200,  
    iframes: [],  
    interval: null,  
    Iframe: function() {  
        this.element = arguments[0];  
        this.cb = arguments[1];   
        this.hasTracked = false;  
    },  
    track: function(element, cb) {  
        this.iframes.push(new this.Iframe(element, cb));  
        if (!this.interval) {  
            var _this = this;  
            this.interval = setInterval(function() { _this.checkClick(); }, this.resolution);  
        }  
    },  
    checkClick: function() {  
        if (document.activeElement) {  
            var activeElement = document.activeElement;  
            for (var i in this.iframes) {  
                if (activeElement === this.iframes[i].element) { // user is in this Iframe  
                    if (this.iframes[i].hasTracked == false) {   
                        this.iframes[i].hasTracked = true;
                        var ifrObj = this.iframes[i];
                        setTimeout(function() {
                            ifrObj.cb( ifrObj.element ); 
                        }, 500);
                    }  
                } else {  
                    this.iframes[i].hasTracked = false;  
                }  
            }  
        }  
    }  
};

// ----------------------------------------- 顶部跑马灯公告栏 ----------------------------------------------------

var notice_channel = 2;  // 顶部通知跑马灯
var host = window.location.host;

function showNotice(id, title, htmlRef, issueDate) {
    tssJS.Cookie.setValue("Article" +host+ id + issueDate, "read");
    var url = htmlRef || ("/tss/notice.html?articleId=" + id);
    $.openIframePanel("noticePanel", "&nbsp;-&nbsp;" + title, 1080, 600, url);
}

let initMQ = false;
function queryNotice(){
    if( $("#notice").length == 0 ) return;
    
    var request = new $.HttpRequest();
    request.url =  "/tss/auth/article/list/xml/" +notice_channel+ "/1/5/false";
    request.method = "GET";
    request.onresult = function(){
        var articleList = this.getNodeValue("ArticleList");
        var id1, title1, issueDate1;
        $("item", articleList).each(function(i, item){
            var id      = $( "id", item ).text(), 
                title   = $( "title", item ).text(),
                htmlRef = $( "htmlRef", item ).text(),
                summary = $( "summary", item ).text(),
                issueDate = $( "issueDate", item ).text();
            var a = tssJS.createElement("a");

            a.href = "javascript:void(0);";
            $(a).attr("onclick", "showNotice(" +id+ ", '" +title+ "', '" +htmlRef+ "')");
            $(a).html( "【" + $( "issueDate", item ).text() + "】" + title);

            $("#s1").appendChild(a);

            if( summary && summary.indexOf('重要') >= 0 && !tssJS.Cookie.getValue("Article" +host+ id + issueDate)) {
                id1 = id;
                title1 = title;
                issueDate1 = issueDate;
            }
        });

        if( id1 ) {
            showNotice(id1, title1,null,issueDate1);
        }

        if( !initMQ ){
            showMQ();
            initMQ = true;
        }
    }
    request.send();

    function showMQ() {
        var speed = 100
        var nDiv = $("#notice")[0];
        var s1 = $("#s1")[0];
        var s2 = $("#s2")[0];

        $(s2).html( $(s1).html() );

        function marquee(){
            if(nDiv.scrollLeft >= s2.offsetWidth) {
                nDiv.scrollLeft -= s1.offsetWidth
            } else {
                nDiv.scrollLeft += 4;
            }
        }

        var mq = setInterval(marquee, speed);
        nDiv.onmouseover = function() { clearInterval(mq); };
        nDiv.onmouseout  = function() { mq = setInterval(marquee, speed); };
    }
};
// ----------------------------------------- 顶部跑马灯公告栏End ----------------------------------------------------

