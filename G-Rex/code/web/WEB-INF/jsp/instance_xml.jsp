<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%-- Shows the state of a service instance --%>
<instance id="${instance.id}">
    <url>${instance.url}</url>
    <c:if test="${instance.description != null}"><description>${instance.description}</description></c:if>
    <c:if test="${instance.exitCode != null}"><exitCode>${instance.exitCode}</exitCode></c:if>
    <parameters>
        <c:forEach var="par" items="${instance.parameters}">
        <param name="${par.key}" value="${par.value}"/>
        </c:forEach>
    </parameters>
    <outputFiles>
        <c:forEach var="file" items="${instance.currentOutputFiles}">
        <outputFile relPath="/outputs/${file.relativePath}" lengthBytes="${file.lengthBytes}"
            lastModified="${file.lastModified}" readyForDownload="${file.readyForDownload}"/>
        </c:forEach>
    </outputFiles>
</instance>