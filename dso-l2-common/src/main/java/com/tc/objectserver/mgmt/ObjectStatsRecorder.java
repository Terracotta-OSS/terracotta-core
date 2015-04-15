/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.mgmt;

import com.tc.statistics.util.NullStatsRecorder;
import com.tc.statistics.util.StatsPrinter;
import com.tc.statistics.util.StatsRecorder;

import java.text.MessageFormat;

public class ObjectStatsRecorder {

  private boolean                    requestDebug;
  private StatsRecorder              requestStatsRecorder;

  private boolean                    broadcastDebug;
  private StatsRecorder              broadcastStatsRecorder;

  private boolean                    commitDebug;
  private StatsRecorder              commmitStatsRecorder;

  private static final StatsRecorder NULL_RECORDER = new NullStatsRecorder();

  public ObjectStatsRecorder() {
    this(false, false, false);
  }

  public ObjectStatsRecorder(boolean requestDebug, boolean broadcastDebug,
                             boolean commitDebug) {
    setRequestDebug(requestDebug);
    setBroadcastDebug(broadcastDebug);
    setCommitDebug(commitDebug);
  }

  public synchronized void setRequestDebug(boolean requestDebug) {
    this.requestDebug = requestDebug;
    if (requestStatsRecorder != null) {
      requestStatsRecorder.finish();
    }
    if (requestDebug) {
      requestStatsRecorder = new StatsPrinter(new MessageFormat("Object-requests in the Last {0} ms"),
                                              new MessageFormat(" {0} instances"), true);
    } else {
      requestStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getRequestDebug() {
    return requestDebug;
  }

  public void updateRequestStats(String type) {
    requestStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized void setBroadcastDebug(boolean broadcastDebug) {
    this.broadcastDebug = broadcastDebug;
    if (broadcastStatsRecorder != null) {
      broadcastStatsRecorder.finish();
    }
    if (broadcastDebug) {
      broadcastStatsRecorder = new StatsPrinter(new MessageFormat("Broadcasted in the Last {0} ms"),
                                                new MessageFormat(" {0} instances"), true);
    } else {
      broadcastStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getBroadcastDebug() {
    return broadcastDebug;
  }

  public void updateBroadcastStats(String type) {
    broadcastStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized void setCommitDebug(boolean commitDebug) {
    this.commitDebug = commitDebug;
    if (commmitStatsRecorder != null) {
      commmitStatsRecorder.finish();
    }
    if (commitDebug) {
      commmitStatsRecorder = new StatsPrinter(new MessageFormat("Commits in the Last {0} ms"),
                                              new MessageFormat(
                                              // hate this stupid formatter, can't figure how to prefix with space
                                                                // " count = {0,number,000000}   bytes = {1,number,0000000}   new = {2,number, 0000}"
                                                                " count = {0}   bytes = {1}   new = {2}"), true);
    } else {
      commmitStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getCommitDebug() {
    return commitDebug;
  }

  public void updateCommitStats(String type, int length, boolean isNew) {
    commmitStatsRecorder.updateStats(type, new long[] { 1, length, (isNew ? 1 : 0) });
  }

}
