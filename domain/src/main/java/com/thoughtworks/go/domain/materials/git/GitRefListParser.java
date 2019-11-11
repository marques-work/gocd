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

package com.thoughtworks.go.domain.materials.git;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitRefListParser {
    private static final Pattern REF_SPLIT = Pattern.compile("^([0-9a-fA-F]+)\\s+(refs/.+)$");

    private GitRefListParser() {
    }

    public static List<GitNamedRef> parse(List<String> lines) {
        List<GitNamedRef> refs = new ArrayList<>();
        for (String line : lines) {
            final Matcher matcher = REF_SPLIT.matcher(line);
            if (matcher.find()) {
                refs.add(new GitNamedRef(matcher.group(2), matcher.group(1)));
            }
        }
        return refs;
    }
}
