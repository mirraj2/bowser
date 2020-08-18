/*
 * PoolQueue.java February 2007
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

package org.simpleframework.util.thread;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The <code>PoolQueue</code> object is used to execute tasks in
 * a thread pool. This creates a thread pool with an unbounded list
 * of outstanding tasks, which ensures that any system requesting
 * a task to be executed will not block when handing it over.
 * 
 * @author Niall Gallagher
 */
class PoolQueue extends ThreadPoolExecutor {
   
   /**
    * Constructor for the <code>PoolQueue</code> object. This is
    * used to create a pool of threads that can be used to execute
    * arbitrary <code>Runnable</code> tasks. If the threads are
    * busy this will simply enqueue the tasks and return.
    * 
    * @param type this is the type of runnable that this accepts
    * @param rest this is the number of threads to use in the pool    
    * @param active this is the maximum size the pool can grow to 
    */    
   public PoolQueue(Class type, int rest, int active) {
      this(type, rest, active, 120, TimeUnit.SECONDS);
   }
  
   /**
    * Constructor for the <code>PoolQueue</code> object. This is
    * used to create a pool of threads that can be used to execute
    * arbitrary <code>Runnable</code> tasks. If the threads are
    * busy this will simply enqueue the tasks and return.
    *
    * @param type this is the type of runnable that this accepts
    * @param rest this is the number of threads to use in the pool    
    * @param active this is the maximum size the pool can grow to
    * @param duration the duration active threads remain idle for
    * @param unit this is the time unit used for the duration 
    */    
   public PoolQueue(Class type, int rest, int active, long duration, TimeUnit unit) {
      super(rest, active, duration, unit, new Queue(), new PoolFactory(type));
   }
   
   /**
    * This is used to wait until such time as the pool has terminated.
    * Using a join such as this allows the user to be sure that there
    * are no further tasks enqueued for execution and there are no
    * tasks currently executing. This helps provide graceful shutdown.
    */   
   public void join() {
      boolean dead = isTerminated();
   
      while(!dead) {
         try {
            dead = awaitTermination(10, SECONDS);
         } catch(InterruptedException e) {
            break;
         }
      }
   }
   
   /**
    * This is used to stop the executor by interrupting all running
    * tasks and shutting down the threads within the pool. This will
    * return once it has been stopped, and no further tasks will be 
    * accepted by this pool for execution.
    */   
   public void stop() {
      shutdown();
      join();
   }
   
   /**
    * This is the internal queue used by this implementation. This
    * provides an unlimited number of positions for new tasks to
    * be queued. Having an unlimited queue prevents deadlocks.
    * 
    * @author Niall Gallagher
    */
   private static class Queue extends LinkedBlockingQueue<Runnable> {
      
      /**
       * Constructor for the <code>Queue</code> object. This will
       * create a linked blocking queue with an unlimited capacity.
       */
      public Queue() {
         super();
      }
   }
}
