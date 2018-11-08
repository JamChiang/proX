<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>cas客户端测试页面</title>
</head>
<body>
<%=request.getRemoteUser()%>；欢迎使用一品优购。
<a href="http://cas.pinyougou.com/logout?service=http://www.itcast.cn">退出</a>
</body>
</html>
