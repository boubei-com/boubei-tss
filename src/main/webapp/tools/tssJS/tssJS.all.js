
;(function(window, undefined) {

    if( window.attachEvent ) {
        alert("您当前的IE浏览器版本过低，为能有更好的展示效果，建议升级到IE11，或换最新版Chrome、FireFox。");
    } 
    
    window.tssJS = (function() {

        // 构建tssJS对象
        var tssJS = function(selector, parent) {
            return new tssJS.fn.init(selector, parent, rootTssJS);
        },

        version = "1.2.0",

        // Map over the $ in case of overwrite
        _$ = window.$,

        rootTssJS,

        // The deferred used on DOM ready
        readyList = [],

        // Used for trimming whitespace
        trimLeft = /^\s+/,
        trimRight = /\s+$/,

        // JSON RegExp
        rvalidchars = /^[\],:{}\s]*$/,

        toString = Object.prototype.toString,
        trim = String.prototype.trim,
        push = Array.prototype.push,    

        ua = navigator.userAgent.toLowerCase(),
        mc = function(_regex) {
            return _regex.test(ua);
        },

        // [[Class]] -> type pairs
        class2type = {};

        // tssJS对象原型
        tssJS.fn = tssJS.prototype = {

            version: version,

            constructor: tssJS,

            init: function(selector, parent, rootTssJS) {
                // Handle $(""), $(null), or $(undefined)
                if (!selector) {
                    return this;
                }

                // Handle $(DOMElement)
                if (selector.nodeType || selector === document) {
                    this[0] = selector;
                    this.length = 1;
                    return this;
                }

                if (typeof selector === "string") {
                    return this.find(selector, parent);
                }

                if (tssJS.isFunction(selector)) {
                    return rootTssJS.ready(selector);
                }
            },

            size: function() {
                return this.length;
            },

            each: function(callback, args) {
                return tssJS.each(this, callback, args);
            },

            ready: function(fn, args) {
                // Attach the listeners
                tssJS.bindReady.call(this, fn, args);

                return this;
            },
        };

        // Give the init function the tssJS prototype for later instantiation
        tssJS.fn.init.prototype = tssJS.fn;

        // 通过tssJS.fn.extend扩展的函数，大部分都会调用通过tssJS.extend扩展的同名函数
        tssJS.extend = tssJS.fn.extend = function(fnMap) {
            fnMap = fnMap || {};
            for (var name in fnMap) {
                this[name] = fnMap[name];
            }

            // Return the modified object
            return this;
        };

        // 在tssJS上扩展静态方法
        tssJS.extend({

            // 释放$的 tssJS 控制权
            // 许多 JavaScript 库使用 $ 作为函数或变量名，tssJS 也一样。
            // 在 tssJS 中，$ 仅仅是 tssJS 的别名，因此即使不使用 $ 也能保证所有功能性。
            // 假如我们需要使用 tssJS 之外的另一 JavaScript 库，我们可以通过调用 $.noConflict() 向该库返回控制权。
            noConflict: function(deep) {
                // 交出$的控制权
                if (window.$ === tssJS) {
                    window.$ = _$;
                }

                return tssJS;
            },

            // Is the DOM ready to be used? Set to true once it occurs.
            isReady: false,

            // Handle when the DOM is ready
            ready: function(fn, args) {
                if (!tssJS.isReady) {
                    // 确保document.body存在
                    if (!document.body) {
                        setTimeout(function() {
                            tssJS.ready(fn, args);
                        },
                        10);
                        return;
                    }

                    // Remember that the DOM is ready
                    tssJS.isReady = true;

                    // If there are functions bound, to execute
                    if (fn) {
                        fn(args);
                    } 
                    else {
                        tssJS.each(readyList, function(i, name) {
                                var _ = readyList[i];
                                _.fn.call(_._this, _.args);
                            }
                        );

                        readyList = [];
                    }
                }
            },

            bindReady: function(fn, args) {
                readyList.push({
                    "_this": this,
                    "fn": fn,
                    "args": args
                });

                if (document.readyState === "complete") {
                    return setTimeout(tssJS.ready, 1);
                }

                document.addEventListener("DOMContentLoaded", DOMContentLoaded, false);
                window.addEventListener("load", tssJS.ready, false);
            },

            // 是否函数
            isFunction: function(obj) {
                return tssJS.type(obj) === "function";
            },

            // 是否数组
            isArray: Array.isArray || function(obj) { return tssJS.type(obj) === "array"; },

            // 简单的判断（判断setInterval属性）是否window对象
            isWindow: function(obj) {
                return obj && typeof obj === "object" && "setInterval" in obj;
            },

            // 获取对象的类型
            type: function(obj) {
                return obj == null ? String(obj) : class2type[toString.call(obj)] || "object";
            },

            // 是否空对象
            isEmptyObject: function(obj) {
                for (var name in obj) {
                    return false;
                }
                return true;
            },

            isNullOrEmpty: function(value) {
                return (value == null || (typeof(value) == 'string' && value.trim() == ""));
            },

            /**
             * 替换各种分隔符（tab、空格、回车、英文逗号、中文逗号）为统一分隔符
             */
            fixSplit: function(value, split) {
                split = split || ',';
                return (value||'').trim().replace(/(\t+|\r|\n|,|，|\s+|、)+/ig, split);
            },

            // 抛出一个异常
            error: function(msg) {
                throw msg;
            },

            // parseJSON把一个字符串变成JSON对象。(注：JSON.parse要求数据必须用双引号，eval转换时则不分单、双引号)
            parseJSON: function(data) {
                if (typeof data !== "string") {
                    return data;
                }

                // Make sure leading/trailing whitespace is removed 
                data = tssJS.trim(data);

                // 原生JSON API。反序列化是JSON.stringify(object)
                if (window.JSON && window.JSON.parse) {
                    try {
                        return window.JSON.parse(data);
                    } catch(e) {
                        return window.eval(data);
                    }
                }

                // ... 大致地检查一下字符串合法性
                if (rvalidchars.test(data.replace(rvalidescape, "@").replace(rvalidtokens, "]").replace(rvalidbraces, ""))) {
                    return (new Function("return " + data))();
                }
                tssJS.error("Invalid JSON: " + data);
            },

            // 解析XML 
            parseXML: function(data) {
                var parser = new DOMParser();
                var xml = parser.parseFromString(data, "text/xml");

                var tmp = xml.documentElement;

                if (!tmp || !tmp.nodeName || tmp.nodeName === "parsererror") {
                    console.log("Invalid XML: " + data);
                } else {
                    var errorNode = tmp.querySelector("parsererror");
                    if( errorNode ) {
                        console.log("Invalid XML: " + $.XML.getText(errorNode) );
                        console.log(data);
                    }
                }

                return xml;
            },

            // globalEval函数把一段脚本加载到全局context（window）中。
            // IE中可以使用window.execScript, 其他浏览器 需要使用eval。
            // 因为整个tssJS代码都是一整个匿名函数，所以当前context是tssJS，如果要将上下文设置为window则需使用globalEval。
            globalEval: function(data) {
                if (data && /\S/.test(data)) { // data非空
                    ( window.execScript || function(data) { window["eval"].call(window, data); } ) (data);
                }
            },

            execCommand: function(callback, param) {
                var returnVal;
                try {
                    if(tssJS.isFunction(callback)) {
                        returnVal = callback(param);
                    }
                    else if(callback) {
                        var rightKH = callback.indexOf(")");
                        if(rightKH < 0 && param) {
                            callback = callback + "('" + param + "')";
                        }
                        returnVal = eval(callback);
                    }
                } catch (e) {
                    console.log(e.message + ", " + e.stack);
                    returnVal = false;
                }
                return returnVal;
            },

            // 遍历对象或数组
            each: function(object, callback, args) {
                var name, i = 0,
                length = object.length,
                isObj = length === undefined || tssJS.isFunction(object);

                // 如果有参数args，调用apply，上下文设置为当前遍历到的对象，参数使用args
                if (args) {
                    if (isObj) {
                        for (name in object) {
                            if (callback.apply(object[name], args) === false) {
                                break;
                            }
                        }
                    } else {
                        for (; i < length;) {
                            if (callback.apply(object[i++], args) === false) {
                                break;
                            }
                        }
                    }
                }
                // 没有参数args，则调用call，上下文设置为当前遍历到的对象，参数设置为key or index 和 value
                else {
                    if (isObj) {
                        for (name in object) {
                            if (callback.call(object[name], name, object[name]) === false) {
                                break;
                            }
                        }
                    } else {
                        for (; i < length; i++) {
                            if (callback.call(object[i], i, object[i]) === false) {
                                break;
                            }
                        }
                    }
                }

                return object;
            },

            // 尽可能的使用本地String.trim方法，否则先过滤开头的空格，再过滤结尾的空格
            trim: trim ? function(text) { return trim.call(text); } :
                function(text) { return text.toString().replace(trimLeft, "").replace(trimRight, ""); },

            // 过滤数组，返回新数组；callback返回true时保留
            grep: function(_array, callback) {
                var ret = [], item;

                for (var i = 0, length = _array.length; i < length; i++) {
                    item = _array[i];
                    if (!!callback(item, i)) {
                        ret.push(item);
                    }
                }

                return ret;
            },

            /* 缓存页面数据（xml、变量等） */
            cache: {
                "Variables": {},
                "XmlDatas":  {}
            },

            /* 负责生成对象唯一编号（为了兼容FF） */
            uid: 0,
            getUniqueID: function(prefix) {
                return (prefix || "_") + String(this.uid ++ );
            },

            // 获取当前时间的便捷函数
            now: function(format) {
                if(format) {
                    return new Date().format('yyyy-MM-dd hh:mm:ss');
                }
                return (new Date()).getTime();
            },

            // 对数据进行简单加密
            encode: function(info, key) {
                if( info == null || typeof(info) != 'string') return "";

                var result = [];
                for(var i=0, length = info.length; i < length; i++) {
                    result.push( info.charCodeAt(i) ^ (key || 100) % 127 );
                }

                return result.join("X");
            },

            decode: function(info, key) {
                if( info == null || typeof(info) != 'string') return "";

                var result = [];
                info.split("X").each(function(){
                    result.push( String.fromCharCode(this ^ (key || 100) % 127) );
                });

                return result.join("");
            },

            hashCode: function(str) {
                var h = 0, len = str.length, t = 2147483648;
                for (var i = 0; i < len; i++) {
                    h = 31 * h + str.charCodeAt(i);
                    if(h > 2147483647) h %= t;
                }
                return h;
            },

            _close: function() {
                if ( $.isFirefox || $.isChrome) {
                    // window.location.href = "about:blank";
                } else {
                    window.opener = null;
                    window.open(" ", "_self");
                    window.close();
                }
            },
            
            isIE: mc(/.net/),
            isChrome: mc(/\bchrome\b/),
            isFirefox: mc(/\bfirefox\b/),
            isWebKit: mc(/webkit/),
            supportCanvas: !!document.createElement('canvas').getContext,
            isMobile: mc(/ipod|ipad|iphone|android|webOS|BlackBerry|IEMobile|Opera Mini/gi),
        });

        // Populate the class2type map
        tssJS.each("Boolean Number String Function Array Date RegExp Object".split(" "),
            function(i, name) {
                class2type["[object " + name + "]"] = name.toLowerCase();
            }
        );

        var DOMContentLoaded = (function() {
            return function() {
                document.removeEventListener("DOMContentLoaded", DOMContentLoaded, false);
                tssJS.ready();
            };
        })();

 
        rootTssJS = tssJS(document);

        // 到这里，tssJS对象构造完成，后边的代码都是对tssJS或tssJS对象的扩展
        return tssJS;

    })();

    /** -------------------------------- Add useful method --------------------------------------- */

    Array.prototype.each = function(fn, args) {
        $.each(this, fn, args);
        return this;
    };

    Array.prototype.contains = function(obj) {
        var i = this.length;
        while (i--) {
            if (this[i] === obj) {
                return true;
            }
        }
        return false;
    };

    Array.prototype.remove = function(item) {
        var i=0, n=0;
        for(; i < this.length; i++) {
            if(this[i] != item) {
                this[n++] = this[i];
            }
        }
        this.length = n;
    };

    Date.prototype.format = function(format) {
        var o = {
            "M+": this.getMonth() + 1,
            "d+": this.getDate(),
            "h+": this.getHours(),
            "m+": this.getMinutes(),
            "s+": this.getSeconds(),
            "q+": Math.floor((this.getMonth() + 3) / 3),  // quarter
            "S": this.getMilliseconds()
        }

        if (/(y+)/.test(format)) {
            format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
        }

        for (var k in o) {
            if (new RegExp("(" + k + ")").test(format)) {
                format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
            }
        }
        return format;
    };

    window.$ = window.tssJS;

    window.$1 = function(id) {
        return $("#" + id.replace(/\./gi, "\\."))[0];
    }

    window.$$ = function(id) {
        return document.getElementById(id);
    }

})(window);


// 扩展tssJS原型方法
; (function($) {
    $.fn.extend({

        find: function(selector, parent) {
            if(this[0]) {
                return $(selector, this[0]);
            }

            parent = parent || document;
            var elements = parent.querySelectorAll(selector);

            this.length = elements.length;
            for (var i = 0; i < this.length; i++) {
                this[i] = elements[i];
            }

            return this;
        },

        //设置CSS
        css: function(attr, value) {
            for (var i = 0; i < this.length; i++) {
                var el = this[i];
                if (arguments.length == 1) {
                    return $.getStyle(el, attr);
                }
                el.style[attr] = value;
            }
            return this;
        },

        height: function(height) {
            height = /^\+?[1-9][0-9]*$/.test(height) ? height + "px" : height;  // 300/300px/30%
            return this.css("height", height);
        },

        width: function(width) {
            width = /^\+?[1-9][0-9]*$/.test(width) ? width + "px" : width;  // 300/300px/30%
            return this.css("width", width);
        },

        is: function(selector) {
            return $.is(this[0], selector);
        },

        hasClass: function(className) {
            if(this.length == 0) {
                return false;
            }
            return $.hasClass(this[0], className);
        },

        // 添加Class
        addClass: function(className) {
            for (var i = 0; i < this.length; i++) {
                var el = this[i];
                if ( !$.hasClass(el, className) ) {
                    el.className += ' ' + className;
                }
            }
            return this;
        },

        // 移除Class
        removeClass: function(className) {
            var reg = new RegExp('(\\s|^)' + className + '(\\s|$)');
            for (var i = 0; i < this.length; i++) {
                var el = this[i];
                if ( $.hasClass(el, className) ) {
                    el.className = el.className.replace(reg, ' ').trim();
                }
            }
            return this;
        },

        removeClasses: function(classNames) {
            var tjObj = this;
            classNames.split(",").each(function(i, className){
                tjObj.removeClass(className);
            });

            return this;
        },

        // 设置innerHTML
        html: function() {
            for (var i = 0; i < this.length; i++) {
                var el = this[i];
                if (arguments.length == 0) {
                    return el.innerHTML;
                }
                el.innerHTML = arguments[0];
            }
            return this;
        },

        // 设置XML node text
        text: function() {
            for (var i = 0; i < this.length; i++) {
                if (arguments.length == 0) {
                    return $.XML.getText( this[i] );
                }
                $.XML.setText(this[i], arguments[0]);  
            }
            return this;
        },

        appendChild: function(el) {
            if ( this.length > 0 ) {
                this[0].appendChild(el);
            }
            return this;
        },

        remove: function() {
            for (var i = 0; i < this.length; i++) {
                $.removeNode(this[i]);
            }
            return this;
        },

        title: function(str) {
            for (var i = 0; i < this.length; i++) {
                this[i].title = str;
            }
            return this;
        },

        // 触发点击事件
        click: function(fn) {
            for (var i = 0; i < this.length; i++) {
                this[i].onclick = fn;
            }
            return this;
        },

        // 设置鼠标移入移出方法
        hover: function(over, out) {
            this.addEvent('mouseover', over);
            this.addEvent('mouseout', out);
            return this;
        },

        // 设置点击切换方法
        toggle: function() {
            for (var i = 0; i < this.length; i++) { 
                (function(el, args) {
                    var count = 0;
                    $.Event.addEvent(el, 'click', function() {
                        args[count++%args.length].call(this);
                    });
                })(this[i], arguments);
            }
            return this;
        },

        blur: function(fn) {
            this.removeEvent('blur', fn).addEvent('blur', fn);
            return this;
        },

        addEvent: function(eventName, fn, capture) {
            for (var i = 0; i < this.length; i++) {
                $.Event.addEvent(this[i], eventName, fn, capture);
            }
            return this;
        },

        removeEvent: function(eventName, fn, capture) {
            for (var i = 0; i < this.length; i++) {
                $.Event.removeEvent(this[i], eventName, fn, capture);
            }
            return this;
        },

        focus: function() {
            if ( this.length > 0 ) {
                this[0].focus();
            }
            return this;
        },

        //设置显示
        show: function(block) {
            for (var i = 0; i < this.length; i++) {
                this[i].style.display = block ? 'block' : '';
            }
            return this;
        },

        //设置隐藏
        hide: function() {
            this.css('display', 'none');
            return this;
        },

        // 设置物体居中
        center: function(width, height) {
            if (arguments.length == 0 && this.length > 0) {
                width  = this[0].clientWidth;
                height = this[0].clientHeight;
            }
            var left = ($.getInner().width - (width || 0) ) / 2;
            var top  = ($.getInner().height - (height || 0) ) / 2;
            return this.position(left, top);
        },

        position: function(left, top) {
            this.css('position', 'absolute').css('left', left + 'px').css('top', top + 'px');
            return this;
        },

        attr: function(name, value) {
            for (var i = 0; i < this.length; i++) {
                var el = this[i];
                if (arguments.length == 1) {
                    return el.getAttribute(name);
                }
                if (arguments.length == 2 && value == null) {
                    return el.removeAttribute(name);
                }
                el.setAttribute(name, value);
            }
            return this;
        },

        value: function() {
            if ( this.length > 0 ) {
                if (arguments.length == 0) {
                    return this[0].value;
                }
                else {
                    for (var i = 0; i < this.length; i++) {
                        this[i].value = arguments[0];
                    }
                }
            }
            return this;
        }
    });
})(tssJS);

// 扩展tssJS操作HTML DOMElement的静态方法
; (function($) {
    $.extend({

        radioValue: function(name) {
            var value;
            $("input[name='" + name + "']").each(function(i, item) {
                if(item.checked) {
                    value = item.value;
                }
            });
            return value;
        },

        hasClass: function(el, cn) {
            var reg = new RegExp('(\\s|^)' + cn + '(\\s|$)');
            return (' ' + el.className + ' ').match(reg);
        },

        is: function(el, selector) {
            return el.tagName.toLowerCase() == selector.toLowerCase();
        },

        // 获取视口大小
        getInner: function() {
            if (typeof window.innerWidth != 'undefined') {
                return {
                    width: window.innerWidth,
                    height: window.innerHeight
                }
            } else {
                return {
                    width: document.documentElement.clientWidth,
                    height: document.documentElement.clientHeight
                }
            }
        },

        // 获取Style。注：computedStyle: style 和 runtimeStyle 的结合
        getStyle: function(el, attr) {
            if (window.getComputedStyle) { // W3C
                return window.getComputedStyle(el, null)[attr];
            } 
            else if (el.currentStyle) { //IE
                return el.currentStyle[attr];
            }
            return null;
        },

        //  获取绝对位置
        absPosition: function(node) {
            var left, right, top, bottom, pEl = node;

            if (typeof node.getBoundingClientRect === 'function') {
                var clientRect = node.getBoundingClientRect();
                left = clientRect.left + window.pageXOffset;
                right = clientRect.right + window.pageXOffset;
                top = clientRect.top + window.pageYOffset;
                bottom = clientRect.bottom + window.pageYOffset;
            } else {
                left = pEl.offsetLeft;
                top = pEl.offsetTop;
                while ((pEl = pEl.offsetParent)) {
                    left += pEl.offsetLeft;
                    top += pEl.offsetTop;
                }

                right = left + node.offsetWidth;
                bottom = top + node.offsetHeight
            }
            return {
                "left": left,
                "top": top,
                "right": right,
                "bottom": bottom
            };
        },

        createElement: function(tagName, className, id) {
            var el = document.createElement(tagName);
            if (className) {
                $(el).addClass(className)
            }
            if(id) {
                el.id = id;
            }
            return el;
        },

        // 创建带命名空间的对象
        createNSElement: function(tagName, ns) {
            var tempDiv = document.createElement("DIV");
            tempDiv.innerHTML = "<" + ns + ":" + tagName + "/>";
            var el = tempDiv.firstChild.cloneNode(false);
            el.uniqueID = $.getUniqueID();

            $.removeNode(tempDiv);

            return el;
        },

        getNSElements: function(el, tagName, ns) {
            return el.getElementsByTagName(ns + ":" + tagName);
        },

        removeNode: function(node) {
            if (node == null) return;

            var parentNode = node.parentNode;
            if (parentNode) {
                parentNode.removeChild(node);
            }
        },

        /* 动态添加脚本 */
        createScript: function(script) {
            var scriptNode = $.createElement("script");
            $.XML.setText(scriptNode, script);
            $('head').appendChild(scriptNode);
        },
        /* 动态添加外挂js文件 */
        createScriptJS: function(jsFile) {
            var scriptNode = $.createElement("script");
            scriptNode.src = jsFile;
            $('head').appendChild(scriptNode);
        },

        /* 设置透明度 */
        setOpacity: function(obj, opacity) {
            if(opacity == null || opacity == "") {
                opacity = 100;
            }

            obj.style.opacity = opacity / 100;
            obj.style.filter = "alpha(opacity=" + opacity + ")";
        },

        waitingLayerCount: 0,
        waitingLayerZIndex: 998,

        showWaitingLayer: function () {
            if (!document.body) return;
            
            var waitingObj = $("#_waiting");
            if(waitingObj.length == 0) {
                var waitingDiv = document.createElement("div");    
                waitingDiv.id = "_waiting";
                document.body.appendChild(waitingDiv);

                var height = "100%";
                if( document.body.scrollHeight > document.body.clientHeight ) {
                    height = document.body.scrollHeight + "px";
                }
                $(waitingDiv).css("width", "100%").css("height", height)
                             .css("position", "absolute").css("left", "0px").css("top", "0px")
                             .css("cursor", "wait").css("zIndex", "998").css("background", "black");
                $.setOpacity(waitingDiv, 33);
            }
            else {
                waitingObj.show(true);
            }

            $.waitingLayerCount ++;
        },

        hideWaitingLayer: function(all) {
            $.waitingLayerCount && $.waitingLayerCount --;
            if(all) {
                $.waitingLayerCount = 0;
            }

            var waitingObj = $("#_waiting");
            if( waitingObj.length > 0 && $.waitingLayerCount <= 0 ) {
                waitingObj.hide();
            }
        }

    });
})(tssJS);

