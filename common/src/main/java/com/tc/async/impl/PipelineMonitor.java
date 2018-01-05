/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    action("COMPLETE", Type.ENQUEUE, null);
  }
  
  public PipelineMonitor(PipelineMonitor parent) {
    this.parent = parent;
  }
  
  public final synchronized PipelineMonitor action(String name, Type type, Object target) {
    PipelineMonitor monitor = checkClosed();
    monitor.items.add(new Record(name, System.nanoTime(), type, target));
    return monitor;
  }
  
  public synchronized PipelineMonitor close() {
    if (closed) {
      throw new IllegalStateException("already closed", closedOn);
    }
    action("COMPLETE", Type.END, null);
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
    builder.append('\n');
    Set<String> names = items.stream().map(i->i.name).collect(Collectors.toSet());
    builder.append(names.toString());
    builder.append('\n');
    for (String name : names) {
      builder.append(name);
      builder.append('\n');
      items.stream().filter(i->i.name.equals(name)).reduce((l, r)->{
        if (l.type != Type.END) {
          builder.append("\t" + l.type + "->" + r.type + " time:" + (r.time - l.time) + " ");
        }
        return r;
      });
      builder.append('\n');
    }
    return builder.toString();
  }
  
  
}
