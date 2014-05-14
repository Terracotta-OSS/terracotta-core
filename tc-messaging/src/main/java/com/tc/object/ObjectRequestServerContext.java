/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.MultiThreadedEventContext;

public interface ObjectRequestServerContext extends ObjectRequestContext, MultiThreadedEventContext {
  public enum LOOKUP_STATE {
    SERVER_INITIATED_FORCED {
      @Override
      public boolean isServerInitiated() {
        return true;
      }

      @Override
      public boolean forceSend() {
        return true;
      }
    },
    SERVER_INITIATED {
      @Override
      public boolean isServerInitiated() {
        return true;
      }

      @Override
      public boolean forceSend() {
        return false;
      }
    },
    CLIENT {
      @Override
      public boolean isServerInitiated() {
        return false;
      }

      @Override
      public boolean forceSend() {
        return false;
      }
    };

    public abstract boolean isServerInitiated();

    public abstract boolean forceSend();
  }

  public abstract String getRequestingThreadName();

  public abstract LOOKUP_STATE getLookupState();

}
