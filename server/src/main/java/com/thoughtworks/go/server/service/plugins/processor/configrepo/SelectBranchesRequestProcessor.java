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

package com.thoughtworks.go.server.service.plugins.processor.configrepo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.v1.SelectBranchesRequest1_0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

@Component
public class SelectBranchesRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectBranchesRequestProcessor.class);
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static final String VERSION_1 = "1.0";
    public static final String SELECT_BRANCHES = "go.processor.configrepo.select-branches";

    private static final List<String> supportedVersions = Collections.singletonList(VERSION_1);
    public static final String DEFAULT_MATERIAL_NAME = "repo";


    private final MaterialService service;
    private final Map<String, MessageHandlerForSelectBranchesRequestProcessor> handlers;

    @Autowired
    public SelectBranchesRequestProcessor(PluginRequestProcessorRegistry registry, MaterialService service) {
        this.service = service;

        handlers = Collections.singletonMap(VERSION_1, new SelectBranchesRequest1_0());
        registry.registerProcessorFor(SELECT_BRANCHES, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        try {
            validatePluginRequest(request);

            final MessageHandlerForSelectBranchesRequestProcessor handler = handlers.get(request.apiVersion());
            final SelectBranchesRequest req = handler.deserialize(request.requestBody());
            final ScmMaterial scm = req.toMaterial();

            final List<BranchContext> results = service.refsMatching(scm, req.pattern()).stream().
                    map((ref) -> BranchContextFactory.create(ref, DEFAULT_MATERIAL_NAME, scm)).
                    collect(Collectors.toList());

            return DefaultGoApiResponse.success(GSON.toJson(results));
        } catch (Exception e) {
            DefaultGoApiResponse response = new DefaultGoApiResponse(DefaultGoApiResponse.INTERNAL_ERROR);
            response.setResponseBody(format("'{' \"message\": \"Error: {0}\" '}'", e.getMessage()));
            LOGGER.warn("Failed to handle message from plugin {}: {}", pluginDescriptor.id(), request.requestBody(), e);
            return response;
        }
    }

    private void validatePluginRequest(GoApiRequest goPluginApiRequest) {
        if (!supportedVersions.contains(goPluginApiRequest.apiVersion())) {
            throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s",
                    goPluginApiRequest.api(), goPluginApiRequest.apiVersion(), supportedVersions));
        }
    }
}
