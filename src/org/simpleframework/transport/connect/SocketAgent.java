/*
 * SocketAgent.java February 2012
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package org.simpleframework.transport.connect;

import java.nio.channels.SocketChannel;

import org.simpleframework.transport.trace.Trace;
import org.simpleframework.transport.trace.Agent;

/**
 * The <code>SocketAgent</code> is used to wrap an agent for safety. 
 * Wrapping an agent in this way ensures that even if the agent is
 * badly written there is little chance that it will affect the
 * operation of the server. All <code>Trace</code> objects returned
 * from this will catch all exceptions within the created trace.
 * 
 * @author Niall Gallagher
 */
class SocketAgent implements Agent {
   
   /**
    * This is the agent that is used to create the trace objects.
    */
   private final Agent agent;
   
   /**
    * Constructor for the <code>SocketAgent</code> object. This will
    * be given the agent that is to be used to create traces. This 
    * can be a null value, in which case the trace provided will be
    * a simple empty void that swallows all trace events.
    * 
    * @param agent the agent that is to be wrapped by this
    */
   public SocketAgent(Agent agent) {
      this.agent = agent;
   }
 
   /**
    * This method is used to attach a trace to the specified channel.
    * Attaching a trace basically means associating events from that
    * trace with the specified socket. It ensures that the events 
    * from a specific channel can be observed in isolation.
    * 
    * @param channel this is the channel to associate with the trace
    * 
    * @return this returns a trace associated with the channel
    */
   public Trace attach(SocketChannel channel) {
      Trace trace = null;
      
      if(agent != null) {
         trace = agent.attach(channel);
      }
      return new SocketTrace(trace);
   }
   
   /**
    * This is used to stop the agent and clear all trace information.
    * Stopping the agent is typically done when the server is stopped
    * and is used to free any resources associated with the agent. If
    * an agent does not hold information this method can be ignored.
    */
   public void stop() {
      if(agent != null) {
         agent.stop();
      }
   }
}