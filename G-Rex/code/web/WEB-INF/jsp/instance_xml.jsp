<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%-- Shows the state of a service instance --%>
<instance id="${instance.id}">
    <url>${instance.url}</url>
    <description>${instance.description}</description>
    <exitCode>${instance.exitCode}</exitCode>
    <parameters>
        <c:forEach var="par" items="${instance.parameters}">
        <param name="${par.key}" value="${par.value}"/>
        </c:forEach>
    </parameters>
</instance>