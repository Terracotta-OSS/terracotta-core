package org.terracotta.voter;

import com.tc.voter.VoterManager;

public interface ClientVoterManager extends VoterManager {

  /**
   *
   * @return the current state of the server that this voter is connected to.
   */
  String getServerState();

  /**
   * Close the connection with the server.
   */
  void close();
}
