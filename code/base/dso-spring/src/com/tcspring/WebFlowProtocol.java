/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;


/**
 * Evil hack to make FlowExecutionImpl rehydration work.
 * 
 * @author Eugene Kuleshov
 */
public class WebFlowProtocol {

//  public void setFlowId(StaticJoinPoint jp, Flow flow, FlowExecution instance) throws Exception {
//    // System.err.println("### WebFlowProtocol.setFlowId() " + flow.getId());
//    setValue(instance, "flowId", flow.getId());
//  }
//  
//  public void setFlowId(StaticJoinPoint jp, Flow flow, FlowSession instance) throws Exception {
//    // System.err.println("### WebFlowProtocol.setFlowId() " + flow.getId());
//    setValue(instance, "flowId", flow.getId());
//  }
//  
//  public void setStateId(StaticJoinPoint jp, State state, FlowSession instance) throws Exception {
//    // System.err.println("### WebFlowProtocol.setStateId() " + state.getId());
//    setValue(instance, "stateId", state.getId());
//  }
//
//  private void setValue(Object instance, String fieldName, String value) throws Exception {
//    // shortcut trough reflection API:
//    // Field field = instance.getClass().getDeclaredField(fieldName);
//    // field.setAccessible(true);
//    // field.set(instance, value);    
//    if(instance instanceof TransparentAccess) {
//      ((TransparentAccess) instance).__tc_setmanagedfield(instance.getClass().getName()+"."+fieldName, value);
//    }
//  }

  public Object isStateRestored() {
    // System.err.println("### WebFlowProtocol.isStateRestored()");
    // return flowExecution.getAttributes()!=null && flowExecution.getConversationScope()!=null ? Boolean.TRUE : Boolean.FALSE;
    return Boolean.FALSE;
  }
  
}

