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

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.List;

public class BlacklistFilter implements DashboardFilter {
    private final String name;
    private final List<CaseInsensitiveString> pipelines;

    public BlacklistFilter(String name, List<CaseInsensitiveString> pipelines) {
        this.name = name;
        this.pipelines = pipelines;
    }

    public List<CaseInsensitiveString> pipelines() {
        return pipelines;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isPipelineVisible(CaseInsensitiveString pipeline) {
        return null == pipelines || !pipelines.contains(pipeline);
    }
}
