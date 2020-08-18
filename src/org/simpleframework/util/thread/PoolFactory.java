/*
 * PoolFactory.java February 2009
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

import static org.simpleframework.util.thread.ThreadBuilder.build;

import java.util.concurrent.ThreadFactory;

/**
 * The <code>PoolFactory</code> object is used to create a thread
 * factory that will instantiate named threads. This provides a
 * means to name the thread started using the type of task the
 * pool is required to execute.
 * 
 * @author Niall Gallagher
 */
class PoolFactory implements ThreadFactory {
   
   /**
    * This is the type of the task this pool will execute.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>PoolFactory</code> object. This 
    * will provide a thread factory that names the threads based 
    * on the type of <code>Runnable</code> the pool executes. Each
    * of the threads is given a unique sequence number.
    * 
    * @param type this is the type of runnable this will execute
    */
   public PoolFactory(Class type) {
      this.type = type;
   }

   /**
    * This is used to create a new thread. The new thread will be
    * given the simple name of the <code>Runnable</code> class that 
    * it accepts. Each thread is also given a unique sequence.
    * 
    * @param task this is the worker that the thread pool uses
    *  
    * @return this returns the thread that is to be used by this 
    */
   public Thread newThread(Runnable task) {
      return build(task, type);
   }
}
