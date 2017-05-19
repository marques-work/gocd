/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPropertiesGenerator;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.util.command.ConsoleLogTags.*;

public class BuildComposer {
    private BuildAssignment assignment;

    public BuildComposer(BuildAssignment assignment) {
        this.assignment = assignment;
    }

    public BuildCommand compose() {
        return BuildCommand.compose(
                echoWithPrefix(NOTICE, "Job Started: ${date}\n"),
                prepare(),
                build(),
                jobResult().runIf("any"),
                reportAction(COMPLETED, "Job completed").runIf("any")
        ).setOnCancel(BuildCommand.compose(
                // can't use jobResult() command here because it starts a new cancel session, and will
                // report the incorrect result
                echoWithPrefix(JOB_CANCELLED, String.format("Current job status: %s", JobResult.Cancelled.toLowerCase())),
                reportAction(COMPLETED, "Job completed"))
        );
    }

    private BuildCommand prepare() {
        return BuildCommand.compose(
                reportAction(PREP, "Start to prepare"),
                reportCurrentStatus(Preparing),
                refreshWorkingDir(),
                updateMaterials());
    }

    private BuildCommand build() {
        return BuildCommand.compose(
                reportCurrentStatus(Building),
                setupSecrets(),
                setupEnvironmentVariables(),
                reportAction(NOTICE, "Start to build"),
                runBuildTasks(),
                BuildCommand.compose(
                        reportCompleting(),
                        reportCurrentStatus(Completing),
                        harvestProperties(),
                        uploadArtifacts()).setRunIfRecurisvely("any"));
    }


    private BuildCommand harvestProperties() {
        List<ArtifactPropertiesGenerator> generators = assignment.getPlan().getPropertyGenerators();
        List<BuildCommand> commands = new ArrayList<>();

        for (ArtifactPropertiesGenerator generator : generators) {
            BuildCommand command = BuildCommand.generateProperty(generator.getName(), generator.getSrc(), generator.getXpath()).setWorkingDirectory(workingDirectory());
            commands.add(command);
        }
        return BuildCommand.compose(
                reportAction(PUBLISH, "Start to create properties"),
                BuildCommand.compose(commands));
    }


    private BuildCommand runBuildTasks() {
        List<BuildCommand> commands = new ArrayList<>();
        for (Builder builder : assignment.getBuilders()) {
            commands.add(runSingleTask(builder));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand runSingleTask(Builder builder) {
        String runIfConfig = builder.resolvedRunIfConfig().toString();
        BuildCommand cancelTask = runCancelTask(builder.getCancelBuilder());
        BuildCommand baseCommand = builder.buildCommand().runIf(runIfConfig).setOnCancel(cancelTask);

        return BuildCommand.task(builder.getDescription(), baseCommand).runIf(runIfConfig);
    }

    private BuildCommand runCancelTask(Builder cancelBuilder) {
        if (cancelBuilder == null) {
            return null;
        }
        return BuildCommand.compose(
                echoWithPrefix(CANCEL_TASK_START, "On Cancel Task: %s", cancelBuilder.getDescription()),
                cancelBuilder.buildCommand(),
                echoWithPrefix(CANCEL_TASK_PASS, "On Cancel Task completed"));
    }

    private BuildCommand uploadArtifacts() {
        List<BuildCommand> commands = new ArrayList<>();
        for (ArtifactPlan ap : assignment.getPlan().getArtifactPlans()) {
            commands.add(uploadArtifact(ap.getSrc(), ap.getDest(), ap.getArtifactType().isTest())
                    .setWorkingDirectory(workingDirectory()));
        }

        return BuildCommand.compose(
                reportAction(PUBLISH, "Start to upload"),
                BuildCommand.compose(commands),
                generateTestReport());
    }

    private BuildCommand generateTestReport() {
        List<String> srcs = new ArrayList<>();
        for (ArtifactPlan ap : assignment.getPlan().getArtifactPlans()) {
            if (ap.getArtifactType() == ArtifactType.unit) {
                srcs.add(ap.getSrc());
            }
        }
        return srcs.isEmpty() ? noop() : BuildCommand.generateTestReport(srcs, "testoutput").setWorkingDirectory(workingDirectory());
    }


    private BuildCommand setupSecrets() {
        List<EnvironmentVariableContext.EnvironmentVariable> secrets = environmentVariableContext().getSecureEnvironmentVariables();
        ArrayList<BuildCommand> commands = new ArrayList<>();
        for (EnvironmentVariableContext.EnvironmentVariable secret : secrets) {
            commands.add(secret(secret.value()));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand setupEnvironmentVariables() {
        EnvironmentVariableContext context = environmentVariableContext();
        ArrayList<BuildCommand> commands = new ArrayList<>();
        commands.add(export("GO_SERVER_URL"));
        for (String property : context.getPropertyKeys()) {
            commands.add(export(property, context.getProperty(property), context.isPropertySecure(property)));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand reportAction(String tag, String action) {
        return echoWithPrefix(tag, "%s %s on ${agent.hostname} [${agent.location}]", action, getJobIdentifier().buildLocatorForDisplay());
    }

    private BuildCommand updateMaterials() {
        if (!assignment.getPlan().shouldFetchMaterials()) {
            return echoWithPrefix(PREP, "Skipping material update since stage is configured not to fetch materials");
        }

        MaterialRevisions materialRevisions = assignment.materialRevisions();
        Materials materials = materialRevisions.getMaterials();
        return BuildCommand.compose(
                materials.cleanUpCommand(workingDirectory()),
                echoWithPrefix(PREP, "Start to update materials \n"),
                materialRevisions.updateToCommand(workingDirectory()));
    }

    private BuildCommand refreshWorkingDir() {
        return BuildCommand.compose(
                cleanWorkingDir(),
                mkdirs(workingDirectory()).setTest(test("-nd", workingDirectory())));
    }

    private BuildCommand cleanWorkingDir() {
        if (!assignment.getPlan().shouldCleanWorkingDir()) {
            return noop();
        }
        return BuildCommand.compose(
                cleandir(workingDirectory()),
                echoWithPrefix(PREP, "Cleaning working directory \"$%s\" since stage is configured to clean working directory", workingDirectory())
        ).setTest(test("-d", workingDirectory()));
    }

    private String workingDirectory() {
        return assignment.getWorkingDirectory().getPath();
    }

    private JobIdentifier getJobIdentifier() {
        return assignment.getPlan().getIdentifier();
    }

    private EnvironmentVariableContext environmentVariableContext() {
        JobPlan plan = assignment.getPlan();
        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.addAll(assignment.initialEnvironmentVariableContext());
        context.setProperty("GO_TRIGGER_USER", assignment.getBuildApprover() , false);
        getJobIdentifier().populateEnvironmentVariables(context);
        assignment.materialRevisions().populateEnvironmentVariables(context, new File(workingDirectory()));
        plan.applyTo(context);
        return context;
    }
}
