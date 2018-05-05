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

const Stream = require('mithril/stream');
const _      = require('lodash');

const Capabilities = function (data) {
  this.supportedAgentAnalytics            = Stream(data.supportedAgentAnalytics);
  this.supportedPipelineAnalytics         = Stream(data.supportedPipelineAnalytics);
  this.supportedStageAnalytics            = Stream(data.supportedStageAnalytics);
  this.supportedAnalyticsDashboardMetrics = Stream(data.supportedAnalyticsDashboardMetrics);
};

Capabilities.fromJSON = (data = {}) => new Capabilities({
  supportedAgentAnalytics:            _.filter(data, {type: 'agent'}),
  supportedPipelineAnalytics:         _.filter(data, {type: 'pipeline'}),
  supportedStageAnalytics:            _.filter(data, {type: 'stage'}),
  supportedAnalyticsDashboardMetrics: _.filter(data, {type: 'dashboard'})
});

module.exports = Capabilities;
