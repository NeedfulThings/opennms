/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.apilayer.pollers;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.opennms.integration.api.v1.pollers.PollerRequestBuilder;
import org.opennms.integration.api.v1.pollers.PollerResult;
import org.opennms.integration.api.v1.pollers.Status;
import org.opennms.netmgt.poller.LocationAwarePollerClient;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerResponse;
import org.opennms.netmgt.poller.support.SimpleMonitoredService;

/**
 * Builder Implementation for {@link org.opennms.integration.api.v1.pollers.ServicePollerClient}
 */
public class PollerRequestBuilderImpl implements PollerRequestBuilder {

    public PollerRequestBuilderImpl(LocationAwarePollerClient pollerClient) {
        this.pollerClient = pollerClient;
    }

    private String className;

    private InetAddress address;

    private String serviceName;

    private Long ttlInMs;

    private Map<String, String> attributes = new HashMap<>();

    private final LocationAwarePollerClient pollerClient;


    @Override
    public PollerRequestBuilder withPollerClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public PollerRequestBuilder withAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public PollerRequestBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    public PollerRequestBuilder withAttribute(String key, String value) {
        this.attributes.put(key, value);
        return this;
    }

    @Override
    public PollerRequestBuilder withAttributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public PollerRequestBuilder withTimeToLive(Long ttlInMs) {
        this.ttlInMs = ttlInMs;
        return this;
    }

    @Override
    public CompletableFuture<PollerResult> execute() {
        Map<String, Object> props = new HashMap<>();
        attributes.forEach(props::put);
        MonitoredService service = new SimpleMonitoredService(address, serviceName);
        CompletableFuture<PollerResponse> future = pollerClient.poll()
                .withService(service)
                .withMonitorClassName(className)
                .withTimeToLive(ttlInMs)
                .withAttributes(props)
                .execute();
        // convert the response to PollResult.
        CompletableFuture<PollerResult> result = new CompletableFuture<>();
        PollStatus pollStatus;
        try {
            PollerResponse pollerResponse = future.get();
            pollStatus = pollerResponse.getPollStatus();
            switch (Status.valueOf(pollStatus.getStatusName())) {
                case Up:
                    result.complete(new PollerResultImpl(Status.Up, pollStatus.getProperties()));
                    break;
                case Down:
                    result.complete(new PollerResultImpl(Status.Down, pollStatus.getReason()));
                    break;
                case Unknown:
                    result.complete(new PollerResultImpl(Status.Unknown, pollStatus.getReason()));
                    break;
                case UnResponsive:
                    result.complete(new PollerResultImpl(Status.UnResponsive, pollStatus.getReason()));
                    break;
                default:
                    result.complete(new PollerResultImpl(Status.Unknown, pollStatus.getReason()));
            }
        } catch (InterruptedException | ExecutionException e) {
           result.complete(new PollerResultImpl(Status.Unknown, e.getMessage()));
        }
        return result;
    }

    private class PollerResultImpl implements PollerResult {

        private final Status status;
        private String reason;
        private Map<String, Number> properties;

        private PollerResultImpl(Status status) {
            this.status = status;
        }

        private PollerResultImpl(Status status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        public PollerResultImpl(Status status, Map<String, Number> properties) {
            this.status = status;
            this.properties = properties;
        }

        @Override
        public Status getStatus() {
            return this.status;
        }

        @Override
        public String getReason() {
            return this.reason;
        }

        @Override
        public Map<String, Number> getProperties() {
            return this.properties;
        }
    }
}