// 扩展tssJS 单元测试的静态方法
; (function($) {
    $.extend({
        /* 前台单元测试断言 */
        assertEquals: function(expect, actual, msg) {
            if (expect != actual) {
                $.error(msg + ": " + "[expect: " + expect + ", actual: " + actual + "]");
            }
        },

        assertTrue: function(result, msg) {
            if (!result && msg) {
                $.error(msg);
            }
        },

        assertNotNull: function(result, msg) {
            if (result == null && msg) {
                $.error(msg);
            }
        }
    });
})(tssJS);


/* 负责获取当前页面地址参数 */
; (function($) {

    $.extend({        
        Query: {
            items: {},

            getItems: function() {
                return items;
            },

            get: function(name, decode) {
                var str = items[name];
                return decode ? unescape(str) : str; // decode=true，对参数值（可能为中文等）进行编码
            },

            init: function(queryString) {
                items = {}; // 先清空
                queryString = queryString || window.location.search.substring(1);

                var params = queryString.split("&");
                for (var i = 0; i < params.length; i++) {
                    var param = params[i].split("=");
                    if(param.length == 2) {
                        var key = param[0].replace(/%20/g, "");
                        items[key] = param[1].trim();
                    }
                }
            }
        }
    });

    $.Query.init();

})(tssJS);

/* 
 * 负责管理页面上cookie数据.
 * Chrome只支持在线网站的cookie的读写操作，对本地html的cookie操作是禁止的。
 */
; (function($) {

    $.extend({
        Cookie: {
            setValue: function(name, value, expires, path) {
                value = value||"";
                if (expires == null) {
                    var exp = new Date();
                    exp.setTime(exp.getTime() + 365 * 24 * 60 * 60 * 1000);
                    expires = exp.toGMTString();
                }

                path = path || "/";
                window.document.cookie = name + "=" + escape(value) + ";expires=" + expires + ";path=" + path;
            },

            getValue: function(name) {
                var value = null;
                var cookies = window.document.cookie.split(";");
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = cookies[i];
                    var index = cookie.indexOf("=");
                    var curName = cookie.substring(0, index).replace(/^ /gi, "");
                    var curValue = cookie.substring(index + 1);

                    if (name == curName) {
                        value = unescape(curValue);
                    }
                }
                return value;
            },

            del: function(name, path) {
                var expires = new Date(0).toGMTString();
                this.setValue(name, "", expires, path);
            },

            delAll: function(path) {
                var cookies = window.document.cookie.split(";");
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = cookies[i];
                    var index = cookie.indexOf("=");
                    var curName = cookie.substring(0, index).replace(/^ /gi, "");
                    $.Cookie.del(curName, path);
                }
            },

            decode: function(name) {
                return $.parseJSON($.decode( $.Cookie.getValue(name), 12 ));
            }
        }
    });
})(tssJS);

/*********************************** 事件（Event）函数  start **********************************/
;(function($){

    $.extend({
        Event: {
            MOUSEDOWN: 1,
            MOUSEUP: 2,
            MOUSEOVER: 4,
            MOUSEOUT: 8,
            MOUSEMOVE:16,
            MOUSEDRAG: 32,

            timeout: {},

            // 同一事件多次添加，不会彼此覆盖，将会多次触发
            addEvent: function(el, eventName, fn, capture) {
                el.addEventListener(eventName, fn, !!capture);
            },

            removeEvent: function(el, eventName, fn, capture) {
                el.removeEventListener(eventName, fn, !!capture);
            },

            /* 取消事件 */
            cancel: function(ev) { 
                ev = ev || window.event;
                if(!ev) return;

                if (ev.preventDefault) {
                    ev.preventDefault();
                } else {
                    ev.returnValue = false;
                }
            },

            // 获得事件触发对象
            getSrcElement: function(ev) {
                return ev.target || ev.srcElement;
            },

            /* 使事件始终捕捉对象。设置事件捕获范围。 */
            setCapture: function(srcElement, eventType) {
                if (srcElement.setCapture) {             
                    srcElement.setCapture();         
                } 
                else if (window.captureEvents) {           
                    window.captureEvents(eventType);         
                }
            },

            /* 使事件放弃始终捕捉对象。 */
            releaseCapture: function(srcElement, eventType) {
                if(srcElement.releaseCapture){
                    srcElement.releaseCapture();
                }
                else if(window.captureEvents) {
                    window.captureEvents(eventType);
                }
            },

            /* 阻止事件向上冒泡 */
            cancelBubble: function(ev) {
                if(!ev) return;
                
                if( ev.stopPropagation ) {
                    ev.stopPropagation();
                }
                else {
                    ev.cancelBubble = true;
                }
            },

            /** 模拟事件 */
            createEventObject: function() { return new Object(); }
        },

        EventFirer: function(obj, eventName) {
            this.fire = function (ev) {
                var func = obj[eventName];
                if( func ) {
                    var funcType = typeof(func);
                    if("string" == funcType) {
                        return eval(func + "(ev)");
                    }
                    else if ("function" == funcType) {
                        if(ev) ev._source = obj;
                        return func(ev);
                    }
                }
            }
        }
    });

})(tssJS);

/*********************************** 事件（Event）函数  end **********************************/

/*********************************** XML相关操作函数  start **********************************/

