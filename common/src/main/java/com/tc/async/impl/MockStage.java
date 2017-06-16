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

import org.terracotta.entity.StateDumpCollector;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;

/**
 * @author orion
 */
public class MockStage implements Stage {

  private final String  name;
  public final MockSink sink;

  public MockStage(String name) {
    this.name = name;
    this.sink = new MockSink();
  }

  @Override
  public void destroy() {
    //
  }

  @Override
  public synchronized Sink getSink() {
    return sink;
  }

  @Override
  public void start(ConfigurationContext context) {
    //
  }

  @Override
  public int pause() {
//
    return 0;
  }

  @Override
  public void unpause() {
//
  }

  @Override
  public String toString() {
    return "MockStage(" + name + ")";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void addStateTo(final StateDumpCollector stateDumper) {
  }

}