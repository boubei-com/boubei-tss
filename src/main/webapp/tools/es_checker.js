(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var api = {
  letConst: { passes: "'use strict'; let a; const b = 2;" },
  letLoop: { passes: "'use strict'; for(let i in {}){}; for(let i=0;;){break}" },
  constLoop: { passes: "'use strict'; for(const i in {}){}; for (const i=0;;){break}" },
  defaultParameter: { passes: "'use strict'; function a(b=2){}" },
  spreadRest: { passes: "'use strict'; var a = [1,2]; +function b(...c){}(...a);" },
  destructuring: { passes: "'use strict'; var a = [1,2], [b,c] = a, d = {e:1,f:2}, {e:E,f} = d;" },
  parameterDestructuring: { passes: "'use strict'; function a({b,c}){}" },
  templateString: { passes: "'use strict'; var a = 1, b = `c${a}d`;" },
  forOf: { passes: "'use strict'; for (var a of [1]) {}" },
  arrow: { passes: "'use strict'; var a = () => {};" },
  generator: { passes: "'use strict'; function *a(){ yield; }" },
  conciseMethodProperty: { passes: "'use strict'; var a = 1, b = { c(){}, a };" },
  computedProperty: { passes: "'use strict'; var a = 1, b = { ['x'+a]: 2 };" },
  moduleExport: { passes: "'use strict'; export var a = 1;" },
  moduleImport: { passes: "'use strict'; import {a} from 'b';" },
  classes: { passes: "'use strict'; class Foo {}; class Bar extends Foo {};" },
  numericLiteral: { passes: "'use strict'; var a = 0o1, b = 0b10;" },
  oldOctalLiteral: { fails: "'use strict'; var a = 01;" },
  symbol: { passes: "'use strict'; var a = Symbol('b');" },
  symbolImplicitCoercion: { dependencies: ["symbol"], fails: "'use strict'; var a = Symbol('a'); a + '';" },
  unicodeEscape: { passes: "'use strict'; var a = '\\u{20BB7}';" },
  unicodeIdentifier: { passes: "'use strict'; var \\u{20BB7};" },
  unicodeRegExp: { passes: "'use strict'; var a = /\\u{20BB7}/u;" },
  stickyRegExp: { passes: "'use strict'; var a = /b/y;" },
  letTDZ: { dependencies: ["letConst"], fails: "'use strict'; a = 1; let a;" },
  letLoopScope: { dependencies: ["letLoop","forOf"], passes: "'use strict'; var x=[],i=0;for(let i=2;i<3;i++){x.push(function(){return i})};for(let i in {3:0}){x.push(function(){return i})};for(let i of [4]){x.push(function(){return i})};if(x[0]()*x[1]()*x[2]()!=24) throw 0;" },
  constRedef: { dependencies: ["letConst"], fails: "'use strict'; const a = 1; a = 2;" },
  objectProto: { passes: "'use strict'; var a = { b: 2 }, c = { __proto__: a }; if (c.b !== 2) throw 0;" },
  objectSuper: { passes: "'use strict'; var a = { b: 2 }, c = { d() { return super.b; } }; Object.setPrototypeOf(c,a); if (c.d() !== 2) throw 0;" },
  extendNatives: { dependencies: ["class"], passes: "'use strict'; class Foo extends Array { }; var a = new Foo(); a.push(1,2,3); if (a.length !== 3) throw 0;" },
  TCO: { passes: "'use strict'; +function a(b){ if (b<6E4) a(b+1); }(0);" },
  functionNameInference: { passes: "'use strict'; var a = { b: function(){} }; if (a.b.name != 'b') throw 0;" },
  ObjectStatics: { is: "'use strict'; return ('getOwnPropertySymbols' in Object) && ('assign' in Object) && ('is' in Object);" },
  ArrayStatics: { is: "'use strict'; return ('from' in Array) && ('of' in Array);" },
  ArrayMethods: { is: "'use strict'; return ('fill' in Array.prototype) && ('find' in Array.prototype) && ('findIndex' in Array.prototype) && ('entries' in Array.prototype) && ('keys' in Array.prototype) && ('values' in Array.prototype);" },
  TypedArrays: { is: "'use strict'; return ('ArrayBuffer' in global) && ('Int8Array' in global) && ('Uint8Array' in global) && ('Int32Array' in global) && ('Float64Array' in global);" },
  TypedArrayStatics: { dependencies: ["TypedArrays"], is: "'use strict'; return ('from' in Uint32Array) && ('of' in Uint32Array);" },
  TypedArrayMethods: { dependencies: ["TypedArrays"], is: "'use strict'; var x = new Int8Array(1); return ('slice' in x) && ('join' in x) && ('map' in x) && ('forEach' in x);" },
  StringMethods: { is: "'use strict'; return ('includes' in String.prototype) && ('repeat' in String.prototype);" },
  NumberStatics: { is: "'use strict'; return ('isNaN' in Number) && ('isInteger' in Number);" },
  MathStatics: { is: "'use strict'; return ('hypot' in Math) && ('acosh' in Math) && ('imul' in Math);" },
  collections: { is: "'use strict'; return ('Map' in global) && ('Set' in global) && ('WeakMap' in global) && ('WeakSet' in global);" },
  Proxy: { is: "'use strict'; return ('Proxy' in global);" },
  Promise: { is: "'use strict'; return ('Promise' in global);"},
  Reflect: { is: "'use strict'; return ('Reflect' in global);" },
};

module.exports = api;

},{}],2:[function(require,module,exports){
var Supports = function(){
  // Variables
  this.letConst = 'letConst';
  this.letTDZ = 'letTDZ';
  this.constRedef = 'constRedef';
  this.destructuring = 'destructuring';
  this.spreadRest = 'spreadRest';
  // Data Types
  this.forOf = 'forOf';
  this.collections = 'collections';
  this.symbol = 'symbol';
  this.Symbol = this.symbol;
  this.symbolImplicitCoercion = 'symbolImplicitCoercion';
  // Number
  this.numericLiteral = 'numericLiteral';
  this.oldOctalLiteral = 'oldOctalLiteral';
  this.MathStatics = 'MathStatics';
  this.mathStatics = this.MathStatics;
  this.NumberStatics = 'NumberStatics';
  this.numberStatics = this.NumberStatics;
  // String
  this.StringMethods = 'StringMethods';
  this.stringMethods = this.StringMethods;
  this.unicodeEscape = 'unicodeEscape';
  this.unicodeIdentifier = 'unicodeIdentifier';
  this.unicodeRegExp = 'unicodeRegExp';
  this.stickyRegExp = 'stickyRegExp';
  this.templateString = 'templateString';
  // Function
  this.arrow = 'arrow';
  this.defaultParameter = 'defaultParameter';
  this.parameterDestructuring = 'parameterDestructuring';
  this.functionNameInference = 'functionNameInference';
  this.TCO = 'TCO';
  this.tco = this.TCO;
  // Array
  this.ArrayMethods = 'ArrayMethods';
  this.arrayMethods = this.ArrayMethods;
  this.ArrayStatics = 'ArrayStatics';
  this.arrayStatics = this.ArrayStatics;
  this.TypedArrayMethods = 'TypedArrayMethods';
  this.typedArrayMethods = this.TypedArrayMethods;
  this.TypedArrayStatics = 'TypedArrayStatics';
  this.typedArrayStatics = this.TypedArrayStatics;
  this.TypedArrays = 'TypedArrays';
  this.typedArrays = this.TypedArrays;
  // Object
  this.objectProto = 'objectProto';
  this.ObjectStatics = 'ObjectStatics';
  this.objectStatics = this.ObjectStatics;
  this.computedProperty = 'computedProperty';
  this.conciseMethodProperty = 'conciseMethodProperty';
  this.Proxy = 'Proxy';
  this.proxy = this.Proxy;
  this.Reflect = 'Reflect';
  this.reflect = this.Reflect;
  // Generator and Promise
  this.generator = 'generator';
  this.Promise = 'Promise';
  this.promise = this.Promise;
  // Class
  this.classes = 'classes';
  this.class = this.classes;
  this.objectSuper = 'objectSuper';
  this.extendNatives = 'extendNatives';
  // Module
  this.moduleExport = 'moduleExport';
  this.moduleImport = 'moduleImport';
};

module.exports = new Supports();

},{}],3:[function(require,module,exports){
var api = require('./api');
var supports = {};
supports._api = api;

function runTest(key){
  if (key === 'class') key = 'classes';
  if (supports._api[key].dependencies) {
    for(var i = 0; i < supports._api[key].dependencies.length; i++){
      var depKey = supports._api[key].dependencies[i];
      if (runTest(depKey) === false) return false;
    }
  }

  if (supports._api[key].passes) {
    return tryPassFail(supports._api[key].passes);
  } else if (supports._api[key].fails) {
    return !tryPassFail(supports._api[key].fails);
  } else if (supports._api[key].is) {
    return tryReturn(supports._api[key].is);
  } else if (supports._api[key].not) {
    return !tryReturn(supports._api[key].not);
  }
}

function tryPassFail(code) {
  try {
    runIt(code);
    return true;
  }
  catch (err) {
    return false;
  }
}

function tryReturn(code) {
  try {
    return runIt(code);
  }
  catch (err) {
    return false;
  }
}

function runIt(code) {
  return (new Function(code))();
}

module.exports =  runTest;

},{"./api":1}],4:[function(require,module,exports){
var supports = require('../../lib/interface');
var api = require('../../lib/api');
var runTest = require('../../lib/runtest');

global = window;
for (var key in supports){
  supports[key] = runTest(supports[key]);
}

supports._api = api;
window.Supports = supports;

},{"../../lib/api":1,"../../lib/interface":2,"../../lib/runtest":3}]},{},[4]);



