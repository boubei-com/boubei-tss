

/* 
 * 跨域调用支持 
 * $.JSONP.getJSON(
        "http://www.boubei.com/tss/data/jsonp/GetAllVisitors", 
        {'param1': 'hello jsonp!'}, 
        function(result) { console.log(result.length); }
   );
 */
;(function($) {

    $.JSONP = {
        // url组装
        parseParams: function(params) {
            var ret = "";
            if(typeof params === "string") {
                ret = params;
            }
            else if(typeof params === "object") {
                for(var key in params) {
                    ret += "&" + key + "=" + encodeURIComponent(params[key]);
                }
            }

            // 加个时间戳，防止url被浏览器缓存
            ret += "&_time=" + $.now();

            return ret.substr(1);
        },
        
        getJSON: function(url, params, func) {
            
            url = url + (url.indexOf("?") === -1 ? "?" : "&") + this.parseParams(params);
            
            // 函数名称
            var name;

            // 检测callback的函数名是否已经定义
            var match = /jsonpCallback=(\w+)/.exec(url);
            if(match && match[1]) {
                name = match[1];
            } 
            else { // 如果未定义函数名的话随机成一个函数名
                name = "jsonp_" + $.getUniqueID();
                url = url + (url.indexOf("?") === -1 ? "?" : "&") + "jsonpCallback=" + name;
            }
            
            // 创建一个script元素, 插入head
            var script = $.createElement("script");
            script.src = url;
            script.id = name; // 设置id，为了后面可以删除这个元素

            var head = document.head || document.getElementsByTagName('head')[0];
            head.appendChild(script);
            
            // 把传进来的函数重新组装，并把它设置为全局函数，远程就是调用这个函数
            window[name] = function(json) {   
                window[name] = undefined; // 执行这个函数后，要销毁这个函数
 
                // 删除head里面插入的script，不污染整个DOM
                $.removeNode($("#" + name)[0]);
                
                // 执行传入的的函数
                func(json, params);
            };
        }
    }

})(tssJS);