/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv1.internalagent.representers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.remote.adapter.RuntimeTypeAdapterFactory;
import com.thoughtworks.go.remote.request.ReportCompleteStatusRequest;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;

public class ReportCompleteStatusRequestRepresenter {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(agentRuntimeInfoAdapter())
            .create();

    public static ReportCompleteStatusRequest fromJSON(String request) {
        return gson.fromJson(request, ReportCompleteStatusRequest.class);
    }

    private static RuntimeTypeAdapterFactory<AgentRuntimeInfo> agentRuntimeInfoAdapter() {
        return RuntimeTypeAdapterFactory.of(AgentRuntimeInfo.class, "type")
                .registerSubtype(AgentRuntimeInfo.class, "AgentRuntimeInfo")
                .registerSubtype(ElasticAgentRuntimeInfo.class, "ElasticAgentRuntimeInfo");
    }
}