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

import io.grpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.samplebroker.core.BrokerPropertiesService;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.Activatable;
import org.powertac.samplebroker.interfaces.BrokerContext;
import org.powertac.samplebroker.interfaces.Initializable;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.powertac.samplebroker.r.CharStreamAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service("GrpcSocketAdapter")
public class GrpcSocketAdapter implements IpcAdapter, Initializable, Activatable
{

  static private Logger log = LogManager.getLogger(CharStreamAdapter.class);

  @Autowired
  private MessageDispatcher dispatcher;

  @Autowired
  private BrokerPropertiesService propertiesService;

  @Autowired
  private ServerMessageStreamService smss;

  @Autowired
  private ControlEventHandlerService cehs;

  // input and output streams. If null, use stdin/stdout
  @ConfigurableValue(valueType = "Integer",
      description = "Port to listen to")
  private Integer port = 1234;

  private Server messageStreamServer;
  private CountDownLatch startSignal = new CountDownLatch(1);

  boolean clientConnected = false;


  @Override
  public void activate(int timeslot)
  {

  }

  @Override
  public void initialize(BrokerContext broker)
  {

    propertiesService.configureMe(this);
    messageStreamServer = this.messageStreamServerFactory(port);
    try {
      messageStreamServer.start();
      log.info("Listening on port {} for GRPC rpc calls", port);
      //messageStreamServerFactory.awaitTermination();
    }
    catch (IOException e) {
      log.error(e.getMessage());
      log.error("GRPC server caused an error");
    }

    //waiting for GRPC client to connect before connecting to the server. This ensures the client is ready to handle the session.
    try {
      boolean connected = startSignal.await(30 * 1000L, TimeUnit.MILLISECONDS);
      if (connected) {
        log.info("GRPC Client connected. Lock removed");
      }
      else {
        log.error("Client not connected, timing out and exiting");
        System.exit(1);
      }
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * builds a Server object and notifies any waiting lock of a connection
   *
   * @param port
   * @return
   */
  private Server messageStreamServerFactory(Integer port)
  {
    ServerInterceptor interceptor;
    interceptor = new ServerInterceptor()
    {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)
      {
        if (!clientConnected) {
          clientConnected = true;
          startSignal.countDown();
        }
        return next.startCall(call, headers);
      }
    };


    return ServerBuilder
        .forPort(port)
        .addService(smss)
        .addService(cehs)
        .intercept(interceptor)
        .build();
  }

  public Integer getPort()
  {
    return port;
  }

  public Server getMessageStreamServer()
  {
    return messageStreamServer;
  }

  @Override
  public void openStreams()
  {
    smss.initQueue();
  }

  /**
   * This sends a message to the client (server-->here-->client)
   * @param message
   */
  @Override
  public void exportMessage(String message)
  {
    smss.exportMessage(message);
  }

  @Override
  public void startMessageImport()
  {

  }
}
