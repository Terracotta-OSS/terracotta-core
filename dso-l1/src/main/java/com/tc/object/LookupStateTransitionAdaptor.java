/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

class LookupStateTransitionAdaptor implements LookupStateTransition {

  private LookupState state = LookupState.UNINITALIZED;

  @Override
  public boolean isPrefetch() {
    return this.state.isPrefetch();
  }

  @Override
  public boolean isMissing() {
    return this.state.isMissing();
  }

  @Override
  public boolean isPending() {
    return this.state.isPending();
  }

  @Override
  public LookupState makeLookupRequest() {
    this.state = this.state.makeLookupRequest();
    return this.state;
  }

  @Override
  public LookupState makeMissingObject() {
    this.state = this.state.makeMissingObject();
    return this.state;
  }

  @Override
  public LookupState makePending() {
    this.state = this.state.makePending();
    return this.state;
  }

  @Override
  public LookupState makePrefetchRequest() {
    this.state = this.state.makePrefetchRequest();
    return this.state;
  }

  @Override
  public LookupState makeUnPending() {
    this.state = this.state.makeUnPending();
    return this.state;
  }

  protected LookupState getState() {
    return this.state;
  }

}
