<%@include file="xml_header.jsp"%>
<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%--
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
--%>

<%-- Used to display GRexExceptions --%>
<html>
    <head>
        <title>ERROR!</title>
    </head>
    <body>

    <h1>Error!</h1>
    
    <%-- The Spring framework injects the GRexException into this page and calls it "exception" --%>
    <p>Message: ${exception.message}</p>
    <p>Code: ${exception.errorCode}</p>
    
    <%--
    This example uses JSTL, uncomment the taglib directive above.
    To test, display the page like this: index.jsp?sayHello=true&name=Murphy
    --%>
    <%--
    <c:if test="${param.sayHello}">
        <!-- Let's welcome the user ${param.name} -->
        Hello ${param.name}!
    </c:if>
    --%>
    
    </body>
</html>
