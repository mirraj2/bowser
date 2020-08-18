/*
 * SocketFlusher.java February 2007
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

package org.simpleframework.transport;

import static org.simpleframework.transport.TransportEvent.ERROR;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.simpleframework.transport.reactor.Operation;
import org.simpleframework.transport.reactor.Reactor;
import org.simpleframework.transport.trace.Trace;

/**
 * The <code>SocketFlusher</code> flushes bytes to the underlying
 * socket channel. This allows asynchronous writes to the socket
 * to be managed in such a way that there is order to the way data
 * is delivered over the socket. This uses a selector to dispatch
 * flush invocations to the underlying socket when the socket is
 * write ready. This allows the writing thread to continue without
 * having to wait for all the data to be written to the socket.
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.transport.PacketController
 */
class SocketFlusher implements PacketFlusher {
   
   /**
    * This is the writer used to queue the packets written.
    */
   private PacketWriter writer;
   
   /**
    * This is the signaller used to determine when to flush.
    */
   private Signaller signaller;
   
   /**
    * This is the scheduler used to block and signal the writer.
    */
   private Scheduler scheduler;
   
   /**
    * This is the trace object used to monitor flushing events.
    */
   private Trace trace;
   
   /**
    * This is used to determine if the socket flusher is closed.
    */
   private boolean closed;
   
   /**
    * Constructor for the <code>SocketFlusher</code> object. This is
    * used to flush buffers to the underlying socket asynchronously.
    * When finished flushing all of the buffered data this signals
    * any threads that are blocking waiting for the write to finish.
    *
    * @param reactor this is used to perform asynchronous writes
    * @param writer this is used to write the buffered packets
    */
   public SocketFlusher(Socket socket, Reactor reactor, PacketWriter writer) throws IOException {
      this.signaller = new Signaller(writer);
      this.scheduler = new Scheduler(socket, reactor, signaller, this);
      this.trace = socket.getTrace();
      this.writer = writer;
   }

   /**
    * Here in this method we schedule a flush when the underlying
    * writer is write ready. This allows the writer thread to return
    * without having to fully flush the content to the underlying
    * transport. If there are references queued this will block.
    */  
   public synchronized void flush() throws IOException { 
      if(closed) {
         throw new TransportException("Flusher is closed");
      }
      boolean block = writer.isBlocking();

      if(!closed) {
         scheduler.schedule(block);
      }
   }
   
   /**
    * This is executed when the flusher is to write all of the data to
    * the underlying socket. In this situation the writes are attempted
    * in a non blocking way, if the task does not complete then this
    * will simply enqueue the writing task for OP_WRITE and leave the
    * method. This returns true if all the buffers are written.
    */   
   private synchronized void execute() throws IOException {      
      boolean ready = writer.flush(); 

      if(!ready) { 
         boolean block = writer.isBlocking(); 

         if(!block && !closed) {
            scheduler.release(); 
         }
         scheduler.repeat();
      } else{
         scheduler.ready();
      }
   }
   
   /**
    * This is used to abort the flushing process when the reactor has
    * been stopped. An abort to the flusher typically happens when the
    * server has been shutdown. It prevents threads lingering waiting
    * for a I/O operation which prevents the server from shutting down.
    */
   private synchronized void abort() throws IOException {
      scheduler.close();
      writer.close();
   }
   
   /**
    * This is used to close the flusher ensuring that all of the
    * data within the writer will be flushed regardless of the 
    * amount of data within the writer that needs to be written. If
    * the writer does not block then this waits to be finished.
    */
   public synchronized void close() throws IOException {
      boolean ready = writer.flush();
      
      if(!closed) {
         closed = true;
      }
      if(!ready) {
         scheduler.schedule(true); 
      }
   }
   
   /**
    * The <code>Signaller</code> is an operation that performs the
    * write operation asynchronously. This will basically determine
    * if the socket is write ready and drain each queued buffer to
    * the socket until there are no more pending buffers.
    */
   private class Signaller implements Operation {
      
      /**
       * This is the writer that is used to write the data.
       */
      private final PacketWriter writer;
      
      /**
       * Constructor for the <code>Signaller</code> object. This will
       * create an operation that is used to flush the packet queue
       * to the underlying socket. This ensures that the data is
       * written to the socket in the queued order.
       *
       * @param writer this is the writer to flush the data to
       */
      public Signaller(PacketWriter writer) {
         this.writer = writer;
      }
      
      /**
       * This returns the socket channel for the connected pipeline. It
       * is this channel that is used to determine if there are bytes
       * that can be written. When closed this is no longer selectable.
       *
       * @return this returns the connected channel for the pipeline
       */
      public SocketChannel getChannel() {
         return writer.getChannel();
      }

      /**
       * This is used to perform the drain of the pending buffer
       * queue. This will drain each pending queue if the socket is
       * write ready. If the socket is not write ready the operation
       * is enqueued for selection and this returns. This ensures
       * that all the data will eventually be delivered.
       */
      public void run() {
         try {
            execute();
         } catch(Exception cause) {
            trace.trace(ERROR, cause);
            cancel();
         }
      }
      
      /**
       * This is used to cancel the operation if it has timed out.
       * If the delegate is waiting too long to flush the contents
       * of the buffers to the underlying transport then the socket
       * is closed and the flusher times out to avoid deadlock.
       */
      public void cancel() {
         try {
            abort();
         }catch(Exception cause){
            trace.trace(ERROR, cause);
         }
      }
   }    
}