(function checkChrome(){
  var userAgent = navigator.userAgent;
  //Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.170 Safari/537.36 OPR/53.0.2907.99
  var isOpera = userAgent.indexOf("OPR") > -1; //判断是否Opera浏览器  
  //Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; rv:11.0) like Gecko
  var isIE11 = userAgent.indexOf("rv") > -1; //判断是否IE浏览器 
  //Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; .NET CLR 2.0.50727; .NET CLR 3.0.30729; .NET CLR 3.5.30729)
  var elseIE = userAgent.indexOf("compatible") > -1 && userAgent.indexOf("MSIE") > -1;
  //Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0
  var isFF = userAgent.indexOf("Firefox") > -1; //判断是否Firefox浏览器 
  //Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.1 Safari/603.1.30 
  var isSafari = userAgent.indexOf("Safari") > -1 && userAgent.indexOf("Chrome") == -1; //判断是否Safari浏览器  
  //Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36
  var isChrome = userAgent.indexOf("Chrome") > -1 && userAgent.indexOf("Safari") > -1 ; //判断Chrome浏览器

  var Sys = {};
  var supES6 = "no";
  if (isOpera) {  
      Sys.opera = userAgent.match(/OPR\/([\d.]+)/)[1];
      if(Supports.letConst){
        supES6 = "yes";
      }
      console.log("Opera"+' '+Sys.opera+' '+supES6);
  }  
  else if (isFF) { 
      Sys.firefox = userAgent.match(/Firefox\/([\d.]+)/)[1];
      if(Supports.letConst){
         supES6 = "yes";
      }
      console.log("Firefox"+' '+Sys.firefox+' '+supES6);
  }              
  else if (isSafari) {  
        Sys.safari = userAgent.match(/Safari\/([\d.]+)/)[1];
        if(Supports.letConst){
           supES6 = "yes";
        }
        console.log("Safari"+' '+Sys.safari+' '+supES6);
  }  
  else if (isChrome) { 
         Sys.chrome = userAgent.match(/Chrome\/([\d.]+)/)[1];
         if(Supports.letConst){
            supES6 = "yes";
         }
         console.log("Chrome"+' '+Sys.chrome+' '+supES6);
  } 
  else if(isIE11){
       Sys.ie = userAgent.match(/rv:([\d.]+)/)[1];
       if(Supports.letConst){
          supES6 = "yes";
       }
       console.log("IE"+' '+Sys.ie+' '+supES6);
  }              
  else if (elseIE) { 
        supES6 = "no";
        console.log("IE-low"+' '+Sys.elseIE+' '+supES6);
  }

  tssJS.supportES6 = supES6;
  tssJS.isChrome = isChrome;
  tssJS.isSafari = isSafari;
  if( supES6 === "no") {
    console.log("您的浏览器过旧，请下载最新版chrome浏览器以获得更好的体验！")
  }
})()