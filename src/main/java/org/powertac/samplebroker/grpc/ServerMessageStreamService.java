/*
 *  Copyright 2009-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an
 *  "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language
 *  governing permissions and limitations under the License.
 */

package org.powertac.samplebroker.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.samplebroker.grpc.Booly;
import org.powertac.samplebroker.grpc.ServerMessagesStreamGrpc;
import org.powertac.samplebroker.grpc.XmlMessage;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.powertac.samplebroker.r.CharStreamAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service()
public class ServerMessageStreamService extends ServerMessagesStreamGrpc.ServerMessagesStreamImplBase
{

  static private Logger log = LogManager.getLogger(CharStreamAdapter.class);

  @Autowired
  private MessageDispatcher dispatcher;

  private StreamObserver<XmlMessage> toClientObserver;

  private LinkedList<String> toClient;
  private int counter;

  /**
   * Hook for client to get a hold of a stream that needs to be served each incoming xml message from the server
   *
   * @param request
   * @param responseObserver
   */
  @Override
  public void registerListener(Booly request, StreamObserver<XmlMessage> responseObserver)
  {
    this.toClientObserver = responseObserver;
    if (this.toClient != null && this.toClient.size() > 0) {
      this.pushAllToClient(this.toClient);
    }
  }

  /**
   * pop all messages out of list and send to client. Assumes a stable observable is present
   *
   * @param toClient
   */
  private void pushAllToClient(LinkedList<String> toClient)
  {
    while (!toClient.isEmpty()) {
      XmlMessage msg = buildMessageForSocket(toClient.removeFirst());
      toClientObserver.onNext(msg);
    }
  }

  /**
   * exposes the ability to send raw xml messages to the server to a RPC.
   *
   * @param responseObserver input from the client, just filled with a filler Booly
   * @return
   */
  @Override
  public StreamObserver<XmlMessage> registerEventSource(StreamObserver<Booly> responseObserver)
  {
    return new StreamObserver<XmlMessage>()
    {
      @Override
      public void onNext(XmlMessage xmlMessage)
      {
        //send to server
        dispatcher.sendRawMessage(xmlMessage.getRawMessage());
      }

      @Override
      public void onError(Throwable throwable)
      {
        log.error("GRPC Client connection error");
        log.error(throwable.getMessage());
      }

      @Override
      public void onCompleted()
      {
        log.info("GRPC Client closed connection on purpose");
        //now what?
        responseObserver.onCompleted();
      }
    };

  }

  /**
   * Allows the JVM side of the client to keep the connection to the  server and receive messages while the GRPC endpoint
   * might not be connected temporarily.
   */
  public void initQueue()
  {
    this.toClient = new LinkedList<>();
    this.counter = 0;
  }

  /**
   * push directly if available otherwise add to list and wait for later
   *
   * @param message to send to broker client
   */
  protected void exportMessage(String message)
  {
    if (toClientObserver == null) {
      toClient.add(message);
    }
    else {
      XmlMessage xmlMessage = buildMessageForSocket(message);
      toClientObserver.onNext(xmlMessage);
    }
  }

  private XmlMessage buildMessageForSocket(String message)
  {
    return XmlMessage.newBuilder()
        .setCounter(++counter)
        .setRawMessage(message)
        .setParsedInJava(false)
        .build();
  }

}
