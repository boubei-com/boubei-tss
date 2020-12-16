var server_time = tssJS.Cookie.getValue("server_time");
var now = server_time ? toDate(server_time) : new Date(),
    cday = now.format("yyyy-MM-dd"), 
    cyear = now.getFullYear(), 
    cmonth = now.format("yyyy-MM"),
    cweek = getWeekNumber(now),
    cseason = Math.ceil( (now.getMonth()+1)/3 );

//日期格式化
Date.prototype.format = function(format) {
    var date = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S+": this.getMilliseconds()
    };
    if (/(y+)/i.test(format)) {
        format = format.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
    }
    for (var k in date) {
        if (new RegExp("(" + k + ")").test(format)) {
            format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? date[k] : ("00" + date[k]).substr(("" + date[k]).length));
        }
    }
    return format;
}

function toDate(dataStr) {
    if(!dataStr) return dataStr;

    var d = new Date(dataStr.replace(/\"/g, "").replace(/-/g, "/"));
    return d;
}
function subDate(day, x) {
    return new Date(day.getTime() - x*1000*60*60*24);
}
function subDateS(day, x) {
    return subDate(day, x).format("yyyy-MM-dd");
}

function getFirstDayOfMonth(day) {
    return day.format("yyyy-MM") + "-01";
}
function getLastDayOfMonth(day) {
    var year  = day.getFullYear();
    var month = day.getMonth() + 1; // 下一个月，先计算出下个月的第一天，再往前一天就是当前月的最后一天
    if(month == 12) {
        month = 0;
        year ++;
    }
    if(month < 10) {
        month = '0' + month;
    }
    var tempDay = new Date(year, month, 1);
    var lastDay = (new Date(tempDay.getTime() - 1000*60*60*24));

    return lastDay.format("yyyy-MM-dd");
}

/**
 * 判断年份是否为润年
 *
 * @param {Number} year
 */
function isLeapYear(year) {
    return (year % 400 == 0) || (year % 4 == 0 && year % 100 != 0);
}
/**
 * 获取某一年份的某一月份的天数
 *
 * @param {Number} year
 * @param {Number} month
 */
function getMonthDays(year, month) {
    return [31, null, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month] || (isLeapYear(year) ? 29 : 28);
}
/**
 * 获取某年的某天是第几周
 * @param {date} day
 */
function getWeekNumber(day) {
    var year  = day.getFullYear(),
        month = day.getMonth(),
        days  = day.getDate();
    // 那一天是那一年中的第多少天
    for (var i = 0; i < month; i++) {
        days += getMonthDays(year, i);
    }

    // 那一年第一天是星期几
    var yearFirstDay = new Date(year, 0, 1).getDay() || 7;
    days -= (7 - yearFirstDay + 1);

    return Math.ceil(days / 7);
}

//获取某年某周的开始日期
function getBeginDateOfWeek(paraYear, weekIndex){
    var firstDay = getFirstWeekBeginDay(paraYear);
    return subDate(firstDay, (weekIndex-1)*7*-1);
}

//获取某年某周的结束日期
function getEndDateOfWeek(paraYear, weekIndex){    
    return subDate(getBeginDateOfWeek(paraYear, weekIndex), -6);
}

//获取日期为某年的第几周
function getWeekIndex(day) {
    var firstDay = getFirstWeekBeginDay(day.getFullYear());
    if (day < firstDay) {
      firstDay = getFirstWeekBeginDay(day.getFullYear() - 1);
    }
    var d = Math.floor((day.getTime() - firstDay.getTime()) / 86400000);
    return Math.floor(d / 7) + 1;  
}

//获取某年的第一天
function getFirstWeekBeginDay(year) {
    var tempdate = new Date(year, 0, 1);
    var temp = tempdate.getDay();
    if (temp == 1){
       return tempdate;
    }
    return subDate(tempdate, (temp == 0 ? 7 : temp) - 8);   
}

//获得某季度的开始日期      
function getQuarterStartDate(year, season){      
    switch (season){      
        case '1' : return year+"-01-01";
        case '2' : return year+"-04-01";
        case '3' : return year+"-07-01";
        case '4' : return year+"-10-01";
    }
}      
     
//获得某季度的结束日期      
function getQuarterEndDate(year, season){      
    switch (season){      
     case '1' : return year+"-03-31";
     case '2' : return year+"-06-30";
     case '3' : return year+"-09-30";
     case '4' : return year+"-12-31";
    }    
}


function forbidTimeScope(delda, p1, p2) {
    var $ = jQuery;
    $('#' + p1).datebox({
        onSelect: function(date){
            var day2 =  $("#" + p2).datebox("getValue");
            if(day2) {
                var maxDay = subDate( date, delda*-1);
                if( toDate(day2).getTime() > maxDay.getTime() ) {
                    $('#' + p2).datebox('setValue', maxDay.format("yyyy-MM-dd"));
                    $.messager.show({ title: '提示', msg: '单次查询日期跨度不能超过' + delda + "天" });
                }
            }
        }
    });
    $('#' + p2).datebox({
        onSelect: function(date){
            var day1 =  $("#" + p1).datebox("getValue");
            if(day1) {
                var minDay = subDate( date, delda);
                if( toDate(day1).getTime() < minDay.getTime() ) {
                    $('#' + p1).datebox('setValue', minDay.format("yyyy-MM-dd"));
                    $.messager.show({ title: '提示', msg: '单次查询日期跨度不能超过' + delda + "天" });
                }
            }
        }
    });
}

function dateDiff(a, b) {
    if (a == '' || b == '') return '';

    var date1 = toDate(a);
    var date2 = toDate(b);
    var s1 = date1.getTime(), s2 = date2.getTime();
    var total = (s2 - s1) / 1000;
    var day = parseInt(total / (24 * 60 * 60)); // 计算整数天数
    var afterDay = total - day * 24 * 60 * 60;  // 取得算出天数后剩余的秒数
    var hour = parseInt(afterDay / (60 * 60));  // 计算整数小时数
    var afterHour = total - day * 24 * 60 * 60 - hour * 60 * 60; // 取得算出小时数后剩余的秒数
    var min = parseInt(afterHour / 60);  // 计算整数分
    var afterMin = total - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60; // 取得算出分后剩余的秒数

    return {day: day, hour: hour, min: min};
}

function dateDifferObj(a, b) {
    if (a == '' || b == '') return '';

    var date1 = toDate(a);
    var date2 = toDate(b);
    var s1 = date1.getTime(), s2 = date2.getTime();
    var total = (s2 - s1) / 1000;
    var day = parseInt(total / (24 * 60 * 60)); //计算整数天数
    var afterDay = total - day * 24 * 60 * 60; //取得算出天数后剩余的秒数
    var hour = parseInt(afterDay / (60 * 60)); //计算整数小时数
    var afterHour = total - day * 24 * 60 * 60 - hour * 60 * 60; // 取得算出小时数后剩余的秒数
    var min = parseInt(afterHour / 60);  // 计算整数分
    var afterMin = total - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60; // 取得算出分后剩余的秒数

    return {day: day, hour: hour, min: min};
}

///// 《end》时间函数
function monthBox(id){
    var id_class = id ? '#' + id : '.monthbox';
    $(id_class).datebox({
        onShowPanel: function () {
            span.trigger('click'); 
            if (!tds)
            //延时触发获取月份对象，因为上面的事件触发和对象生成有时间间隔
            setTimeout(function() { 
                tds = p.find('div.calendar-menu-month-inner td');
                tds.click(function(e) {
                    //禁止冒泡执行easyui给月份绑定的事件
                    e.stopPropagation(); 
                    //得到年份
                    var year = /\d{4}/.exec(span.html())[0] ,
                    //月份
                    month = parseInt($(this).attr('abbr'), 10);  

                    //隐藏日期对象                     
                    $(id_class).datebox('hidePanel') 
                    //设置日期的值
                    .datebox('setValue', year + '-' + month); 
                });
            }, 0);
        },
        //配置parser，返回选择的日期
        parser: function (s) {
            if (!s) return new Date();
            var arr = s.split('-');
            return new Date(parseInt(arr[0], 10), parseInt(arr[1], 10) - 1, 1);
        },
        //配置formatter，只返回年月 之前是这样的d.getFullYear() + '-' +(d.getMonth()); 
        formatter: function (d) { 
            var currentMonth = (d.getMonth()+1);
            var currentMonthStr = currentMonth < 10 ? ('0' + currentMonth) : (currentMonth + '');
            return d.getFullYear() + '-' + currentMonthStr; 
        }
    });

    //日期选择对象
    var p = $(id_class).datebox('panel'), 
    //日期选择对象中月份
    tds = false, 
    //显示月份层的触发控件
    span = p.find('span.calendar-text'); 
}

// function myformatter(date) {
//     var y = date.getFullYear();
//     var m = date.getMonth() + 1;
//     return y + '-' + m;
// }

function monthBoxValue(id){
    return $('#' + id).datebox('getValue') + '-01';
}