/*
 * ThreadBuilder.java February 2009
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

/**
 * The <code>ThreadBuilders</code> object is used to build threads 
 * and prefix the thread with a type name. Prefixing the threads with
 * the type that it represents allows the purpose of the thread to
 * be determined and also provides better debug information.
 * 
 * @author Niall Gallagher
 */
class ThreadBuilder {
   
   /**
    * This is used to create a thread from the provided runnable. The
    * thread created will contain a unique name which is prefixed with
    * the type of task it has been created to execute. This provides 
    * some detail as to what the thread should be doing.
    * 
    * @param task this is the task that the thread is to execute
    * 
    * @return this returns a thread that will executed the given task
    */
   public static Thread build(Runnable task) {
      Thread thread = getThread(task);
      String name = getName(task, thread);
      
      if(!thread.isAlive()) {
         thread.setName(name);
      }
      return thread;
   }
   
   /**
    * This is used to create a thread from the provided runnable. The
    * thread created will contain a unique name which is prefixed with
    * the type of task it has been created to execute. This provides 
    * some detail as to what the thread should be doing.
    * 
    * @param task this is the task that the thread is to execute
    * @param type this is the type of object the thread is to execute
    * 
    * @return this returns a thread that will executed the given task
    */
   public static Thread build(Runnable task, Class type) {
      Thread thread = getThread(task);
      String name = getName(type, thread);
      
      if(!thread.isAlive()) {
         thread.setName(name);
      }
      return thread;
   }
   
   /**
    * This will create a thread name that is unique. The thread name
    * is a combination of the original thread name with a prefix
    * of the type of the object that will be running within it.
    * 
    * @param task this is the task to be run within the thread
    * @param thread this is the thread containing the original name
    * 
    * @return this will return the new name of the thread
    */
   private static String getName(Runnable task, Thread thread) {
      Class type = task.getClass();
      
      return getName(type, thread);
   }
   
   /**
    * This will create a thread name that is unique. The thread name
    * is a combination of the original thread name with a prefix
    * of the type of the object that will be running within it.
    * 
    * @param type this is the type of object to be executed
    * @param thread this is the thread containing the original name
    * 
    * @return this will return the new name of the thread
    */
   private static String getName(Class type, Thread thread) {
      String prefix = type.getSimpleName();
      String name = thread.getName();

      return String.format("%s: %s", prefix, name);
   }
   
   /**
    * This is used to create the thread that will be used to execute
    * the provided task. The created thread will be renamed after 
    * it has been created and before it has been started. 
    * 
    * @param task this is the task that is to be executed
    * 
    * @return this returns a thread to execute the given task
    */
   private static Thread getThread(Runnable task) {
      return new Thread(task);
   }
}
