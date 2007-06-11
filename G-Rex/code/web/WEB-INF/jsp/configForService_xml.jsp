<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%-- Generates the config information for a service.  This does not show
     everything that is in the original config file: it only reveals what
     a client needs to know to build its GUI or parse its command line --%>
<gridservice name="${gridservice.name}" description="${gridservice.description}">
    <params>
        <%-- "param" is a reserved name so we use "par" --%>
        <c:forEach var="par" items="${gridservice.params}">
        <param name="${par.name}" paramType="${par.type}" required="${par.required}"
            description="${par.description}"/>
        </c:forEach>
    </params>
    <inputs>
        <c:forEach var="input" items="${gridservice.inputs}">
        <input name="${input.name}"/>
        </c:forEach>
    </inputs>
    <outputs>
        <c:forEach var="output" items="${gridservice.outputs}">
        <output name="${output.name}" stream="${output.stream}"/>
        </c:forEach>
    </outputs>
</gridservice>