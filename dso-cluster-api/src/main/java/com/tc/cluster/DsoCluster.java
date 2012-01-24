/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tcclient.cluster.DsoNode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The {@code DsoCluster} interface provides access to Terracotta DSO cluster events and meta data.
 * <p>
 * When Terracotta DSO is active, an instance of this interface will be injected into a field of an instrumented class
 * when that field is annotated with the {@link InjectedDsoInstance} annotation or when it's included in the {@code
 * injected-instances} section of the Terracotta XML configuration. Field injection will always replace any values that
 * were already assigned to those fields and prevent any other assignments from replacing the value. The injection
 * happens before any constructor logic.
 * <p>
 * To allow cluster events and meta data to be tested without Terracotta DSO being active, the
 * SimulatedDsoCluster class can be used.
 * <p>
 * Note that only DSO client nodes are taken into account for the cluster events and meta data, information about DSO
 * server nodes is not available.
 * <p>
 * See {@link DsoClusterListener} for more information about the events themselves.
 *
 * @since 3.0.0
 */
public interface DsoCluster {
  /**
   * Adds a cluster events listener.
   * <p>
   * If the cluster events listener instance has already been registered before, this method will not register it again.
   * <p>
   * When the cluster is already joined or the operations have already been enabled, those events will be immediately
   * triggered on the listener when it's registered.
   *
   * @param listener the cluster listener instance that will be registered
   */
  public void addClusterListener(DsoClusterListener listener);

  /**
   * Removes a cluster events listener.
   * <p>
   * If the cluster events listener instance was not registered before, this method will have no effect.
   *
   * @param listener the cluster listener instance that will be unregistered
   */
  public void removeClusterListener(DsoClusterListener listener);

  /**
   * Retrieves a view of the topology of the cluster, as seen from the current node.
   * <p>
   * Note that the returned topology instance will be updated internally as nodes joined and leave the cluster. If you
   * want a snapshot of the current nodes in the cluster, you should use the {@link DsoClusterTopology#getNodes()}
   * method.
   *
   * @return an instance of the cluster topology as seen from the current node
   */
  public DsoClusterTopology getClusterTopology();

  /**
   * Retrieves the {@code DsoNode} instance that corresponds to the current node. May return null if this node is not
   * connected to the cluster yet.
   * 
   * @return the {@code DsoNode} instance that corresponds to the current node. May return null if this node is not
   *         connected to the cluster yet.
   */
  public DsoNode getCurrentNode();

  /**
   * Waits until this node joins the cluster. This operation can be interrupted.
   * 
   * @return the {@code DsoNode} instance that corresponds to the current node. May return null if the thread is
   *         interrupted before the current node joins the cluster.
   */
  public DsoNode waitUntilNodeJoinsCluster();

  /**
   * Indicates whether the current node has joined the cluster.
   *
   * @return {@code true} if the current node has joined the cluster; {@code false} otherwise
   */
  public boolean isNodeJoined();

  /**
   * Indicates whether operations are enabled on the current node.
   *
   * @return {@code true} if operations are enabled on the current node; {@code false} otherwise
   */
  public boolean areOperationsEnabled();

  /**
   * Determine on which nodes a particular object is faulted.
   *
   * @param object the object that will be checked
   * @throws UnclusteredObjectException when the object isn't clustered
   * @return the set of nodes where the object is faulted;
   *         <p>
   *         this never returns {@code null}, so null checks aren't needed
   */
  public Set<DsoNode> getNodesWithObject(Object object) throws UnclusteredObjectException;

  /**
   * Determine where a series of clustered objects is faulted.
   * <p>
   * Each object will be a key in the map that is returned, with sets of nodes as values that indicate where the objects
   * are faulted.
   *
   * @param objects the objects that will be checked
   * @throws UnclusteredObjectException when any of the objects isn't clustered
   * @return the map of nodes where the objects are faulted;
   *         <p>
   *         this never returns {@code null}, so null checks aren't needed
   */
  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException;

  /**
   * Determine where a collection of clustered objects is faulted.
   * <p>
   * Each object will be a key in the map that is returned, with sets of nodes as values that indicate where the objects
   * are faulted.
   *
   * @param objects the objects that will be checked
   * @throws UnclusteredObjectException when any of the objects isn't clustered
   * @return the map of nodes where the objects are faulted;
   *         <p>
   *         this never returns {@code null}, so null checks aren't needed
   */
  public Map<?, Set<DsoNode>> getNodesWithObjects(Collection<?> objects) throws UnclusteredObjectException;

  /**
   * Retrieve a set of keys for map values that are not faulted anywhere out of a clustered map for which partialness is
   * supported.
   *
   * @param map the map with the values that will be checked
   * @throws UnclusteredObjectException when the map isn't clustered
   * @return the set of keys for the values that are faulted nowhere;
   *         <p>
   *         an empty set if the map doesn't support partialness;
   *         <p>
   *         this never returns {@code null}, so null checks aren't needed
   */
  public <K> Set<K> getKeysForOrphanedValues(Map<K, ?> map) throws UnclusteredObjectException;

  /**
   * Retrieve a set of keys for map values that are faulted on the current node out of a clustered map for which
   * partialness is supported.
   *
   * @param map the map with the values that will be checked
   * @throws UnclusteredObjectException when the map isn't clustered
   * @return the set of keys for the values that are faulted on the current node;
   *         <p>
   *         an empty set if the map doesn't support partialness;
   *         <p>
   *         this never returns {@code null}, so null checks aren't needed
   */
  public <K> Set<K> getKeysForLocalValues(Map<K, ?> map) throws UnclusteredObjectException;
}