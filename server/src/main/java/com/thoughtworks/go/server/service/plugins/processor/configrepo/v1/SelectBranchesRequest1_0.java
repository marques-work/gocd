/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.configrepo.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.MessageHandlerForSelectBranchesRequestProcessor;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.PatternDeserializer;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.PatternSerializer;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.SelectBranchesRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class SelectBranchesRequest1_0 implements SelectBranchesRequest, MessageHandlerForSelectBranchesRequestProcessor {
    private static final Gson GSON = new GsonBuilder().
            excludeFieldsWithoutExposeAnnotation().
            registerTypeAdapter(Pattern.class, new PatternDeserializer()).
            registerTypeAdapter(Pattern.class, new PatternSerializer()).
            create();

    public static SelectBranchesRequest1_0 fromJSON(String json) {
        SelectBranchesRequest1_0 req = GSON.fromJson(json, SelectBranchesRequest1_0.class);

        if (StringUtils.isBlank(req.url())) {
            throw new JsonParseException("\"url\" is a required field");
        }

        if (null == req.pattern()) {
            throw new JsonParseException("\"pattern\" is a required field");
        }

        return req;
    }

    @Expose
    private String url;

    @Expose
    private Pattern pattern;

    @Expose
    private String username;

    @Expose
    private String password;

    @Override
    public String url() {
        return url;
    }

    @Override
    public Pattern pattern() {
        return pattern;
    }

    @Override
    public ScmMaterial toMaterial() {
        GitMaterial m = new GitMaterial(url, true);

        if (StringUtils.isNotBlank(username)) {
            m.setUserName(username);
        }

        if (StringUtils.isNotBlank(password)) {
            m.setPassword(password);
        }

        return m;
    }

    @Override
    public SelectBranchesRequest deserialize(String requestBody) {
        return SelectBranchesRequest1_0.fromJSON(requestBody);
    }
}
