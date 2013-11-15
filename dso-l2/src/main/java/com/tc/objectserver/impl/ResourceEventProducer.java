/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tc.objectserver.impl;

import com.tc.net.protocol.transport.ReconnectionRejectedCallback;

/**
 *
 * @author mscott
 */
public interface ResourceEventProducer extends ReconnectionRejectedCallback {
    public void unregisterForResourceEvents(ResourceEventListener listener);
    public void registerForResourceEvents(ResourceEventListener listener);
    
}
