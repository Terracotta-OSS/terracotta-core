/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.util.State;

import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
public class ElectionContext {
  private final NodeID node;
  private final Set<String> servers;
  private final boolean isNew;
  private final WeightGeneratorFactory factory;
  private final Consumer<NodeID> winner;
  private final State currentState;

  public ElectionContext(NodeID node,
                         Set<String> servers,
                         boolean isNew,
                         WeightGeneratorFactory factory,
                         State currentState,
                         Consumer<NodeID> winner) {
    this.node = node;
    this.servers = servers;
    this.isNew = isNew;
    this.factory = factory;
    this.winner = winner;
    this.currentState = currentState;
  }

  public NodeID getNode() {
    return node;
  }

  public boolean isNew() {
    return isNew;
  }

  public WeightGeneratorFactory getFactory() {
    return factory;
  }
  
  public void setWinner(NodeID node) {
    winner.accept(node);
  }

  public State getCurrentState() {
    return currentState;
  }

  public Set<String> getServers() {
    return servers;
  }
}
