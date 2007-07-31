<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
Displays the details of a particular service instance
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 

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
            <tr><th>Number of sub-jobs</th><td>${instance.numSubJobs}</td></tr>
            <tr><th>State</th><td>${instance.state}</td></tr>
            <tr><th>Exit code</th><td>${instance.exitCode}</td></tr>
            <tr><th>Owner</th><td>${instance.owner}</td></tr>
            <tr><th>Group</th><td>${instance.group}</td></tr>
            <tr><th>Owner Permissions</th><td>${instance.ownerPermissions}</td></tr>
            <tr><th>Group Permissions</th><td>${instance.groupPermissions}</td></tr>
            <tr><th>Other Permissions</th><td>${instance.otherPermissions}</td></tr>
        </tbody>
    </table>
    
    <p><strong>Parameters:</strong></p>
    <table border="1">
        <tbody>
            <tr><th>Name</th><th>Value</th></tr>
            <c:forEach var="par" items="${instance.parameters}">
            <tr><td>${par.key}</td><td>${par.value}</td></tr>
            </c:forEach>
        </tbody>
    </table>
    
    <p><strong>Output files:</strong></p>
    <table border="1">
        <tbody>
            <tr><th>Output file</th><th>Size (bytes)</th><th>Last Modified</th></tr>
            <c:forEach var="file" items="${instance.currentOutputFiles}">
            <c:set var="fileUrl" value="${instance.url}/outputs/${file.relativePathUrlEncoded}"/>
            <%-- only add the full URL when the file is ready for download --%>
            <tr>
                <td><c:if test="${file.readyForDownload}"><a href="${fileUrl}"></c:if>${file.relativePath}<c:if test="${file.readyForDownload}"></a></c:if></td>
                <td>${file.lengthBytes}</td>
                <td>${file.lastModified}</td>
                <td>${file.checkSum}</td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
    
    </body>
</html>
