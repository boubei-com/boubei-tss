
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

})(tssJS);