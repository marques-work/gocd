/*
 * Copyright 2017 ThoughtWorks, Inc.
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

;(function ($) {
  "use strict";

  function MultiplexingTransformer(transformers) {
    this.transform = function processAllTransformers(logLines) {
      for (var i = 0, len = transformers.length; i < len; i++) {
        transformers[i].transform(logLines);
      }
    };

    this.dequeue = function dequeueTransformers(name) {
      for (var i = 0, len = transformers.length; i < len; i++) {
        if (!name || transformers[i].name === name) {
          transformers[i].dequeue();
        }
      }
    }
  }

  $(function initConsolePageDomReady() {
    var jobDetails = $(".job_details_content");

    if (!jobDetails.length) return;

    var jobStatusUrl = "jobStatus.json?pipelineName=" + jobDetails.data("pipeline") + "&stageName=" + jobDetails.data("stage") + "&jobId=" + jobDetails.data("job");
    var executor     = new DashboardPeriodicalExecutor(jobStatusUrl, function detectJobCompleted(jobInfo) {
      return jobInfo[0].building_info.is_completed.toString() === "true";
    });

    var build = $("[data-console-url]");

    function triggerLogDequeue() {
      var subTab = this;
      jobDetails.trigger("dequeue", subTab.tab_name);
    }

    var uid = [jobDetails.data("pipeline"), jobDetails.data("stage"), jobDetails.data("job"), jobDetails.data("build")].join("-");
    var tabsManager = new TabsManager(null, "build", uid, "console", {
      "console":  new SubTabs($(".sub_tabs_container #build_console")[0], triggerLogDequeue),
      "failures": new SubTabs($(".sub_tabs_container #failures_console")[0], triggerLogDequeue)
    });

    if (build.length) {
      var consoleUrl = context_path("files/" + build.data("console-url"));
      var containers = build.find(".buildoutput_pre"), transformers = [];

      containers.on("consoleUpdated", function detectFoldable(e) {
        var el = e.currentTarget;
        var c = $(el);
        if (!c.data("detected") && el.querySelector(".log-fs-type")) {
          c.siblings(".console-action-bar").find("[data-collapsed]").show();
          c.data("detected", true);
        }
      });

      $.each(containers, function initEachConsoleArea(i, area) {
        var container = $(area);
        var name;

        if (container.is("#tab-content-of-console *")) {
          new ConsoleScroller(container, $("#build_console"), $('.auto-scroll')).startScroll();
          name = "console"; // needs match tab name for dequeue() to work
        } else {
          name = "failures";
        }

        var tfm = new LogOutputTransformer(container, FoldableSection, tabsManager.getCurrentTab() !== name);
        tfm.name = name;
        transformers.push(tfm);

        container.find(".console-action-bar").on("click", ".toggle-timestamps", function toggleLogTimestamps(e) {
          e.stopPropagation();
          e.preventDefault();

          container.toggleClass("with-timestamps");
        });
      });

      build.find(".console-action-bar").on("click", ".toggle-timestamps", function toggleLogTimestamps(e) {
        e.stopPropagation();
        e.preventDefault();

        $(e.currentTarget).closest(".console-area").toggleClass("with-timestamps");
      });

      build.find(".console-action-bar").on("click", ".toggle-folding", function toggleCollapseAll(e) {
        e.stopPropagation();
        e.preventDefault();

        var trigger = $(e.currentTarget).removeData("collapsed");
        var consoleArea = trigger.closest(".console-area");
        var foldableSections = consoleArea.find(".log-fs-type");

        if (!foldableSections.length) return;

        if (trigger.data("collapsed")) {
          foldableSections.addClass("open");
          trigger.attr("data-collapsed", "false");
        } else {
          foldableSections.removeClass("open");
          trigger.attr("data-collapsed", "true");
        }
        consoleArea.find(".buildoutput_pre").trigger("consoleUpdated");
      });

      var multiTransformer = new MultiplexingTransformer(transformers);

      executor.register(new ConsoleLogObserver(consoleUrl, multiTransformer, {
        onUpdate:   function () {
          containers.trigger("consoleUpdated");
        },
        onComplete: function () {
          containers.trigger("consoleCompleted");
        }
      }));

      jobDetails.on("dequeue", function (e, name) {
        multiTransformer.dequeue(name);
      });

    }

    executor.register(new TimerObserver(jobDetails.data("build")));
    executor.register(new BuildSummaryObserver($('.build_detail_summary')));

    executor.start();

    build.on('click.changeTheme', '.change-theme', function () {
      $('.sub_tab_container_content').toggleClass('white-theme');
    });
  });

})(jQuery);
