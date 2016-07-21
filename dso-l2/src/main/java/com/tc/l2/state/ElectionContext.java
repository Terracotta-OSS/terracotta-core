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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.util.State;
import java.util.function.Consumer;

/**
 *
 */
public class ElectionContext {
  private final NodeID node;
  private final boolean isNew;
  private final WeightGeneratorFactory factory;
  private final Consumer<NodeID> winner;
  private final State currentState;

  public ElectionContext(NodeID node, boolean isNew, WeightGeneratorFactory factory, State currentState, Consumer<NodeID> winner) {
    this.node = node;
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
}
