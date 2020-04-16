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
import _ from "lodash";
import {ConfigValue, EncryptedValue, PlainTextValue} from "models/shared/config_value";

export interface PropertyErrors {
  configuration_key?: string[];
  configuration_value?: string[];
  encrypted_value?: string[];
}

export interface ErrorIndex {
  key: string;
  errors?: PropertyErrors;
}

export interface PropertyJSON extends ErrorIndex {
  value?: string | null | undefined;
  encrypted_value?: string | null | undefined;
}

export class Configurations {
  private readonly configurations: Configuration[];

  constructor(configurations: Configuration[]) {
    this.configurations = configurations;
  }

  public static fromJSON(properties: PropertyJSON[]): Configurations {
    return new Configurations(properties.map((property) => Configuration.fromJSON(property)));
  }

  public valueFor(key: string): string | undefined {
    const config = this.findConfiguration(key);
    if (config) {
      return config.getValue();
    }
  }

  public setConfiguration(key: string, value: string): void {
    const existingConfig = this.findConfiguration(key);
    if (!existingConfig) {
      this.configurations.push(new Configuration(key, new PlainTextValue(value)));
    } else {
      existingConfig.updateValue(value);
    }
  }

  public findConfiguration(key: string): Configuration | undefined {
    return _.find(this.configurations, ["key", key]);
  }

  public asMap(): Map<string, string> {
    return new Map<string, string>(this.configurations.map((config) => [config.key, config.displayValue()] as [string, string]));
  }

  public allConfigurations(): Configuration[] {
    return this.configurations;
  }

  public count(): number {
    return this.configurations.length;
  }

  public toJSON() {
    return this.configurations;
  }

  public get(index: number) {
    return this.configurations[index];
  }

  public asString(): string {
    const configValues = this.configurations
                             .filter((config) => config.isEncrypted() === false && !_.isEmpty(config.displayValue()))
                             .map((config) => `${config.key}=${config.displayValue()}`)
                             .join(",");
    return `[${configValues}]`;
  }
}

export class Configuration {
  readonly key: string;
  readonly errors: string[];
  private __value: ConfigValue;
  private __secure: boolean = false;

  constructor(key: string, value: EncryptedValue | PlainTextValue, errors: string[] = []) {
    this.key    = key;
    this.__value = value;
    this.errors = errors;
  }

  static fromJSON(config: PropertyJSON): Configuration {
    let errors: string[] | undefined;
    if (config.errors) {
      errors = _.reduce(config.errors, (all, msgs) => all.concat(msgs || []), [] as string[]);
    }

    let value;
    if (config.encrypted_value) {
      value = new EncryptedValue(config.encrypted_value);
    } else {
      value = new PlainTextValue(config.value || "");
    }
    return new Configuration(config.key, value, errors);
  }

  get value(): string {
    return this.getValue();
  }

  set value(val: string) {
    if (val === this.getValue()) {
      return;
    }
    this.__value = new PlainTextValue(val);
  }

  get encrypted(): boolean {
    return this.isEncrypted();
  }

  get secure(): boolean {
    return this.encrypted || this.__secure;
  }

  set secure(val: boolean) {
    this.__secure = val;
  }

  public displayValue(): string {
    return this.__value.getDisplayValue();
  }

  public getValue(): string {
    return this.__value.getValue();
  }

  public isEncrypted(): boolean {
    return this.__value.isEncrypted();
  }

  public updateValue(value: string) {
    if (value !== this.getValue()) {
      this.__value = new PlainTextValue(value);
    }
  }

  public toJSON(): object {
    const { key, value, secure, encrypted } = this;

    if (encrypted) {
      return { key, encrypted_value: value };
    }

    if (secure) {
      return { key, value, secure };
    }

    return { key, value };
  }
}
