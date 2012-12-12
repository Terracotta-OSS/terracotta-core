/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.search.AggregateFunction;
import org.terracotta.toolkit.search.Attribute;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SortDirection;
import org.terracotta.toolkit.search.ToolkitSearchQuery;
import org.terracotta.toolkit.search.expression.Clause;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NoOpInvocationHandler implements InvocationHandler {
  private volatile NoOpQueryBuilder queryBuilder;

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // no-op and return default for primitive types and null for Objects.
    if (method.getReturnType().isPrimitive()) {
      if (method.getReturnType() == Byte.TYPE) {
        return 0;
      } else if (method.getReturnType() == Short.TYPE) {
        return 0;
      } else if (method.getReturnType() == Integer.TYPE) {
        return 0;
      } else if (method.getReturnType() == Long.TYPE) {
        return 0L;
      } else if (method.getReturnType() == Float.TYPE) {
        return 0.0f;
      } else if (method.getReturnType() == Double.TYPE) {
        return 0.0d;
      } else if (method.getReturnType() == Character.TYPE) {
        return '\u0000';
      } else if (method.getReturnType() == Boolean.TYPE) { return false; }
    } else if (Map.class.isAssignableFrom(method.getReturnType())) {
      return Collections.EMPTY_MAP;
    } else if (List.class.isAssignableFrom(method.getReturnType())) {
      return Collections.EMPTY_LIST;
    } else if (QueryBuilder.class.isAssignableFrom(method.getReturnType())) {
      if (queryBuilder == null) {
        queryBuilder = new NoOpQueryBuilder(this);
      }
      return queryBuilder;
    } else if (Set.class.isAssignableFrom(method.getReturnType())) { return Collections.EMPTY_SET; }
    return null;
  }

  private static class NoOpQueryBuilder implements QueryBuilder {
    private final NoOpInvocationHandler noOpInvocationHandler;

    public NoOpQueryBuilder(NoOpInvocationHandler noOpInvocationHandler) {
      this.noOpInvocationHandler = noOpInvocationHandler;
    }

    @Override
    public QueryBuilder includeKeys(boolean choice) {
      return this;
    }

    @Override
    public QueryBuilder includeValues(boolean choice) {
      return this;
    }

    @Override
    public QueryBuilder maxResults(int max) {
      return this;
    }

    @Override
    public QueryBuilder includeAttribute(Attribute<?>... attr) {
      return this;
    }

    @Override
    public QueryBuilder addGroupBy(Attribute<?>... attr) {
      return this;
    }

    @Override
    public QueryBuilder addOrderBy(Attribute<?> attr, SortDirection dir) {
      return this;
    }

    @Override
    public QueryBuilder includeAggregator(AggregateFunction... aggregators) {
      return this;
    }

    @Override
    public QueryBuilder addClause(Clause clause) {
      return this;
    }

    @Override
    public ToolkitSearchQuery build() {
      return (ToolkitSearchQuery) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                         new Class[] { ToolkitSearchQuery.class },
                                                         noOpInvocationHandler);
    }

  }
}