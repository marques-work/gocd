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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.plugins.processor.configrepo.SelectBranchesRequestProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.server.service.plugins.processor.configrepo.SelectBranchesRequestProcessor.SELECT_BRANCHES;
import static com.thoughtworks.go.server.service.plugins.processor.configrepo.SelectBranchesRequestProcessor.VERSION_1;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SelectBranchesRequestProcessorV1Test {
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    @Mock
    private MaterialService materialService;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void processRespondsWithListOfBranchContexts() {
        final SelectBranchesRequestProcessor processor = new SelectBranchesRequestProcessor(pluginRequestProcessorRegistry, materialService);
        final DefaultGoApiRequest req = new DefaultGoApiRequest(SELECT_BRANCHES, VERSION_1, null);
        final Gson gson = new GsonBuilder().create();

        final Map<String, String> payload = new HashMap<>();
        payload.put("url", "https://foohub.com/repo.git");
        payload.put("pattern", "^refs/[^/]+/f.+");
        req.setRequestBody(gson.toJson(payload));

        when(materialService.branchesMatching(any(ScmMaterial.class), any(Pattern.class))).thenReturn(asList("refs/heads/foo", "refs/heads/fuu"));
        final GoApiResponse response = processor.process(pluginDescriptor, req);

        assertEquals(SUCCESS_RESPONSE_CODE, response.responseCode());

        final List<Map<String, Object>> result = gson.fromJson(response.responseBody(), new TypeToken<List<Map<String, Object>>>() {
        }.getType());
        assertNotNull(result);
        assertEquals(2, result.size());

        final Map<String, Object> c1 = result.get(0);
        final Map<String, Object> c2 = result.get(1);

        assertTrue(c1.containsKey("branch_name"));
        assertEquals("foo", c1.get("branch_name"));

        assertTrue(c1.containsKey("sanitized_branch_name"));
        assertEquals("foo", c1.get("sanitized_branch_name"));

        assertTrue(c1.containsKey("full_ref_name"));
        assertEquals("refs/heads/foo", c1.get("full_ref_name"));

        assertTrue(c1.containsKey("repo"));
        assertEquals("https://foohub.com/repo.git", ((Map<String, Object>) c1.get("repo")).get("url"));

        assertTrue(c2.containsKey("branch_name"));
        assertEquals("fuu", c2.get("branch_name"));

        assertTrue(c2.containsKey("sanitized_branch_name"));
        assertEquals("fuu", c2.get("sanitized_branch_name"));

        assertTrue(c2.containsKey("full_ref_name"));
        assertEquals("refs/heads/fuu", c2.get("full_ref_name"));

        assertTrue(c2.containsKey("repo"));
        assertEquals("https://foohub.com/repo.git", ((Map<String, Object>) c2.get("repo")).get("url"));
    }
}
