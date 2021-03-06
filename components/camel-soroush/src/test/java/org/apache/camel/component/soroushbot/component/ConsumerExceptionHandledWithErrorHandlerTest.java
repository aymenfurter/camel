/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.soroushbot.component;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.soroushbot.models.SoroushAction;
import org.apache.camel.component.soroushbot.models.SoroushMessage;
import org.apache.camel.component.soroushbot.support.SoroushBotTestSupport;
import org.apache.camel.component.soroushbot.utils.CongestionException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Flaky test")
public class ConsumerExceptionHandledWithErrorHandlerTest extends SoroushBotTestSupport {
    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                onException(CongestionException.class).process(exchange -> {
                    SoroushMessage originalMessage = exchange.getProperty("OriginalMessage", SoroushMessage.class);
                    if (originalMessage == null || exchange.getIn().getBody(SoroushMessage.class) != null) {
                        exchange.setProperty(Exchange.ROUTE_STOP, true);
                    }
                }).handled(true).to("mock:exceptionRoute");
                from("soroush://" + SoroushAction.getMessage
                     + "/7?concurrentConsumers=2&queueCapacityPerThread=1&bridgeErrorHandler=true")
                             .process(exchange -> Thread.sleep(1000))
                             .to("mock:mainRoute");

            }
        };
    }

    @Test
    public void checkIfMessageGoesToExceptionRoute() throws InterruptedException {
        MockEndpoint exceptionEndpoint = getMockEndpoint("mock:exceptionRoute");
        MockEndpoint mainEndPoint = getMockEndpoint("mock:mainRoute");
        exceptionEndpoint.setExpectedCount(3);
        mainEndPoint.setExpectedCount(4);
        exceptionEndpoint.assertIsSatisfied();
        mainEndPoint.assertIsSatisfied();
    }
}
