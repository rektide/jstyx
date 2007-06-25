<%-- This page is called when we have created a new service instance from another web page
     We simply redirect to another web page that displays the details of the new instance.
     We do the redirect to prevent the user from accidentally creating more instances by
     pressing refresh--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:redirect url="${instance.url}.html"/>
