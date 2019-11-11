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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectBranchesRequest1_0Test {
    @Test
    void fromJSON() {
        SelectBranchesRequest1_0 req = SelectBranchesRequest1_0.fromJSON("{" +
                "\"url\": \"https://github.com/gocd/gocd.git\"," +
                "\"pattern\": \"^refs/.+\"" +
                "}");

        assertNotNull(req.pattern());
        assertEquals("^refs/.+", req.pattern().pattern());

        assertNotNull(req.url());
        assertEquals("https://github.com/gocd/gocd.git", req.url());
    }

    @Test
    void toMaterial() {
        SelectBranchesRequest1_0 req = SelectBranchesRequest1_0.fromJSON("{" +
                "\"url\": \"https://github.com/gocd/gocd.git\"," +
                "\"pattern\": \"^refs/.+\"," +
                "\"username\": \"foo\"," +
                "\"password\": \"bar\"" +
                "}");

        Material material = req.toMaterial();

        assertNotNull(material);
        assertTrue(material instanceof GitMaterial);

        assertEquals("https://foo:bar@github.com/gocd/gocd.git", ((GitMaterial) material).urlForCommandLine());
    }
}