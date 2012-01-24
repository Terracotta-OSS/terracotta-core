/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

/**
 * A listener interface that can be implemented to be notified about DSO cluster events.
 * <p>
 * Listeners can be registered through the {@link DsoCluster#addClusterListener(DsoClusterListener) addClusterListener}
 * method and removed through the {@link DsoCluster#removeClusterListener(DsoClusterListener) removeClusterListener}
 * method of a {@code DsoCluster} instance.
 * <p>
 * The ordering and the timing of the events isn't guaranteed across the cluster. It is however guaranteed that the
 * events can only appear in the following succession on each individual node:
 * <ul>
 * <li>node joined</li>
 * <li>operations enabled</li>
 * <li>operations disabled</li>
 * <li>node left</li>
 * </ul>
 * <p>
 * The {@code node joined} and {@code node left} events are sent to all the nodes in the cluster and happen once for the
 * lifetime of a node.
 * <p>
 * The {@code operations enabled} and {@code operations disabled} events are repeatable and indicate temporary
 * situations that may resolve themselves automatically over time. Only the current node will receive events concerning
 * its own cluster operations. Nodes in the cluster don't get cluster operation events about other nodes.
 *
 * @since 3.0.0
 */
public interface DsoClusterListener {
  /**
   * Sent when a node joined the cluster, including the current node.
   * <p>
   * This event happens once for the lifetime of a node.
   *
   * @param event provides more information about the event
   * @see DsoCluster#isNodeJoined()
   */
  public void nodeJoined(DsoClusterEvent event);

  /**
   * Sent when a node left the cluster, including the current node.
   * <p>
   * This event happens once for the lifetime of a node.
   * <p>
   * Note that this event might never be triggered for the node in question, other nodes in the cluster will however
   * always receive this event about nodes that have permanently left the cluster.
   *
   * @param event provides more information about the event
   * @see DsoCluster#isNodeJoined()
   */
  public void nodeLeft(DsoClusterEvent event);

  /**
   * Sent when cluster operations are enabled on a node, any operations will go through and propagate through the
   * cluster.
   * <p>
   * This event can be repeated as many times as appropriate, but you're guaranteed to have always received a {@code
   * node left} or {@code operations disabled} event before.
   * <p>
   * Only the current node will receive events concerning its own cluster operations.
   *
   * @param event provides more information about the event
   * @see DsoCluster#areOperationsEnabled()
   */
  public void operationsEnabled(DsoClusterEvent event);

  /**
   * Sent when cluster operations are disabled on a node, no cluster operations can go through.
   * <p>
   * They might propagate through the cluster if the operations are enabled again afterwards, however it's also possible
   * that the nodes is forced to leave the cluster instead.
   * <p>
   * This event can be repeated as many times as appropriate, but you're guaranteed to have always received an {@code
   * operations enabled} event before.
   * <p>
   * Only the current node will receive events concerning its own cluster operations.
   *
   * @param event provides more information about the event
   * @see DsoCluster#areOperationsEnabled()
   */
  public void operationsDisabled(DsoClusterEvent event);
}