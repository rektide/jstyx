<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
Displays all the instances of a grid service
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Instances of ${serviceName}</title>
    </head>
    <body>

    <h1>Instances of ${serviceName}</h1>
    
    <form action="clone.action" method="POST">
        <input type="hidden" name="operation" value="create"/>
        <input type="submit" value="Create new instance"/>
    </form>
    
    <table border="1">
        <tbody>
            <tr><th>Instance ID</th><th>Working directory</th></tr>
            <c:forEach var="instance" items="${instances}">
            <tr><td>${instance.id}</td><td>${instance.workingDirectory}</td></tr>
            </c:forEach>
        </tbody>
    </table>
    
    </body>
</html>
