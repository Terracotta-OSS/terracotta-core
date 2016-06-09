/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.util.List;
import java.util.Vector;

import org.terracotta.testing.client.TestClientStub;


/**
 * An implementation of IClientArgumentBuilder which uses the TestClientStub client main class.
 */
public class BasicClientArgumentBuilder implements IClientArgumentBuilder {
  private final String testClassName;


  public BasicClientArgumentBuilder(String testClassName) {
    this.testClassName = testClassName;
  }

  @Override
  public String getMainClassName() {
    return TestClientStub.class.getCanonicalName();
  }

  @Override
  public List<String> getArgumentsForSetupRun(String connectUri) {
    return buildList("SETUP", connectUri, 1, 0);
  }

  @Override
  public List<String> getArgumentsForTestRun(String connectUri, int totalClientCount, int thisClientIndex) {
    return buildList("TEST", connectUri, totalClientCount, thisClientIndex);
  }

  @Override
  public List<String> getArgumentsForDestroyRun(String connectUri) {
    return buildList("DESTROY", connectUri, 1, 0);
  }


  private List<String> buildList(String task, String connectUri, int totalClientCount, int thisClientIndex) {
    List<String> args = new Vector<String>();
    args.add("--task");
    args.add(task);
    args.add("--testClass");
    args.add(this.testClassName);
    args.add("--connectUri");
    args.add(connectUri);
    args.add("--totalClientCount");
    args.add(Integer.toString(totalClientCount));
    args.add("--thisClientIndex");
    args.add(Integer.toString(thisClientIndex));
    return args;
  }
}