<%@ include file="includeTop.jsp" %>

form=priceAndItemCountForm

<spring:nestedPath path="sale">

<spring:bind path="price">

# input price
<c:out value="${status.expression}"/>=<c:out value="${status.value}"/>

<c:if test="${status.error}">
# price status.error
statusError=<c:out value="${status.errorMessage}"/>
</c:if>
</spring:bind>


<spring:bind path="itemCount">

# input price
<c:out value="${status.expression}"/>=<c:out value="${status.value}"/>

<c:if test="${status.error}">
# itemCount status.error
<c:out value="${status.error}"/>=<c:out value="${status.errorMessage}"/>
</c:if>
</spring:bind>


flowExecutionKey=<c:out value="${flowExecutionKey}"/>

# submit name="_eventId_submit" value="Next"
# submit name="_eventId_preview" value="Preview Sale

</spring:nestedPath>
