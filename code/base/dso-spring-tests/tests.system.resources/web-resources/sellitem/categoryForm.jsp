<%--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

--%>

<%@ include file="includeTop.jsp" %>

form=categoryForm

price=<c:out value="${sale.price}"/>

itemCount:<c:out value="${sale.itemCount}"/>

<spring:nestedPath path="sale">

<spring:bind path="category">
# category ("", "A", "B")
<c:out value="${status.expression}"/>=<c:out value="${status.value}"/>
</spring:bind>

<spring:bind path="shipping"> 
# shipping
_<c:out value="${status.expression}"/>=marker
<c:out value="${status.expression}"/>=<c:out value="${status.value}"/>
</spring:bind>

flowExecutionKey=<c:out value="${flowExecutionKey}"/>

# submit name="_eventId_submit" value="Next"
# submit name="_eventId_preview" value="Preview Sale"

</spring:nestedPath>
