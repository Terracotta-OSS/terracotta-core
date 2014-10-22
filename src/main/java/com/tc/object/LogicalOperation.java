/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc.object;

/**
 *
 * @author cdennis
 */
public enum LogicalOperation {

  ADD, //L2:ListManagedObjectState L1:ToolkitListImplApplicator L1:ToolkitListImpl
  ADD_ALL, //L1:ToolkitListImpl
  ADD_ALL_AT,  //L1:ToolkitListImpl
  ADD_AT, //L2:ListManagedObjectState L1:ToolkitListImplApplicator L1:ToolkitListImpl
  PUT, //bunches of places...
  CLEAR, 
  REMOVE, 
  REMOVE_AT, 
  SET, 
  REMOVE_RANGE, 
  REPLACE_IF_VALUE_EQUAL, 
  PUT_IF_ABSENT, 
  REMOVE_IF_VALUE_EQUAL, 
  CLEAR_LOCAL_CACHE, 
  EVICTION_COMPLETED, 
  CLUSTERED_NOTIFIER, 
  DESTROY, 
  FIELD_CHANGED, 
  INT_FIELD_CHANGED, 
  SET_LAST_ACCESSED_TIME, 
  EXPIRE_IF_VALUE_EQUAL, 
  PUT_VERSIONED, 
  REMOVE_VERSIONED, 
  PUT_IF_ABSENT_VERSIONED, 
  CLEAR_VERSIONED, 
  REGISTER_SERVER_EVENT_LISTENER, 
  UNREGISTER_SERVER_EVENT_LISTENER, 
  REGISTER_SERVER_EVENT_LISTENER_PASSIVE, 
  UNREGISTER_SERVER_EVENT_LISTENER_PASSIVE, 
  REMOVE_EVENT_LISTENING_CLIENT, 
  NO_OP,
  CREATE_ENTITY,
  INVOKE_WITH_PAYLOAD;
}
