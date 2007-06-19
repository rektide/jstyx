<%@include file="xml_header.jsp"%>
<%-- Very simple exception format
     TODO: put the message between the tags (so we can have stack traces, etc?) --%>
<exception type="${exception.class}" code="${exception.errorCode}" message="${exception.message}"/>