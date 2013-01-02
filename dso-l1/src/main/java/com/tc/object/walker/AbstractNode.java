/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.walker;

abstract class AbstractNode implements Node {

    private final Object object;

    protected AbstractNode(Object o) {
        this.object = o;
  }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public abstract boolean done();

    @Override
    public abstract MemberValue next();
}