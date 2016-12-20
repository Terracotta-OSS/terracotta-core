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
  private final String errorClassName;


  public BasicClientArgumentBuilder(String testClassName, String errorClassName) {
    this.testClassName = testClassName;
    this.errorClassName = errorClassName;
  }

  @Override
  public String getMainClassName() {
    return TestClientStub.class.getCanonicalName();
  }

  @Override
  public List<String> getArgumentsForSetupRun(String connectUri, ClusterInfo clusterInfo, int numberOfStripes, int numberOfServersPerStripe, int totalClientCount) {
    return buildList("SETUP", connectUri, clusterInfo, numberOfStripes, numberOfServersPerStripe, totalClientCount, 0);
  }

  @Override
  public List<String> getArgumentsForTestRun(String connectUri, ClusterInfo clusterInfo, int numberOfStripes, int numberOfServersPerStripe, int totalClientCount, int thisClientIndex) {
    return buildList("TEST", connectUri, clusterInfo, numberOfStripes, numberOfServersPerStripe, totalClientCount, thisClientIndex);
  }

  @Override
  public List<String> getArgumentsForDestroyRun(String connectUri, ClusterInfo clusterInfo, int numberOfStripes, int numberOfServersPerStripe, int totalClientCount) {
    return buildList("DESTROY", connectUri, clusterInfo, numberOfStripes, numberOfServersPerStripe, totalClientCount, 0);
  }


  private List<String> buildList(String task, String connectUri, ClusterInfo clusterInfo, int numberOfStripes, int numberOfServersPerStripe, int totalClientCount, int thisClientIndex) {
    List<String> args = new Vector<String>();
    args.add("--task");
    args.add(task);
    args.add("--testClass");
    args.add(this.testClassName);
    args.add("--connectUri");
    args.add(connectUri);
    args.add("--clusterInfo");
    args.add(clusterInfo.encode());
    args.add("--numberOfStripes");
    args.add(Integer.toString(numberOfStripes));
    args.add("--numberOfServersPerStripe");
    args.add(Integer.toString(numberOfServersPerStripe));
    
    args.add("--totalClientCount");
    args.add(Integer.toString(totalClientCount));
    args.add("--thisClientIndex");
    args.add(Integer.toString(thisClientIndex));
    
    if (null != this.errorClassName) {
      args.add("--errorClass");
      args.add(this.errorClassName);
    }
    return args;
  }
}