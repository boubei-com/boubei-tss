s<#assign Long = statics["java.lang.Long"] />
<#assign Integer = statics["java.lang.Integer"] />
<#assign manager = statics["com.boubei.tss.portal.engine.FreemarkerParser"] />
<#assign Global  = statics["com.boubei.tss.framework.Global"]/>
<#assign Environment = statics["com.boubei.tss.framework.sso.Environment"] />

<#assign menuService = Global.getContext().getBean("NavigatorService")/>
<#assign articleService = Global.getContext().getBean("RemoteArticleService")/>

<#assign defaultPageUrl  = "/tss/index.portal"/>
<#assign articleListUrl  = "/tss/articleList.portal?isRobot=true"/>
<#assign articlePageUrl  = "/tss/articlePage.portal?isRobot=true"/>
<#assign searchResultUrl = "/tss/searchResult.portal?isRobot=true"/>
<#assign afterLoginUrl   = "/tss/afterLogin.portal?isRobot=true"/>

<#macro showMenu menuId>
	<#assign data = menuService.getNavigatorXML(menuId) />
	<#assign doc  = manager.translateValue(data) />
	<#assign menu = doc.Menu />
	<TABLE height=33 cellSpacing=0 cellPadding=0 width="90%" align=center><TBODY><TR> 
	<#list menu.MenuItem as item>
	  <TD class=mainTd id=td_${item.@id}>
		 <SPAN class=menuSpan id=manegeMenu_${item.@id}>
			<a href='${item.@url}'>${item.@name}</a>
		 </SPAN>
		 <DIV id=span_div_27 style="FONT-SIZE: 1px; Z-INDEX: 10; LEFT: 0px; VISIBILITY: hidden; POSITION: relative; TOP: 7px; HEIGHT: 1px" />
	  </TD> 
	</#list>
	</TR></TBODY></TABLE>
</#macro>  

<#macro getArticleListXML channelId, page=1, pagesize=5>
	<#assign data = articleService.getArticleListByChannel(channelId, page, pagesize, true) />
	<#assign doc  = manager.translateValue(data)/>
	<#assign articleList = doc.Response.ArticleList.rss/>
        <#assign channelName = articleList.channelName?default('')/>
	<#assign totalPageNum = articleList.totalPageNum/>
	<#assign totalRows = articleList.totalRows/>
	<#assign currentPage = articleList.currentPage/>
</#macro> 

<#macro getArticleXML articleId>
	<#assign data = articleService.getArticleXML(articleId) />
	<#assign doc  = manager.translateValue(data) />
	<#assign article = doc.Response.ArticleInfo.rss.Article/>
</#macro> 

<#macro getChannelTree4Portlet channelId>
	<#assign data = articleService.getChannelTree4Portlet(channelId)/>
	<#assign doc  = manager.translateValue(data) />
</#macro> 