/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

@ConfigTag("rake")
public class RakeTask extends BuildTask {
    public static final String TYPE = "rake";

    @Override
    public String getTaskType() {
        return "rake";
    }

    public String getTypeForDisplay() {
        return "Rake";
    }

    public String arguments() {
        ArrayList<String> args = new ArrayList<>();
        if (buildFile != null) {
            args.add("-f \"" + FileUtil.normalizePath(buildFile) + "\"");
        }

        if (target != null) {
            args.add(target);
        }

        return StringUtils.join(args, " ");
    }

    @Override
    public String command() {
        return TYPE;
    }
}
