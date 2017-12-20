package org.terracotta.voter;

import com.tc.voter.VoterManager;

public interface ClientVoterManager extends VoterManager {

  default long vote(String id, long term) {
    return vote(id + ":" + term);
  };

  void close();
}
