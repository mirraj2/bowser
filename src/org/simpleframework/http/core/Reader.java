/*
 * Reader.java February 2001
 *
 * Copyright (C) 2001, Niall Gallagher <niallg@users.sf.net>
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
 
package org.simpleframework.http.core;

import java.nio.channels.SocketChannel;

import org.simpleframework.transport.Channel;
import org.simpleframework.transport.reactor.Operation;

/**
 * The <code>Reader</code> object is used to read the bytes to form 
 * a request entity. In order to execute a read operation the socket 
 * must be read ready. This is determined using the socket object, 
 * which is registered with a selector. If at any point the reading
 * results in an error the operation is canceled and the collector 
 * is closed, which shuts down the connection.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.transport.reactor.Reactor
 */ 
class Reader implements Operation {

   /**
    * This is the selector used to process the collection events.
    */
   private final Selector source;

   /**
    * This is the collector used to consume the entity bytes.
    */
   private final Collector task;
   
   /**
    * This is the channel object associated with the collector.
    */
   private final Channel channel;
   
   /**
    * Constructor for the <code>Reader</code> object. This requires 
    * a selector and a collector object in order to consume the data
    * from the connected socket which forms a HTTP request entity.
    * 
    * @param source the selector object used to process events
    * @param task this is the task used to collect the entity
    */
   public Reader(Selector source, Collector task){
      this.channel = task.getChannel();
      this.source = source;
      this.task = task;      
   }
   
   /**
    * This is the <code>SocketChannel</code> used to determine if the
    * connection has some bytes that can be read. If it contains any
    * data then that data is read from and is used to compose the 
    * request entity, which consists of a HTTP header and body.
    * 
    * @return this returns the socket for the connected pipeline
    */
   public SocketChannel getChannel() {
      return task.getSocket();
   }

   /**
    * This <code>run</code> method is used to collect the bytes from
    * the connected channel. If a sufficient amount of data is read
    * from the socket to form a HTTP entity then the collector uses
    * the <code>Selector</code> object to dispatch the request. This
    * is sequence of events that occur for each transaction.
    */
   public void run() {
      try {
         task.collect(source);
      }catch(Throwable e){
         cancel();
      } 
   }
   
   /**
    * This is used to cancel the operation if it has timed out. If 
    * the retry is waiting too long to read content from the socket
    * then the retry is canceled and the underlying transport is 
    * closed. This helps to clean up occupied resources.     
    */       
   public void cancel() {
      try {
         channel.close();
      } catch(Throwable e) {
         return;
      }
   }  
}
