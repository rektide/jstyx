<%-- This page is called when we have created a new service instance from another web page
    We simply redirect to another web page that displays the details of the new instance --%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:redirect url="../${instance.serviceName}/instances/${instance.id}.html"/>
