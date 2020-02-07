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

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRGitMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRScmMaterial;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class BranchContextFactory {
    private BranchContextFactory() {
    }

    private static final Pattern GIT_EXTRACT_SHORT_REF_NAME = Pattern.compile("^refs/[^/]+/(.+)$");

    /**
     * Creates a {@link BranchContext} from a ref and {@link ScmMaterial}
     *
     * @param fullRefName     - the full ref of the branch, tag, etc.
     * @param nameForMaterial - sets the name of the material config, allowing the user to use multiple
     *                        materials in generated pipeline configs
     * @param scm             - the {@link ScmMaterial} representing the base repository from which material configs for matching
     *                        refs will be generated
     * @return a {@link BranchContext} representing a matching ref
     */
    public static BranchContext create(String fullRefName, String nameForMaterial, ScmMaterial scm) {
        final ScmMaterialConfig mat = (ScmMaterialConfig) scm.config();
        mat.ensureEncrypted();

        final String shortName;
        final CRScmMaterial repo;

        // Add more conditions if we decide to support other SCMs
        if (mat instanceof GitMaterialConfig) {
            GitMaterialConfig git = (GitMaterialConfig) mat;
            shortName = toGitShortRef(fullRefName);
            repo = toGitRepo(nameForMaterial, shortName, git);
        } else {
            throw new IllegalArgumentException(format("BranchContextFactory: Material config of type `%s` is not supported.", mat.getType()));
        }

        return new BranchContext(fullRefName, shortName, repo);
    }

    private static String toGitShortRef(String ref) {
        final Matcher matcher = GIT_EXTRACT_SHORT_REF_NAME.matcher(ref);

        if (!matcher.find()) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot extract short name from git ref: `%s`", ref));
        }

        return matcher.group(1);
    }

    private static CRGitMaterial toGitRepo(String name, String branch, GitMaterialConfig git) {
        return new CRGitMaterial(name, "", true, false, git.getUserName(), null, git.getUrl(), branch, true);
    }
}
