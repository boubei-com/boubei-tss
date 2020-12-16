// ------------------------------------------------- 控制录入表单的常用方法 ------------------------------------------------

/** 
 * 判断当前的录入表单是否为新建记录；如果是在编辑已有记录，返回false
 */
function isNew() {
    var tag = ws.getActiveTab().SID;
    if(tag && (tag.indexOf("_new") > 0 || tag.indexOf("_copy") > 0) ) {
        return true;
    }
    return false;
}

/** 
 * 更新字段输入框里的值
 *
 * @param string field  单个字段ID
 * @param string value  新值
 * @param boolean value  是否不允许再编辑
 * @example updateField("applier", "Jack");
 */
function updateField(field, value, readonly) {
    var xform = tssJS.F("page1Form");
    if(!xform) return; // queryForm触发

    xform.updateDataExternal(field, value);    
    xform.updateData( $1(field) );

    readonly && forbid(field);
}

// 读取Form里指定字段值
function getFiledVal(field) {
    var xform = tssJS.F("page1Form");
    if(!xform) return; // queryForm触发

    return x.getData(field);
}

/** 
 * 检查当前用户是否拥有指定角色集中的一个
 *
 * @param string roles  逗号分隔的一至多个角色ID|名称
 * @example checkRole("12,13") 、 checkRole("12")
 */
function checkRole(roles) {    
    if(!roles) return true; // 默认通过

    var result = false;
    (roles + "").split(",").each(function(i, role){
        if( ( isInt(role) && userRoles.contains( parseInt(role) ) ) 
            || userRoleNames.contains( role ) ) {
            
            result = true;
        }
    });
    return result;
}

function isInt(x) {
    return /^-?[1-9]+[0-9]*]*$/.test(x)
}

/** 
 * 检查当前用户是否拥有指定用户组集中的一个
 *
 * @param string groups  逗号分隔的一至多个用户组ID
 * @example checkGroup("12,13") 、 checkGroup("12")
 */
function checkGroup(groups) {
    if(!groups) return true; // 默认通过

    var result = false;
    if(userGroups.length) {
        var g = userGroups[userGroups.length - 1];
        (groups + "").split(",").each(function(i, group){
            if(group == (g[0]+"") && group == g[1]) {
                result = true;
            }
        });
    }
    return result;
}

/** 
 * 设置字段是否可编辑
 *
 * @param string field  逗号分隔的一至多个字段ID
 * @param string tag  "true": 可编辑，"false": 不可编辑
 * @example permit("f1,f2", "true")
 */
function permit(field, tag) {
    var xform = tssJS.F("page1Form");
    var fields = (field || '').split(",");
    fields.each(function(i, _field) {
        xform.setFieldEditable(_field, tag || "false"); 
    });
}

/* 
 * 依据当前用户的角色和组织判断用户是否能对指定字段可编辑，除指定的角色和组织之外一律不可编辑 
 * forbid( "score", "r1,r2", "g1, g2");
 */
function forbid(field, roles, groups) {
    var editable = false;
    if( (roles && checkRole(roles)) || (groups && checkGroup(groups)) ) {
        editable = true;
    } 

    !editable && permit(field, "false");
}

/** 
 * 检查当前用户是否为特定用户集中的一员
 *
 * @example check("f1,f2", "User1,User2")
 */
function check(field, users) {
    users = (users || '').split(',');
    !users.contains(userCode) && permit(field, "false");
}

/** 
 * 在输入框显示提示气泡
 *
 * @example tssJS("money").notice("请输入金额");
 */
function notice(field, msg) {
    tssJS("#" + field).click(function(){  
        tssJS(this).notice(msg); 
    });
}

/** 
 * 隐藏Grid列表的右键的删除按钮
 */
function hideDelButton() {
    $1("grid").contextmenu.delItem("_item_id3");
}

/** 
 * 禁止编辑录入表单
 */
function disableForm() {
    tssJS("#saveBt").hide(); 
    tssJS.F("page1Form").setEditable("false");
}
function enableForm() {
    tssJS("#saveBt").show(); 
    tssJS.F("page1Form").setEditable("true");
}

/** 
 * 将指定字段从录入表单里隐藏起来不显示
 *
 * @param string field  逗号分隔的一至多个字段ID
 * @example hideFiled("f1,f2")
 */
function hideField(field) {
    var fields = (field || '').split(",");
    fields.each(function(i, fID) {
        tssJS("*", $1(fID).parentNode).hide();
        tssJS("#label_" + fID).hide();
    });
}

var hideFiled = hideField; /* 曾用拼写错误名，需保留 */

/** 
 * 将指定字段（隐藏状态）从录入表单里重新显示出来
 *
 * @param string field  逗号分隔的一至多个字段ID
 * @example showFiled("f1,f2")
 */
