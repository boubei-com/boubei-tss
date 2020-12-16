CONTEXTPATH = "tss";

/*  AJAX相关封装
    $.ajax({
        url : url,
        method : "GET",
        headers : { "noAlert": true },
        params : {......},
        exEmpty: true,
        ondata : function() { },
        onresult : function() { },
        onexception : function() { },
        onsuccess : function() { }
    });
*/
;(function ($, factory) {

    $.HttpRequest = factory($);

    $.ajax = function(arg) {
        var request = new $.HttpRequest();
        request.url = arg.url;
        request.type = arg.type;
        request.method = arg.method || "POST";
        request.waiting = arg.waiting || false;
        request.async = arg.async != false;

        request.params  = arg.params  || {};
        request.headers = arg.headers || {};
        request.exEmpty = arg.exEmpty != false;

        if(arg.formNode) {
            request.setFormContent(arg.formNode);
        }

        request.ondata = arg.ondata || request.ondata;
        request.onresult = arg.onresult || request.onresult;
        request.onsuccess = arg.onsuccess || request.onsuccess;
        request.onexception = arg.onexception || function(errorMsg) {
            errorMsg.description && console.log(errorMsg.description); // 遇到异常却看不到任何信息，可尝试放开这里的注释
            errorMsg.msg && console.log(errorMsg.msg);
        };

        request.send();
    };

    $.Ajax = $.AJAX = $.ajax;

    $.getJson = $.getJSON = function(url, params, callback, method, waiting) {
        var _params = params;
        if (tssJS.isFunction(params)) { // no params
            _params = {};
            waiting = method;
            method = callback;
            callback = params;
        }
        $.ajax({
            url : url,
            type : "json",
            method : method || "POST",
            params : _params,
            waiting : waiting !== false, 
            ondata : function() { 
                var data = this.getResponseJSON();
                callback(data);
            }
        });
    };

    $.get = function(url, params, callback, waiting) {
        $.getJSON(url, params, callback, "GET", waiting);
    };

    $.post = function(url, params, callback, method) {
        $.ajax({
            url : url,
            type : "json",
            method : method || "POST",
            params : params,
            waiting : true, 
            ondata : function() { 
                var data = this.getResponseJSON();
                callback && callback(data);
            }
        });
    };

    $.delete = function(url, params, callback) {
        $.post(url, params, callback, "DELETE");
    };

    $.getXml = $.getXML = function(url, params, callback, method, waiting) {
        $.ajax({
            url : url,
            type : "xml",
            method : method || "POST",
            params : params, 
            waiting : waiting || false, 
            onresult : function() { 
                var data = this.getResponseXML();
                callback(data);
            }
        });
    };

})(tssJS, function ($) {

    'use strict';

    var 
    /* 通讯用XML节点名 */
    _XML_NODE_RESPONSE_ROOT    = "Response",
    _XML_NODE_REQUEST_ROOT     = "Request",
    _XML_NODE_RESPONSE_ERROR   = "Error",
    _XML_NODE_RESPONSE_SUCCESS = "Success",
    _XML_NODE_REQUEST_NAME     = "Name",
    _XML_NODE_REQUEST_VALUE    = "Value",
    _XML_NODE_REQUEST_PARAM    = "Param",

    /* HTTP响应解析结果类型 */
    _HTTP_RESPONSE_DATA_TYPE_EXCEPTION = "exception",
    _HTTP_RESPONSE_DATA_TYPE_SUCCESS = "success",
    _HTTP_RESPONSE_DATA_TYPE_DATA = "data",

    /* HTTP响应状态 */
    _HTTP_RESPONSE_STATUS_LOCAL_OK  = 0,    // 本地OK
    _HTTP_RESPONSE_STATUS_REMOTE_OK = 200,  // 远程OK

    /* HTTP超时(3分钟) */
    _HTTP_TIMEOUT = 3*60*1000,

    popupMessage = function (msg) {
        ($.alert || alert)(msg);
    },

    /*
     *  XMLHTTP请求对象，负责发起XMLHTTP请求并接收响应数据。例:
            var request = new HttpRequest();
            request.url = URL_GET_USER_NAME;
            request.addParam("loginName", loginName);
            request.setHeader("appCode", APP_CODE);

            request.onresult = function(){
                // 处理响应结果
            }
            request.send();
     */
    HttpRequest = function() {
        this.url;
        this.method = "POST";
        this.type   = "xml"; // "xml or json"
        this.async  = true;
        this.params = {};
        this.headers = {};
        this.waiting = false;
        this.exEmpty = true;

        this.responseText;
        this.responseXML;

        if( window.XMLHttpRequest ) {
            this.xmlhttp = new XMLHttpRequest();
        } 
        else {
            popupMessage("您的浏览器版本过旧，不支持XMLHttpRequest，请先升级浏览器。");
        }
    };

    HttpRequest.prototype = {

        /* 设置请求头信息  */
        setHeader: function(name, value) {
            this.headers[name] = value;
        },

        addParam: function(name, value) {
            this.params[name] = value;
        },

        /* 设置xform专用格式发送数据 */
        setFormContent: function(dataNode) {
            if(dataNode == null || dataNode.nodeName != "data") return;

            var nodes = dataNode.querySelectorAll("row *");
            for(var i = 0; i < nodes.length; i++) {
                var node = nodes[i];
                this.addParam(node.nodeName, $.XML.getText(node));
            }
        },

        /* 获取响应数据源代码 */
        getResponseText: function() {
            return this.responseText;
        },

        /* 获取响应数据XML文档 */
        getResponseXML: function() {
            return this.responseXML;
        },

        getResponseJSON: function() {
            var obj = $.parseJSON(this.responseText);
            if(obj && obj.errorMsg) {
                popupMessage(obj.errorMsg);
            }
            
            return obj;
        },

        /*
         *  获取响应数据XML文档指定节点对象值
         *  参数： string:name             指定节点名
         *  返回值：any:value               根据节点内容类型不同而定
         */
        getNodeValue: function(name) {
            if(this.responseXML == null) return;

            var node = this.responseXML.querySelector(_XML_NODE_RESPONSE_ROOT + ">" + name);
            if(node == null) return;

            var data;
            var childNodes = node.childNodes; 
            for(var i = 0; i < childNodes.length; i++) {
                var childNode = childNodes[i];
                switch (childNode.nodeType) {
                    case $.XML._NODE_TYPE_TEXT:
                        if(childNode.nodeValue.replace(/\s*/g, "") != "") {
                            data = childNode;
                        }
                        break;
                    case $.XML._NODE_TYPE_ELEMENT:
                    case $.XML._NODE_TYPE_CDATA:
                        data = childNode;
                        break;
                }
                
                if( data ) break;
            }

            if( data ) {
                switch(data.nodeType) {
                    case $.XML._NODE_TYPE_ELEMENT:
                        return data;
                    case $.XML._NODE_TYPE_TEXT:
                    case $.XML._NODE_TYPE_CDATA:
                        return data.nodeValue;
                }
            }
        },

        /* 发起XMLHTTP请求 */
        send: function(wait) {
            var oThis = this;

            try {
                this.waiting && $.showWaitingLayer();

                this.xmlhttp.onreadystatechange = function() {
                    if(oThis.xmlhttp.readyState == 4) {
                        oThis.clearTimeout();

                        var response = {};
                        response.responseText = oThis.xmlhttp.responseText;
                        response.responseXML  = oThis.xmlhttp.responseXML;
                        response.status       = oThis.xmlhttp.status;
                        response.statusText   = oThis.xmlhttp.statusText;

                        if(oThis.isAbort) {
                            if(oThis.waiting) $.hideWaitingLayer();
                        }
                        else {
                            setTimeout( function() {
                                oThis.abort();

                                if(oThis.waiting) $.hideWaitingLayer();
                                oThis.onload(response);

                            }, 100);
                        }
                    }
                }

                this.packageRequestParams();
                this.xmlhttp.open(this.method, this.url, this.async);
                 
                this.setTimeout(); // 增加超时判定
                this.customizeRequestHeader();

                this.xmlhttp.send(this.requestBody);
            } 
            catch (e) {
                if(oThis.waiting) $.hideWaitingLayer();

                var result = {
                    dataType: _HTTP_RESPONSE_DATA_TYPE_EXCEPTION,
                    type: 1,
                    msg: e.description || e.message
                };

                this.onexception(result);
            }
        },

        /* 超时中断请求 */
        setTimeout: function(noConfirm) {
            var oThis = this;

            this.timeout = setTimeout(function() {
                if(noConfirm != true && confirm("服务器响应较慢，需要中断请求吗？") == true) {
                    oThis.isAbort = true;
                    oThis.abort();
                    oThis.isAbort = false;
                }
                else {
                    oThis.clearTimeout();
                    oThis.setTimeout(true);
                }
            }, _HTTP_TIMEOUT);
        },

        /* 清除超时 */
        clearTimeout: function() {
            clearTimeout(this.timeout);
        },

        /* 对发送数据进行封装，以XML格式发送 */
        packageRequestParams: function() {
            if( (this.method||"").toUpperCase() == 'GET') { // 将params里参数拼到url
                var url = this.url;
                $.each(this.params, function(key, val) {
                    if(url.indexOf("?") < 0) {
                        url += '?';
                    } else {
                        url += '&';
                    }
                    url += key+ "=" + val;
                });
                this.url = encodeURI( url );
                return;
            }

            var contentXml = $.parseXML("<" + _XML_NODE_REQUEST_ROOT+"/>");
            var contentXmlRoot = contentXml.documentElement;
         
            for(var name in this.params) {
                var value = this.params[name];
                if( !$.isNullOrEmpty(value) || !this.exEmpty ) {
                    var paramNode = $.XML.createNode(_XML_NODE_REQUEST_PARAM);
                    paramNode.appendChild($.XML.appendCDATA(_XML_NODE_REQUEST_NAME, name));
                    paramNode.appendChild($.XML.appendCDATA(_XML_NODE_REQUEST_VALUE, value));

                    contentXmlRoot.appendChild(paramNode);
                }
            }

            this.requestBody = $.XML.toXml(contentXml);
            /* 对参数条件进行加密 */
            if(location.hostname != "localhost" && location.hostname != "127.0.0.1") {
                this.headers.encodeKey = this.headers.encodeKey || 12;
                if( this.headers.encodeKey ) {
                    this.requestBody = $.encode( this.requestBody, this.headers.encodeKey );
                } 
            }           
        },

        /* 自定义请求头信息 */
        customizeRequestHeader: function() {
            this.xmlhttp.setRequestHeader("REQUEST-TYPE", "xmlhttp");
            this.xmlhttp.setRequestHeader("CONTENT-TYPE", "text/xml");
            this.xmlhttp.setRequestHeader("CONTENT-TYPE", "application/octet-stream");

            // 设置header里存放的参数到requestHeader中
            var oThis = this;
            $.each(this.headers, function(item, itemValue) {
                try {
                    oThis.xmlhttp.setRequestHeader( item, String(itemValue) );
                } catch (e) { // chrome往header里设置中文会报错
                }
            });

            // 当页面url具有参数token则加入Cookie（可用于跨应用转发，见redirect.html）
            var token = $.Query.get("token");
            if( token ) {
                var exp = new Date();  
                exp.setTime(exp.getTime() + (30*1000));
                var expires = exp.toGMTString();  // 过期时间设定为30s
                $.Cookie.setValue("token", token, expires, "/" + CONTEXTPATH);
            }
        },

        /*
         *  加载数据完成，对结果进行处理
         *  参数： Object:response     该对象各属性值继承自xmlhttp对象
         */
        onload: function(response) {
            this.responseText = response.responseText;

            var _this = this;
            if( ['need_sms_check_code','need_email_check_code','need_img_check_code'].contains(this.responseText) ) {
                var codeType = _this.responseText;
                $.checkCode(codeType, function(randomKey) {
                    _this.addParam("randomKey", randomKey);
                    _this.send();
                });
                return;
            }

            //远程(200) 或 本地(0)才允许
            var httpStatus = response.status; 
            if(httpStatus != _HTTP_RESPONSE_STATUS_LOCAL_OK && httpStatus != _HTTP_RESPONSE_STATUS_REMOTE_OK) {
                var param = {
                    dataType: _HTTP_RESPONSE_DATA_TYPE_EXCEPTION,
                    type: 1,
                    source: this.responseText,
                    msg: "HTTP " + httpStatus + " 错误\r\n" + response.statusText,
                    description: "请求远程地址\"" + this.url + "\"出错"
                };

                new Message_Exception(param, this);
                return;
            }

            // JSON数据：因json的请求返回数据非XML格式，但出异常时异常信息是XML格式，所以如果没有异常，则直接执行ondata
            if(this.type == "json" && this.responseText.indexOf("<Error>") < 0 && this.responseText.indexOf("<script>") < 0) {
                this.ondata();
                return;
            }

            // XML数据：解析返回结果，判断是success、error or 普通XML数据
            var rp = new HTTP_Response_Parser(this.responseText);
            this.responseXML = rp.xmlValueDom;

            // 当返回数据中含脚本内容则自动执行
            var script = this.getNodeValue("script");
            if( script ) {
                $.createScript(script); // 创建script元素并添加到head中.
                return;
            }

            if(rp.result.dataType == _HTTP_RESPONSE_DATA_TYPE_EXCEPTION) {
                new Message_Exception(rp.result, this);
            }
            else if(rp.result.dataType == _HTTP_RESPONSE_DATA_TYPE_SUCCESS) {
                new Message_Success(rp.result, this);
            }
            else {
                this.ondata();
                this.onresult();
            }
        },

        // 定义空方法做为默认的回调方法
        ondata: function() { },
        onresult: function() { },
        onsuccess: function() { },
        onexception: function() { },

        /* 终止XMLHTTP请求 */
        abort: function() {
            if( this.xmlhttp ) {
                this.xmlhttp.abort();
            }
        }
    };

    var 

    /*
     *  对象名称：HTTP_Response_Parser对象
     *  职责：负责分析处理后台响应数据
     *
     *  成功信息格式：
     *  <Response>
     *      <Success>
     *          <type>1</type>
     *          <msg><![CDATA[ ]]></msg>
     *          <description><![CDATA[ ]]></description>
     *      </Success>
     *  </Response>
     *
     *  错误信息格式：
     *  <Response>
     *      <Error>
     *          <type>1</type>
     *          <relogin>1</relogin>
     *          <msg><![CDATA[ ]]></msg>
     *          <description><![CDATA[ ]]></description>
     *      </Error>
     *  </Response>
     */
    HTTP_Response_Parser = function(responseText) {
        this.source = responseText;
        this.result = {};

        try {
            this.xmlValueDom = $.parseXML(responseText);
        } catch (e) {
            // 尝试以JSON解析，如能解析，则不是异常，是发起ajax请求时没有填写type: "json"
            try { if( $.parseJSON(responseText) ) return; } 
            catch (e) { }
            
            console.log(e);
            this.result.dataType = _HTTP_RESPONSE_DATA_TYPE_EXCEPTION;
            this.result.source = this.source;
            this.result.msg = "服务器异常";
            this.result.description = $.XML.getParseError(this.xmlValueDom);
            return;
        }
 
        var responseNode = this.xmlValueDom.querySelector(_XML_NODE_RESPONSE_ROOT);
        var isSuccessOrError = false;

        if(responseNode) {
            if( responseNode.querySelector(_XML_NODE_RESPONSE_ERROR) ) { // Error
                this.result.dataType = _HTTP_RESPONSE_DATA_TYPE_EXCEPTION;
                this.result.source = this.source;
                isSuccessOrError = true;
            }
            else if( responseNode.querySelector(_XML_NODE_RESPONSE_SUCCESS) ) { // Success
                this.result.dataType = _HTTP_RESPONSE_DATA_TYPE_SUCCESS;
                isSuccessOrError = true;
            } 
        }
 
        if(isSuccessOrError) {
            var detailNodes = responseNode.querySelectorAll("* * *");
            var oThis = this;
            $.each(detailNodes, function(index, node) {
                oThis.result[node.nodeName] = $.XML.getText(node);
            });
        } 
        else {
            this.result.dataType = _HTTP_RESPONSE_DATA_TYPE_DATA; //  1:普通XML数据节点（非Success、Erroe）; 2:非XML（text、json）
        }
    },

    /*
     *  对象名称：Message_Success对象
     *  职责：负责处理成功信息
     */
    Message_Success = function(info, request) {
        request.ondata();

        if( info.type != "0" ) {
            
            !request.headers.noAlert && popupMessage(info.msg);
            $("#alert_box").addClass("ajax_msg_box");

            // 3秒后自动自动隐藏成功提示信息
            setTimeout(function() {
                $("div.ajax_msg_box").hide();
            }, 3000);
        }

        request.onsuccess(info);
    },

    /*
     *  对象名称：Message_Exception对象
     *  职责：负责处理异常信息
     *
     *  注意：本对象除了展示异常信息外，
     *  还可以根据是否需要重新登录来再一次发送request请求，注意此处参数Message_Exception(param, request)，该
     *  request依然还是上一次发送返回异常信息的request，将登陆信息加入后（loginName/pwd等，通过_relogin.htm页面获得），
     *  再一次发送该request请求，从而通过AutoLoginFilter的验证，取回业务数据。  
     *  这样做的好处是，当session过期需要重新登陆时，无需离开当前页面回到登陆页登陆，保证了用户操作的连贯性。
     * 
     * info.type：(参考 ErrorMessageEncoder)
     * <li>0－不显示
     * <li>1－普通业务逻辑错误信息，没有异常发生的
     * <li>2－有异常发生，同时被系统捕获后添加友好错误消息的
     * <li>3－其他系统没有预见的异常信息
     */
    Message_Exception = function(info, request) {
        var str = [];
        str[str.length] = "Error";
        str[str.length] = "type=\"" + info.type + "\"";
        str[str.length] = "msg=\"" + info.msg + "\"";
        str[str.length] = "description=\"" + info.description + "\"";
        str[str.length] = "source=\"" + (info.source || "") + "\"";

        if( info.msg && info.type != "0" && info.relogin != "1") {
            popupMessage(info.msg);
            console.log(str.join("\r\n"))
        }

        request.onexception(info);

        // 是否需要重新登录
        if(info.relogin == "1") {
            /* 重新登录前，先清除token cookie，防止在门户iframe登录平台应用（如DMS），而'/tss'目录下的token依旧是过期的，
             * 这样再次点击菜单（需redirect.html跳转的菜单）时还是会要求重新登录。 */
            $.Cookie.del("token", "");
            $.Cookie.del("token", "/");
            $.Cookie.del("token", "/tss");
            $.Cookie.del("token", "/" + CONTEXTPATH);

            location.href = "/" + CONTEXTPATH + "/login.html";
        }
    }   

    return HttpRequest;
});