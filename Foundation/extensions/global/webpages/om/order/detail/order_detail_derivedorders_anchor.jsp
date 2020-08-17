<%/*******************************************************************************
Licensed Materials - Property of IBM
IBM Sterling Selling And Fulfillment Suite
(C) Copyright IBM Corp. 2005, 2013 All Rights Reserved.
US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ********************************************************************************/%>
<%@include file="/yfsjspcommon/yfsutil.jspf"%>
<%@ include file="/console/jsp/order.jspf" %>

<%
    prepareRelatedToOrderElement((YFCElement) request.getAttribute("DerivedToOrderLineList"));   
%>

<table class="anchor">
    <tr>
        <td>
            <jsp:include page="/yfc/innerpanel.jsp" flush="true">
                <jsp:param name="CurrentInnerPanelID" value="I01"/>
            </jsp:include>
        </td>
    </tr>
    <% if (isRelatedFrom((YFCElement) request.getAttribute("Order"),"DerivedFromOrderHeaderKey")) { %>
    <tr>
        <td>
            <jsp:include page="/yfc/innerpanel.jsp" flush="true">
                <jsp:param name="RelatedFromNamespace" value="DerivedFromOrderDetails"/>
                <jsp:param name="RelatedFromBinding" value="xml:/OrderLine/@DerivedFromOrderHeaderKey"/>
                <jsp:param name="CurrentInnerPanelID" value="I02"/>
            </jsp:include>
        </td>
    </tr>
    <%  }   %>
    <yfc:hasXMLNode binding="xml:DerivedToOrderLineList:/OrderLineList/OrderLine">
    <tr>
        <td>
            <jsp:include page="/yfc/innerpanel.jsp" flush="true">
                <jsp:param name="OrderListNamespace" value="DerivedToOrderLineList"/>
                <jsp:param name="CurrentInnerPanelID" value="I03"/>
            </jsp:include>
        </td>
    </tr>
    </yfc:hasXMLNode>
</table>
