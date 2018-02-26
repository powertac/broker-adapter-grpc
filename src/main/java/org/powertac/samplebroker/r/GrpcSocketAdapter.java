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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.samplebroker.core.BrokerPropertiesService;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.Activatable;
import org.powertac.samplebroker.interfaces.BrokerContext;
import org.powertac.samplebroker.interfaces.Initializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service("GrpcSocket")
public class GrpcSocketAdapter implements Initializable, Activatable {

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

    @Override
    public void activate(int timeslot) {

    }

    @Override
    public void initialize(BrokerContext broker) {

        propertiesService.configureMe(this);
        messageStreamServer = this.messageStreamServer(port);
        try {
            messageStreamServer.start();
            log.info("Listening on port {} for GRPC rpc calls", port);
            //messageStreamServer.awaitTermination();
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("GRPC server caused an error");
        }
    }

    private Server messageStreamServer(Integer port) {
        return ServerBuilder
                .forPort(port)
                .addService(smss)
                .addService(cehs)
                .build();
    }

    public Server getMessageStreamServer() {
        return messageStreamServer;
    }
}
