/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc.async.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class PipelineMonitor {
  public enum Type {
    ENQUEUE,
    RUN,
    END
  }
  
  public class Record {
    public final String name;
    public final String thread;
    public final long time;
    public final Type type;
    public final Object record;

    public Record(String name, long time, Type type, Object record) {
      this.name = name;
      this.time = time;
      this.type = type;
      this.record = record;
      this.thread = Thread.currentThread().getName();
    }

    @Override
    public String toString() {
      return "Record{" + "name=" + name + ", thread=" + thread + ", time=" + time + ", type=" + type + '}';
    }
  }
  
  private final List<Record> items = new LinkedList<>();
  private volatile boolean closed = false;
  private Exception closedOn;
  private final PipelineMonitor parent;
  
  public PipelineMonitor() {
    this.parent = null;
  }
  
  public PipelineMonitor(PipelineMonitor parent) {
    this.parent = parent;
  }
  
  public synchronized PipelineMonitor action(String name, Type type, Object target) {
    PipelineMonitor monitor = checkClosed();
    monitor.items.add(new Record(name, System.nanoTime(), type, target));
    return monitor;
  }
  
  public synchronized PipelineMonitor close() {
    if (closed) {
      throw new IllegalStateException("already closed", closedOn);
    }
    this.closed = true;
    this.closedOn = new Exception();
    return this;
  }
  
  private PipelineMonitor checkClosed() {
    if (closed) {
      PipelineMonitor clone = new PipelineMonitor(this);
      return clone;
    } else {
      return this;
    }
  }
  
  private synchronized boolean isClosed() {
    return closed;
  }

  @Override
  public String toString() {
    if (!isClosed()) {
      throw new IllegalStateException("not closed");
    }
    StringBuilder builder = new StringBuilder("\n" + parent + "\n");
    items.forEach(r->builder.append("\t" + r + "\n"));
    builder.append("time:" + TimeUnit.NANOSECONDS.toMicros(items.get(items.size()-1).time - items.get(0).time));
    return builder.toString();
  }
  
  
}
