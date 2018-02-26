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
import org.powertac.broker.adapter.grpc.Booly;
import org.powertac.broker.adapter.grpc.ControlEvent;
import org.powertac.broker.adapter.grpc.ControlEventHandlerGrpc;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.BrokerContext;
import org.powertac.samplebroker.interfaces.Initializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControlEventHandlerService extends ControlEventHandlerGrpc.ControlEventHandlerImplBase implements Initializable {

    @Autowired
    MessageDispatcher dispatcher;
    private BrokerContext broker;

    /**
     * takes controlEvents from the client and triggers pause/release with server.
     * @param request
     * @param responseObserver
     */
    @Override
    public void submitControlEvent(ControlEvent request, StreamObserver<Booly> responseObserver) {
        switch (request.getValueValue()) {
            case 0:
                //todo, took this from the CharStreamAdapter but idk what it's for tbh
                break;
            case 1:
                PauseRequest pause = new PauseRequest(broker.getBroker());
                dispatcher.sendMessage(pause);
                break;
            case 2:
                PauseRelease release = new PauseRelease(broker.getBroker());
                dispatcher.sendMessage(release);
                break;
            default:
                break;
        }
    }

    @Override
    public void initialize(BrokerContext broker) {
        this.broker = broker;
    }
}
