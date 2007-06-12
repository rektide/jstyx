<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
Displays the details of a particular service instance
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Instance ${instance.id} of ${instance.serviceName}</title>
    </head>
    <body>

    <h1>Instance ${instance.id} of ${instance.serviceName}</h1>
    
    <table border="1">
        <tbody>
            <tr><th>ID</th><td>${instance.id}</td></tr>
            <tr><th>Description</th><td>${instance.description}</td></tr>
        </tbody>
    </table>

    
    </body>
</html>
