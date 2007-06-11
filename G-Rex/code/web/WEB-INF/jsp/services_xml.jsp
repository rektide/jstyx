<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
    
<gridservices>
<c:forEach var="gridservice" items="${gridservices}">
    <gridservice name="${gridservice.name}" description="${gridservice.description}"/>
</c:forEach>
</gridservices>