/*
 * Initiator.java February 2007
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

package org.simpleframework.http.core;

import java.io.IOException;

import org.simpleframework.transport.Channel;

/**
 * The <code>Initiator</code> represents an interface in to the main
 * processor for the server kernel. It is used to read and parse a
 * new HTTP request from the connected socket and dispatch that
 * request to the <code>Container</code> when ready. All channels
 * begin at the initiator, and when a request has finished it is
 * passed back in to the initiator to further process requests from
 * the HTTP pipeline. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Monitor
 */
interface Initiator {
   
   /**
    * This is used to initiate the processing of the channel. Once
    * the channel is passed in to the initiator any bytes ready on
    * the HTTP pipeline will be processed and parsed in to a HTTP
    * request. When the request has been built a callback is made
    * to the <code>Container</code> to process the request. Also
    * when the request is completed the channel is passed back in
    * to the initiator so that the next request can be dealt with.
    * 
    * @param channel the channel to process the request from
    */
   void start(Channel channel) throws IOException;
}
