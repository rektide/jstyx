<%@include file="xml_header.jsp"%>
<%@page import="uk.ac.rdg.resc.grex.exceptions.GRexException"%>
<%-- Used when a bug happens on the server, producing an unexpected error --%>
<exception type="${exception.class}" code="<%=GRexException.UNEXPECTED_ERROR%>" message="${exception.message}"/>