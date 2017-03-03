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
;(function ($, c) {
  "use strict";

  var Types = {
    INFO: "##", ALERT: "@@",
    PREP: "pr", PREP_ERR: "pe",
    TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1",
    CANCEL_TASK_START: "!x", CANCEL_TASK_PASS: "x0", CANCEL_TASK_FAIL: "x1",
    JOB_PASS: "j0", JOB_FAIL: "j1"
  };

  function LineWriter() {

    var cmd_re = /^(\s*\[go] (?:On Cancel )?Task: )(.*)/,
        status_re = /^(\s*\[go] (?:Current job|Task) status: )(.*)/,
        ansi = new AnsiUp();
    ansi.use_classes = true;

    function isTaskLine(prefix) {
      return [Types.TASK_START, Types.CANCEL_TASK_START].indexOf(prefix) > -1;
    }

    function isStatusLine(prefix) {
      return [Types.PASS, Types.FAIL, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function formatContent(node, prefix, line) {
      var parts;

      if (isTaskLine(prefix)) {
        parts = line.match(cmd_re);
        c(node, parts[1], c("code", parts[2]));
      } else if (isStatusLine(prefix)) {
        parts = line.match(status_re);
        if (parts) {
          c(node, parts[1], c("code", parts[2]));
        } else {
          c(node, line); // Usually the end of an onCancel task
        }
      } else {
        if ("" === $.trim(line)) {
          c(node, c("br"));
        } else {
          node.innerHTML = ansi.ansi_to_html(line);
        }
      }
    }

    function insertBasic(cursor, line) {
      var output = c("dt");

      output.innerHTML = ansi.ansi_to_html(line);
      cursor.write(output);
      return $(output);
    }

    function insertHeader(cursor, prefix, line) {
      var header = c("dt", {"data-prefix": prefix});

      formatContent(header, prefix, line);
      cursor.write(header);
      return $(header);
    }

    function insertLine(cursor, prefix, line) {
      var output = c("dd", {"data-prefix": prefix});

      formatContent(output, prefix, line);
      cursor.write(output);

      return $(output);
    }

    this.insertHeader = insertHeader;
    this.insertLine = insertLine;
    this.insertBasic = insertBasic;
  }

  function SectionCursor(node, section) {
    var cursor, me = this;

    if (node instanceof $) node = node[0];
    if (!(section instanceof $)) section = $(section);

    // the internal cursor reference is the Node object to append new content.
    // sometimes this is the section element, and sometimes it is the "node" argument,
    // which may be a DocumentFragment that
    cursor = $.contains(node, section[0]) ? section[0] : node;

    function write(childNode) {
      cursor.appendChild(childNode);
    }

    function hasType() {
      return section[0].hasAttribute("data-type");
    }

    function getSection() {
      return section;
    }

    function onFinishSection() {
      if (!section.data("errored")) {
        section.removeClass("open");
      }
    }

    function detectError(prefix) {
      if ([Types.FAIL, Types.JOB_FAIL, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1) {
        section.data("errored", true);
      }

      // canceling a build generally leaves no task status, so infer it
      // by detecting the CANCEL_TASK_START prefix
      if (section.data("type") === "task" && Types.CANCEL_TASK_START === prefix) {
        // No, "cancelled" is not misspelled. We use both the British and American spellings inconsistently in our codebase,
        // but we should go with whatever JobResult.Cancelled is, which uses the British spelling "cancelled"
        section.attr("data-task-status", "cancelled").removeData("task-status");
        section.data("errored", true);
      }

      if (Types.PASS === prefix || Types.CANCEL_TASK_PASS === prefix) {
        section.attr("data-task-status", "passed").removeData("task-status");
      }

      if (Types.FAIL === prefix || Types.CANCEL_TASK_FAIL === prefix) {
        section.attr("data-task-status", "failed").removeData("task-status");
      }

      if (Types.JOB_PASS === prefix) {
        section.attr("data-job-status", "passed").removeData("job-status");
      }

      if (Types.JOB_FAIL === prefix) {
        section.attr("data-job-status", "failed").removeData("job-status");
      }
    }

    function assignType(prefix) {
      if (Types.INFO === prefix) {
        section.attr("data-type", "info");
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.attr("data-type", "prep");
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "task");
      } else if (Types.CANCEL_TASK_START === prefix) {
        section.attr("data-type", "cancel");
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "result");
      } else {
        section.attr("data-type", "info");
      }

      section.removeData("data-type");
    }

    function isPartOfSection(prefix) {
      if (section.data("type") === "info") {
        return Types.INFO === prefix;
      }

      if (section.data("type") === "prep") {
        return [Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1;
      }

      if (section.data("type") === "task") {
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1;
      }

      if (section.data("type") === "cancel") {
        return [Types.OUT, Types.ERR, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
      }

      return false;
    }

    function isExplicitEndBoundary(prefix) {
      return [Types.PASS, Types.FAIL, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function closeAndStartNew(container) {
      // close section and start a new one
      onFinishSection();

      return SectionCursor.addCursorTo(container);
    }

    this.detectError = detectError;
    this.isExplicitEndBoundary = isExplicitEndBoundary;

    this.assignType = assignType;
    this.isPartOfSection = isPartOfSection;
    this.closeAndStartNew = closeAndStartNew;

    this.write = write;

    this.markMultiline = function markMultiline() {
      if (!section.data("multiline")) {
        section.prepend(c("a", {class: "fa toggle"}));
        section.data("multiline", true);
      }
    };

    this.hasType = hasType;
    this.element = getSection;
  }

  SectionCursor.addCursorTo = function addBlankSection(node) {
    if (node instanceof $) node = node[0]; // node may be a real element or document fragment

    var section = c("dl", {class: "foldable-section open"});
    node.appendChild(section);

    return new SectionCursor(node, $(section));
  };

  window.SectionCursor = SectionCursor;
  window.LineWriter = LineWriter;
})(jQuery, crel);
