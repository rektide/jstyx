<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%-- Generates the config information for a service.  This does not show
     everything that is in the original config file: it only reveals what
     a client needs to know to build its GUI or parse its command line --%>
<gridservice name="${gridservice.name}" description="${gridservice.description}">
    <params>
        <%-- "param" is a reserved name so we use "par" --%>
        <c:forEach var="par" items="${gridservice.params}">
        <param name="${par.name}" type="${par.type}" required="${par.required}"
            greedy="${par.greedy}" description="${par.description}"
            <c:if test="${par.flag != null}">flag="${par.flag}"</c:if>
            <c:if test="${par.longFlag != null}">longFlag="${par.longFlag}"</c:if>
            <c:if test="${par.defaultValue != null}">defaultValue="${par.defaultValue}"</c:if> />
        </c:forEach>
    </params>
    <inputs>
        <c:forEach var="input" items="${gridservice.inputs}">
        <input name="${input.name}"/>
        </c:forEach>
    </inputs>
    <outputs>
        <c:forEach var="output" items="${gridservice.outputs}">
        <output name="${output.name}" appendOnly="${output.appendOnly}"/>
        </c:forEach>
    </outputs>
</gridservice>