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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powertac.samplebroker.grpc.Booly;
import org.powertac.samplebroker.grpc.ServerMessagesStreamGrpc;
import org.powertac.samplebroker.grpc.XmlMessage;
import org.powertac.samplebroker.core.BrokerPropertiesService;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GrpcSocketAdapterTest
{

  private String testXml = "<test/>";
  GrpcSocketAdapter gsa;

  //all dependencies that we mock away
  ServerMessageStreamService messageStreamServiceMock = Mockito.mock(ServerMessageStreamService.class);
  ControlEventHandlerService eventHandlerServiceMock = Mockito.mock(ControlEventHandlerService.class);
  CountDownLatch countDownLatchMock = Mockito.mock(CountDownLatch.class);
  MessageDispatcher md = Mockito.mock(MessageDispatcher.class);
  BrokerPropertiesService ps = Mockito.mock(BrokerPropertiesService.class);

  CountDownLatch lock = new CountDownLatch(1);

  @Before
  public void setUp()
  {
    gsa = new GrpcSocketAdapter();
    ReflectionTestUtils.setField(gsa, "smss", messageStreamServiceMock);
    ReflectionTestUtils.setField(gsa, "cehs", eventHandlerServiceMock);
    ReflectionTestUtils.setField(gsa, "dispatcher", md);
    ReflectionTestUtils.setField(gsa, "propertiesService", ps);
  }

  @After
  public void tearDown()
  {
    if(gsa.getMessageStreamServer() != null)
      {
        gsa.getMessageStreamServer().shutdownNow();
      }
  }

  @Test
  public void initialize()
  {
    try {
      ReflectionTestUtils.setField(gsa, "startSignal", countDownLatchMock);
      when(countDownLatchMock.await(anyLong(),any(TimeUnit.class))).thenReturn(true);
      gsa.initialize(null);
      assertEquals(2, gsa.getMessageStreamServer().getServices().size());
      verify(countDownLatchMock, times(1)).await(anyLong(), any(TimeUnit.class));
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }


  }

  @Test
  public void integration()
  {
    try {
      ServerMessageStreamService realSmss = new ServerMessageStreamService();
      ReflectionTestUtils.setField(realSmss, "dispatcher", md);
      ReflectionTestUtils.setField(gsa, "smss", realSmss);

      //connecting to server first (it allows a bit of a timeout
      connectToServer();
      //initialize, which blocks until a connection has arrived
      gsa.initialize(null);

      lock.await(1000, TimeUnit.MILLISECONDS);
      verify(md, times(1)).sendRawMessage(anyObject());
    }
    catch (InterruptedException ignored) {

    }
  }

  private void connectToServer() throws InterruptedException
  {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", gsa.getPort()).usePlaintext(true).build();
    ServerMessagesStreamGrpc.ServerMessagesStreamStub stub = ServerMessagesStreamGrpc.newStub(channel);
    StreamObserver<XmlMessage> source = stub.registerEventSource(getMockStreamObserver());
    source.onNext(XmlMessage.newBuilder().setRawMessage(testXml).build());
    source.onCompleted();

  }

  @Test
  public void getMessageStreamServer()
  {
  }

  private StreamObserver<Booly> getMockStreamObserver()
  {
    return new StreamObserver<Booly>()
    {
      @Override
      public void onNext(Booly booly)
      {

      }

      @Override
      public void onError(Throwable throwable)
      {

      }

      @Override
      public void onCompleted()
      {

      }
    };
  }
}
