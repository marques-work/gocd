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

package com.thoughtworks.go.apiv1.pipelineselection.representers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.PipelineSelections;

import java.util.ArrayList;
import java.util.List;

public class PipelineSelectionResponse {
    private final PipelineSelections selectedPipelines;
    private final List<PipelineConfigs> pipelineConfigs;

    public PipelineSelectionResponse(PipelineSelections selectedPipelines, List<PipelineConfigs> pipelineConfigs) {
        this.selectedPipelines = selectedPipelines;
        this.pipelineConfigs = pipelineConfigs;
    }

    public boolean isBlacklist() {
        return selectedPipelines.isBlacklist();
    }

    public List<String> selectedPipelinesList(List<PipelineConfigs> pipelineConfigs) {
        List<String> result = new ArrayList<>();
        for (PipelineConfigs group : pipelineConfigs) {
            for (PipelineConfig pipeline : group) {
                if (selectedPipelines.includesPipeline(pipeline.name())) {
                    result.add(CaseInsensitiveString.str(pipeline.name()));
                }
            }
        }

        return result;
    }

    public List<PipelineConfigs> getPipelineConfigs() {
        return pipelineConfigs;
    }
}
