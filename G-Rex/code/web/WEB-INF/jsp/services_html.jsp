<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%@taglib uri="http://acegisecurity.org/authz" prefix="authz"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>G-Rex services on this server</title>
    </head>
    <body>

    <h1>G-Rex Services</h1>
    
    <p>Welcome <authz:authentication operation="username"/>!</p>
    
    <table border="1">
        <tbody>
            <tr><th>Service name</th><th>Description</th><th>Instances</th><th>Config</th></tr>
            <c:forEach var="gridservice" items="${gridservices}">
            <tr>
                <td>${gridservice.name}</td>
                <td>${gridservice.description}</td>
                <td><a href="${gridservice.name}/instances.html">link</a></td>
                <!-- TODO: show config as HTML from here? -->
                <td><a href="${gridservice.name}/config.xml">link</a></td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
    </body>
</html>