function showFiled(field) {
    var fields = (field || '').split(",");
    fields.each(function(i, fID) {
        tssJS("#" + fID).show();
        tssJS("#label_" + fID).show();
    });
}

/** 
 * 在Grid表头上方添加一个操作按钮，且只有特定角色（或用户组）的人可见
 *
 * @param string name  按钮标题
 * @param function fn  点击按钮触发此方法
 * @param string roles 逗号分隔的一至多个角色ID|名称
 * @param string groups 逗号分隔的一至多个用户组ID
 * @example 
 *      addOptBtn('批量打分', function() { batchUpdate("score", "及格") });
 */
function addOptBtn(name, fn, roles, groups, readonly) {
    if( !checkRole(roles) && !checkGroup(groups||'-1212') ) {
        return;
    } 

    // readonly 的按钮始终显示，其它的则需要对录入表有编辑权限才会显示
    var batchOpBtn = tssJS.createElement('button', 'tssbutton small white' + (readonly ? ' readonly' : ''));
    tssJS(batchOpBtn).html(name).click( fn );  
    tssJS('#customizeBox').appendChild(batchOpBtn);
}

/** 
 * 在Grid表头上方添加一个批量操作按钮，且只有特定角色（或用户组）的人可见。
 * 用以更新所有选中记录行的某字段为某个特定值
 *
 * @param string name  按钮标题
 * @param string field 字段名
 * @param string value 新值
 * @param string roles 逗号分隔的一至多个角色ID|名称
 * @param string groups 逗号分隔的一至多个用户组ID
 * @param function checkfn 检查选中的列是否都允许进行批量操作
 * @example 
 *      batchOpt('批量审批', "status", "审核通过", "r1,r2", "g1, g2");
 */
function batchOpt(name, field, value, roles, groups, checkfn) {
    addOptBtn(name, 
        function() { 
            if( !checkfn || checkfn() ) {
                batchUpdate(field, value); 
            }
        }, 
    roles, groups);  
}

// 批量更新选中行某一列的值
function batchUpdate(field, value) {
    var ids = tssJS.G("grid").getCheckedRows();
    if(!ids) {
        return alert("你没有选中任何记录，请勾选后再进行批量操作。");
    }
    if(ids.split(",").length >= 1000) {
        return alert("单次批量操作行数不能超过999行。")
    }
    tssJS.ajax({
        url: URL_BATCH_OPERATE + recordId,
        params: {"ids": ids, "field": field, "value": value},
        onsuccess: function() { 
            loadGridData( $1("GridPageList").value || 1 ); // 更新Grid
        }
    });
}

/*
    var mi = {
        label:"查看执行日志",
        callback: function() { showRunLog(); },
        visible:function() { return true; }
    }
*/
function addGridRightBtn(mi) {
    $1("grid").contextmenu.addItem(mi);
}

// ----------------------------------------------- 非常用方法 start------------------------------------------------
// 针对指定的字段，检查Grid中选中行该字段的值是否和预期的值一致，如不一致，弹框提醒
function checkBatch(field, expectVal, msg) {
    var values = tssJS.G("grid").getCheckedRowsValue(field);
    var flag = true;
    values.each(function(i, val) {
        if(val != expectVal) {
            flag = false;
        }
    });

    !flag && msg && tssJS.alert(msg);
    return flag;
}

/* 手动二级下拉（只能电脑端用，小程序不支持）：
 * nextLevel("season", "month", 
 *   {"春":"三月|四月|五月", "夏":"六月|七月|八月", "秋":"九月|十月|十一月", "冬":"十二月|一月|二月"});
 */
function nextLevel(current, next, map) {
    var currentVal = tssJS("#" + current).value();
    var nextOpts = map[currentVal];
    if(!nextOpts) {
        return;
    }

    var xform = tssJS.F("page1Form");
    xform.updateField(next, [
        {"name": "texts", "value": nextOpts},
        {"name": "values", "value": nextOpts}
     ]);
}

function calculateSum(totalField, fields) {
    forbid(totalField); 
    fields.each(function(i, field){
        tssJS("#" + field).blur(function(){
            var value = 0;
            fields.each(function(j, f){
                value += getFloatValue(f);
            });
            updateField(totalField, value);    
        });
    });
}

function getFloatValue(field) {
    return parseFloat(tssJS("#" + field).value() || '0');
}

// onlyOne(["udf1", "udf2", "udf3"]);  只有一个可编辑
function onlyOne( fields ) {
    var xform = tssJS.F("page1Form"); 
    fields.each(function(i, field){  
        tssJS("#" + field).blur(function(){
            var value = this.value;
            
            fields.each(function(j, f){ 
                if(field !== f) {
                    xform.setFieldEditable(f, !value ? "true" : "false");   
                }
            });

            xform.updateData(this);
        });

        var tempV = tssJS("#" + field).value(); 
        if(tempV) {
            fields.each(function(j, f){
                if(field !== f) {
                    setTimeout(function() {
                        xform.setFieldEditable(f, "false"); 
                    }, 50*j);
                }
            });
        }
    });
}

