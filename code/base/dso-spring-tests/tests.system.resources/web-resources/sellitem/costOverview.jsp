<%--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

--%>

<%@ include file="includeTop.jsp" %>

form=costOverview

price=<c:out value="${sale.price}"/>

itemCount=<c:out value="${sale.itemCount}"/>

category=<c:out value="${sale.category}"/>

shipping=<c:out value="${sale.shipping}"/>

baseAmount=<c:out value="${sale.amount}"/>

deliveryCost=<c:out value="${sale.deliveryCost}"/>

discount=<c:out value="${sale.savings}"/>
discountRate=<c:out value="${sale.discountRate}"/>

totalCost=<c:out value="${sale.totalCost}"/>
