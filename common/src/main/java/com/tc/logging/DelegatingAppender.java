/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
        @Override
        public boolean requiresLayout() {
          return false;
        }

        @Override
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

  @Override
  public void addFilter(Filter arg0) {
    delegate().addFilter(arg0);
  }

  @Override
  public void clearFilters() {
    delegate().clearFilters();
  }

  @Override
  public void close() {
    closeDelegate();
  }

  @Override
  public void doAppend(LoggingEvent arg0) {
    delegate().doAppend(arg0);
  }

  @Override
  public ErrorHandler getErrorHandler() {
    return delegate().getErrorHandler();
  }

  @Override
  public Filter getFilter() {
    return delegate().getFilter();
  }

  @Override
  public Layout getLayout() {
    return delegate().getLayout();
  }

  @Override
  public String getName() {
    return delegate().getName();
  }

  @Override
  public boolean requiresLayout() {
    return delegate().requiresLayout();
  }

  @Override
  public void setErrorHandler(ErrorHandler arg0) {
    delegate().setErrorHandler(arg0);
  }

  @Override
  public void setLayout(Layout arg0) {
    delegate().setLayout(arg0);
  }

  @Override
  public void setName(String arg0) {
    delegate().setName(arg0);
  }

}
