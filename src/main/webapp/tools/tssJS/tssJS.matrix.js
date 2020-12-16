;(function($, factory) {

    $.Matrix = factory($);

    var _matrix_cache = {};

    $.MT = function(id, data) {
        var matrix = _matrix_cache[id];
        if( matrix == null && data == null ) return null;

        if( matrix == null || data ) {
            matrix = new $.Matrix($1(id), data);
            _matrix_cache[id] = matrix;   
        }
        
        return matrix;
    }

})(tssJS, function($) {

    'use strict';

    var _Option = function(node) {
        this.id   = node.id || $("id", node).text();
        this.name = node.name || $("name", node).text();
    },

    /*
     * 设定选中状态
     *           0 未选中
     *           1 选中
     */
    setOptionCheckState = function(optionChecker, nextState) {
        // 单选，将横向其它选项全部设置为未选中
        $.each(optionChecker.node._options, function(_optionId, _option){
            var $dependOptionChecker = $("span[oid='" + _option.id + "']", optionChecker.parentNode);
            $dependOptionChecker.css("backgroundImage", "url(images/optionState0.gif)");
            $dependOptionChecker[0].state = optionChecker.node.attrs[_optionId] = "0";
        });

        optionChecker.state = nextState;
        optionChecker.node.attrs[optionChecker.option.id] = nextState;
        $(optionChecker).css("backgroundImage", "url(images/optionState" + (nextState || "0") + ".gif)");
    },

    /*
     * 改变权限项选中状态为下一状态
     *       选中状态：1选中 / 0未选
     *
     * 参数：  optionChecker               权限项
               boolean: shiftKey           是否同时按下shiftKey
     */
    changeOptionCheckState = function(ev, optionChecker, shiftKey) {
        var curState = optionChecker.state, nextState;
        switch(curState || "0") {            
            case "0":
                nextState = "1";
                break;
            case "1":
                nextState = "0";
                break;
        }        

        // 修改权限项显示图标
        setOptionCheckState(optionChecker, nextState);

        var treeNode = optionChecker.node;
        var option = optionChecker.option;
        var optionId = option.id;
 
        // 同时按下shift键时
        if( shiftKey ) {
            $("li[nodeId]", treeNode.li).each(function(i, li){
                var optionChecker = $(".optionBox span[oid='" + optionId + "']", li)[0];
                setOptionCheckState(optionChecker, nextState);
            });
        }
    },

    Matrix = function(el, data) {
        this.el = el;
        this.rootList = [];
        this._options = {};
        this.optionNum = 0;
        this.optionBoxWidth = 700,

        this.getOptionWidth = function() {
            return this.optionBoxWidth / tThis.optionNum + "px";
        }

        this.init = function() {
            if(data.nodeType) {
                loadXML(data);
            } else {
                loadJson(data);
            }

            $(this.el).html("");         

            var headEl = $.createElement("div", "optionTitle");
            $.each(this._options, function(id, _option) {
                var optionTitle = $.createElement("span");
                $(optionTitle).html(_option.name).css("width", tThis.getOptionWidth());
                headEl.appendChild(optionTitle);

            });
            this.el.appendChild(headEl);

            var ul = $.createElement("ul");
            this.rootList.each(function(i, root){
                var li = root.toHTMLTree();
                ul.appendChild(li);
            });
            this.el.appendChild(ul);

            // tip: firefox下float元素和非float元素共占一行时，float元素位置会下移
            $.isFirefox && $(this.el).find("span.optionBox").css("margin-top", "-20px");
        }

        var tThis = this;
        var loadXML = function(data) {
            var nodes = data.querySelectorAll("treeNode");
            var parents = {};
            $.each(nodes, function(i, xmlNode) {
                var nodeAttrs = {};
                $.each(xmlNode.attributes, function(j, attr) {
                    nodeAttrs[attr.nodeName] = attr.value;
                });

                var parentId = xmlNode.parentNode.getAttribute(_TREE_NODE_ID);
                var parent = parents[parentId];
                var treeNode = new TreeNode(nodeAttrs, parent);

                if(parent == null) {
                    tThis.rootList.push(treeNode);
                }   
                parents[treeNode.id] = treeNode;
            });

            var optionNodes = data.querySelectorAll("options>option");
            $.each(optionNodes, function(i, node) {
                var _option = new _Option(node);
                tThis._options[_option.id] = _option;
                tThis.optionNum ++;
            });
        };

        var loadJson = function(data) {
            var stack = [];
            var parents = {};

            data.treeNodes.each(function(i, nodeAttrs) {
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

            data.options.each(function(i, item) {
                var _option = new _Option(item);
                tThis._options[_option.id] = _option;
                tThis.optionNum ++;
            });
        };

        // 树控件上禁用默认右键和选中文本（默认双击会选中节点文本）
        this.el.oncontextmenu = this.el.onselectstart = function(_event) {
            $.Event.cancel(_event || window.event);
        }   

        var 
            _TREE_NODE = "treeNode",
            _TREE_NODE_ID = "id",
            _TREE_NODE_NAME = "name",

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

        TreeNode = function(attrs, parent) {            
            this.id   = attrs[_TREE_NODE_ID];
            this.name = attrs[_TREE_NODE_NAME];
            this.opened = (attrs._open == "true");
            this.attrs = attrs;

            // 维护成可双向树查找
            this.children = [];
            this._options = tThis._options;
          
            if(parent) {
                this.parent = parent;
                this.parent.children.push(this);
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
                li.node = this;
                this.li = li;

                // 节点打开、关闭开关
                li.switchIcon = $.createElement("span", "switch");
                li.appendChild(li.switchIcon);

                // 节点名称
                li.a = $.createElement("a");
                $(li.a).html(this.name.length > 15 ? this.name.substring(0, 12) + "..." : this.name).title(this.name);
                li.appendChild(li.a);

                // 每个节点都可能成为父节点
                li.ul = $.createElement("ul");
                li.ul.setAttribute("pID", this.id);
                li.appendChild(li.ul);

                if(this.children.length > 0) {                  
                    this.opened = !this.opened;
                    clickSwich(this);
                }
                else { // is leaf
                    $(li.switchIcon).addClass("node_leaf").css("cursor", "default");
                }

                // 添加option（权限操作项）
                var nThis = this;
                var optionBox = $.createElement("span", "optionBox");
                $.each(tThis._options, function(_optionId, _option) {
                    var optionChecker = $.createElement("span");
                    optionChecker.setAttribute("oid", _optionId);
                    optionChecker.option = _option;
                    optionChecker.node = nThis;

                    optionChecker.state = nThis.attrs[_optionId] || "0";

                    $(optionChecker).html(_option.name).css("width", tThis.getOptionWidth())
                        .css("backgroundImage", "url(images/optionState" + optionChecker.state + ".gif)");
                    optionBox.appendChild(optionChecker);

                    $(optionChecker).click(function(ev) {
                        var shiftKey = (ev || event).shiftKey; // 是否同时按下shiftKey
                        changeOptionCheckState(ev, optionChecker, shiftKey);
                    });
                });
                li.insertBefore(optionBox, li.ul);

                // 添加事件
                $(li.switchIcon).click( function() { clickSwich(nThis); } );

                return li;
            },

            openNode: function() {
                clickSwich(this);
            }
        };

        /********************************************* 定义树节点TreeNode end *********************************************/

        tThis.init();
    };

    return Matrix;
});