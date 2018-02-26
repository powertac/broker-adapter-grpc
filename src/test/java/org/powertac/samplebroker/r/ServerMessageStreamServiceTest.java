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

package org.powertac.samplebroker.r;

import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powertac.broker.adapter.grpc.Booly;
import org.powertac.broker.adapter.grpc.XmlMessage;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.*;

public class ServerMessageStreamServiceTest {

    ServerMessageStreamService smss;

    private String testXml = "<test/>";

    StreamObserver<XmlMessage> xso;
    StreamObserver<Booly> bso;
    MessageDispatcher md;

    @Before
    public void setUp() throws Exception {
        xso = Mockito.mock(StreamObserver.class);
        bso = Mockito.mock(StreamObserver.class);
        md = Mockito.mock(MessageDispatcher.class);

        smss = new ServerMessageStreamService();
        smss.openStreams();
    }

    @Test
    public void registerListener() {

        smss.registerListener(Booly.newBuilder().build(), xso);
        smss.exportMessage(testXml);

        verify(xso, times(1)).onNext(anyObject());
    }

    @Test
    public void registerEventSource() {
        StreamObserver<XmlMessage> x = smss.registerEventSource(bso);
        ReflectionTestUtils.setField(smss, "dispatcher", md);
        x.onNext(XmlMessage.newBuilder().setRawMessage(testXml).build());
        verify(md, times(1)).sendRawMessage(anyObject());
    }

    @Test
    public void openStreams() {
        smss.exportMessage(testXml);
        LinkedList<String> msgL = (LinkedList<String>) ReflectionTestUtils.getField(smss, "toClient");
        assertEquals(testXml, msgL.removeFirst());
    }

    @Test
    public void exportMessage() {
        smss.registerListener(Booly.newBuilder().build(), xso);
        smss.exportMessage(testXml);
        verify(xso, times(1)).onNext(any(XmlMessage.class));
    }


}