<jsp:include page="xml_header.jsp"/>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 

<html>
    <head>
        <title>JSP Page</title>
    </head>
    <body>

    <h1>JSP Page</h1>
    
    <instances serviceName="${serviceName}">
    <c:forEach var="instance" items="${instances}">
        <instance id="${instance.id}" workingDirectory="${instance.workingDirectory}"/>
    </c:forEach>
    </instances>
    
    </body>
</html>
