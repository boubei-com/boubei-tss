

/* 将json格式数据转换成Form组件 */
;(function($) {

var Field = function(info) {
		this.name  = info.name;
		this.label = info.label;
		this.type  = info.type || "string";
		this.nullable = (info.nullable == null ? "true" : info.nullable);
		this.readonly = (info.readonly == "true") || false;
		this.checkReg = info.checkReg;
		this.errorMsg = info.errorMsg;
		this.options  = info.options;
		this.jsonUrl  = info.jsonUrl;
		this.multiple = (info.multiple == "true") || false;
		this.onchange = info.onchange;
		this.defaultValue = info.defaultValue;

		if(this.defaultValue) {
            var dv = (this.defaultValue+"").trim();
            if( dv.indexOf("#_") == 0 ) {
                var _dvs = (dv+"||").split("||");  // #_day1||today-1  或 #_day1
                dv = $.Cookie.getValue(_dvs[0].substring(1)) || _dvs[1]; // 通用查询条件写在cookie里
            } 
            else if( dv == 'userName' || dv == 'userCode' ) {
            	dv = eval(dv);
            }
            this.defaultValue = (dv == "undefined" ? "" : dv);
        }

		this.width  = (info.width || "220px").trim();
		if( !this.jsonUrl ) {
			this.height = info.height;	
		}

		if( /^\d*$/.test(this.width) ) {
			this.width += "px";
		}
		if( this.height && /^\d*$/.test(this.height.trim()) ) {
			this.height = this.height.trim() + "px";
		}

		this.mode = this.type.toLowerCase();
		if(this.mode !== "hidden") {
			if(this.jsonUrl) {
				this.mode = "combotree";
			} else if(this.options && this.options.codes && this.options.codes.length) {
				this.mode = "combotree";
			}
			if(this.type == "file") {
				this.mode = "combotree";
				this.multiple = true;
			}
		}
		
		switch(this.mode) {
			case "number":
				this.checkReg = this.checkReg || "^(-?\\d+)(\\.\\d+)?$"; // 浮点数
				this._errorMsg = this.errorMsg || "请输入数字";
				break;
			case "int":
				this.checkReg = this.checkReg || "^(-?\\d+)$"; // 整数
				this._errorMsg = this.errorMsg || "请输入整数";
				break;
			case "string":
			case "combo":
			case "combotree":
			case "hidden":
				break;
			case "date":
			case "datetime":
				this.width = "200px";
				this.defaultValue = $.transDay(this.defaultValue, this.mode);
				break;
		}
	};

	Field.prototype = {
		createColumn: function() {
			var column = "<column name='" +this.name+ "' caption='" +this.label+ "' mode='" +this.mode+ "' empty='" +this.nullable+ "' ";
			if(this.checkReg) {
				column += " checkReg='" +this.checkReg+ "' ";
			}
			if(this._errorMsg) {
				column += " errorMsg='" +this._errorMsg+ "' ";
			}
			if(this.errorMsg) {
				column += " placeholder='" +this.errorMsg+ "' ";
			}
			if(this.multiple) {
				column += " multiple='multiple' ";
			}
			if(this.readonly ) {
				column += " editable='false' ";
			}
			if(this.onchange) {
				column += " onchange='" + this.onchange + "' ";
			}
			if(this.height) {
				column += " height='" + this.height + "' ";
			}
			if(this.mode == 'int' || this.mode == 'number') {
				column += " maxLength='16' ";
			}

			// 如果下拉列表需要后续生成，可先填入任意值，默认初始化成为codes和names都为空
			if(this.options && this.options.codes == undefined) {
				this.options = {"codes": "", "names": ""};
			}
			if(this.options) {
				// 如果只有Code列表
				this.options.names = this.options.names || this.options.codes;
				this.options.names = this.options.names.replace(/&/ig, ' '); // 替换&等符号
				this.options.codes = this.options.codes.replace(/&/ig, ' ');

				if (this.options.codes == "year") {
					this.options.codes = '2010|2011|2012|2013|2014|2015|2016|2017|2018|2019|2020|2021|2022|2023|2024';
				}
				if (this.options.codes == "month") {
					this.options.codes = '1|2|3|4|5|6|7|8|9|10|11|12';
					this.options.names = '一月|二月|三月|四月|五月|六月|七月|八月|九月|十月|十一月|十二月';
				}
				column += " values='" + this.options.codes + "' texts='" + this.options.names + "'";
			}

			return column + "/>";
		},

		createLayout: function() {	
			var height = this.height||'20px';
			var tag = (parseInt(height.replace('px', '')) > 20 && !this.options && !this.jsonUrl) ? 'textarea' : 'input';		
			var layout = [];
			layout[layout.length] = " <TR>";
			layout[layout.length] = "    <TD width='88'><label binding='" + this.name + "'/></TD>";
			layout[layout.length] = "    <TD><" + tag + " binding='" + this.name + "' style='width:" + this.width + ";height:" + height + ";'/></TD>";
			layout[layout.length] = " </TR>";

			return layout.join("");
		},		

		createDataNode: function() {
		 	if(this.defaultValue || this.defaultValue === 0) {
		 		return "<" + this.name + "><![CDATA[" + this.defaultValue + "]]></" + this.name + ">";
		 	}
			return "";
		}
	}

	$.json2Form = function(formId, defines, buttonBox, customizeJS) {
		var infos = defines ? (typeof(defines) === "string" ? $.parseJSON(defines) : defines) : [];

		var fields = [];
		var columns = [];
		var layouts = [];
		var datarow = [];
		infos.each(function(i, info) {
			info.name = info.name || info.code || "param" + (i+1);
			
			var item = new Field(info);
			fields.push(item);

			columns.push(item.createColumn());
			if(item.mode !== "hidden") {
				layouts.push(item.createLayout());
			}
			datarow.push(item.createDataNode());
		});
		
		var str = [];
		str[str.length] = "<xform>";
		str[str.length] = "    <declare>";
		str[str.length] = columns.join("");
		str[str.length] = "    </declare>";
		str[str.length] = "    <layout>";
		str[str.length] = layouts.join("") + (buttonBox || "");
		str[str.length] = "    </layout>";
		str[str.length] = "    <data><row>" + datarow.join("") + "</row></data>";
		if(customizeJS) {
			str[str.length] = "<script> <![CDATA[" + customizeJS + "]]> </script>";
		}
		str[str.length] = "</xform>";
		
		var tssForm = $.F(formId, $.XML.toNode(str.join("")));

		fields.each(function(i, field){
			if( !field.jsonUrl || field.type === 'hidden' ) return;

			field.jsonUrl = decodeURI(field.jsonUrl); // 从浏览器复制过来已经带了encode
			function loadList() {
				function setList(result) { 
					if( result.length == 0 ) {
						result.push({"text": "没有数据", "value": ""})
					}
					
					var values = [], texts = [];
					result.each(function(i, item){
						values.push( $.vt(item).value );
						texts.push( $.vt(item).text );
					});
					tssForm.updateField(field.name, [
						{"name": "texts", "value": texts.join('|')},
					 	{"name": "values", "value": values.join('|')}
					 ]);

					// 列表数据加载后刷新下显示值（form生成时，因列表数据还没取到，没法显示下拉控件的值）
					field.defaultValue && tssForm.updateDataExternal(field.name, field.defaultValue); 
				}

				$.get(field.jsonUrl, {}, setList);
			}

			loadList();

			var tdNode = tssJS("#" + field.name)[0].parentNode;
			var refreshBT = tssJS.createElement('span', 'icon-sync');
            refreshBT.title = "刷新附件下拉列表";
            tssJS(refreshBT).css("margin-left", "4px").css("font-size", '15px').css("cursor", "pointer").click( function() {     
                loadList();
            } );  
            tdNode.appendChild(refreshBT);
		});

		return tssForm;
	};

	// ---------------------------- 多级下拉选择联动 ------------------------------------------------
	$.getNextLevelOption = function(form, currLevel, currLevelValue, service, nextLevel) {
		if(currLevel == null || currLevelValue == null || service == null || nextLevel == "") return;
		
		var params = {};
		params[currLevel] = currLevelValue;
 
		$.getJSON(service, params, 
			function(result) { 
				if( result ) {
					if( result.length == 0 ) {
						result.push({"text": "没有数据", "value": ""})
					}

					var values = [], texts = [];
					result.each(function(i, item){
						values.push( $.vt(item).value );
						texts.push( $.vt(item).text );
					});
					
					$("#" + nextLevel).css("height", "20px");
					form.updateField(nextLevel, [
						{"name": "mode", "value": "combotree"},
						{"name": "texts", "value": texts.join('|')},
					 	{"name": "values", "value": values.join('|')}
					 ]);
				}				
			}, 'GET'
		);
	};

	/* 判断方法是否相等 */
	$.funcCompare = function(func1, func2) {
		if(func1 == null && func2 != null) {
			return false;
		}
		if(func2 == null && func1 != null) {
			return false;
		}
		if(func2 == null && func1 == null) {
			return true;
		}

		var fn = /^(function\s*)(\w*\b)/;
		return func1.toString().replace(fn,'$1') === func2.toString().replace(fn,'$1'); 
	}

	// item的类型允许为[id, code, name] or [pk, id, text]
	$.createOption = function(item, text, value) {
		var option = new Option();
		$.copy(option, $.vt(item, text, value));
		return option;
	};

	$.vt = function(item, text, value) {
		var result = {};
		result.value = item[value] || item.value || item.name || item.code  || item.id   || item[0] || '';
		result.text  = item[text]  || item.name  || item.text || item.value || item.code || item[2] || item[1] || item[0] || '';
		return result;
	};

	$.copy = function(to, from) {
		$.each(from, function(name, value) {
			to[name] = value;
		});
	}

	// 将today-x or today+x转换成具体日期
	$.transDay = function(day, mode) {
		if( !day) return day;

		var result, today = new Date();

		if( (/today[\s]*-/gi).test(day) ) {
			var delta = parseInt( day.split("-")[1] );
			today.setDate(today.getDate() - delta);
		} 
		else if( (/today[\s]*\+/gi).test(day) ) {
			var delta = parseInt( day.split("+")[1] );
			today.setDate(today.getDate() + delta);
		}
		else if( 'cur_year_01' == day ){
			today = new Date( today.getYear() + 1900 , 0 , 1)
		}
		else if( 'cur_month_01' == day ){
			today = new Date( today.getYear() + 1900 , today.getMonth() , 1 )
		}
		else if( 'cur_week_01' == day ){
			var weekday = today.getDay()||7; //获取星期几,getDay()返回值是 0（周日） 到 6（周六） 之间的一个整数。0||7为7，即weekday的值为1-7
	    	today.setDate(today.getDate()-weekday+1)
		}
		else {
			return day;
		}

		var result = today.format('yyyy-MM-dd') + (mode == "datetime" ? " 00:00:00" : "");
		return result;
	}

}) (tssJS);