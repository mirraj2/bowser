/*
 * Daemon.java February 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.util.thread;

import static java.lang.Thread.State.NEW;
import static org.simpleframework.util.thread.ThreadBuilder.build;

import java.lang.Thread.State;

/**
 * The <code>Daemon</code> object provides a named daemon thread
 * which will execute the <code>run</code> method when started. 
 * This offers some convenience in that it hides the normal thread
 * methods and also allows the object extending this to provide
 * the name of the internal thread, which is given an incrementing
 * sequence number appended to the name provided.
 * 
 * @author Niall Gallagher
 */
public abstract class Daemon implements Runnable {
   
   /**
    * This is the internal thread used by this daemon instance.
    */
   private final Thread thread;
   
   /**
    * Constructor for the <code>Daemon</code> object. This will 
    * create the internal thread and ensure it is a daemon. When it
    * is started the name of the internal thread is set using the
    * name of the instance as taken from <code>getName</code>. If
    * the name provided is null then no name is set for the thread.
    */
   protected Daemon() {
      this.thread = build(this);
   }
   
   /**
    * This is used to start the internal thread. Once started the
    * internal thread will execute the <code>run</code> method of
    * this instance. Aside from starting the thread this will also
    * ensure the internal thread has a unique name.
    */
   public void start() {
      State state = thread.getState();
      
      if(state == NEW) {
         thread.start();
      }
   }
   
   /**
    * This is used to interrupt the internal thread. This is used
    * when there is a need to wake the thread from a sleeping or
    * waiting state so that some other operation can be performed.
    * Typically this is required when killing the thread.
    */
   public void interrupt() {
      thread.interrupt();
   }
   
   /**
    * This is used to join with the internal thread of this daemon.
    * Rather than exposing the internal thread a <code>join</code>
    * method is provided. This allows asynchronous threads to wait
    * for the daemon to complete simulating synchronous action.
    * 
    * @throws InterruptedException if the thread is interrupted
    */
   public void join() throws InterruptedException {
      thread.join();
   }
}