;(function($) {

    String.prototype.convertEntry = function() {
        return this.replace(/\&/g, "&amp;").replace(/\"/g, "&quot;").replace(/\</g, "&lt;").replace(/\>/g, "&gt;");
    }

    String.prototype.revertEntry = function() {
        return this.replace(/&quot;/g, "\"").replace(/&lt;/g, "\<").replace(/&gt;/g, "\>").replace(/&amp;/g, "\&");
    }

    String.prototype.convertCDATA = function() {
        return this.replace(/\<\!\[CDATA\[/g, "&lt;![CDATA[").replace(/\]\]>/g, "]]&gt;");
    }

    String.prototype.revertCDATA = function() {
        return this.replace(/&lt;\!\[CDATA\[/g, "<![CDATA[").replace(/\]\]&gt;/g, "]]>");
    }

    $.extend({

        XML: {
            _NODE_TYPE_ELEMENT    : 1,
            _NODE_TYPE_ATTRIBUTE  : 2,
            _NODE_TYPE_TEXT       : 3,
            _NODE_TYPE_CDATA      : 4,
            _NODE_TYPE_COMMENT    : 8,
            _NODE_TYPE_DOCUMENT   : 9,

            /* 将字符串转化成xml节点对象 */
            toNode: function(xml) {
                xml = xml.revertEntry().replace(/\x02|\x01/g, '');
                return $.parseXML(xml).documentElement;
            },

            toXml: function(node) { // xml node、xml doc
                var xmlSerializer = new XMLSerializer();
                return xmlSerializer.serializeToString(node.documentElement || node);
            },

            toString: function(node) {
                return $.XML.toXml(node);
            },

            getText: function(node) {
                return node ? (node.text || node.textContent || "").trim() : ""; // chrome 用 textContent
            },

            setText: function(node, textValue) {
                textValue = (typeof(textValue) == 'string') ? String(textValue).replace(/\x02|\x01/g, '') : textValue;
                node.text = textValue;
                if (node.textContent || node.textContent == "") {
                    node.textContent = textValue; // chrome
                }
            },

            EMPTY_XML_DOM: (function() {
                var parser = new DOMParser();
                var xmlDom = parser.parseFromString("<null/>", "text/xml");
                xmlDom.parser = parser;
 
                return xmlDom;
            })(),

            createNode: function(name) {
                return $.XML.EMPTY_XML_DOM.createElement(name);
            },

            createCDATA: function(data) {
                // 替换掉0x01/0x02等一些导致XML失败的特殊字符
                data = (typeof(data) == 'string') ? String(data).replace(/\x02|\x01/g, '') : data;
                data = String(data).convertCDATA();
                if(window.DOMParser) {
                    return $.parseXML("<root><![CDATA[" + data + "]]></root>").documentElement.firstChild;
                }
                else {
                    return $.XML.EMPTY_XML_DOM.createCDATASection(data);
                }
            },

            appendCDATA: function(name, data) {
                var xmlNode   = $.XML.createNode(name);
                var cdataNode = $.XML.createCDATA(data);
                xmlNode.appendChild(cdataNode);
                return xmlNode;
            },

            getCDATA: function(pnode, name) {
                var nodes = pnode.getElementsByTagName(name);
                if(nodes.length == 0) return null;

                var cdataValue = $.XML.getText(nodes[0]);
                return cdataValue.revertCDATA();
            },

            setCDATA: function(pnode, name, value) {               
                var cdateNode = $.XML.appendCDATA(name, value);

                var oldNode = pnode.getElementsByTagName(name)[0];
                if(oldNode == null) {
                    pnode.appendChild(cdateNode);
                }
                else {
                    $.removeNode(oldNode.firstChild);
                    oldNode.appendChild(cdateNode);
                }
            },

            removeCDATA: function(pnode, name) {
                var node = pnode.getElementsByTagName(name)[0];
                if( node ) {
                    pnode.removeChild(node);
                }
            },

            /* 获取解析错误 */
            getParseError: function(xmlDom) {
                if(xmlDom == null) return "";

                var errorNodes = xmlDom.getElementsByTagName("parsererror");
                if(errorNodes.length > 0) {
                    return errorNodes[0].innerHTML;
                }
                return "";
            }
        }
    });

})(tssJS);CONTEXTPATH = "tss";

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

/*
 *  大数据显示进度
 *  参数： string:url                    同步进度请求地址
        xmlNode:data                    
        string:cancelUrl                取消进度请求地址
 */
;(function($){
    
    var Progress = function(url, data, cancelUrl) {
        this.progressUrl = url;
        this.cancelUrl = cancelUrl;
        this.refreshData(data);
    };

    Progress.prototype = {
        /* 更新数据 */
        refreshData: function(data) {
            if( data == 'not found') {
                this.percent = "100";
                return this.hide();
            }
            this.percent      = $.XML.getText(data.querySelector("percent"));
            this.delay        = $.XML.getText(data.querySelector("delay"));
            this.estimateTime = $.XML.getText(data.querySelector("estimateTime"));
            this.code         = $.XML.getText(data.querySelector("code"));

            var feedback = data.querySelector("feedback");
            if( feedback ) {
                $.alert($.XML.getText(feedback));
            }
        },

        /* 开始执行  */
        start: function() {
            if( parseInt(this.percent) >= 100 ) return;  // 已完成

            this.show();
            this.next();
        },

        /* 停止执行  */
        stop: function() {
            var pThis = this;
            $.ajax({
                url: this.cancelUrl + pThis.code,
                method: "DELETE",
                onsuccess: function() {
                    pThis.hide();
                    clearTimeout(pThis.timeout);
                }
            });
        },

        /* 显示进度  */
        show: function() {
            var pThis = this;

            var graph = $(".progressBar")[0];
            if(graph == null) {
                graph = $.createElement("div", "progressBar");
                $(graph).center(500, 50)
                    .css("width", "600px")
                    .css("color", "#fff").css("backgroundColor", "#fff")
                    .css("fontSize", "20px").css("fontWeight", "bold")
                    .css("box-shadow", "1px 3px 19px 5px #a6a6a6");

                var bar = $.createElement("div", "bar");
                $(bar).css("backgroundColor", "#e0f6c8");  

                var passBar = $.createElement("div", "passBar");
                $(passBar).css("backgroundColor", "#009966").html("1212").css("textAlign", "center").css("line-height", "25px");    
                
                graph.appendChild(bar);
                bar.appendChild(passBar);  

                var info = $.createElement("span", "info");
                $(info).html("剩余时间: <span>1</span> 秒").css("padding", "5px 0 0 120px").css("color", "#666").css("fontSize", "14px");

                var cancel = $.createElement("span");
                $(cancel).html("<a href='#'>隐 藏</a>").css("padding", "5px 0 0 100px").css("fontSize", "14px")
                    .click(function() { pThis.stop(); });

                graph.appendChild(info);
                graph.appendChild(cancel);
                document.body.appendChild(graph);
            }

            // this.percent = Math.round(Math.random() * 100); // just for test
            $(".passBar", graph).css("width", this.percent + "%").html(this.percent + "%"); 
            $(".info span", graph).html(this.estimateTime); 
        },

        /* 隐藏进度 */
        hide: function() {
            $(".progressBar").each(function(i, el) {
                $.removeNode(el);
            })
        },

        /* 同步进度  */
        sync: function() {
            var pThis = this;
            $.ajax({
                url: this.progressUrl + this.code,
                method: "GET",
                async: false,
                onresult: function() {
                    var data = this.getNodeValue("ProgressInfo");
                    pThis.refreshData(data);
                    pThis.show();
                    pThis.next();
                },
                onexception: function() {
                    pThis.hide();
                }
            });
        },

        /* 延时进行下一次同步  */
        next: function() {
            var pThis = this;

            var percent = parseInt(this.percent);
            var delay   = parseInt(this.delay) * 1000;
            if(100 > percent) {
                this.timeout = setTimeout(function() {
                    pThis.sync();
                }, delay);
            }
            else if( this.oncomplete ) {
                setTimeout(function() {
                    pThis.hide();
                    pThis.oncomplete();
                }, 200);
            }
        }
    }

    $.Progress = Progress;

})(tssJS);
/* tip, 提示气泡 */
;(function($, factory) {

    $.Bubble = $.Balloon = factory($);

    $.notice = function(targetEl, msg, delay) {
        var balloon = new $.Bubble(msg);
        balloon.dockTo(targetEl, delay);
    };

    $.fn.extend({
        notice: function(msg, delay) {
            if(this.length > 0) {
                $.notice(this[0], msg, delay);
            }
        }
    });

})(tssJS, function($){
    'use strict';

    var _STYLE_BUBBLE = "tssJS-bubble",
        _STYLE_ARROW_TOP = "tssJS-bubble-arrow-top",
        _STYLE_ARROW_BOTTOM = "tssJS-bubble-arrow-bottom",      
        NEXT_ZINDEX  = 1000,
        BUBBLE_WIDTH = 200,
        ARROW_HEIGHT = 15,

    timeout,
    dispose = function() {
        var buddles = $("." + _STYLE_BUBBLE);
        buddles.each(function() {
            $.removeNode(this);
        });
        
        $(document).removeEvent("mousedown", dispose);
    },

    Bubble = function(content) {
        this.el = $.createElement("div", _STYLE_BUBBLE);
        $(this.el).html("<p>" + content + "</p>");
        $(this.el).css("width", BUBBLE_WIDTH + "px");

        // 绑定事件，鼠标按下后气球消失
        $(document).addEvent("mousedown", dispose);
    };

    Bubble.prototype.dockTo = function(targetEl, delay){
        var position = $.absPosition(targetEl), 
            x = position.left + targetEl.offsetWidth/2 - BUBBLE_WIDTH/2, 
            y;

        if( position.bottom + 50 >= document.body.clientHeight) {
            y = position.top - ARROW_HEIGHT * 4.5 - this.el.offsetHeight; 
            $(this.el).addClass(_STYLE_ARROW_BOTTOM);
        }
        else {
            y = position.bottom + ARROW_HEIGHT;
            $(this.el).addClass(_STYLE_ARROW_TOP);
        } 
        
        $(this.el).css("zIndex", NEXT_ZINDEX++).css("left", x + "px").css("top", y + "px");
        document.body.appendChild(this.el);

        clearTimeout(timeout);
        timeout = setTimeout( dispose, delay || 3000 );
    }

    return Bubble;
});


;(function($) {

    var setHeight = function(tipEL, direction) {
        var popH = parseInt(tipEL.style.height);
        if (direction == "up") {
            if (popH <= 100) {
                $(tipEL).css("height", (popH + 4) + "px");
            } else {
                clearInterval(show);
            }
        }
        if (direction == "down") {
            if (popH >= 4) {
                $(tipEL).css("height", (popH - 4) + "px");
            } else {
                clearInterval(hide);
                $(tipEL).hide();
            }
        }
    }

    $.tssTip = function(content, title) {
        var $tipEL = $("#tssTip");
        if(!$tipEL.length) {
            var el = $.createElement("div", null, "tssTip");
            var html = [];
            html.push('<div class="title"><b>' + (title || '消息提醒') + '</b><span class="close" onclick="$.tssTip()">×</span></div>');
            html.push('<div class="content">' + content + '</div>');
            $tipEL = $(el);
            $tipEL.html(html.join("\n"));
            $tipEL.css("height", '0');

            $(document.body).appendChild(el);
        }

        title && $tipEL.find("div.title>b").html(title);
        content && $tipEL.find("div.content").html(content);

        var popH = parseInt($tipEL[0].style.height);
        if (popH == 0 || content) {
            $tipEL.show(true);
            show = setInterval( function() { setHeight($tipEL[0], 'up'); }, 20);
        } else {
            hide = setInterval( function() { setHeight($tipEL[0], 'down'); }, 20);
        }
    }

})(tssJS);


/* Tip/Alert/Confirm/Prompt */
(function($) {

    var popupBox = function(title, callback) {
        var $box = $("#alert_box");
        if($box.length > 0) {
            $box.remove();
        }

        var boxEl = $.createElement("div", "moveable", "alert_box");
        document.body.appendChild(boxEl);

        var html = [];
        html.push('  <h1 class="title"></h1>');
        html.push('  <span class="close"></span>');
        html.push('  <div class="content">');
        html.push('    <div class="message"></div>');
        html.push('    <div class="btbox"><input type="button" value="确 定" class="ok"></div>');
        html.push('  </div>');

        $box = $(boxEl).html(html.join("\n")).show().drag();
        $(".close", boxEl).click(closeBox);
        $("h1", boxEl).html(title).addClass("text2");
        $(".content .message", boxEl).addClass("text1");

        $(boxEl).center();   
        callback && $.showWaitingLayer();

        return $box[0];
    },

    closeBox = function(all) {
        $("#alert_box").hide().remove();
        $.hideWaitingLayer(all);
    },

    bindEnterKey = function(el, fn) {
        $(el).addEvent("keydown", function(ev) {
            if(13 == ev.keyCode) { // enter
                $.Event.cancel(ev);
                setTimeout(fn, 10);
            }
        });
    };

    // content：内容，title：对话框标题  
    $.tip = function(content, title, callback) {
        var boxEl = popupBox(title || '消息提醒');
        $(".content", boxEl).addClass("tip");
        $(".btbox .ok", boxEl).attr("value", "我知道了");
        $(".content .message", boxEl).html(content);
        $(boxEl).css("width", "250px").css("position", "fixed").css("right", "1px").css("bottom", "1px").css("top", "").css("left", "");

        function ok() {
            closeBox();
            callback && callback();
        }
        $(".btbox .ok", boxEl).click(ok);
    };

    // content：内容，title：对话框标题，callback：回调函数    
    $.alert = function(content, title, callback) {
        var boxEl = popupBox(title || '提示', callback);
        $(".content", boxEl).addClass("alert");
        $(".content .message", boxEl).html(content);
        $(boxEl).center(); 

        function ok() {
            closeBox();
            callback && callback();
        }
        $(".btbox .ok", boxEl).click(ok);
    };

    // content：内容，title：对话框标题，callback：回调函数
    $.confirm = function(content, title, callback, cancelCallback){
        var boxEl = popupBox(title || '确认框', callback);
        $(".content", boxEl).addClass("confirm");
        $(".content .message", boxEl).html(content || '你确认此操作吗?');
        $(".btbox", boxEl).html($(".btbox", boxEl).html() + '<input type="button" value="取 消" class="cancel">');
        $(boxEl).center(); 

        function ok(result) {
            closeBox();
            if(result) {
                callback && callback();
            } else {
                cancelCallback && cancelCallback();
            }
        }
        $(".btbox .ok", boxEl).click(function() { ok(true); });
        $(".btbox .cancel", boxEl).click(function() { ok(false); });
    };

    // content：内容，deinput：输入框的默认值，title：对话框标题，callback：回调函数
    $.prompt = function(content, title, callback, deinput, isPasswd){
        var type = isPasswd ? "password" : "text";
        var boxEl = popupBox(title || '输入框', callback);
        $(".content", boxEl).addClass("prompt");
        $(".content .message", boxEl).html( (content || "请输入：") + ':<br><input type="' +type+ '">' );
        $(".content .message input", boxEl).value(deinput || '');
        $(".btbox", boxEl).html($(".btbox", boxEl).html() + '<input type="button" value="取 消" class="cancel">');  
        $(boxEl).center();      

        function ok() {
            var value = $(".content .message input", boxEl).value();

            closeBox();
            callback && callback(value);
        }
        $(".btbox .ok", boxEl).click(ok);
        $(".btbox .cancel", boxEl).click(closeBox);
        bindEnterKey(boxEl, ok);
    };

    $.checkCode = function(codeType, callback){
        var boxEl = popupBox('当前操作需要校验验证码', callback);
        $(".content", boxEl).addClass("prompt");
        var _html;
        if(codeType === 'need_sms_check_code') {
            _html = '请输入收到的手机短信验证码:<br><br><input type="text" style="width:150px"><button id="ckcode">获取验证码</button>';
        }
        if(codeType === 'need_email_check_code') {
            _html = '请到您注册的邮箱里读取验证码并输入:<br><br><input type="text" style="width:150px">';
        }
        if(codeType === 'need_img_check_code') {
            _html = '请输入图形验证码:<br><br><input type="text" style="width:150px;margin-right:30px;"><img id="imgcode" style="vertical-align: middle;" src=""/>';
        }
        $(".content .message", boxEl).html( _html + '<br><h2 id="_abn_" style="color: red;"></h2>' );
        $(".btbox", boxEl).html($(".btbox", boxEl).html() + '<input type="button" value="取 消" class="cancel">');  
        $(boxEl).center();      

        function ok() {
            var value = $(".content .message input", boxEl).value();
            if( !value || value.trim().length === 0 ) return;

            callback && callback(value);

            $(this).attr('disabled', true);
            $('#_abn_').text( "正在校验......" );
            setTimeout( function() { closeBox(true); }, 2000);
        }
        $(".btbox .ok", boxEl).click(ok);
        $(".btbox .cancel", boxEl).click( function() { closeBox(true); } );

        $("#ckcode").click(function(){
            $('#ckcode').attr('disabled', true)
            $.ajax({
                url : "/tss/checkMobile.in"
            });

            var waitSeconds = 60;
            var interval = setInterval(function(){
                $('#ckcode').text( waitSeconds-- + '秒后重新获取' );
                if(waitSeconds <= 0){
                    $('#ckcode').text("获取验证码");
                    $('#ckcode').attr('disabled', false);
                    clearInterval(interval);
                }
            }, 1000)
        });

        var img_url = "/tss/img/api/ck/randomKey?";
        $("#imgcode").attr("src", img_url + $.now());
        $("#imgcode").click(function(){
            $(this).attr("src", img_url + $.now());
        });
    }

})(tssJS);


/* 右键菜单 */
;(function ($, factory) {

    $.Menu = factory($);

})(tssJS, function ($) {

    'use strict';

    var
    /* 样式名称 */
    CSS_CLASS_MENU = "rmenu",
    CSS_CLASS_MENU_ITEM_ACITVE = "active",
    CSS_CLASS_MENU_SEPARATOR = "separator",

    /* 菜单唯一编号名前缀 */
    _UNIQUE_ID_MENU_PREFIX = "_menu_id",
    _UNIQUE_ID_ITEM_PREFIX = "_item_id",

    Menus = {
        menuZIndex: 1001,
        collection: {},
        add: function(menu) {
            Menus.collection[menu.id] = menu;
        },
        del: function(menu) {
            delete Menus.collection[menu.id];
        },
        inactiveAllMenus: function() {
            for(var menuId in this.collection) {
                Menus.collection[menuId].inactive();
            }
        },
        hideAllMenus: function() {
            for(var menuId in this.collection) {
                Menus.collection[menuId].hide();
            }
        },

        // 根据菜单项ID获取所属Menu实例
        getMenuByItemID: function(id) {
            for(var menuId in this.collection) {
                var curMenu = this.collection[menuId];
                var menuItem = curMenu.items[id];
                if(menuItem) {
                    return curMenu;
                }
            }
        }
    },

    Menu = function() {
        this.items = {};
        this.parentMenuItem; //submenu所属的菜单项

        this.id = $.getUniqueID(_UNIQUE_ID_MENU_PREFIX);
        this.el = $.createElement("div", CSS_CLASS_MENU);
        this.el.id = this.id;

        this.isActive = false;
        this.setVisible(false);
        
        // 绑定事件
        this.el.onselectstart = _Menu_onSelectStart;
        $.Event.addEvent(document, "mousedown", _Menu_Document_onMouseDown);
        $.Event.addEvent(window, "resize", _Menu_Window_onResize);
        
        Menus.add(this);
    };
        
    Menu.prototype = {
            
        /*
         *  将实例绑定到指定对象
         *  参数：  object:srcElement       HTML对象
                    string:eventName        事件名称
         */
        attachTo: function(srcElement, eventName) {
            this.srcElement = srcElement;
            
            var oThis = this;
            $.Event.addEvent(srcElement, eventName, function(ev) {
                $.Event.cancel(ev);

                var x = ev.clientX + document.body.scrollLeft;
                var y = ev.clientY + document.body.scrollTop;
                oThis.show(x, y);
            });
        },

        /*
         *  显示菜单
         *  参数：  number:x            菜单参考点位置
                    number:y            菜单参考点位置
         */
        show: function(x, y) {
            Menus.inactiveAllMenus();
            
            var visibleItemsCount = this.refreshItems();
            if(0 == visibleItemsCount) {
                return;
            }

            this.active();

            if( $("#" + this.id).length == 0 ) {
                document.body.appendChild(this.el);
            }

            this.el.style.zIndex = Menus.menuZIndex++;

            if(y + this.el.offsetHeight > document.body.offsetHeight) {
                y = document.body.offsetHeight - this.el.offsetHeight;
            }

            this.moveTo(x, y);
            this.setVisible(true);
        },

        hide: function() {
            this.setVisible(false);
            this.moveTo(0, 0);
            this.inactive();
        },

        moveTo: function(x, y) {
            this.el.style.left = x + "px";
            this.el.style.top  = y + "px";
        },

        /* 激活当前菜单 */
        active: function() {
            this.isActive = true;
        },

        inactive: function() {
            this.isActive = false;
        },

        /* 不激活当前菜单的所有菜单项 */
        inactiveAllItems: function() {
            for(var key in this.items) {
                this.items[key].inactive();
            }
        },

        /*
         *  刷新菜单项状态 
         *  返回值：number:visibleItemsCount   可见菜单项的数量
         */
        refreshItems: function() {
            var visibleItemsCount = 0;
            for(var item in this.items) {
                var curMenuItem = this.items[item];
                curMenuItem.refresh();
                if(curMenuItem.isVisible) {
                    visibleItemsCount ++;
                }
            }
            return visibleItemsCount;
        },

        /*
         *  设置菜单是否可见
         *  参数：  boolean:visible     菜单是否可见
         */
        setVisible: function(visible) {
            this.el.style.visibility = visible ? "visible" : "hidden";
        },

        /*
         *  添加菜单项
         *  参数：      object:menuItem     菜单项定义
         *  返回值：    string:id     菜单项唯一ID
         */
        addItem: function(menuItem) {
            var menuItem = new MenuItem(menuItem);
            menuItem.dockTo(this.el);

            this.items[menuItem.id] = menuItem;
            return menuItem.id;
        },

        /* 删除菜单项 */
        delItem: function(id) {
            var menuItem = this.items[id];
            if(menuItem) {
                menuItem.dispose();
                delete this.items[id];
            }
        },

        /* 添加分隔线 */
        addSeparator: function() {
            var separator = $.createElement("div", CSS_CLASS_MENU_SEPARATOR);
            this.el.appendChild(separator);
        },

        /* 释放实例 */
        dispose: function() {
            for(var item in this.items) {
                this.delItem(item);
            }
            $.removeNode(this.el);

            for(var item in this) {
                delete this[item];
            }
        }
    };

    /* 控制右键菜单项 */
    var MenuItem = function(itemProperties) {
        for(var name in itemProperties) {
            this[name] = itemProperties[name];
        }

        this.isEnable = true;

        this.id = $.getUniqueID(_UNIQUE_ID_ITEM_PREFIX);

        this.el = document.createElement("div");
        this.el.id = this.id;
        this.el.noWrap = true;
        this.el.innerHTML = this.bold ? ("<b>" + this.label + "</b>") : this.label;

        if( this.icon ) {
            var iconEL;
            if( this.icon.indexOf(".") < 0 ) {  // class
                iconEL = $.createElement("span");
                $(iconEL).addClass(this.icon);
            } else { // 图片
                iconEL = $.createElement("img");
                iconEL.src = this.icon;
            }
            this.el.appendChild(iconEL);
        }
        if( this.submenu ) {
            var img = $.createElement("div", "hasChild icon icon-triangle-right");
            this.el.appendChild(img);
            
            this.submenu.parentMenuItem = this;
        }
        
        this.el.onmouseover   = _Menu_Item_onMouseOver;
        this.el.onmouseout    = _Menu_Item_onMouseOut;
        this.el.onmousedown   = _Menu_Item_onMouseDown;
        this.el.onclick       = _Menu_Item_onClick;
        this.el.oncontextmenu = _Menu_Item_onContextMenu;
    };

    MenuItem.prototype = {

        /* 将菜单项插入指定容器  */
        dockTo: function(container) {
            container.appendChild(this.el);
        },

        /* 高亮菜单项 */
        active: function() {
            if( !!this.isEnable ) {
                $(this.el).addClass(CSS_CLASS_MENU_ITEM_ACITVE);
            }
        },

        /* 低亮菜单项 */
        inactive: function() {
            if( !!this.isEnable ) {
                this.el.className = "";
            }
            if( this.submenu ) {
                this.submenu.inactiveAllItems();
                this.submenu.hide();
            }
        },

        setVisible: function(visible) {
            this.isVisible = !!visible;
            this.el.style.display = this.isVisible ? "block" : "none";
        },

        /* 设置菜单项是否可用 */
        setEnable: function(enable) {
            this.isEnable = !!enable;
            this.el.className = this.isEnable ? "" : "disable";
        },

        /* 刷新菜单项状态 */
        refresh: function() {
            var isVisible = true;
            if(this.visible) {
                isVisible = $.execCommand(this.visible);
            }

            var isEnable = true;
            if(this.enable) {
                isEnable = $.execCommand(this.enable);
            }

            this.setVisible(isVisible);
            this.setEnable(isEnable);
        },

        /* 执行菜单项回调方法 */
        execCallBack: function(event) {
            if(this.isEnable) {
                $.execCommand(this.callback, event);
            }
        },

        /* 显示子菜单 */
        showSubMenu: function() {
            if( this.submenu ) {
                var position = $.absPosition(this.el);
                this.submenu.show(position.right, position.bottom);
            }
        },

        dispose: function() {
            $.removeNode(this.el);

            for(var propertyName in this) {
                delete this[propertyName];
            }
        }
    };

    var _Menu_Document_onMouseDown = function(ev) {
        Menus.hideAllMenus();
    }, 

    _Menu_Window_onResize = function(ev) {
        Menus.hideAllMenus();
    },

    _Menu_Item_onMouseDown = function(ev) {
        ev = ev || window.event;
        $.Event.cancelBubble(ev);
    },

    // 高亮菜单项
    _Menu_Item_onMouseOver = function(ev) {
        ev = ev || window.event;

        var id = this.id;
        var menu = Menus.getMenuByItemID(id);
        if(menu) {
            menu.inactiveAllItems();
            var menuItem = menu.items[id];
            menuItem.active();
            menuItem.showSubMenu();
        }
    },

    // 低亮菜单项
    _Menu_Item_onMouseOut = function(ev) {
        var id = this.id;
        var menu = Menus.getMenuByItemID(id);
        if(menu) {
            var menuItem = menu.items[id];
            if(null == menuItem.submenu || false == menuItem.submenu.isActive) {
                menuItem.inactive();            
            }
        }
    },

    // 执行菜单项回调方法
    _Menu_Item_onClick = function(ev) {
        ev = ev || window.event;

        var id = this.id;
        var menu = Menus.getMenuByItemID(id);
        if(menu) {
            var menuItem = menu.items[id];
            if(menuItem.isEnable) {
                if(menuItem.callback) {
                    Menus.hideAllMenus();
                }
                menuItem.execCallBack(ev);

                if(null == menuItem.submenu) {
                    Menus.inactiveAllMenus();
                    Menus.hideAllMenus();
                }
            }
        }
    },

    /* 鼠标右键点击 */
    _Menu_Item_onContextMenu = function(ev) {
        ev = ev || window.event;
        $.Event.cancel(ev);
    },

    /* 鼠标拖动选择文本 */
    _Menu_onSelectStart = function(ev) {
        ev = ev || window.event;
        $.Event.cancel(ev);
    };

    return Menu;
});

/* 日期时间选择控件 */
;(function ($, factory) {

    $.Calendar = factory($);

    $.createCalendar = function(el, careTime) {
        $(el).calendar({
            format: 'yyyy-MM-dd',
            careTime: careTime || false
        });
    };

    $.fn.extend({
        calendar: function(inits) {
            if(this.length > 0) {
                inits = inits || {};
                inits.field = this[0];

                return new $.Calendar( inits );
            }
        }
    });

})(tssJS, function ($) {
    
    'use strict';

    var  document = window.document,
 
    isDate = function(obj) {
        return (/Date/).test(Object.prototype.toString.call(obj)) && !isNaN(obj.getTime());
    },

    string2Date = function(_s) {
        var d = new Date(Date.parse(_s.replace(/-/g, "/"))); // Date.parse in IE only support yyyy/mm/dd
        return d;
    },

    isLeapYear = function(year) {
        return year % 4 === 0 && year % 100 !== 0 || year % 400 === 0;
    },

    getDaysInMonth = function(year, month) {
        return [31, (isLeapYear(year) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month];
    },

    getSelectdTime = function($ranges) {
        if($ranges == null || $ranges.length < 3) return "00:00:00";

        var _hour   = $ranges[0].value;
        var _minute = $ranges[1].value;
        var _second = $ranges[2].value;
        _hour = _hour.length == 1 ? "0" + _hour : _hour;
        _minute = _minute.length == 1 ? "0" + _minute : _minute;
        _second = _second.length == 1 ? "0" + _second : _second;

        return _hour + ":" + _minute + ":" + _second;
    },

    setSelectdTime = function($ranges, self) {
        $ranges[0].value = self._hour || 0;
        $ranges[1].value = self._minute || 0;
        $ranges[2].value = self._second || 0;

        $('.timeAera .range1', self.el).html( self._hour || '00');
        $('.timeAera .range2', self.el).html( self._minute || '00');
        $('.timeAera .range3', self.el).html( self._second || '00');
    },

    setToStartOfDay = function(date) {
        if ( isDate(date) )  date.setHours(0, 0, 0, 0); // 对时分秒进行清零
    },

    compareDates = function(a, b) {
        return a.format('yyyy-MM-dd') === b.format('yyyy-MM-dd');
    },

    extend = function(to, from, overwrite) {
        var prop, hasProp;
        for (prop in from) {
            hasProp = to[prop] !== undefined;
            if (hasProp && typeof from[prop] === 'object' && from[prop].nodeName === undefined) {
                if (isDate(from[prop])) {
                    if (overwrite) {
                        to[prop] = new Date(from[prop].getTime());
                    }
                }
                else if ($.isArray(from[prop])) {
                    if (overwrite) {
                        to[prop] = from[prop].slice(0);
                    }
                } else {
                    to[prop] = extend({}, from[prop], overwrite);
                }
            } else if (overwrite || !hasProp) {
                to[prop] = from[prop];
            }
        }
        return to;
    },

    /** defaults and localisation */
    defaults = {

        // bind the picker to a form field
        field: null,
 
        // position of the datepicker, relative to the field (default to bottom & left)
        position: 'bottom left',

        // the default output format for `.toString()` and `field` value
        format: 'yyyy-MM-dd',

        // hh:mi:ss
        careTime: false,

        // the initial date to view when first opened
        defaultDate: null,

        // make the `defaultDate` the initial selected value
        setDefaultDate: false,

        // first day of week (0: Sunday, 1: Monday etc)
        firstDay: 0,

        // the minimum/maximum date that can be selected
        minDate: null,
        maxDate: null,

        // number of years either side, or array of upper/lower range
        yearRange: 10,

        // used internally (don't config outside)
        minYear: 0,
        maxYear: 9999,
        minMonth: undefined,
        maxMonth: undefined,

        // Additional text to append to the year in the calendar title
        yearSuffix: '',

        // Render the month after year in the calendar title
        showMonthAfterYear: false,

        // internationalization
        i18n: {
            previousMonth : ' ',
            nextMonth     : ' ',
            months        : ['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月'],
            weekdays      : ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],
            weekdaysShort : ['日','一','二','三','四','五','六']
        },

        // callback function
        onSelect: null,
        onOpen: null,
        onClose: null
    },


    /**
     * templating functions to abstract HTML rendering
     */
    renderDayName = function(opts, day, abbr) {
        day += opts.firstDay;
        while (day >= 7) {
            day -= 7;
        }
        return abbr ? opts.i18n.weekdaysShort[day] : opts.i18n.weekdays[day];
    },

    renderDay = function(i, isSelected, isToday, isDisabled, isEmpty) {
        if (isEmpty) {
            return '<td class="is-empty"></td>';
        }
        var arr = [];
        if (isDisabled) {
            arr.push('is-disabled');
        }
        if (isToday) {
            arr.push('is-today');
        }
        if (isSelected) {
            arr.push('is-selected');
        }
        return '<td data-day="' + i + '" class="' + arr.join(' ') + '"><button class="pika-button" type="button">' + i + '</button>' + '</td>';
    },

    renderRow = function(days) {
        return '<tr>' + days.join('') + '</tr>';
    },

    renderBody = function(rows) {
        return '<tbody>' + rows.join('') + '</tbody>';
    },

    renderHead = function(opts) {
        var i, arr = [];
        for (i = 0; i < 7; i++) {
            arr.push('<th scope="col"><abbr title="' + renderDayName(opts, i) + '">' + renderDayName(opts, i, true) + '</abbr></th>');
        }
        return '<thead>' + arr.join('') + '</thead>';
    },

    renderTitle = function(instance) {
        var i, j, arr,
            opts  = instance._o,
            month = instance._m,
            year  = instance._y,
            isMinYear = year === opts.minYear,
            isMaxYear = year === opts.maxYear,
            html = '<div class="pika-title">',
            monthHtml,
            yearHtml,
            prev = true,
            next = true;

        for (arr = [], i = 0; i < 12; i++) {
            arr.push('<option value="' + i + '"' + (i === month ? ' selected': '') +
                ((isMinYear && i < opts.minMonth) || (isMaxYear && i > opts.maxMonth) ? 'disabled' : '') + '>' +
                opts.i18n.months[i] + '</option>');
        }
        monthHtml = '<div class="pika-label">' + opts.i18n.months[month] + '<select class="pika-select pika-select-month">' + arr.join('') + '</select></div>';

        if ($.isArray(opts.yearRange)) {
            i = opts.yearRange[0];
            j = opts.yearRange[1] + 1;
        } else {
            i = year - opts.yearRange;
            j = 1 + year + opts.yearRange;
        }

        for (arr = []; i < j && i <= opts.maxYear; i++) {
            if (i >= opts.minYear) {
                arr.push('<option value="' + i + '"' + (i === year ? ' selected': '') + '>' + (i) + '</option>');
            }
        }
        yearHtml = '<div class="pika-label">' + year + opts.yearSuffix + '<select class="pika-select pika-select-year">' + arr.join('') + '</select></div>';

        if (opts.showMonthAfterYear) {
            html += yearHtml + monthHtml;
        } else {
            html += monthHtml + yearHtml;
        }

        if (isMinYear && (month === 0 || opts.minMonth >= month)) {
            prev = false;
        }

        if (isMaxYear && (month === 11 || opts.maxMonth <= month)) {
            next = false;
        }

        html += '<button class="pika-prev' + (prev ? '' : ' is-disabled') + '" type="button">' + opts.i18n.previousMonth + '</button>';
        html += '<button class="pika-next' + (next ? '' : ' is-disabled') + '" type="button">' + opts.i18n.nextMonth + '</button>';

        return html += '</div>';
    },

    renderTable = function(opts, data) {
        return '<table class="pika-table">' + renderHead(opts) + renderBody(data) + '</table>';
    },

    renderTime = function(opts, time) {
        var html = [];
        html.push('<div class="timeAera">');
        html.push('时：<input type="range" min="0" max="23" value="0" step="1" onchange="$(\'.range1\', this.parentNode).html(this.value);" /><span class="range1">00</span><br/>');
        html.push('分：<input type="range" min="0" max="59" value="0" step="1" onchange="$(\'.range2\', this.parentNode).html(this.value);" /><span class="range2">00</span><br/>');
        html.push('秒：<input type="range" min="0" max="59" value="0" step="1" onchange="$(\'.range3\', this.parentNode).html(this.value);" /><span class="range3">00</span><br/>');
        html.push('<button type="button">确定</button>');
        html.push('</div>');
        return html.join("\n");
    },


    /**  JCalendar constructor */
    JCalendar = function(options) {
        var self = this,
            opts = self.config(options);

        self._onMouseDown = function(e) {
            if (!self._v)  return;

            e = e || window.event;
            var target = e.target || e.srcElement;
            if (!target) return;

            if ( target.parentNode && $.hasClass(target.parentNode, 'timeAera') ) {
                self._c = true;
                return;
            }

            if (!$.hasClass(target, 'is-disabled')) {
                if ($.hasClass(target, 'pika-button') && !$.hasClass(target, 'is-empty')) {
                    var selectedDate = new Date(self._y, self._m, parseInt(target.innerHTML, 10));

                    if( opts.careTime ) {
                        selectedDate.setHours(self._hour||0, self._minute||0, self._second||0, 0)
                    }
                    else {
                        window.setTimeout(function() { self.hide(); }, 100);
                    }
                    self.setDate(selectedDate);
 
                    return;
                }
                else if ($.hasClass(target, 'pika-prev')) {
                    self.prevMonth();
                }
                else if ($.hasClass(target, 'pika-next')) {
                    self.nextMonth();
                }
            }
            if (!$.hasClass(target, 'pika-select')) {
                $.Event.cancel(e);
            } else {
                self._c = true;
            }
        };

        self._onChange = function(e) {
            e = e || window.event;
            var target = e.target || e.srcElement;
            if (!target) return;
            
            if ($.hasClass(target, 'pika-select-month')) {
                self.gotoMonth(target.value);
            }
            else if ($.hasClass(target, 'pika-select-year')) {
                self.gotoYear(target.value);
            }
        };

        self._onInputChange = function(e) {
            if (e.firedBy === self) return;
 
            var date = string2Date(opts.field.value);
            self.setDate(isDate(date) ? date : null);
            if (!self._v) {
                self.show();
            }
        };

        self._onInputFocus = function() {
            self.show();
        };

        self._onInputClick = function() {
            self.show();
        };

        self._onInputBlur = function() {
            if ( !self._c ) {
                self._b = window.setTimeout(function() {
                    self.hide();
                }, 50);
            }
            self._c = false;
        };

        self._onClick = function(e) {
            e = e || window.event;
            var target = e.target || e.srcElement,
                pEl = target;
            if (!target) return;

            if ( $.hasClass(target, 'pika-select') ) {
                if (!target.onchange) {
                    target.setAttribute('onchange', 'return;');
                    $.Event.addEvent(target, 'change', self._onChange);
                }
            }

            do {
                if ($.hasClass(pEl, 'pika-single')) {
                    return;
                }
            }
            while ((pEl = pEl.parentNode));

            if (self._v && target !== opts.trigger && !opts.careTime) {
                self.hide();
            }
        };

        self.el = document.createElement('div');
        self.el.className = 'pika-single';

        $.Event.addEvent(self.el, 'mousedown', self._onMouseDown, true);
        $.Event.addEvent(self.el, 'change', self._onChange);

        if (opts.field) {
            document.body.appendChild(self.el);
            $.Event.addEvent(opts.field, 'change', self._onInputChange);

            if (!opts.defaultDate) {
                opts.defaultDate = string2Date(opts.field.value);
                opts.setDefaultDate = true;
            }
        }

        var defDate = opts.defaultDate;

        if (isDate(defDate)) {
            if (opts.setDefaultDate) {
                self.setDate(defDate, true);
            } else {
                self.gotoDate(defDate);
            }
        } else {
            self.gotoDate(new Date());
        }

        this.hide();
        self.el.className += ' is-bound';
        $.Event.addEvent(opts.trigger, 'click', self._onInputClick);
        $.Event.addEvent(opts.trigger, 'focus', self._onInputFocus);
        $.Event.addEvent(opts.trigger, 'blur', self._onInputBlur);
    };


    /** public JCalendar API */
    JCalendar.prototype = {

        config: function(options) {
            if (!this._o) {
                this._o = extend({}, defaults, true);
            }

            var opts = extend(this._o, options, true);

            opts.field = (opts.field && opts.field.nodeName) ? opts.field : null;

            opts.trigger = (opts.trigger && opts.trigger.nodeName) ? opts.trigger : opts.field;

            if (!isDate(opts.minDate)) {
                opts.minDate = false;
            }
            if (!isDate(opts.maxDate)) {
                opts.maxDate = false;
            }
            if ((opts.minDate && opts.maxDate) && opts.maxDate < opts.minDate) {
                opts.maxDate = opts.minDate = false;
            }
            if (opts.minDate) {
                setToStartOfDay(opts.minDate);
                opts.minYear  = opts.minDate.getFullYear();
                opts.minMonth = opts.minDate.getMonth();
            }
            if (opts.maxDate) {
                setToStartOfDay(opts.maxDate);
                opts.maxYear  = opts.maxDate.getFullYear();
                opts.maxMonth = opts.maxDate.getMonth();
            }

            if ($.isArray(opts.yearRange)) {
                var fallback = new Date().getFullYear() - 10;
                opts.yearRange[0] = parseInt(opts.yearRange[0], 10) || fallback;
                opts.yearRange[1] = parseInt(opts.yearRange[1], 10) || fallback;
            } else {
                opts.yearRange = Math.abs(parseInt(opts.yearRange, 10)) || defaults.yearRange;
                opts.yearRange = Math.min(opts.yearRange, 50);
            }

            if(opts.careTime) {
                opts.format += opts.format.length == 10 ? ' hh:mm:ss' : "";
            }
            return opts;
        },

        toString: function(format) {
            format = format || this._o.format;
            return !isDate(this._d) ? '' : this._d.format(format);
        },

        getDate: function() {
            return isDate(this._d) ? new Date(this._d.getTime()) : null;
        },
 
        setDate: function(date, preventOnSelect) {
            if (!date) {
                this._d = null;
                return this.draw();
            }
            if (typeof date === 'string') {
                date = string2Date(date);
            }
            if (!isDate(date)) {
                return;
            }

            var min = this._o.minDate,
                max = this._o.maxDate;

            if (isDate(min) && date < min) {
                date = min;
            } else if (isDate(max) && date > max) {
                date = max;
            }

            this._d = new Date(date.getTime());
            if(!this._o.careTime) {
                setToStartOfDay(this._d);
            } else {
                this._hour = this._d.getHours();
                this._minute = this._d.getMinutes();
                this._second= this._d.getSeconds();
            }
            this.gotoDate(this._d);

            if (this._o.field) {
                this._o.field.value = this.toString();
            }

            if (!preventOnSelect && typeof this._o.onSelect === 'function') {
                this._o.onSelect.call(this, this.getDate());
            }
        },

        gotoDate: function(date) {
            if (!isDate(date))  return;

            this._y = date.getFullYear();
            this._m = date.getMonth();
            this.draw();
        },

        gotoToday: function() {
            this.gotoDate(new Date());
        },
 
        gotoMonth: function(month) {
            if (!isNaN( (month = parseInt(month, 10)) )) {
                this._m = month < 0 ? 0 : month > 11 ? 11 : month;
                this.draw();
            }
        },

        nextMonth: function() {
            if (++this._m > 11) {
                this._m = 0;
                this._y++;
            }
            this.draw();
        },

        prevMonth: function() {
            if (--this._m < 0) {
                this._m = 11;
                this._y--;
            }
            this.draw();
        },

        gotoYear: function(year) {
            if (!isNaN(year)) {
                this._y = parseInt(year, 10);
                this.draw();
            }
        },

        /** refresh the HTML */
        draw: function(force) {
            if (!this._v && !force)  return;

            var opts = this._o,
                minYear = opts.minYear,
                maxYear = opts.maxYear,
                minMonth = opts.minMonth,
                maxMonth = opts.maxMonth;

            if (this._y <= minYear) {
                this._y = minYear;
                if (!isNaN(minMonth) && this._m < minMonth) {
                    this._m = minMonth;
                }
            }
            if (this._y >= maxYear) {
                this._y = maxYear;
                if (!isNaN(maxMonth) && this._m > maxMonth) {
                    this._m = maxMonth;
                }
            }

            this.el.innerHTML = renderTitle(this) + this.render(this._y, this._m);
            if(opts.careTime) {
                this.el.innerHTML += renderTime(opts);
                var self = this;
                var $ranges = $('.timeAera input', self.el);
                setSelectdTime($ranges, self);

                $('.timeAera button', self.el).click(function(){
                    var _time = getSelectdTime( $ranges );
                    if( $.isNullOrEmpty(self._o.field.value) ) {
                        self._o.field.value = new Date().format(opts.format);
                    }
                    self._o.field.value = self._o.field.value.split(" ")[0] + " " + _time;
                    self._o.field.focus();

                    var date = string2Date(opts.field.value);
                    self._hour = date.getHours();
                    self._minute = date.getMinutes();
                    self._second = date.getSeconds();

                    window.setTimeout(function() {
                        self.hide();
                    }, 50);
                });
            }

            this.adjustPosition();
            if(opts.field.type !== 'hidden') {
                window.setTimeout(function() {
                    opts.trigger.focus();
                }, 1);
            }
        },

        adjustPosition: function() {
            var field = this._o.trigger, pEl = field,
                width = this.el.offsetWidth, 
                height = this.el.offsetHeight,
                viewportWidth  = window.innerWidth || document.documentElement.clientWidth,
                viewportHeight = window.innerHeight || document.documentElement.clientHeight,
                scrollTop = window.pageYOffset || document.body.scrollTop || document.documentElement.scrollTop,
                left, top, clientRect;

            if (typeof field.getBoundingClientRect === 'function') {
                clientRect = field.getBoundingClientRect();
                left = clientRect.left + window.pageXOffset;
                top = clientRect.bottom + window.pageYOffset;
            } else {
                left = pEl.offsetLeft;
                top  = pEl.offsetTop + pEl.offsetHeight;
                while((pEl = pEl.offsetParent)) {
                    left += pEl.offsetLeft;
                    top  += pEl.offsetTop;
                }
            }

            // default position is bottom & left
            if (left + width > viewportWidth ||
                ( this._o.position.indexOf('right') > -1 &&  left - width + field.offsetWidth > 0) ) {

                left = left - width + field.offsetWidth;
            }
            if (top + height > viewportHeight + scrollTop ||
                ( this._o.position.indexOf('top') > -1 && top - height - field.offsetHeight > 0  ) ) {

                top = top - height - field.offsetHeight;
            }

            this.el.style.cssText = [
                'position: absolute',
                'left: ' + left + 'px',
                'top: ' + top + 'px'
            ].join(';');
        },

        /** render HTML for a particular month */
        render: function(year, month)  {
            var opts   = this._o,
                now    = new Date(),
                days   = getDaysInMonth(year, month),
                before = new Date(year, month, 1).getDay(),
                data   = [],
                row    = [];

            setToStartOfDay(now);
            if (opts.firstDay > 0) {
                before -= opts.firstDay;
                if (before < 0) {
                    before += 7;
                }
            }

            var cells = days + before,
                after = cells;
            while(after > 7) {
                after -= 7;
            }
            cells += 7 - after;
            for (var i = 0, r = 0; i < cells; i++) {
                var day = new Date(year, month, 1 + (i - before)),
                    isDisabled = (opts.minDate && day < opts.minDate) || (opts.maxDate && day > opts.maxDate),
                    isSelected = isDate(this._d) ? compareDates(day, this._d) : false,
                    isToday = compareDates(day, now),
                    isEmpty = i < before || i >= (days + before);

                row.push(renderDay(1 + (i - before), isSelected, isToday, isDisabled, isEmpty));

                if (++r === 7) {
                    data.push(renderRow(row));
                    row = [];
                    r = 0;
                }
            }
            return renderTable(opts, data);
        },

        isVisible: function() {
            return this._v;
        },

        show: function() {
            if (!this._v) {
                $.Event.addEvent(document, 'click', this._onClick);
                $(this.el).removeClass('is-hidden');
                this._v = true;
                this.draw();

                if (typeof this._o.onOpen === 'function') {
                    this._o.onOpen.call(this);
                }
            }
        },

        hide: function() {
            var v = this._v;
            if (v !== false) {
                $.Event.removeEvent(document, 'click', this._onClick);
                this.el.style.cssText = '';
                $(this.el).addClass('is-hidden');
                this._v = false;

                if (v !== undefined && typeof this._o.onClose === 'function') {
                    this._o.onClose.call(this);
                }
            }
        },

        /** GAME OVER */
        destroy: function() {
            this.hide();
            $.Event.removeEvent(this.el, 'mousedown', this._onMouseDown, true);
            $.Event.removeEvent(this.el, 'change', this._onChange);
            if (this._o.field) {
                $.Event.removeEvent(this._o.field, 'change', this._onInputChange);
                $.Event.removeEvent(this._o.trigger, 'click', this._onInputClick);
                $.Event.removeEvent(this._o.trigger, 'focus', this._onInputFocus);
                $.Event.removeEvent(this._o.trigger, 'blur', this._onInputBlur);
            }
            if (this.el.parentNode) {
                this.el.parentNode.removeChild(this.el);
            }
        }

    };

    return JCalendar;
});/* Form组件 */
;(function ($, factory) {

    $.Form = factory($);

    var FormCache = {};

    $.F = function(id, data) {
        var form = FormCache[id];
        if( form == null && data == null ) {
            return null;
        }

        if( form == null || data ) {
            form = new $.Form($1(id));
            FormCache[form.id] = form;  

            form.load(data)
        }
        
        return form;
    };

    $.fn.extend({
        form: function(data) {
            if(this.length > 0) {
                return $.F(this[0].id, data);
            }
        }
    });

})(tssJS, function ($) {

    'use strict';

    var showErrorInfo = function(errorMsg, obj) {
        setTimeout(function() {
            if( $.Balloon ) {
                $(obj).notice(errorMsg);
            }
        }, 100);
    },

    setFocus = function(){
        try { this.el.focus(); } catch(e) { }
    },

    validate = function() {
        var name     = this.el.getAttribute("id");
        var empty    = this.el.getAttribute("empty");
        var caption  = (this.el.getAttribute("caption")||"").replace(/\s/g, "");
        var checkReg = this.el.getAttribute("checkReg") || this.el.getAttribute("inputReg");
        
        var errorMsg = "";
        var value = this.form ? this.form.getData(name) : this.el.value;
        if(!value && this.el.value && this.tree) {
            errorMsg = "[" + caption + "]手输无效，要求必须选中不少于一个下拉节点。";
        }

        if( !value && empty == "false") {
            errorMsg += "[" + caption + "] 不允许为空。";
        }
        else if(checkReg && value && !(new RegExp(checkReg)).test(value)) {
            errorMsg = this.el.getAttribute("errorMsg");
            errorMsg = errorMsg || "[" + caption + "] 格式不正确，请更正.";
        }

        if( errorMsg ) {
            showErrorInfo(errorMsg, this.el);

            if( !!this.isInstance ) {
                this.setFocus();
            }

            $.Event.cancel();
            return false;
        }
        return true;
    },

    value2List = function(value) {
        var valueList = {};
        if( !$.isNullOrEmpty(value) ) {
            value.split(",").each(function(i, item){
                valueList[item] = true;
            })
        }           
        return valueList;
    },

    createFieldObj = function(field, $el) {
        var fieldObj;
        var fieldType = field.getAttribute("mode");
        switch(fieldType) {
            case "date":
            case "function":
            case "datetime":
                fieldObj = new FunctionField($el, this, fieldType === "datetime");
                break;
            case "hidden":
                fieldObj = new HiddenFiled($el, this);
                break;
            case "combo":
                fieldObj = new ComboField($el, this);
                break;
            case "combotree":
                fieldObj = new ComboTreeField($el, this);
                break;
            case "int":
            case "number":
            case "string":
            default:
                fieldObj = new StringField($el, this);
                break;
        }
        return fieldObj;
    },

    fireOnChangeEvent = function(el, newValue) {
        var onchangeFunc = el.getAttribute("onchange");
        if(onchangeFunc) {
            onchangeFunc = onchangeFunc.replace(/\^/g, "'"); // 有些地方的配置無法直接使用引号（如JSON），用^代替，這裡替換回來
            var rightKH = onchangeFunc.indexOf(")");
            if(rightKH > 0) {
                onchangeFunc = onchangeFunc.substring(0, rightKH) + ", '" + newValue + "')"; 
            }
            else {
                onchangeFunc = onchangeFunc + "('" + newValue + "')";
            }

            eval(onchangeFunc);
        }
    },

    XMLTemplate = function(dataXML) {
        this.sourceXML = dataXML;
             
        this.declare = $("xform>declare", dataXML)[0];
        this.layout  = $("xform>layout", dataXML)[0]; 
        this.script  = $("xform>script", dataXML)[0];
    
        this.dataNode =  $("xform>data", dataXML)[0];
        if(this.dataNode == null) {             
            this.dataNode = $.XML.createNode("data");
            this.sourceXML.appendChild(this.dataNode);
        }
        
        this.rowNode = $("row", this.dataNode)[0];;
        if(this.rowNode == null) {
            this.rowNode = $.XML.createNode("row");
            this.dataNode.appendChild(this.rowNode);    
        }
        
        var oThis = this;
        this.fieldsMap = {};
        $("column", this.declare).each( function(i, column) {
            oThis.fieldsMap[column.getAttribute("name")] = column;
        } );
    };

    XMLTemplate.prototype = {

        /* 获取row节点上与column对应的值 */
        getFieldValue: function(name) {
            var node = this.rowNode.querySelector(name.replace(/\./gi, "\\."));
            if( node ) {
                return $.XML.getText(node).convertEntry();
            }
            return null;
        },

        toHTML: function() {
            var htmls = [], oThis = this;
            htmls.push("<form class='tssForm' method='post' onsubmit='return false;' autocomplete='off'>");
            htmls.push('<table>');

            // 添加隐藏字段           
            $("column[mode='hidden']", this.declare).each( function(i, column){
                var name = column.getAttribute("name");
                var value = oThis.getFieldValue(name);
                value = value ? "value=\"" + value + "\"" : "";
                htmls.push('<input type="hidden" ' + value + ' id="' + name + '"/>');
            } );
            htmls.push('<input type="hidden" name="xml" id="xml"/>');

            var trList = this.layout.querySelectorAll("TR");
            for(var i=0; i < trList.length; i++) {
                var trNode = trList[i];
                htmls.push("<tr>");

                var tdList = trNode.querySelectorAll("TD");
                for(var j=0; j < tdList.length; j++) {
                    var tdNode = tdList[j];
                    htmls.push("<td "+ copyNodeAttribute(tdNode) +">");

                    var childNodes = tdNode.childNodes;
                    for(var n=0; n < childNodes.length; n++) {
                        var childNode = childNodes[n];
                        if(childNode.nodeType != $.XML._NODE_TYPE_ELEMENT) {
                            htmls.push(childNode.value);
                            continue;
                        }

                        var binding = childNode.getAttribute("binding");
                        var column = this.fieldsMap[binding];
                        if(column == null) {
                            htmls.push($.XML.toXml(childNode));
                            continue;
                        }

                        var mode    = column.getAttribute("mode") || 'string';
                        var caption = column.getAttribute("caption");
                        var value   = this.getFieldValue(binding);
                        var _value  = (value ? " value=\"" + value + "\"" : " ");
                        
                        var nodeName = childNode.nodeName.toLowerCase(); // label、input、textarea等 
                        if(nodeName == "label" && binding && binding != "") {
                            htmls.push("<label id='label_" + binding + "'>" + caption + "</label>");
                        }
                        else if(mode == 'combo') {
                            htmls.push("<select " + copyNodeAttribute(childNode) + copyColumnAttribute(column) + _value + "></select>");
                        }
                        else if(mode == "string" && nodeName == 'textarea') {
                            htmls.push("<textarea " + copyNodeAttribute(childNode) + copyColumnAttribute(column) + " spellcheck='false'>" + (value ? value : "") + "</textarea>");
                        }
                        else if(mode == "string" || mode == "number" || mode == "int" 
                             || mode == "function" || mode == "date" || mode == "datetime" || mode == "combotree") {
                            htmls.push("<input " + copyNodeAttribute(childNode) + copyColumnAttribute(column) + _value + " spellcheck='false'></input>");
                        }
                    }
                    htmls.push("</td>");
                }   
                htmls.push("</tr>");
             }

             htmls.push("</table>");
             htmls.push("</form>");
             return htmls.join("");

             // some private function define
             function copyColumnAttribute(column) {
                var returnVal = " ";
                $.each(column.attributes, function(i, attr){
                    var name  = attr.nodeName;
                    var value = attr.value;
                    if(value && value != "null") {
                        if(name == "name") {
                            name = "id";
                        }
                        returnVal += name + " = \"" + value + "\" ";
                    }
                } );
 
                return returnVal;
             }

             function copyNodeAttribute(node) {
                var returnVal = "";
                var hasBinding = node.getAttribute("binding") != null;
                $.each(node.attributes, function(i, attr){
                    if(attr.nodeName != "style" || !hasBinding) {
                        returnVal += attr.nodeName + "=\"" + attr.value + "\" ";
                    }
                    if(attr.nodeName == "style" && hasBinding) {
                        returnVal += "style=\"" + attr.value + "\" ";
                    }
                } );
                return returnVal;
             }
        }
    };
 
    var Form = function(element) {
        this.id  = element.id;
        this.box = element;

        this.editable = element.getAttribute("editable") || "true";
        this.fieldObjMap = {};
    };

    Form.prototype = {

        load: function(dataXML) {
            if("object" != typeof(dataXML) || dataXML.nodeType != $.XML._NODE_TYPE_ELEMENT) {
                return $.alert("传入的Form数据有问题，请检查。");
            }
            
            this.template = new XMLTemplate(dataXML);   
            this.box.innerHTML = this.template.toHTML(); 

            // 绑定各个字段输入框对应的编辑方式
            this.attachEditor();

            // 添加tssForm定制的script
            if(this.template.script) {
                $.createScript($.XML.getText(this.template.script));
            }
        
            // 绑定事件
            this.box.onselectstart = function(ev) {
                $.Event.cancelBubble(ev); // 拖动选择事件取消冒泡
            }

            var form = this.box.querySelector("form");
            if(form) {
                $.Event.addEvent(form, "submit", this.checkForm);
            }   

            $(".fullscreenable", form).each(function(i, el){
                $(el).fullscreen();
            })
        },
 
        attachEditor: function() {
            var fieldsMap = this.template.fieldsMap;
            for(var fieldName in fieldsMap) {
                var field = fieldsMap[fieldName];

                // 取layout中绑定该field column的element，如无，则字段无需展示。
                var el = $1(fieldName);
                if( !el )  continue;

                this.fieldObjMap[fieldName] = createFieldObj.call(this, field, $(el));

                if(field.getAttribute('empty') == "false") {
                    var requiredTag = $.createElement("span", "required");
                    $(requiredTag).html("*");
                    el.parentNode.appendChild(requiredTag);
                }
            }

            this.setEditable();
        },
 
        checkForm: function() {
            for(var fieldName in this.fieldObjMap) {
                var fieldObj = this.fieldObjMap[fieldName];
                if( !fieldObj.validate() ) {
                    return false;
                }
            }

            $$("xml").value = $.XML.toXml(this.template.dataNode);
            return true;
        },

        setEditable: function(status) {
            status = status || this.editable;

            $(".buttonBox", this.box.parentNode).css("display", status == "true" ? "block": "none");

            var oThis = this, firstEditableField;
            $.each(this.fieldObjMap, function(name, fieldObj) {
                var _status = status;

                // 如果field column上默认定义为不可编辑，则永远不可编辑
                var mode = oThis.getFieldConfig(name, "mode");
                var editable = oThis.getFieldConfig(name, "editable");
                if ( editable == "false" ) {
                    _status = "false";
                } 
                fieldObj.setEditable(_status);

                if( !firstEditableField && _status == "true" && mode == "string") {
                    firstEditableField = fieldObj;
                }               
            });

            if(firstEditableField) {
                firstEditableField.setFocus();
            }
        },

        setFieldEditable: function(name, value) {
            var fieldObj = this.fieldObjMap[name];
            if( fieldObj ) {
                fieldObj.setEditable(value);
            }
        },

        /* 设置row节点上与field column对应的值 */
        setFieldValue: function(name, value) {
            var rowNode = this.template.rowNode;
            var node = rowNode.querySelector(name.replace(/\./gi, "\\."));
            if( node == null ) { 
                rowNode.appendChild(node = $.XML.createNode(name)); // 创建单值节点
            }

            var CDATANode = node.firstChild;
            if( CDATANode == null ) {
                node.appendChild(CDATANode = $.XML.createCDATA(value));
            } else {
                $.XML.setText(CDATANode, value);
            }

            var eventOndatachange = new $.EventFirer(this, "ondatachange");
            var ev = $.Event.createEventObject();
            ev.id = this.id + "_" + name;
            eventOndatachange.fire(ev);  // 触发事件
        },

        // 将界面数据更新到Form模板的data/row/里
        updateData: function(el) {
            var newValue;
            var ev = window.event;
            if(ev && ev.propertyName == "checked") {
                newValue = el.checked == true ? 1 : 0;
            }
            else if(el.tagName.toLowerCase() == "select") {
                newValue = el._value;            
            }
            else {
                newValue = el.value;
            }

            var oldValue = this.getData(el.id);
            if( $.isNullOrEmpty(newValue) && $.isNullOrEmpty(oldValue) ) {
                return;
            }
            if(newValue != oldValue) {
                this.setFieldValue(el.id, newValue);
            }
        },

        // 将数据设置到界面输入框上显示，同时更新到data/row/里
        updateDataExternal: function(name, value) {
            this.setFieldValue(name, value);
            
            // 更改页面显示数据
            var fieldObj = this.fieldObjMap[name];
            if(fieldObj) {
                fieldObj.setValue(value);
            }
        },

        getData: function(name) {
            return this.template.getFieldValue(name);
        },

        showCustomErrorInfo: function(name, str) {
            var fieldObj = this.fieldObjMap[name];
            if( fieldObj ) {
                showErrorInfo(str, fieldObj.el);
            }
        },

        getFieldConfig: function(name, attrName) {
            var field = this.template.fieldsMap[name];
            if( field == null ) {
                return $.alert("指定的字段[" + name + "]不存在");
            }
            return field.getAttribute(attrName);
        },

        /* 示例：动态更新下拉树（注：录入表已经全部使用combotree，不在用combo）：
            form.updateField("center2", [
                        {"name": "mode", "value": "combotree"},
                        {"name": "texts", "value": texts.join('|')},
                        {"name": "values", "value": values.join('|')}
                     ]) */
        updateField: function(name, attrs) {
            var field = this.template.fieldsMap[name];
            if(!field) {
                field = this.template.fieldsMap[name.replace('p_', '')]; // recorder.html的查询form在字段前加了p_
            }
            if( field ) {
                var $el = $($1(name));
                $.each(attrs, function(i, attr) {
                    $(field).attr(attr.name, attr.value);
                    $el.attr(attr.name, attr.value);
                });

                var fieldObj = createFieldObj.call(this, field, $el);
                this.fieldObjMap[name] = fieldObj;

                fieldObj.refresh && fieldObj.refresh();
            }
        },

        getXmlDocument: function() {
            return this.template.sourceXML;
        }
    };

    // 普通文本输入框
    var StringField = function($el, form) {
        this.el = $el[0];
        this.el._value = this.el.value; // 备份原值

        var oThis = this;
        this.el.onblur = function() {
            if("text" == this.type) { // 判断input的类型
                this.value = this.value.trim(); // 去掉前后的空格
            }
            form.updateData(this);
        };
    };

    StringField.prototype = {
        setValue : function(value) {
            this.el._value = this.el.value = value;
        },

        validate: validate,
        
        setEditable : function(status) {
            this.el.editable = status || $(this.el).attr("editable");

            var disabled = (this.el.editable == "false");
            if(disabled) {
                $(this.el).addClass("field_disabled").removeClass("string");
            } else {
                $(this.el).removeClass("field_disabled").addClass("string");
            }

            if(this.el.tagName == "textarea") {
                this.el.readOnly = disabled;  // textarea 禁止状态无法滚动显示所有内容，所以改为只读
            } else {
                this.el.disabled = disabled;        
            }
        },
 
        setFocus : setFocus
    };

    // 自定义方法输入值类型
    var FunctionField = function($el, form, isDatetime) {
        this.el = $el[0];
        this.el._value = this.el.value; // 备份原值
        this.isdate = ($el.attr("mode").toLowerCase() == "date") || isDatetime;
     
        if( !this.el.disabled ) {
            if(this.isdate) {
                if(this.picker == null) {
                    this.picker = new $.Calendar( {
                        field: $1(this.el.id),
                        firstDay: 1,
                        minDate: new Date('2000-01-01'),
                        maxDate: new Date('2050-12-31'),
                        yearRange: [2000,2050],
                        format: 'yyyy-MM-dd',
                        careTime: isDatetime
                    });
                }
            }
            else { 
                var funcIcon = $.createElement("span", "functionBt"); // 添加点击按钮
                if(this.el.nextSibling) {
                    this.el.parentNode.insertBefore(funcIcon, this.el.nextSibling);
                } else {
                    this.el.parentNode.appendChild(funcIcon);
                }               
 
                var cmd = $el.attr("cmd");
                funcIcon.onclick = function() {
                    $.execCommand(cmd);
                };
            }
        }   

        this.el.onblur = function() {
            form.updateData(this);
        };
    };
 
    FunctionField.prototype = {
        setValue : function(value) {
            this.el._value = this.el.value = value;
        },

        validate: validate,
        
        setEditable : function(status) {
            this.el.disabled  = (status == "false");
            this.el.className = (this.el.disabled ? "field_disabled" : "function");

            // function图标
            if(!this.isdate) {
                this.el.nextSibling.className = (this.el.disabled ? "hidden" : "functionBt");
                this.el.readOnly = true;
            }
            
            this.el.editable = status;
        },
 
        setFocus : setFocus
    };

    // 下拉选择框，单选或多选
    var ComboField = function($el, form) {
        this.el = $el[0];
        this.multiple = $el.attr("multiple") != null;
        this.required = $el.attr('empty') == "false";

        var valueList = ($el.attr("values") || "").trim();
        var textList  = ($el.attr("texts")  || "").trim();   
        
        var valueNode = this.el.attributes["value"];
        this.el._value = valueNode ? valueNode.value : "";

        var selectedValues = value2List(this.el._value);

        // 加一个空白的选项
        if(!this.required) {
            valueList = ("|" + valueList);
            textList = ("|" + textList);
        }
        valueList = valueList.length > 1 ? valueList.split('|') : [];
        textList  = textList.length > 1 ? textList.split('|') : [];
        this.el.options.length = 0; // 先清空
        for(var i=0; i < valueList.length; i++) {
            var value = valueList[i];
            this.el.options[i] = new Option(textList[i], value);
     
            if( selectedValues[value] ) {
                this.el.options[i].selected = true;
            }
        }

        if(this.multiple && $el.attr("height") == null) {
            $el.css("height", Math.min(Math.max(valueList.length, 4), 4) * 25 + "px");
        }   

        // 当empty = false(表示不允许为空)时，下拉列表的默认值自动取第一项值
        if( this.required ) {
            var oldVal = this.el._value; 

            // 原值为空，或当前的下拉列表已经不包含原值，则重新设值
            if( !oldVal || (valueList.length && !valueList.contains(oldVal)) ) {
                this.setValue(valueList[0]);
                form.setFieldValue(this.el.id, valueList[0]);
            }            
        }
        
        this.el.onchange = function() {
            var x = [];
            for(var i=0; i < this.options.length; i++) {
                var option = this.options[i];
                if(option.selected) {
                    x[x.length] = option.value;
                }
            }
            this._value = x.join(",");
            form.updateData(this);
            
            fireOnChangeEvent(this, this._value);
        }
    };

    ComboField.prototype = {
        setValue: function(value) {
            var valueList = value2List(value);

            $.each(this.el.options, function(i, option){
                if(valueList[option.value]) {
                    option.selected = true;
                }
            });

            this.el._value = value;
            fireOnChangeEvent(this.el, value);
        },

        setEditable: function(status) {
            this.el.disabled  = (status == "true" ? false : true);
            this.el.className = (status == "true" ? "combo" : "field_disabled");
            this.el.editable  = status;
        },

        validate: validate,

        setFocus: setFocus
    };

    var ComboTreeField = function($el, form) {
        this.form = form;
        this.el = $el[0];
        this.multiple = $el.attr("multiple") != null;
        
        var treeEl;
        if( !this.el.treeEl ) {
            var treeEl = $.createElement("Tree", "comboTree", $.getUniqueID("comboTree"));
            document.body.appendChild(treeEl);
            this.el.treeEl = treeEl;
        } else {
            treeEl = this.el.treeEl;
        }
        
        if(this.multiple) {
            $(treeEl).attr("treeType", "multi");
        } 

        var oThis = this;
        this.load = function() {
            var valueList = ($el.attr("values") || "").split('|');
            var textList  = ($el.attr("texts")  || "").split('|');
            this.height = Math.max(4, Math.min(valueList.length, 10)) * 25 + "px";

            this.nodesData = [];
            for(var i=0; i < valueList.length; i++) {
                this.nodesData.push( {"id": valueList[i], "name": textList[i], })
            }

            this.tree = $(treeEl).tree(this.nodesData); 
            this.tree.onTreeNodeActived = onSelectNode;
            this.tree.onTreeNodeChecked = onSelectNode;

            var valueNode = this.el.attributes["value"];
            this.el._value = valueNode ? valueNode.value : "";
            var texts;
            if(this.el._value) {
                if(this.multiple) {
                    this.tree.setCheckValues(this.el._value, true);
                    texts = this.tree.getCheckedIds(false, "name");
                } else {
                    this.tree.setActiveTreeNode(this.el._value);
                    texts = this.tree.getActiveTreeNodeName();
                }
            }
            this.el.value = texts || "";
        }
        this.load();

        this.position = function() {
            var elPosition = $.absPosition(this.el);
            $(treeEl).position(elPosition.left, elPosition.bottom).hide();
            $(treeEl).css("height", this.height).css("width", $.getStyle(this.el, "width"));
        }
        this.position();

        // 控制mousedown事件，使的在树节点上点击时，下拉框不会消失（不加的话一点击this.el的obblur会被触发）
        $.Event.addEvent(this.tree.el, 'mousedown', function(e) {
            e = e || window.event;
            $.Event.cancel(e);
        }, true);

        this.el.onblur = function() {
            $(oThis.tree.el).hide();
        };

        this.el.onfocus = function() {
            oThis.position();
            $(oThis.tree.el).show();
        };

        this.el.onclick = function() {
            oThis.position();
            $(oThis.tree.el).show();
        };

        this.el.oninput = function() {
            // 自动过滤树节点
            var temp = [], inputVal = this.value;
            oThis.nodesData.each(function(i, nodeData){
                inputVal.split(",").each(function(i, item) {
                    if(nodeData.name.indexOf(item) >=0 && !temp.contains(nodeData)) {
                        temp.push(nodeData);
                    }
                });
            });
            oThis.tree = $(treeEl).tree(temp); 
            oThis.tree.onTreeNodeActived = onSelectNode;
            oThis.tree.onTreeNodeChecked = onSelectNode;

            // 如果删空了，则清除已经选中的值
            if( $.isNullOrEmpty(inputVal) ) {
                oThis.setValue(inputVal);
                form.setFieldValue(oThis.el.id, inputVal);
            }
        };

        function onSelectNode(event) {
            if(oThis.multiple) {
                var value = oThis.setValue();
                form.setFieldValue(oThis.el.id, value);
            }
            else {
                $(treeEl).hide();

                var selectedNodeId = event.node.id;
                oThis.setValue(selectedNodeId);
                form.setFieldValue(oThis.el.id, selectedNodeId);
            }
        }

        this.tree.onTreeNodeActived = onSelectNode;
        this.tree.onTreeNodeChecked = onSelectNode;
    };

    ComboTreeField.prototype = {
        // 下拉多选树是勾选，不会传入value值，需要直接去取已经选中的树节点
        setValue: function(value) {
            var text;
            if(this.multiple) {
                if(value) {                   
                    this.tree.setCheckValues(value.split(','), true);
                }
                else {
                    value = this.tree.getCheckedIds(false);
                }
                text = this.tree.getCheckedIds(false, "name");
            } 
            else {
                this.tree.setActiveTreeNode(value);
                text = this.tree.getActiveTreeNodeName();
            }

            this.el.value = text || "";
            this.el._value = value || "";
            fireOnChangeEvent(this.el, value);

            return value;
        },

        setEditable: function(status) {
            this.el.editable  = status;
            this.el.disabled  = (status == "true" ? false : true);
            this.el.className = this.el.disabled ? "field_disabled" : "combo";
        },

        validate: validate,
        setFocus: setFocus,

        refresh: function() {
            this.load();
        }
    };
 
    // 隐藏hidden字段
    var HiddenFiled = function($el, form) {
        this.el = $el[0];
    };

    HiddenFiled.prototype = {
        setValue: function(s) {},
        setEditable: function(s) {},
        validate: function() { return true; },
        setFocus: function() {}
    };

    return Form;
});

/* Grid组件 
 * 事件: 
 * 1、onLoad:   Grid更新加载完成后触发 $("#grid").attr("onLoad", "f1"); 注意此时Grid对象还没有完全生成
 * 2、onFinish: Grid生成后触发 $("#grid").attr("onFinish", "f2"); 
 * 3. onScrollToBottom
 * 
 * TODO:
 *  !. Grid控件表头增加求和功能
 */
;(function ($, factory) {

    $.Grid = factory($);

    var GridCache = {};

    $.G = function(id, data) {
        var grid = GridCache[id];
        if( data ) {
            grid = new $.Grid($1(id), data);
            GridCache[grid.id] = grid;  

            var onFinish = $("#" + id).attr("onFinish");
            onFinish && $.execCommand( onFinish + "()" );
        }
        
        return grid;
    };

    $.fn.extend({
        grid: function(data) {
            if(this.length > 0) {
                return $.G(this[0].id, data);
            }
        }
    });

})(tssJS, function ($) {

    'use strict';

    var cellHeight = 22,  // 数据行高

    getAlign = function(column) {
        var align = column.getAttribute("align");
        if(align) {
            return align;
        }

        switch(column.getAttribute("mode")) {
            case "number":
            case "int":
                return "right";
            case "boolean":
            case "date":
            default:
                return "center";
        }
    },

    getCellValue = function(tr, colName) {
        var cells = curRow.cells;
        for(var j=0; j < cells.length; j++) {
            var cell = cells[j];
            if( cell.getAttribute("name") == colName ) {
                return cell.getAttribute("value");
            }
        }
    },

    bindAdjustTHHandler = function(table) { 

        $(".checkAll", table).click(function(){
            var allCheck = this.checked;
            $("input[name='grid_cb']", table).each(function(){
                this.checked = allCheck;
            });
        });

        $("thead tr td", table).each(function(i, th) {
            // 双击隐藏列
            th.ondblclick = function() {
                $(th).hide();
                $("tbody tr", table).each( function(j, row) {
                    $(row.cells[i]).hide();
                });
            };

            th.onmousedown = function() {
                if(window.event.offsetX > this.offsetWidth - 5) {
                    this.mouseDown = true;
                    this.oldX = window.event.x;
                    this.oldWidth = this.offsetWidth;
                }
            };

            // 结束宽度调整 
            th.onmouseup = function() {
                this.mouseDown = false;
                $(this).css("cursor", "default");
            };

            th.onmousemove = function(ev) {
                ev = ev || window.event;
                var colseToEdge = ev.offsetX > this.offsetWidth - 7;
                $(this).css("cursor", colseToEdge ? "col-resize" : "default");
                
                if( !!this.mouseDown ) {
                    $(this).css("cursor", "col-resize");

                    var distance = (ev.x - this.oldX);
                    $(this).css("width", (this.oldWidth + distance) + "px");
                }
            }
        });
    },

    bindSortHandler = function(table) { 
        var rows = [];
        var tbody = $("tbody", table)[0];
        $("tr", tbody).each( function(i, row) {
            rows[i] = row;
        });

        var thList = $("thead tr td", table);
        var direction = 1;
     
        thList.each( function(i, th) {
            var sortable = th.getAttribute("sortable");
            if( sortable == "true") {
                th._colIndex = i;

                $(th).click(function() {
                    // 先清除已有的排序
                    thList.each(function(i, _th) {
                        $(_th).removeClass("desc").removeClass("asc");
                    });
                    $(this).addClass(direction == 1 ? "desc" : "asc");

                    var columnIndex = this._colIndex;
                    rows.sort(function(row1, row2) {
                        var x = $.XML.getText(row1.cells[columnIndex]);
                        var y = $.XML.getText(row2.cells[columnIndex]);
                        var compareValue;
                        if( !x || isNaN(x) ) {
                            compareValue = x.localeCompare(y);
                        }
                        else {
                            compareValue = Number(x) - Number(y);
                        }
                        return compareValue * direction;
                    });

                    // 按排序结果对table进行更新，并设置排序列的样式                    
                    rows.each(function(i, row) {
                        $.each(row.cells, function(j, cell){
                            if(j == columnIndex) {
                                $(cell).addClass("sorting");
                            } else {
                                $(cell).removeClass("sorting");
                            }
                        });
                        
                        tbody.appendChild(row);
                        row.setAttribute("_index", i + 1);
                    });

                    direction = direction * -1;
                });
            }   
        });
    },

    XMLTempalte = function(dataXML) {
        this.declare = $("declare", dataXML)[0];
        this.script  = $("script", dataXML)[0];
        this.columns = this.declare.querySelectorAll("column");
        this.dataRows = dataXML.querySelectorAll("data row");

        var columnsMap = {};
        $.each(this.columns, function(index, column) {
            columnsMap[column.getAttribute("name")] = column;
        });
        this.columnsMap = columnsMap;

        this.hasHeader    = this.declare.getAttribute("header") == "checkbox";
        this.needSequence = this.declare.getAttribute("sequence") != "false";
    },

    JsonTemplate = function(dataJson) {
        // TODO 支持json格式的数据源
    };

    XMLTempalte.prototype = {
        toHTML: function(startNum) {
            var htmls = [], thead = [], tbody = [];

            thead.push('<thead><tr>');
            if(this.hasHeader) {
                thead.push('<td name="cellheader" style="width:20px"><input type="checkbox" class="checkAll"/></td>');
            }
            if(this.needSequence) {
                thead.push('<td name="sequence" style="min-width:30px;width:30px">序号</td>');
            }
            $.each(this.columnsMap, function(name, column) {
                var caption = column.getAttribute("caption");
                var width   = column.getAttribute("width");
                var style = (width ? ' style="width:' + width + '"': '');
                var _class   = column.getAttribute("display")  == "none" ? ' class="hidden"' : '';
                var sortable = column.getAttribute("sortable") == "true" ? ' sortable="true"' : '';
                thead.push('<td name="' + name + '" ' + _class + style + sortable + '>' + caption + '</td>');
            });
            thead.push("</tr></thead>");
 
            var oThis = this;
            tbody.push('<tbody>');
            $.each(this.dataRows, function(i, row) {
                var index = startNum + i + 1;
                tbody.push('<tr _index="' + index + '">');

                if(oThis.hasHeader) {
                    tbody.push('<td></td>');
                }
                if(oThis.needSequence) {
                    tbody.push('<td></td>');
                }

                var columnsMap = oThis.columnsMap;
                for(var name in columnsMap) {
                    var value  = row.getAttribute(name) || "";
                    var _class = columnsMap[name].getAttribute("display")  == "none" ? ' class="hidden"' : '';
                    tbody.push('<td name="' + name + '" value="' + value + '" ' + _class + '>' + value + '</td>');
                }

                tbody.push("</tr>");
            });
            tbody.push("</tbody>");

            htmls.push("<table>");
            htmls.push(thead.join(""));
            htmls.push(tbody.join(""));
            htmls.push("</table>");
            return htmls.join("");
        }
    }; 

    var Grid = function(element, data) {
        this.id = element.id;
        this.gridBox = this.element = this.el = element;
        this.gridBox.innerHTML = "";

        // Grid控件上禁用默认右键
        (element.parentNode || document).oncontextmenu = function(_event) {
            $.Event.cancel(_event || window.event);
        }   

        this.gridBox.style.width = element.getAttribute("width")  || "100%";

        var pointHeight = element.getAttribute("height");
        if( pointHeight == null || pointHeight == '0' ) {
            pointHeight = element.clientHeight || element.parentNode.clientHeight; 
            pointHeight = Math.max(pointHeight, parent.document.body.clientHeight-50);
        }
        $(this.gridBox).css("height", pointHeight + "px"); // hack 固定住grid高度，以免在IE部分版本及FF里被撑开
        
        this.windowHeight = pointHeight;
        this.pageSize = Math.floor(this.windowHeight / cellHeight);
  
        this.load(data);    

        // 添加Grid事件处理
        this.addGridEvent();    
    };

    Grid.prototype = {
        load: function(data, append) {
            if("object" != typeof(data) || data.nodeType != $.XML._NODE_TYPE_ELEMENT) {
                return $.tip("Grid数据加载异常，请刷新重试");   
            } 

            // 初始化变量
            var startNum = append ? this.totalRowsNum : 0;  

            this.template = new XMLTempalte(data);  
            var gridTableHtml = this.template.toHTML(startNum); // 解析成Html
          
            if(append) {
                var tempParent = $.createElement("div");
                $(tempParent).html(gridTableHtml);
                var newRows = tempParent.childNodes[0].tBodies[0].rows;
                for(var i=0; i < newRows.length; i++) {
                    var cloneRow = newRows[i].cloneNode(true);
                    this.tbody.appendChild(cloneRow);
                }
            }
            else {
                $(this.gridBox).html(gridTableHtml);
                this.tbody = $("tbody", this.gridBox)[0];
            }
          
            var table  = $("table", this.gridBox)[0];
            this.totalRowsNum = this.tbody.rows.length;
            for(var i = startNum; i < this.totalRowsNum; i++) {
                this.processDataRow(this.tbody.rows[i]); // 表格行TR
            }
           
            bindAdjustTHHandler(table);
            bindSortHandler(table);

            // Grid更新加载完成后触发
            var onLoad = $(this.el).attr("onLoad");
            onLoad && $.execCommand( onLoad + "()" );
        }, 

        /* 处理数据行,将值解析成下拉列表值、图片、选择框等 */
        processDataRow: function(curRow) {
            $(curRow).hover(
                function() { 
                    $(curRow).addClass("highlight"); 
                }, 
                function() { 
                    $(curRow).removeClass("highlight");
                } 
            );
            
            var cells = curRow.cells;
            for(var j=0; j < cells.length; j++) {
                var cell = cells[j];

                var hasHeader = this.template.hasHeader;
                if(hasHeader && j == 0) {
                    cell.setAttribute("name", "cellheader");
                    cell.innerHTML = '<input name="' + this.id + '_cb" type="checkbox" >';
                    continue;
                } 
                else if(this.template.needSequence && ((!hasHeader && j == 0) || (hasHeader && j == 1)) ) {
                    cell.setAttribute("name", "sequence");
                    $(cell).html(curRow.getAttribute("_index"));
                    continue;
                }

                this.processDataCell(cell);                     
            }   
        },

        processDataCell: function(cell) {
            var colName = cell.getAttribute("name");
            var column = this.template.columnsMap[colName]; 
            if( colName == null || column == null) {
                return;
            } 

            if(column.getAttribute("highlight") == "true") {
                $(cell).addClass("highlight");
            }
            $(cell).css("text-align", getAlign(column));

            var value = cell.getAttribute("value") ;
            if(value == null) return;

            var mode  = column.getAttribute("mode") || "string";
            switch( mode ) {
                case "number": 
                case "int":  
                case "date":
                    cell.title = value;
                    break;         
                case "function":                          
                    break;    
                case "image":          
                    cell.innerHTML = "<img src='" + value + "'/>";
                    break;    
                case "boolean":      
                    var checked = (value =="true") ? "checked" : "";
                    cell.innerHTML = "<form><input class='selectHandle' type='radio' " + checked + "/></form>";
                    cell.querySelector("input").disabled = true;
                    break;
                case "string":
                default:
                    var texts = column.getAttribute("texts");
                    var values = column.getAttribute("values");
                    if(values && texts) {
                        var listNames  = texts.split("|");
                        var listValues = values.split("|");
                        listValues.each(function(n, optionValue) {
                            if(value == optionValue) {
                                value = listNames[n];
                            }
                        });
                    }
                    value = value.replace(/\^/gi, "\""); // @see GridNode 104行
                    $(cell).html(value);
                    
                    var showTitle = column.getAttribute("showTitle");
                    if(showTitle != 'false') {
                        $(cell).title(value);  
                    }
                                            
                    break;
            }                           
        },

        /*
         * 根据页面上的行序号，获取相应的Row对象
         * 参数：  index   行序号
         * 返回值： Row对象
         */
        getRowByIndex: function(index) {
            for(var i = 0; i < this.tbody.rows.length; i++) {
                var row = this.tbody.rows[i];
                if(row.getAttribute("_index") == index) {
                    return row;
                }
            }
        },

        // 获取选中行中指定列的值
        getColumnValue: function(columnName) {
            var value;
            var rowIndex = this.gridBox.selectRowIndex; 
            if(rowIndex) {
                var row = this.getRowByIndex(rowIndex);
                $.each(row.cells, function(i, cell) {
                    if(cell.getAttribute("name") == columnName) {
                        value = cell.getAttribute("value");
                    }
                });
            }
            return value;
        },

        // 获取某一列的值
        getColumnValues: function(columnName) {
            var values = [];
            var cells = $("tr>td[name='" + columnName + "']", this.tbody);
            cells.each(function(i, cell){
                values[i] = cell.getAttribute("value");
            });

            return values;
        },

        // 新增一行
        insertRow: function(map) {
            var trList = this.gridBox.querySelectorAll("table tbody tr");
            var lastRow = trList[trList.length - 1];
            var lastRowIndex = lastRow ? parseInt(lastRow.getAttribute("_index")) : 0; 

            var newRow = this.tbody.insertRow(this.totalRowsNum ++);
            newRow.setAttribute("_index", lastRowIndex + 1);

            var thList = $("table thead td", this.gridBox);
            thList.each( function(i, th) {
                var colName = th.getAttribute("name");
                
                var cell = newRow.insertCell(i);
                cell.setAttribute( "name", colName );

                if(map[colName]) {
                    $(cell).html(map[colName]);
                    cell.setAttribute( "value", map[colName] );
                }

                if($(th).hasClass("hidden")) {
                    $(cell).addClass("hidden");
                }
            });
 
            this.processDataRow(newRow);

            bindSortHandler(this.tbody.parentNode);
        },

        // 删除单行
        deleteRow: function(row) {
            this.tbody.removeChild(row);
            this.totalRowsNum --;
        },

        deleteRowByIndex: function(rowIndex) {
            var row = this.getRowByIndex(rowIndex);
            this.deleteRow(row);

            bindSortHandler(this.tbody.parentNode);
        },

        deleteSelectedRow: function() {
            var rowIndex = this.gridBox.selectRowIndex;
            this.deleteRowByIndex(rowIndex);
            delete this.gridBox.selectRowIndex;
        },
            
        // 更新单行记录的某个属性值
        modifyRow: function(row, attrName, value) {
            var oThis = this;
            $.each(row.cells, function(i, cell) {
                if(cell.getAttribute("name") == attrName) {
                    cell.setAttribute("value", value);
                    oThis.processDataCell(cell);
                }
            });
        },

        modifyRowByIndex: function(rowIndex, attrName, value) {
            var row = this.getRowByIndex(rowIndex);
            this.modifyRow(row, attrName, value);
        },

        modifySelectedRow: function(attrName, value) {
            var rowIndex = this.gridBox.selectRowIndex;
            this.modifyRowByIndex(rowIndex, attrName, value);
        },

        getHighlightRow: function() {
            return $(".highlight", this.tbody)[0];
        },

        getCheckedRows: function() {
            return this.getCheckedRowsValue("id").join(",");
        },

        getCheckedRowsValue: function(field) {
            var result = [];
            $("input[name='grid_cb']", this.tbody).each(function(){
                if(this.checked){
                    var tr = this.parentNode.parentNode;
                    var $td = $("td[name='" + field + "']", tr);
                    if( $td.length ) {
                        result.push( $td.attr("value") );
                    }
                }
            });
            return result;
        },

        // 添加Grid事件处理
        addGridEvent: function() {          
            var oThis = this, deltaY = 10;

            this.gridBox.onscroll = function() {
                 /* 
                  * 判断是否到达底部，需满足: scrollTop + clientHeight == scrollHeight 
                  * scrollTop：滚动条在Y轴上的滚动距离。
                  * clientHeight：内容可视区域的高度。
                  * scrollHeight：内容可视区域的高度加上溢出（滚动）的距离。
                  * 前提条件：是在向下滚动。
                  */
                var scrollDown = false;
                if(this.scrollTop > deltaY)  { // down
                    deltaY = this.scrollTop; 
                    scrollDown = true;
                } 
                if(this.scrollTop == deltaY) { } // 横向运动, scrollLeft在变
                if(this.scrollTop < deltaY)  { // up
                    deltaY = this.scrollTop; 
                }

                if( scrollDown && (this.scrollHeight - this.scrollTop <= this.clientHeight) ) {
                    var eventFirer = new $.EventFirer(oThis.el, "onScrollToBottom");
                    eventFirer.fire();
                }
            };

            this.gridBox.onmousewheel = function() {
                this.scrollTop += -Math.round(window.event.wheelDelta / 120) * cellHeight;
            };
            
            this.gridBox.onkeydown = function() {
                switch (window.event.keyCode) {
                    case 33:    //PageUp
                        oThis.gridBox.scrollTop -= oThis.pageSize * cellHeight;
                        return false;
                    case 34:    //PageDown
                        oThis.gridBox.scrollTop += oThis.pageSize * cellHeight;
                        return false;
                    case 35:    //End
                        oThis.gridBox.scrollTop = oThis.gridBox.offsetHeight - oThis.windowHeight;
                        return false;
                    case 36:    //Home
                        oThis.gridBox.scrollTop = 0;
                        return false;
                    case 37:    //Left
                        oThis.gridBox.scrollLeft -= 10;
                        return false;
                    case 38:    //Up
                        oThis.gridBox.scrollTop -= cellHeight;
                        return false;
                    case 39:    //Right
                        oThis.gridBox.scrollLeft += 10;
                        return false;
                    case 40:    //Down
                        oThis.gridBox.scrollTop += cellHeight;
                        return false;
                }
            };
         
            this.gridBox.onclick = function(ev) { // 单击行
                fireClickRowEvent(this, ev, "onClickRow");
            };

            this.gridBox.ondblclick = function(ev) { // 双击行
                fireClickRowEvent(this, ev, "onDblClickRow");
            };

            this.gridBox.oncontextmenu = function(ev) {
                fireClickRowEvent(this, ev, "onRightClickRow"); // 触发右键事件
            };

            // 触发自定义事件
            function fireClickRowEvent(gridBox, ev, firerName) {
                var _srcElement = $.Event.getSrcElement(ev);
                if( _srcElement && notOnGridHead(_srcElement) ) { // 确保点击处不在表头
                    var trObj = _srcElement;
                    while( trObj && trObj.tagName.toLowerCase() != "tr" ) {
                        trObj = trObj.parentElement;
                    }

                    if(trObj && trObj.getAttribute("_index") ) {
                        var rowIndex = parseInt( trObj.getAttribute("_index") );
                        var oEvent = $.Event.createEventObject();
                        oEvent.result = {
                            rowIndex: rowIndex,
                            ev: ev
                        };

                        gridBox.selectRowIndex = rowIndex;
                        var eventFirer = new $.EventFirer(gridBox, firerName);
                        eventFirer.fire(oEvent);  // 触发右键事件
                    }   
                }       
            }
            
            // 确保点击处不在表头
            function notOnGridHead(srcElement) { 
                return !isContainTag(srcElement, "THEAD");
            }
            
            function isContainTag(obj, tag) {
                while( obj ) {
                    if (obj.tagName == tag) {
                        return true;
                    }
                    obj = obj.parentElement;
                }
                return false;
            }
        }
    };

    return Grid;
});


;(function($){

    /*
     *  翻页工具条
     *  参数： object:pageBar      工具条对象
                XmlNode:pageInfo        XmlNode实例
                function:callback       回调函数
     */
    $.initGridToolBar = function(pageBar, pageInfo, callback) {
        pageBar.init = function() {
            this.innerHTML = ""; // 清空内容
            this.callback = callback;

            var totalpages = pageBar.getTotalPages();
            var curPage = pageBar.getCurrentPage();

            var str = [];
            str[str.length] = '<span>共 ' +pageBar.getTotalrecords()+ ' 行 ' +totalpages+ ' 页</span>';
            str[str.length] = '<span class="button refresh" id="GridBtRefresh" title="刷新"></span>';
            str[str.length] = '<span class="button first"   id="GridBtFirst"   title="第一页"></span>';
            str[str.length] = '<span class="button prev"    id="GridBtPrev"    title="上一页"></span>';
            str[str.length] = '<span class="button next"    id="GridBtNext"    title="下一页"></span>';
            str[str.length] = '<span class="button last"    id="GridBtLast"    title="最后一页"></span>';
            
            str[str.length] = '<select id="GridPageList">';
            for(var i=1; i <= totalpages; i++) {
                str[str.length] = '  <option value="' + i + '"' + (curPage == i ? ' selected' : '') + '>' + i + '</option>';
            }
            str[str.length] = "</select>";

            this.innerHTML = str.join("");
 
            $("#GridBtRefresh").click(function() {
                var curPage = pageBar.getCurrentPage();
                pageBar.gotoPage(curPage);
            });
            $("#GridBtFirst").click(function() {
                pageBar.gotoPage("1");
            });
            $("#GridBtLast").click(function() {
                var lastpage = pageBar.getLastPage();
                pageBar.gotoPage(lastpage);
            });
            $("#GridBtNext").click(function() {
                var curPage  = pageBar.getCurrentPage();
                var lastpage = pageBar.getLastPage();
                if(curPage < lastpage) {
                    pageBar.gotoPage(curPage + 1);
                }
            });
            $("#GridBtPrev").click(function() {
                var curPage = pageBar.getCurrentPage();
                if(curPage > 1) {
                    pageBar.gotoPage(curPage - 1);
                }
            });
            $.Event.addEvent($1("GridPageList"), "change", function() {
                pageBar.gotoPage(this.value);
            });
        }
        
        pageBar.getCurrentPage = function() {
            var currentpage = pageInfo.getAttribute("currentpage");
            return currentpage ? parseInt(currentpage) : 1;
        }
        
        pageBar.getLastPage = function() {
            var lastpage = this.getTotalPages();
            return lastpage ? parseInt(lastpage) : 1;
        }
        
        pageBar.getTotalPages = function() {
            var totalpages = pageInfo.getAttribute("totalpages");
            return totalpages ? parseInt(totalpages) : 1;
        }
        pageBar.getTotalrecords = function() {
            var totalrecords = pageInfo.getAttribute("totalrecords");
            return totalrecords ? parseInt(totalrecords) : 0;
        }
        
        pageBar.gotoPage = function(page) {
            if( this.callback ) {
                this.callback(page); // 转到指定页
                $1("GridPageList").value = page;
                pageInfo.setAttribute("currentpage", page);
            }
        }
        
        pageBar.init();
    };

    $.showGrid = function(serviceUrl, dataNodeName, editRowFuction, gridName, page, requestParam, pageBar) {
        pageBar  = pageBar  || $1("gridToolBar");
        gridName = gridName || "grid";
        page     = page || "1";

        var request = new $.HttpRequest();
        request.url = serviceUrl + "/" + page;
        request.params = requestParam || {};
        request.waiting = true;

        request.onresult = function() {
            var gridBox = $1(gridName);
            if(gridBox.getAttribute("height") == null) {
                gridBox.setAttribute("height", gridBox.clientHeight); // hack for IE11
            }
            
            var grid = $.G(gridName, this.getNodeValue(dataNodeName)); 
     
            var gotoPage = function(page) {
                request.url = serviceUrl + "/" + page;
                request.onresult = function() {
                    $.G(gridName, this.getNodeValue(dataNodeName)); 
                    $.initGridToolBar(pageBar, this.getNodeValue("PageInfo"), gotoPage);
                }               
                request.send();
            }

            var pageInfoNode = this.getNodeValue("PageInfo");            
            $.initGridToolBar(pageBar, pageInfoNode, gotoPage);
            
            gridBox.onDblClickRow = function(evObj) {
                editRowFuction();
            }
            gridBox.onRightClickRow = function(evObj) {
                var ev = evObj.result.ev || window.event;
                gridBox.contextmenu.show(ev.clientX, ev.clientY);
            }   
            gridBox.onScrollToBottom = function () {           
                var currentPage = pageBar.getCurrentPage();
                if(pageBar.getTotalPages() <= currentPage) return;

                var nextPage = parseInt(currentPage) + 1; 
                request.url = serviceUrl + "/" + nextPage;
                request.onresult = function() {
                    $.G(gridName).load(this.getNodeValue(dataNodeName), true);

                    var pageInfoNode = this.getNodeValue("PageInfo");
                    $.initGridToolBar(pageBar, pageInfoNode, gotoPage);
                    $.hideWaitingLayer();
                }               
                request.send();
            }
        }
        request.send();
    };

    // 删除选中Grid行
    $.delGridRow = function(url, gridName) {
        if( !confirm("您确定要删除该行记录吗？") ) return;
        
        var grid = $.G(gridName || "grid");
        var objectID = grid.getColumnValue("id");
        if( objectID ) {
            $.ajax({
                url : url + objectID,
                method : "DELETE",
                onsuccess : function() { 
                    grid.deleteSelectedRow();
                }
            }); 
        }
    }

})(tssJS);

/* 树控件 */
;(function($, factory) {

    $.Tree = factory($);

    var TreeCache = {};

    $.T = function(id, data, _default) {
        var tree = TreeCache[id];
        if( tree == null && data == null ) return tree;

        if( tree == null || data ) {
            tree = new $.Tree($1(id), data);
            TreeCache[id] = tree;   

            _default = $.Query.get("_default") || _default;
            if(_default) {
                tree.searchNode( decodeURI(_default) );
                if(tree.getActiveTreeNode()) {
                    setTimeout(function() {
                        var callback = tree.onTreeNodeDoubleClick || tree.onTreeNodeActived;
                        callback && callback();
                    }, 100); 
                }
            }
        }   
        
        return tree;
    };

    $.fn.extend({
        tree: function(data, _default) {
            if(this.length > 0) {
               return $.T(this[0].id, data, _default);
            }
        }
    });

})(tssJS, function($) {

    'use strict';

    var
        _TREE_TYPE = "treeType",
        _TREE_TYPE_SINGLE = "single",
        _TREE_TYPE_MULTI  = "multi",

        _TREE_NODE_MOVEABLE = "moveable",      // 是否可以移动树节点，默认false
        _TREE_NODE_CHECK_SELF = "selectSelf",  // 选中节点时只改变自己的选择状态，与父、子节点无关

    Tree = function(el, data) {
        /*  自定义事件 */
        var eventTreeReady       = new $.EventFirer(this, "onLoad"),
            eventTreeChange      = new $.EventFirer(this, "onChange"),
            eventNodeActived     = new $.EventFirer(this, "onTreeNodeActived"), 
            eventNodeDoubleClick = new $.EventFirer(this, "onTreeNodeDoubleClick"),
            eventNodeRightClick  = new $.EventFirer(this, "onTreeNodeRightClick"),
            eventNodeMoved       = new $.EventFirer(this, "onTreeNodeMoved"),
            eventNodeChecked     = new $.EventFirer(this, "onTreeNodeChecked");

        this.el = el;
        this.treeType  = el.getAttribute(_TREE_TYPE) || _TREE_TYPE_SINGLE;
        this.moveable  = el.getAttribute(_TREE_NODE_MOVEABLE) == "true";
        this.checkSelf = el.getAttribute(_TREE_NODE_CHECK_SELF) == "true";

        this.rootList = [];

        this.init = function() {
            if(data.nodeType) {
                loadXML(data);
            } else {
                loadJson(data);
            }

            $(this.el).html("");

            var ul = $.createElement("ul");
            this.rootList.each(function(i, root){
                var li = root.toHTMLTree();
                ul.appendChild(li);
            });
            this.el.appendChild(ul);

            eventTreeReady.fire(); // 触发载入完成事件
        }

        // 定义Tree私有方法
        var tThis = this;
        var loadXML = function(node) {
            var xmlNodes = node.querySelectorAll("treeNode");
            var parents = {};
            $.each(xmlNodes, function(i, xmlNode) {
                var nodeAttrs = {};
                $.each(xmlNode.attributes, function(j, attr) {
                    nodeAttrs[attr.nodeName] = attr.value;
                });

                var parentId = xmlNode.parentNode.getAttribute(_TREE_NODE_ID);
                var parent = parents[parentId];
                var treeNode = new TreeNode(nodeAttrs, parent);

                if(parent == null) {
                    tThis.rootList.push(treeNode); // 可能存在多个根节点
                }   
                parents[treeNode.id] = treeNode;
            });
        };

        // 借助于stack. [{id:1, name:node1, children:[ {id:3, name:node3, children:[......]} ], xx:xx}, {id:2......}]
        // array.unshift(x), 先进先出；array.push(x), 后进先出
        var loadJson = function(data) {
            var stack = [];
            var parents = {};

            data.each(function(i, nodeAttrs) {
                stack.unshift(nodeAttrs);
            });

            var current;
            while(stack.length > 0) {
                current = stack.pop();

                var treeNode = new TreeNode(current, current.parent); 
                if(current.parent == null) {
                    tThis.rootList.push(treeNode);
                }

                (current.children || []).each(function(i, child) {
                    child.parent = treeNode;
                    stack.unshift(child);
                });
            }
        };

        // 树控件上禁用默认右键和选中文本（默认双击会选中节点文本）
        this.el.oncontextmenu = this.el.onselectstart = function(_event) {
            $.Event.cancel(_event || window.event);
        }       

        /********************************************* 定义树节点TreeNode start *********************************************/
        var 
            _TREE_NODE = "treeNode",
            _TREE_NODE_ID = "id",
            _TREE_NODE_CODE = "code",
            _TREE_NODE_NAME = "name",
            _TREE_ROOT_NODE_ID = "_root",  /* “全部”节点的ID值  */
            _TREE_NODE_STATE = "disabled",       // 停用、启用
            
            /* 
             * 树节点的选择状态：没选(UN_CHECKED)、半选(HALF_CHECKED)、全选(CHECKED)  0/1/2/ 
             * 禁选(CHECKED_DISABLED)状态下，也分没选、半选、全选
             */
            _TREE_NODE_CHECK_STATE = "checkState",  

        clickSwich = function(node) {
            node.opened = !node.opened;

            var styles = ["node_close", "node_open"],
                index = node.opened ? 0 : 1;

            $(node.li.switchIcon).removeClass(styles[index]).addClass(styles[++index % 2]);

            if(node.li.ul) {
                if(node.opened) {
                    $(node.li.ul).removeClass("hidden");
                    var parent = node;
                    while(parent = parent.parent) {
                        $(parent.li.ul).removeClass("hidden");
                        $(parent.li.switchIcon).removeClass(styles[0]).addClass(styles[1]);
                    }
                } 
                else {
                    $(node.li.ul).addClass("hidden");
                }
            }
        },

        /* 根据现有状态改成下一个选择状态，0-->2,  1|2-->0, 同时改变子节点及父节点的check状态 */
        checkNode = function(node, excludeDisabledNode) {
            if( !node.isEnable() && (excludeDisabledNode || true) ) {
                return;
            }

            var oldState = node.checkState;
            switch(oldState) {
                case 0:
                    node.checkState = 2;

                    var parent = node;
                    while(parent = parent.parent) {
                        var oldState = parent.checkState;
                        parent.refreshCheckState(Math.max(oldState, 1));
                    }

                    if(!tThis.checkSelf && node.li.ul) {
                        $("li", node.li.ul).each(function(i, childLi){
                            childLi.node.refreshCheckState(2);
                        });
                    }
                    break;
                case 1:
                case 2:
                    node.checkState = 0;

                    if(!tThis.checkSelf) {
                        $("li", node.li).each(function(i, childLi){
                            childLi.node.refreshCheckState(0);
                        });
 
                        var parent = node;
                        while(parent = parent.parent) {
                            calculateParentState(parent);
                        }
                    }

                    break;
            }

            node.refreshCheckState();
        },

        // 计算父亲节点的checkSate。判断兄弟节点还有没有选中状态的，有则所有父节点一律为半选，无则一律不选
        calculateParentState = function(parent) {
            if(parent == null) return;

            var hasCheckedChilds = false;
            parent.children.each(function(i, child){
                if(child.checkState > 0) {
                    hasCheckedChilds = true;
                }
            });

            parent.refreshCheckState( hasCheckedChilds ? 1 : 0 );
        },

        TreeNode = function(attrs, parent) {            
            this.id   = attrs[_TREE_NODE_ID];
            this.name = attrs[_TREE_NODE_NAME];
            this.code = attrs[_TREE_NODE_CODE];

            this.opened = (attrs._open == "true");
            this.disabled = attrs[_TREE_NODE_STATE] || "0";  // 状态： 停用/启用  1/0
            this.checkState = parseInt(attrs[_TREE_NODE_CHECK_STATE] || "0"); /* 节点的选择状态 */

            this.attrs = attrs;

            // 维护成可双向树查找
            this.children = [];

            this.parent = parent;
            if(this.parent) {
                this.level = this.parent.level + 1;
                this.parent.children.push(this);
            } else {
                this.level = 1;
                this.opened = true; // 默认打开第一层
            }               

            this.toHTMLTree = function() {
                var stack = [];
                stack.push(this);

                var current, currentEl, rootEl, ul;
                while(stack.length > 0) {
                    current = stack.pop();
                    var currentEl = current.toHTMLEl();
                    if(rootEl == null) {
                        rootEl = currentEl;
                    }
                    else {
                        ul = rootEl.querySelector("ul[pID ='" + current.parent.id + "']");
                        ul.pNode = current;
                        ul.insertBefore(currentEl, ul.firstChild);
                    }

                    current.children.each(function(i, child) {
                        stack.push(child);
                    });
                }

                return rootEl;
            };
        };

        TreeNode.prototype = {
            toHTMLEl: function() {
                var li = $.createElement("li");
                li.setAttribute("nodeID", this.id);
                this.code && li.setAttribute("code", this.code);
                li.draggable = tThis.moveable;
                li.node = this;
                this.li = li;

                // 节点打开、关闭开关
                li.switchIcon = $.createElement("span", "switch");
                li.appendChild(li.switchIcon);

                // checkbox
                li.checkbox = $.createElement("span", "checkbox");
                li.appendChild(li.checkbox);

                // 自定义图标
                var selfIcon = $.createElement("div", "selfIcon");
                li.appendChild(selfIcon);
                li.selfIcon = $(selfIcon);

                if(this.attrs["icon"]) {
                    li.selfIcon.css("backgroundImage", "url(" + this.attrs["icon"] + ")");
                    li.selfIcon.addClass = function(cn) {
                        return this; // 如果已经自定义了图标，则忽略后面的folder、leaf等图标设置
                    }
                }

                // 节点名称
                li.a = $.createElement("a");
                $(li.a).html(this.name).title(this.name);
                li.appendChild(li.a);
                if( !this.isEnable() ) {
                    this.disable();
                }

                // 每个节点都可能成为父节点
                li.ul = $.createElement("ul");
                li.ul.setAttribute("pID", this.id);
                li.appendChild(li.ul);

                if(tThis.treeType == _TREE_TYPE_SINGLE) {
                    $(li.checkbox).addClass("hidden");
                }

                if(this.children.length > 0) {                  
                    this.opened = !this.opened;
                    clickSwich(this);

                    li.selfIcon.addClass("folder");
                }
                else { // is leaf
                    $(li.switchIcon).addClass("node_leaf").css("cursor", "default");
                    li.selfIcon.addClass("leaf");
                }

                // 添加事件
                var nThis = this;
                li.a.onclick = function(event) {
                    nThis.active();

                    event.node = nThis;
                    eventNodeActived.fire(event);
                };
                li.a.ondblclick = function(event) {
                    nThis.active();

                    event.node = nThis;
                    eventNodeDoubleClick.fire(event);
                };
                li.a.oncontextmenu = function(event) {
                    nThis.active();

                    // 触发右键激活节点事件
                    var _event = $.Event.createEventObject();
                    _event.treeNode = nThis;
                    _event.clientX = event.clientX;
                    _event.clientY = event.clientY;
                    eventNodeRightClick.fire(_event);
                };

                $(li.switchIcon).click( function() { clickSwich(nThis); } );
                $(li.checkbox).click( function() { checkNode(nThis); } );

                // 添加拖到事件处理
                $(li).addEvent("dragstart", function(ev){
                    var dt = ev.dataTransfer;
                    dt.effectAllowed = 'move';
                    dt.setData("text", li.node.id);
                }, true);        

                $(li).addEvent("dragend", function(ev) {
                    ev.dataTransfer.clearData("text");
                    $.Event.cancel(ev); // 不执行默认处理，拒绝被拖放
                }, true);


                $(li).addEvent("drop", function(ev){
                    var dt = ev.dataTransfer;
                    var nodeId = dt.getData("text");
                    var dragEL = $("li[nodeId='" + nodeId + "']")[0];

                    // 平级拖动，用以排序.暂不支持跨级拖动（根节点不允许拖动）
                    if( this.node.parent == dragEL.node.parent && this.node.id != '_root' ) {
                        // 触发自定义事件
                        var eObj = $.Event.createEventObject();
                        eObj.dragNode = dragEL.node;
                        eObj.destNode = this.node;
                        eObj.ownTree  = tThis;
                        eventNodeMoved.fire(eObj); 
                    }                   

                    $.Event.cancel(ev);
                }, true);

                $(li).addEvent("dragover", function(ev) {
                    $.Event.cancel(ev);
                }, true);

                return li;
            },
            
            disable: function() {
                this.disabled = "1";
                $(this.li.a).addClass("disable");
                this.li.node.refreshCheckState();
            },

            isEnable: function() {
                return this.disabled != "1";
            },

            active: function() {
                $.each(tThis.el.querySelectorAll("li"), function(i, li) {
                    $(li.a).removeClass("active");
                });

                $(this.li.a).addClass("active");
            },

            inactive: function() {
                $(this.li.a).removeClass("active");
            },

            openNode: function() {
                clickSwich(this);
            },

            refreshCheckState: function(newState) {
                this.checkState = newState != null ? newState : this.checkState;
                $(this.li.checkbox).removeClass("checkstate_0_" + this.disabled)
                    .removeClass("checkstate_1_" + this.disabled)
                    .removeClass("checkstate_2_" + this.disabled)
                    .addClass("checkstate_" + this.checkState + "_" + this.disabled);

                var ev = window.event || {};
                ev.node = this;
                ev.checkState = this.checkState;
                eventNodeChecked.fire(ev);
            },

            getAttribute: function(name) {
                return this.attrs[name];
            },

            setAttribute: function(name, value) {
                if(value) {
                    this.attrs[name] = value;
                } else {
                    delete this.attrs[name];
                }
            }
        };
        /********************************************* 定义树节点TreeNode end *********************************************/

        tThis.init();
        tThis.searcher = new Searcher(tThis);

        tThis.checkNode = checkNode;
        tThis.TreeNode = TreeNode;
    };

    Tree.prototype = {

        getTreeNodeById: function(id) {
            var li = this.el.querySelector("li[nodeId='" + id + "']");
            return li ? li.node : null;
        },

        /* 获取当前高亮（激活）的节点（被激活的节点一次只有一个）。如没有，则返回null。*/
        getActiveTreeNode: function() {
            var lis = this.el.querySelectorAll("li[nodeId]");
            var activeNode;
            $.each(lis, function(i, li) {
                if( $(li.a).hasClass("active") ) {
                    activeNode = li.node;
                }
            });

            return activeNode
        },

        getActiveTreeNodeId: function() {
            var activeNode = this.getActiveTreeNode();
             return activeNode ? activeNode.id : "";
        }, 

        getActiveTreeNodeName: function() {
            var activeNode = this.getActiveTreeNode();
            return activeNode ? activeNode.name : "";
        }, 

        getActiveTreeNodeAttr: function(key) {
            var activeNode = this.getActiveTreeNode();
            return activeNode ? activeNode.attrs[key] : "";
        }, 

        // 如果找不到指定的treeNode，或id为空，则清空原active节点
        setActiveTreeNode: function(idOrnode) {
            var activeNode = this.getActiveTreeNode();
            if(activeNode) {
                activeNode.inactive();
            }
            if(!idOrnode) return;

            var treeNode = idOrnode.active ? idOrnode : this.getTreeNodeById(idOrnode);
            if(treeNode) {
                treeNode.active();
                this.scrollTo(treeNode);
            }
        },

        // 让新增节点出现在可视区域内。
        addTreeNode: function(newNode, parent) {
            parent = parent || this.getActiveTreeNode();

            if(newNode.nodeType) { // xml
                var nodeAttrs = {};
                $.each(newNode.attributes, function(j, attr) {
                    nodeAttrs[attr.nodeName] = attr.value;
                });

                newNode = nodeAttrs;
            }

            var treeNode = new this.TreeNode(newNode, parent);
            if( $("li", parent.li.ul).length == 0 ) {
                $(parent.li.switchIcon).removeClasses("node_leaf,node_close").addClass("node_open");
                parent.li.selfIcon.removeClass("leaf").addClass("folder");
            }

            parent.li.ul.appendChild(treeNode.toHTMLEl());

            this.scrollTo(treeNode);
        },

        // 删除li, 并从其parent.children中去除
        removeTreeNode: function(treeNode, retainEl) {
            retainEl = retainEl || false;
            if( !retainEl ) {
                $.removeNode(treeNode.li);
            }

            var parent = treeNode.parent;
            parent.children.remove(treeNode);
            if(parent.children.length == 0) {
                $(parent.li.switchIcon).removeClasses("node_open,node_close").addClass("node_leaf");
                parent.li.selfIcon.removeClass("folder").addClass("leaf");
            }
        },
        // 删除当前选中的节点
        removeActiveNode: function() {
            this.removeTreeNode(this.getActiveTreeNode());
        },

        /*
         * 移动节点位置。
         * 参数：  from    移动节点TreeNode对象
         *          to      目标节点TreeNode对象
         */
        moveTreeNode: function(from, to) {
            var parent = to;
            while(parent) {
                if(parent == from) {
                    return $.alert("不能向自己的内部节点移动。"); // 不能移动到子节点里
                }
                parent = parent.parent;
            }

            this.removeTreeNode(from); // 将from从其原parent.children里剔除

            from.parent = to;
            to.children.push(from);

            to.li.ul.appendChild(from.li);
        },

        sortTreeNode: function(dragNode, destNode) {
            destNode.li.parentNode.insertBefore(dragNode.li, destNode.li);
        },

        searchNode: function(searchStr) {
            this.searcher.search(searchStr)
        },

        /* 将节点滚动到可视范围之内 */
        scrollTo: function(treeNode) {
            var temp = treeNode;
            while(temp = temp.parent) {
                if(!temp.opened) {
                    temp.openNode();
                }
            }

            this.el.scrollTop = treeNode.li.offsetTop - this.el.clientHeight / 2;
        },

        getCheckedIds: function(includeHalfChecked, idName) {
            var checkedNodes = this.getCheckedNodes(includeHalfChecked);
            var checkedNodeIds = [];
            checkedNodes.each(function(i, node){
                checkedNodeIds.push(node[idName || "id"]);
            });

            return checkedNodeIds;
        },

        getCheckedNodes: function(includeHalfChecked) {
            var lis = this.el.querySelectorAll("li[nodeId]");
            var checkedNodes = [];
            $.each(lis, function(i, li) {
                if( $(li.checkbox).hasClass("checkstate_2_0") || $(li.checkbox).hasClass("checkstate_2_1") ) {
                    checkedNodes.push(li.node);
                }

                if(includeHalfChecked) {
                    if( $(li.checkbox).hasClass("checkstate_1_0") || $(li.checkbox).hasClass("checkstate_1_1") ) {
                        checkedNodes.push(li.node);
                    }
                }
            });

            return checkedNodes;
        },

        setCheckValues: function(checkedIds, clearOld) {
            var checkedNodes = this.getCheckedNodes(true);
            checkedNodes.each(function(i, node){
                node.refreshCheckState(0);
            });

            if( !checkedIds ) return;

            checkedIds = $.isArray(checkedIds) ? checkedIds : checkedIds.split(',');
            for(var i = 0; i < checkedIds.length; i++) {
                var li = this.el.querySelector("li[nodeId='" + checkedIds[i] + "']");
                if(li) {
                    this.checkNode(li.node, false);
                }
            } 
        },

        getAllNodes: function() {
            var lis = this.el.querySelectorAll("li[nodeId]");
            var nodes = [];
            $.each(lis, function(i, li) {
                if(li.node.id != "_root") {
                     nodes.push(li.node);
                }
            });
            return nodes;
        },

        getAllNodeIds: function() {
            var nodes = this.getAllNodes();
            var nodeIds = [];
            nodes.each(function(i, node){
                nodeIds.push(node.id);
            });
            return nodeIds;
        }
    };

    /********************************************* 定义树查找对象 start *********************************************/
    var Searcher = function(tree) {
        var findedNodes, currentIndex, lastSearchStr;

        this.search = function(searchStr, showTip) {
            if($.isNullOrEmpty(searchStr)) {
                return $.alert("查询条件不能为空！");
            }

            if(lastSearchStr == searchStr) {
                this.next(showTip);
                return; 
            }

            findedNodes = [];
            currentIndex = -1;
            lastSearchStr = searchStr;

            if( searchStr.charAt(0) == "@" ) { // 按ID来搜索
                var liNode = tree.getTreeNodeById( searchStr.replace("@", "") );
                liNode && findedNodes.push( liNode );
            }
            else if( searchStr.charAt(0) == "#" ) { // 按ID来搜索
                var code = searchStr.replace("#", "");
                var list = tree.el.querySelectorAll("li[code*='" + code + "']");
                $.each(list, function(i, li) {
                    findedNodes.push(li.node);
                });
            }
            else {
                var aNodeList = tree.el.querySelectorAll("li>a[title*='" + searchStr + "']");
                $.each(aNodeList, function(i, aNode) {
                    findedNodes.push(aNode.parentNode.node);
                });
            }           

            this.next(showTip);
        }

        this.next = function(showTip) {
            var node;
            if(findedNodes.length) {
                node = findedNodes[++ currentIndex % findedNodes.length];
            }
            else {
                showTip && $.alert("没有找到名称含有【" + lastSearchStr + "】的树节点。");
            }
            tree.setActiveTreeNode(node);
        }
    }

    return Tree;
});

/* 布局组件 */
;(function ($, factory) {

    $.WorkSpace = factory($);

})(tssJS, function($) {

    'use strict';

    var 
    /* 样式名 */
    CSS_CLASS_TAB_BOX_HAS_TAB = "hasTab",
    CSS_CLASS_TAB_ACTIVE      = "active",
    CSS_CLASS_PHASE_ACTIVE    = "active",

    /* 点击Phase到展现内容的时间差(ms) */
    _TIMEOUT_PHASE_CLICK = 100,
    phaseClickTimeout,

    /* 自定义标签名（不含命名空间） */
    WS_NAMESPACE     = "WorkSpace",
    WS_TAG_PAGE      = "Page",
    WS_TAG_TAB       = "Tab",
    WS_TAG_TAB_BOX   = "TabBox",
    WS_TAG_PHASE     = "Phase",
    WS_TAG_PHASE_BOX = "PhaseBox",
    WS_TAG_ICON      = "Icon",
 
    /*******  Page: 管理单个子页面的显示、隐藏等控制 *********/
    Page = function (obj) { 
        this.el = obj;
        this.id = obj.id;       
        this.hide(); 
    };

    Page.prototype = {
        /* Page隐藏  */
        hide: function() {
            this.el.style.display = "none"; 
            this.isActive = false;
        },

        /* Page显示 */
        show: function() {
            this.el.style.display = "block";
            this.el.scrollTop  = 0;
            this.el.scrollLeft = 0;
            this.isActive = true;
        }
    };
 
    /*******  Tab: 负责生成水平标签页 *********/
    var Tab = function(label, phasesParams, callback, workspace) {
        this.ws = workspace;

        this.isActive = false;
        this.callback = callback;
        this.link;
        
        this.phases = {};
        this.phasesParams = phasesParams;  
        
        this.el = $.createNSElement(WS_TAG_TAB, WS_NAMESPACE);
        this.id = this.el.uniqueID;
         
        var closeIcon = $.createNSElement(WS_TAG_ICON, WS_NAMESPACE);
        closeIcon.title = "关闭";     
        this.el.appendChild(closeIcon);
        
        var div = $.createElement("div");
        $(div).html(label).title(label);
        div.noWrap = true; // 不换行
        this.el.appendChild(div);
        
        var oThis = this;
        closeIcon.onclick = this.el.ondblclick = function() {
            oThis.close();
        };  
        this.el.onclick = function() {
            if (!oThis.isActive && oThis.el) {
                oThis.click();
            }       
        };  
    };

    Tab.prototype = {

        close: function() {

            // 如果关闭的是当前激活的Tab，则需要在关闭完成后切换到第一个Tab
            var isCloseActiveTab = (this == this.ws.getActiveTab()); 
            if( isCloseActiveTab ) {
                this.clearPhases();
                if( this.link ) {
                    this.hideLink();
                }
            }

            delete this.ws.tabs[this.id];

            $.removeNode(this.el);

            this.el = this.id = this.link = null;
            this.phases = {};
            this.phasesParams = null;

            // 执行Tab页上定义的回调方法
            this.execCallBack("onTabClose", this.SID);

            if( this.ws.noTabOpend() ) {
                $(this.ws.element).hide();
            } 
            else if( isCloseActiveTab ) {
                this.ws.switchToTab(this.ws.getFirstTab());
            }
        },

        /* 点击标签 */
        click: function() {
            this.ws.inactiveAllTabs();
            this.active();

            // 执行Tab页上定义的回调方法
            this.execCallBack("onTabChange");

            if( this.link ) {
                this.showLink();
                this.refreshPhases();
            }
        },

        /* 显示关联子页面  */
        showLink: function() {
            this.ws.showPage(this.link);
        },

        /* 关闭（隐藏）关联子页面 */
        hideLink: function() {
            this.link.hide();
        },

        /* 高亮标签 */
        active: function() {
            $(this.el).addClass(CSS_CLASS_TAB_ACTIVE);
            this.isActive = true;
        },

        /* 低亮标签  */
        inactive: function() {
            $(this.el).removeClass(CSS_CLASS_TAB_ACTIVE);
            this.isActive = false;
        },

        /* 将标签与Page对象关联 */
        linkTo: function(page) {
            this.link = page;
        },

        /* 将标签插入指定容器 */
        dockTo: function(container) {
            container.appendChild(this.el);
        },
 
        /* 切换到指定Tab页 */
        switchToPhase: function(phase) {
            if( phase ) {
                phase.click();
            }       
        },

        /* 刷新纵向标签  */
        refreshPhases: function() {
            this.clearPhases();
            
            if( this.phasesParams == null ) return;

            // 重新创建纵向标签
            for(var i=0; i < this.phasesParams.length; i++) {
                var param  = this.phasesParams[i];
                var pageId = param.page;
                var page   = this.ws.pages[pageId];

                var phase  = new Phase(param.label, this.ws);
                phase.linkTo(page);
                phase.dockTo(this.ws.phaseBox);
                if(pageId == this.link.id) {
                    phase.active();
                }

                this.phases[phase.id] = phase;
            }

            this.ws.phaseBox.style.display = "inline";  /* 显示右侧容器 */
        },

        /* 清除纵向标签  */
        clearPhases: function() {
            for(var item in this.phases) {
                var phase = this.phases[item];
                phase.dispose();
            }
            this.ws.phaseBox.innerHTML = "";
        },

        /* 低亮所有Phase标签 */
        inactiveAllPhases: function() {
            $.each(this.phases, function(name, phase) {
                phase.inactive();
            });
        },

        /* 获取激活的纵向标签 */
        getActivePhase: function() {
            for(var item in this.phases) {
                var curPhase = this.phases[item];
                if( curPhase.isActive ) {
                    return curPhase;
                }
            }
        },

        /* 激活上一个纵向标签  */
        prevPhase: function() {       
            var phaseArray = [], activePhaseIndex;

            $.each(this.phases, function(name, phase) {
                if( phase.isActive ) {
                    activePhaseIndex = phaseArray.length;
                }
                phaseArray[phaseArray.length] = phase;
            });
            
            // activePhaseIndex == 0 表示当前激活的是第一个Phase，即到顶了，则不再往上
            if( activePhaseIndex === 0) { 
                return;
            }
            this.switchToPhase(phaseArray[--activePhaseIndex]);
        },

        /* 激活下一个纵向标签 */
        nextPhase: function() {
            var phaseArray = [], activePhaseIndex = 0;
            $.each(this.phases, function(name, phase) {
                if( phase.isActive ) {
                    activePhaseIndex = phaseArray.length;
                }
                phaseArray[phaseArray.length] = phase;
            });

            // activePhaseIndex == phaseArray.length - 1 表示当前激活的是最后一个Phase，即到末尾了
            if(activePhaseIndex === phaseArray.length - 1) { 
                return;
            }
            this.switchToPhase(phaseArray[++activePhaseIndex]);
        },

        /*
         *  执行回调函数
         *  参数：  string:eventName        事件名称
                    object:params           回调函数可用参数
         */
        execCallBack: function(eventName, params) {
            if( this.callback ) {
                $.execCommand(this.callback[eventName], params);
            }
        }
    };

    /* **************** Phase ：负责生成右侧纵向标签页 ***********************/
    var Phase = function(label, workspace) {
        this.ws = workspace;

        this.link;
        this.isActive = false;
        
        this.el = $.createNSElement(WS_TAG_PHASE, WS_NAMESPACE);
        this.id = this.el.uniqueID;
        
        var div = $.createElement("div");
        $(div).html(label).title(label);
        div.noWrap = true;
        
        this.el.appendChild(div);       
        
        var oThis = this;
        this.el.onclick = function() {
            if (!oThis.isActive) {
                oThis.click();
            }       
        };  
    }

    Phase.prototype = {
        /* 将标签与Page对象关联  */
        linkTo: function(pageInstance) {
            this.link = pageInstance;
        },

        /* 将标签插入指定容器 */
        dockTo: function(container) {
            container.appendChild(this.el);
        },

        /* 释放纵向标签实例 */
        dispose: function() {
            var curActiveTab = this.ws.getActiveTab();
            delete curActiveTab.phases[this.id];
            $.removeNode(this.el);
            this.el = this.id = this.link = null;
        },

        /* 点击标签  */
        click: function() {
            var activeTab = this.ws.getActiveTab();
            activeTab.inactiveAllPhases();

            this.active();
            this.scrollToView();

            var thisPhase = this;

            // 避免切换太快时显示内容跟不上响应
            clearTimeout( phaseClickTimeout );
            
            phaseClickTimeout = setTimeout( function() {
                /* 显示关联子页面 */
                if( thisPhase.link ) {
                    this.ws.showPage(thisPhase.link);
                    activeTab.linkTo(thisPhase.link);
                }
            }, _TIMEOUT_PHASE_CLICK );
        },

        /* 将控制标签显示在可见区域内 */
        scrollToView: function() {
            var tempTop = this.el.offsetTop;
            var tempBottom = this.el.offsetTop + this.el.offsetHeight;
            var areaTop = this.ws.phaseBox.scrollTop;
            var areaBottom = areaTop + this.ws.phaseBox.offsetHeight;
            if(tempTop < areaTop) {
                this.ws.phaseBox.scrollTop = tempTop;
            }
            else if(tempBottom > areaBottom) {
                this.ws.phaseBox.scrollTop = tempBottom - this.ws.phaseBox.offsetHeight;
            }
        },

        /* 高亮纵向标签 */
        active: function() {
            $(this.el).addClass(CSS_CLASS_PHASE_ACTIVE);
            this.isActive = true;
        },

        /* 低亮纵向标签 */
        inactive: function() {
            $(this.el).removeClass(CSS_CLASS_PHASE_ACTIVE);
            this.isActive = false;
        }
    }

    /* ***********************************************************************************************
       控件名称：标签式工作区
       功能说明：1、动态创建Tab标签
                 2、动态创建纵向Tab标签
                 3、Tab标签控制子页面显示
                 4、双击Tab标签可关闭 
    * ***********************************************************************************************/
    var WorkSpace = function(ws) {
        this.element = ws.nodeType ? ws : $("#" + ws)[0];
 
        this.tabs  = {};
        this.pages = {};

        /* 初始子页面 */
        var childs = $.getNSElements(this.element, WS_TAG_PAGE, WS_NAMESPACE);
        for(var i=0; i < childs.length; i++) {
            var curNode = childs[i]; 
            this.pages[curNode.id] = new Page(curNode);
        }

        this.createUI();
    };

    WorkSpace.prototype = {

        /* 创建界面展示 */
        createUI: function() {
            /* 创建Tab标签的容器 */
            this.tabBox = $.createNSElement(WS_TAG_TAB_BOX, WS_NAMESPACE);
            this.element.appendChild(this.tabBox);
            
            var refChild = this.element.firstChild;
            if(refChild != this.tabBox) {
                this.element.insertBefore(this.tabBox, refChild); // 插入到第一个
            }

            /* 创建纵向Tab标签的容器 */
            this.phaseBox = $.createNSElement(WS_TAG_PHASE_BOX, WS_NAMESPACE);
            this.element.appendChild(this.phaseBox);

            var refChild = this.element.childNodes[1];
            if(this.phaseBox != refChild && refChild) {
                this.element.insertBefore(this.phaseBox, refChild);
            }

            // 隐藏右侧容器
            this.phaseBox.style.display = "none"; 
        },
         
        /* 打开子页面  */
        open: function(inf) {
            this.element.style.display = "";

            var tab;
            for(var item in this.tabs) {
                if(inf.SID == this.tabs[item].SID) {
                    tab = this.tabs[item];
                }
            }
            
            // 不存在同一数据源tab则新建
            if(null == tab) {             
                tab = new Tab(inf.label, inf.phases, inf.callback, this);
                tab.SID = inf.SID; // 标记
                this.tabs[tab.id] = tab;

                var page = this.pages[inf.defaultPage];
                tab.linkTo(page);
                tab.dockTo(this.tabBox);

                $(this.tabBox).addClass(CSS_CLASS_TAB_BOX_HAS_TAB);
            }
            
            tab.click();
            return tab;
        },

        showPage: function(page) {
            /* 先隐藏所有子页面  */
            for(var item in this.pages) {
                var curPage = this.pages[item];
                if(curPage.isActive) {
                    curPage.hide();
                }
            }

            page.show();
        },

        /* 获得第一个Tab */
        getFirstTab: function() {
            for(var item in this.tabs) {
                return this.tabs[item];
            }
        },

        /* 切换到指定Tab页 */
        switchToTab: function(tab) {
            if( tab ) {
                tab.click();
            } 
            else {
                $(this.tabBox).removeClass(CSS_CLASS_TAB_BOX_HAS_TAB);
            }
        },

        /* 获取当前激活标签 */
        getActiveTab: function() {
            for(var item in this.tabs) {
                var tab = this.tabs[item];
                if( tab.isActive ) {
                    return tab;
                }
            }
        },

        getTab: function(sid) {
            for(var item in this.tabs) {
                var tab = this.tabs[item];
                if( tab.SID === sid ) {
                    return tab;
                }
            }
        },

        closeTab: function(sid) {
            var tab = this.getTab(sid);
            if(tab) {
                tab.close();
            }
        },

        /* 关闭当前激活标签 */
        closeActiveTab: function() {
            var tab = this.getActiveTab();
            if( tab ) {
                tab.close();
            }
        },

        closeAllTabs: function() {
            for(var item in this.tabs) {
                var curTab = this.tabs[item];
                curTab.close();
            }
        },

        /* 低亮所有标签 */
        inactiveAllTabs: function() {
            for(var item in this.tabs) {
                var curTab = this.tabs[item];
                curTab.inactive();
            }
        },
         
        /* 激活上一个Phase标签 */
        prevPhase: function() {
            var tab = this.getActiveTab();
            if( tab ) {
                return tab.prevPhase();
            }
        },

        /* 激活下一个Phase标签 */
        nextPhase: function() {
            var tab = this.getActiveTab();
            if( tab ) {
                return tab.nextPhase();
            }
        },

        switchToPhase: function(pageId) {
            var page = this.pages[pageId];
            this.showPage(page);
        },

        noTabOpend: function() {
            var length = 0; 
            for(var item in this.tabs) {
                length ++;
            }
            return length == 0;
        }
    };

    return WorkSpace;
}); 

/* 
 * panel 
 */
;(function($){

    $.openIframePanel = function(panelId, title, width, height, src, hideMaxMin) {
        var $panel = $("#" + panelId);
        if( !$panel.length ) {
            var panel = $.createElement("div", "panel", panelId);
            document.body.appendChild(panel);
            
            $panel = $(panel);
            $panel.css("width", width + "px").css("height", height + "px").center().css("position", "fixed");
            $panel.panel(title, '<iframe frameborder="0"></iframe>', false);

            if(hideMaxMin) {
                $panel.find(".max").hide();
                $panel.find(".min").hide();
            }       
        }

        $panel.find(".title>h2").html(title);
        $panel.find("iframe").attr("src", src);
        $panel.show();
    }

    $.fn.extend({
        panel: function(title, contentHtml, resizable, moveable) {
            if(this.length > 0) {
                return new Panel(this[0], title, contentHtml, resizable, moveable);
            }
        }
    });

    var panelIndex = 1;

    var Panel = function(el, title, contentHtml, resizable, moveable) {
        this.el = el;
        this.$el = $(el);
        this.$el.html(
            '<div class="title">' + 
                '<h2> - ' +title+ '</h2>' + 
                '<div>' + 
                    '<span class="min" title="最小化"></span>' + 
                    '<span class="max" title="最大化"></span>' + 
                    '<span class="revert" title="还原"></span>' + 
                    '<span class="close" title="关闭"></span>' + 
                '</div>' + 
            '</div>' + 
            '<div class="content">' + contentHtml + '</div>'
        );
        this.$el.addClass("tss-panel").css("zIndex", panelIndex++);

        if(moveable == null || moveable) {
            this.$el.drag();
        }
        if(resizable) {
            this.$el.resize().resize("col").resize("row");
        }

        this.width  = el.clientWidth;
        this.height = el.clientHeight;
        this.location = $.absPosition(el);

        var $title = $(".title", el);
        var $content = $(".content", el);

        var $min = $(".min", el);
        var $max = $(".max", el);
        var $revert = $(".revert", el);
        var $close = $(".close", el);

        var oThis = this;

        this.$el.click(function() {
            oThis.$el.css("zIndex", panelIndex++);
        });

        $max.click(function(){
            var inner = $.getInner();
            oThis.$el.css('width',  "100%")
                .css('height', "100%")
                .css('top', '0px').css('left', '0px');
            oThis.$el.addClass('tss-panel-max').removeClass("tss-panel-min");

            $max.hide();
            $min.hide();
            $revert.show(true);
            $content.show();
        });

        $revert.click(function(){
            var inner = $.getInner();
            oThis.$el.removeClass('tss-panel-max')
                .css('width', oThis.width + "px")
                .css('height', oThis.height + "px")
                .css('left', oThis.location.left + 'px').css('top', oThis.location.top + 'px');
            $max.show(true);
            $min.show(true);
            $revert.hide();
        });

        $min.click(function(){
            if(oThis.$el.hasClass("tss-panel-min")) {
                oThis.$el.removeClass("tss-panel-min").css('height', oThis.height + "px");
                $content.show();
            } 
            else {
                oThis.$el.addClass("tss-panel-min").removeClass('tss-panel-max')
                    .css('width', oThis.width + "px")
                    .css('height', "");
                $max.show(true);
                $revert.hide();
                $content.hide();
            }
        });

        $close.click(function(){
            oThis.$el.hide();
        });

        function cancelBubble(ev) {
            this.onfocus = function () {this.blur()};
            $.Event.cancel(ev)
        }
        $min.addEvent("mousedown", cancelBubble);
        $max.addEvent("mousedown", cancelBubble);
        $revert.addEvent("mousedown", cancelBubble);
        $close.addEvent("mousedown", cancelBubble);
    }

})(tssJS);

/*
 *  左栏
 */
;(function($) {
 
    $.leftbar = function(fn1, fn2) {

        var t,
            closeLeftbar = function() {
                $(".leftbar").removeClass("leftbar-open").addClass("leftbar-hidden");
                clearTimeout( t );
            },
            showLeftbar = function() {
                $(".leftbar").removeClass("leftbar-hidden").addClass("leftbar-open");
                t = setTimeout( closeLeftbar, 3000);
            };

        var barWidth = 30, f = 0;

        this.init = function() {
            this.createInDomTree();

            $(".leftbar-menu").toggle(fn1, fn2 || fn1);
            $(document).addEvent("mousemove", this.onMouseMove);
        };

        this.createInDomTree = function() {
            var el = $.createElement("div", "leftbar");
            $(el).addClass("leftbar-hidden");
            $(el).html('<div class="leftbar-menu"><span>&nbsp;</span></div>');

            document.body.appendChild(el);
        };
        
        this.onMouseMove = function(ev) {
            ev.stopImmediatePropagation();

            var _x = ev.clientX > 0 ? ev.clientX : 1, 
                i = parseInt(100 / parseInt(document.body.clientWidth / _x, 10), 10), 
                s = !1;

            if (Math.abs(i - f) < 10)
                return;

            $(".leftbar").hasClass("leftbar-open") && _x > barWidth && closeLeftbar();

            if (i >= 10 && i <= 50) {
                var o = barWidth * i * 12 / 100;
                f > i ? s = barWidth - o : s = -o;
                s = Math.ceil(s);
                s > 0 && (s = 0)
            } 
            else {
                i > 50 ? s = -parseInt(barWidth, 10) : i >= 0 && i < 10 && (s = 0);
            }
            
            if (s === 0) {
                showLeftbar();
            }
            f = i;
        };

        this.init();
    }

})(tssJS);


/*
 *  drag / resize / fullscreen
 */
tssJS(function() {
    // 钩子
    $(".moveable").each(function(i, el){
        $(el).drag();
    });

    $(".resizable").each(function(i, el){
        $(el).resize().resize("col").resize("row");
    });

    $(".fullscreenable").each(function(i, el){
        $(el).fullscreen();
    });
});

tssJS.fn.extend({
    drag: function(handle) {
        var element = this[0];
        handle = handle || $("h1", element)[0] || $("h2", element)[0] || element; // 拖动条
        if(handle == null) return;

        var mouseStart  = {x:0, y:0};  // 鼠标起始位置
        var elementStart = {x:0, y:0};  // 拖动条起始位置

        handle.onmousedown = function(ev) {
            var oEvent = ev || window.event;
            mouseStart.x  = oEvent.clientX;
            mouseStart.y  = oEvent.clientY;
            elementStart.x = element.offsetLeft;
            elementStart.y = element.offsetTop;

            if (handle.setCapture) {
                handle.onmousemove = doDrag;
                handle.onmouseup = stopDrag;
                handle.setCapture();
            } else {
                document.addEventListener("mousemove", doDrag, true);
                document.addEventListener("mouseup", stopDrag, true);
            }

            return false;
        };

        function doDrag(ev) {
            ev = ev || window.event;

            var x = ev.clientX - mouseStart.x + elementStart.x;
            var y = ev.clientY - mouseStart.y + elementStart.y;

            element.style.left = x + "px";
            element.style.top  = y + "px";
        };

        function stopDrag() {
            if (handle.releaseCapture) {
                handle.onmousemove = null;
                handle.onmouseup = null;
                handle.releaseCapture();
            } else {
                document.removeEventListener("mousemove", doDrag, true);
                document.removeEventListener("mouseup", stopDrag, true);
            }
        };

        return this;
    },

    resize: function(type) {
        var element = this[0];

        var handle = document.createElement("DIV"); // 拖动条
        var cssText = "position:absolute;overflow:hidden;z-index:3;";
        if (type == "col") {
            handle.style.cssText = cssText + "cursor:col-resize;top:0px;right:0px;width:3px;height:100%;";
        } else if(type == "row") {
            handle.style.cssText = cssText + "cursor:row-resize;left:0px;bottom:0px;width:100%;height:3px;";
        } else {
            handle.style.cssText = cssText + "cursor:nw-resize;right:0px;bottom:0px;width:8px;height:8px;background:#99CC00";
        }
        
        element.appendChild(handle);

        var mouseStart  = {x:0, y:0};  // 鼠标起始位置
        var handleStart = {x:0, y:0};  // 拖动条起始位置

        handle.onmousedown = function(ev) {
            var oEvent = ev || window.event;
            mouseStart.x  = oEvent.clientX;
            mouseStart.y  = oEvent.clientY;
            handleStart.x = handle.offsetLeft;
            handleStart.y = handle.offsetTop;

            document.addEventListener("mousemove", doDrag, true);
            document.addEventListener("mouseup", stopDrag, true);

            $.showWaitingLayer(); // 创建一个遮罩层，防止存在iframe时捕捉不到mousemove事件

            return false;
        };

        function doDrag(ev) {
            var oEvent = ev || window.event;

            // 水平移动距离
            if (type == "col" || type == null) {
                var _width = oEvent.clientX - mouseStart.x + handleStart.x + handle.offsetWidth;
                if (_width < handle.offsetWidth) {
                    _width = handle.offsetWidth;
                } 
                else if (_width > document.documentElement.clientWidth - element.offsetLeft) {
                    _width = document.documentElement.clientWidth - element.offsetLeft - 2; // 防止拖出窗体外
                }
                element.style.width = _width + "px";
            }

            // 垂直移动距离
            if (type == "row" || type == null) {
                var _height = oEvent.clientY - mouseStart.y + handleStart.y + handle.offsetHeight;
                if (_height < handle.offsetHeight) {
                    _height = handle.offsetHeight;
                } 
                else if (_height > document.documentElement.clientHeight - element.offsetTop) {
                    _height = document.documentElement.clientHeight - element.offsetTop - 2; // 防止拖出窗体外
                }
                element.style.height = _height + "px";
            }
        };

        function stopDrag() {
            document.removeEventListener("mousemove", doDrag, true);
            document.removeEventListener("mouseup", stopDrag, true);
            $.hideWaitingLayer();
        };

        return this;
    },

    /* Fullscreen text editor plugin */
    fullscreen: function() {
        var isFullscreen = false, 
            $el = this, el = this[0], $ttDiv, $icon;

        if ( !$el.is('textarea') ) {
            return $el.notice('It can only work on textarea element.');
        }

        var tt = $.createElement("div", "tss-textarea");
        el.parentNode.insertBefore(tt, el);
        $(tt).html('<div style="height:100%;"><a href="#"></a></div>');
        $ttDiv = $(tt).find('div');
        $icon  = $ttDiv.find('div>a');
        $ttDiv.appendChild(el);

        // 设置tt的高、宽为原始textarea的高、宽
        $(tt).width(el.offsetWidth).height(el.offsetHeight);
        $el.width('100%').height('100%').css('resize', 'none');

        var expand = function() {
                $ttDiv.addClass('expanded');
                $el.focus();
                isFullscreen = true;
            },
            minimize = function() {
                $ttDiv.removeClass('expanded'); 
                isFullscreen = false;
            };

        $.Event.addEvent(window, "keyup", function(e) {
            if (e.keyCode == 27) {
                isFullscreen && minimize();
            }
        });

        $icon.click(function(ev) {
            $.Event.cancel(ev);
            isFullscreen ? minimize(): expand();
        });
    }
});
