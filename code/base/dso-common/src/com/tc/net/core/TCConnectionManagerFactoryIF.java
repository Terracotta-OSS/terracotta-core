package com.tc.net.core;

/**
 * Interface for Connection manager factories
 * 
 * @author teck
 */
interface TCConnectionManagerFactoryIF {
  TCConnectionManager getInstance(TCComm comm);
}