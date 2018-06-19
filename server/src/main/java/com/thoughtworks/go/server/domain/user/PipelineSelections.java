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

package com.thoughtworks.go.server.domain.user;

import com.google.gson.Gson;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.domain.PersistentObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.*;

public class PipelineSelections extends PersistentObject implements Serializable {
    private static final Gson GSON = new Gson();
    private static int CURRENT_SCHEMA_VERSION = 1;

    private Date lastUpdate;
    public static final PipelineSelections ALL = new PipelineSelections() {
        @Override public boolean includesGroup(PipelineConfigs group) {
            return true;
        }

        @Override public boolean includesPipeline(PipelineConfig pipeline) {
            return true;
        }
    };

    private List<String> pipelines;
    private Long userId;
    private List<CaseInsensitiveString> caseInsensitivePipelineList = new ArrayList<>();
    private boolean isBlacklist;
    private Filters viewFilters;
    private int version;

    public String getFilters() {
        return Filters.toJson(this.viewFilters);
    }

    public void setFilters(String filters) {
        this.viewFilters = Filters.fromJson(filters);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean needsUpgrade() {
        return CURRENT_SCHEMA_VERSION > version;
    }

    public PipelineSelections() {
        this(new ArrayList<>());
    }

    public PipelineSelections(List<String> unselectedPipelines) {
        this(unselectedPipelines, new Date(), null, true);
    }

    public PipelineSelections(List<String> unselectedPipelines, Date date, Long userId, boolean isBlacklist) {
        update(unselectedPipelines, date, userId, isBlacklist);
    }

    public Date lastUpdated() {
        return lastUpdate;
    }

    public void update(List<String> selections, Date date, Long userId, boolean isBlacklist) {
        this.userId = userId;
        this.isBlacklist = isBlacklist;
        updateSelections(selections);
        this.lastUpdate = date;
    }

    public boolean includesGroup(PipelineConfigs group) {
        for (PipelineConfig pipelineConfig : group) {
            if (!includesPipeline(pipelineConfig)) {
                return false;
            }
        }
        return true;
    }

    public boolean includesPipeline(PipelineConfig pipeline) {
        return includesPipeline(pipeline.name());
    }

    public boolean includesPipeline(String pipelineName) {
        return includesPipeline(new CaseInsensitiveString(pipelineName));
    }

    public boolean includesPipeline(CaseInsensitiveString pipelineName) {
        boolean isInCurrentSelection = caseInsensitivePipelineList().contains(pipelineName);

        if (isBlacklist) {
            return !isInCurrentSelection;
        }
        return isInCurrentSelection;
    }

    public List<String> pipelineList() {
        return pipelines;
    }

    private List<CaseInsensitiveString> caseInsensitivePipelineList() {
        return caseInsensitivePipelineList;
    }


    public String getSelections() {
        return StringUtils.join(pipelineList(), ",");
    }

    private void setSelections(String unselectedPipelines) {
        this.pipelines = Arrays.asList(StringUtils.split(unselectedPipelines, ","));
        List<CaseInsensitiveString> pipelineList = new ArrayList<>();
        for (String pipeline : pipelines) {
            pipelineList.add(new CaseInsensitiveString(pipeline));
        }
        this.caseInsensitivePipelineList = pipelineList;
    }

    public static PipelineSelections singleSelection(final String pipelineName) {
        return new PipelineSelections() {
            @Override public boolean includesPipeline(PipelineConfig pipeline) {
                return compare(pipelineName, CaseInsensitiveString.str(pipeline.name()));
            }

            @Override public boolean includesPipeline(String pipeline) {
                return compare(pipelineName, pipeline);
            }

            @Override public boolean includesGroup(PipelineConfigs group) {
                return true;
            }

            private boolean compare(String pipelineName, String name) {
                return name.equalsIgnoreCase(pipelineName);
            }
        };
    }

    public Long userId() {
        return userId;
    }

    public boolean isBlacklist() {
        return isBlacklist;
    }

    public PipelineSelections upgrade() {
        ArrayList<DashboardFilter> views = new ArrayList<>();

        this.viewFilters = new Filters(views);

        views.add(isBlacklist ?
                new BlacklistFilter(null, null, caseInsensitivePipelineList) :
                new WhitelistFilter(null, null, caseInsensitivePipelineList)
        );

        this.version = CURRENT_SCHEMA_VERSION;

        return this;
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public PipelineSelections addPipelineToSelections(CaseInsensitiveString pipelineToAdd) {
        ArrayList<String> updatedListOfPipelines = new ArrayList<>();
        updatedListOfPipelines.addAll(pipelines);
        updatedListOfPipelines.add(CaseInsensitiveString.str(pipelineToAdd));

        this.updateSelections(updatedListOfPipelines);
        return this;
    }

    private void updateSelections(List<String> selections) {
        this.setSelections(StringUtils.join(selections, ","));
    }
}
