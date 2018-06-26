/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.pipelineselection;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionResponse;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionsRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.NamedSubSelection;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.SystemEnvironment;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

public class PipelineSelectionControllerDelegate extends ApiController {
    private static final int ONE_YEAR = 3600 * 24 * 365;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineSelectionsService pipelineSelectionsService;
    private final PipelineConfigService pipelineConfigService;
    private final SystemEnvironment systemEnvironment;

    public PipelineSelectionControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper,
                                               PipelineSelectionsService pipelineSelectionsService,
                                               PipelineConfigService pipelineConfigService,
                                               SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineSelectionsService = pipelineSelectionsService;
        this.pipelineConfigService = pipelineConfigService;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineSelection.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            get("", mimeType, this::show);
            put("", mimeType, this::update);
        });
    }

    public String show(Request request, Response response) throws IOException {
        String fromCookie = request.cookie("selected_pipelines");
        final String filterName = request.queryParams("viewName");

        PipelineSelections selectedPipelines = pipelineSelectionsService.getPersistedSelectedPipelines(fromCookie, currentUserId(request));

        selectedPipelines = new NamedSubSelection(selectedPipelines, filterName);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsFor(currentUsername());

        PipelineSelectionResponse pipelineSelectionResponse = new PipelineSelectionResponse(selectedPipelines, pipelineConfigs);

        return writerForTopLevelObject(request, response, writer -> PipelineSelectionsRepresenter.toJSON(writer, pipelineSelectionResponse));
    }

    public String update(Request request, Response response) {
        String fromCookie = request.cookie("selected_pipelines");
        final String filterName = request.queryParams("viewName");

        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        PipelineSelectionResponse selectionResponse = PipelineSelectionsRepresenter.fromJSON(jsonReader);

        Long recordId = pipelineSelectionsService.persistSelectedPipelines(fromCookie, currentUserId(request), filterName, selectionResponse.selectedPipelinesList(), selectionResponse.isBlacklist());

        if (!apiAuthenticationHelper.securityEnabled()) {
            response.cookie("/go", "selected_pipelines", String.valueOf(recordId), ONE_YEAR, systemEnvironment.isSessionCookieSecure(), true);
        }

        response.status(204);
        return NOTHING;
    }
}
