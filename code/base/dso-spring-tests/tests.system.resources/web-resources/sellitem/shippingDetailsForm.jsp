<%--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

--%>

<%@ include file="includeTop.jsp" %>

form=shippingDetailsForm

price=<c:out value="${sale.price}"/>
itemCount=<c:out value="${sale.itemCount}"/>
category=<c:out value="${sale.category}"/>
shipping=<c:out value="${sale.shipping}"/>

<spring:bind path="sale.shippingType">
statusValue=<c:out value="${status.value}"/>
</spring:bind>

flowExecutionKey=<c:out value="${flowExecutionKey}"/>

# input status.expression ("S", "E")
# submit name="_eventId_submit" value="Next"
# submit name="_eventId_preview" value="Preview Sale"
