package org.terracotta.entity;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

public class PassThroughEntityActiveInvokeChannel<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  
  private final InvokeMonitor<R> monitor;

  public PassThroughEntityActiveInvokeChannel(InvokeMonitor<R> monitor) {
    this.monitor = monitor;
  }

  @Override
  public void sendResponse(R r) {
    monitor.accept(r);
  }

  @Override
  public void sendException(Exception ee) {
    if (ee instanceof EntityException) {
      monitor.exception((EntityException)ee);
    } else {
      monitor.exception(new EntityServerException(null, null, null, ee));
    }
  }

  @Override
  public void close() {
    monitor.close();
  }
}
