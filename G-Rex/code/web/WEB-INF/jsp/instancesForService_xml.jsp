<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
    
<instances serviceName="${serviceName}">
<c:forEach var="instance" items="${instances}">
    <instance id="${instance.id}" description="${instance.description}"/>
</c:forEach>
</instances>