function before(day, delta) {
    var today = new Date();
    today.setDate(today.getDate() - delta);
    return new Date(day) < today;
}
// ----------------------------------------------- 非常用方法 End ------------------------------------------------

/*
 *  多级下拉选择联动，录入表单和查询表单都使用本方法
 *
 *  参数： nextL    下一级联动字段的code
        serviceID       下一级联动的service地址             
        serviceParam    service接收的参数code
        curFiledVal     当前联动字段的值
 */
function getNextLevelOption(nextL, serviceID, serviceParam, curFiledVal) {
    if( !nextL || !serviceID || !serviceParam || tssJS.isNullOrEmpty(curFiledVal)) return;

    if( tssJS("#" + nextL).length == 0 ) return; 

    // serviceID maybe is ID of record, maybe a serviceUrl
    var url = isInt(serviceID) ? '/tss/data/json/' + serviceID : serviceID;
    
    var xform;
    if( (serviceParam+"").indexOf('p_') >= 0 || url.indexOf('p_') >= 0) { // 查询表单的级联下拉
        serviceParam = serviceParam.replace('p_', '')
        url = url.replace('p_', '');
        xform = tssJS.F("searchForm");
    } 
    else {
        xform = tssJS.F("page1Form");
    }

    if( isInt(serviceParam) ) { // 数字
        serviceParam = "param" + serviceParam;
    }
    
    tssJS.getNextLevelOption(xform, serviceParam, curFiledVal, url, nextL);
}

/* 示例：
  var wm_url = 'http://wanma.800best.com'; 
  var bi_url = 'http://btrbi.800best.com'; 
  recordId = 905; // BI系统里，【分拨】上传举证材料的录入ID
  loadRemoteAttach(recordId, itemId, bi_url, 'TSS', function() {
      if( tssJS("#attachGrid td>a").length ) return;  // 如果有找到附件了，说明是分拨提交的，不用再去万马找了

      recordId = 25; // 万马系统里，【网点】上传举证材料的录入ID
      loadRemoteAttach(recordId, itemId, wm_url, 'WM');
  });
 */
function loadRemoteAttach(recordId, itemId, appUrl, appCode, callback) {
    tssJS.ajax({ 
        url: URL_ATTACH_LIST + recordId + "/" + itemId + "?anonymous=true", 
        method: "POST", 
        headers: {"anonymous": "true", "appCode": appCode},
        onresult: function(){
            var attachNode  = this.getNodeValue("RecordAttach");
            tssJS("column[name='delOpt']", attachNode).attr("display", "none");  // 隐藏删除附件操作
            tssJS.G("attachGrid", attachNode);   

            tssJS("#attachGrid td>a").each(function(i, item){
                if( tssJS(item).text() == '查看' ) {
                    tssJS(item).attr('href', appUrl + tssJS(item).attr('href') + '?anonymous=true');
                }
            });
            callback && callback();
        } 
    });
}

function val2Text(colomn, url, _text, _value) {
    _text  = _text  || 'text';
    _value = _value || "value";

    var params = {}, grid = tssJS.G("grid");
    if( colomn.endsWith('_id') && grid) {
        var values = grid.getColumnValues(colomn);
        var ids = [];
        values.each(function(i, val) {
            val && isInt(val) && ids.push(val);
        });

        if(ids.length) {
            params["id"] = ids.join();
        }
    }

    $.get(url, params, function(data) {
        var map = {};
        data.each(function(i, item) {
            var text = item[_text], val = item[_value];
            map[val] = text;
        })

        var $tds = $("#grid td[name='" +colomn+ "']");
        $tds.each(function(i, td) { 
            if( i > 0 ) {
                var val = $(td).attr("value");
                $(td).text( map[val] );
            }
        })
    } );
}

/*
    function onGridLoad() { 
        val2TextII("f4");
    }
*/
function val2TextII(colomn, _text, _value) {
    recordDefine.each(function(i, field){
        if(  colomn === field.code ) {
            return val2Text(colomn, field.jsonUrl, _text, _value);
        }
    });
}

/* 表单里控制字段值的唯一性：
var uniqueFlag = false;
tssJS("#name").blur( function() {
     tssJS.getJSON("/tss/auth/xdata/json/xxx", {"name": tssJS("#name").value(), "strictQuery": "true"}, function(result) {
            if( isNew() ) { uniqueFlag = result.length > 0; }
            else { uniqueFlag = result.length > 1; }  // 修改行自身不算 
    
            if(uniqueFlag) tssJS.alert("同名记录已经存在，请更换名称");
     }  );
});

preListener = function() {  if(uniqueFlag) tssJS.alert("同名记录已经存在，请更换名称"); return flag; }
 */