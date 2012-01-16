/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import com.tc.util.Assert;

public class DelegatingAppender implements Appender {

  private Appender delegate;

  public DelegatingAppender(Appender delegate) {
    Assert.assertNotNull(delegate);
    this.delegate = delegate;
  }

  private synchronized Appender delegate() {
    return this.delegate;
  }

  private void closeDelegate() {
    final Appender prev;
    synchronized (this) {
      prev = delegate;
      delegate = new AppenderSkeleton() {
        public boolean requiresLayout() {
          return false;
        }

        public void close() {
          //
        }

        @Override
        protected void append(LoggingEvent loggingevent) {
          //
        }

        @Override
        public void doAppend(LoggingEvent event) {
          //
        }

        @Override
        public void finalize() {
          // don't want super impl
        }

      };
    }

    prev.close();
  }

  public synchronized Appender setDelegate(Appender delegate) {
    Assert.assertNotNull(delegate);
    Appender out = this.delegate;
    this.delegate = delegate;
    return out;
  }

  public void addFilter(Filter arg0) {
    delegate().addFilter(arg0);
  }

  public void clearFilters() {
    delegate().clearFilters();
  }

  public void close() {
    closeDelegate();
  }

  public void doAppend(LoggingEvent arg0) {
    delegate().doAppend(arg0);
  }

  public ErrorHandler getErrorHandler() {
    return delegate().getErrorHandler();
  }

  public Filter getFilter() {
    return delegate().getFilter();
  }

  public Layout getLayout() {
    return delegate().getLayout();
  }

  public String getName() {
    return delegate().getName();
  }

  public boolean requiresLayout() {
    return delegate().requiresLayout();
  }

  public void setErrorHandler(ErrorHandler arg0) {
    delegate().setErrorHandler(arg0);
  }

  public void setLayout(Layout arg0) {
    delegate().setLayout(arg0);
  }

  public void setName(String arg0) {
    delegate().setName(arg0);
  }

}
