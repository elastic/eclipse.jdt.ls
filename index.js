/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

import path from 'path';
import fs from 'fs';
import { platform } from 'os';

export default function (kibana) {
  return new kibana.Plugin({
    id: 'java-langserver',
    require: ['elasticsearch', 'kibana'],
    name: 'java-langserver',
    config(Joi) {
      return Joi.object({
        enabled: Joi.boolean().default(true),
      }).default();
    },
    init(server) {
      const jdtConfigPath = path.resolve(server.config().get('path.data'), 'code/jdt_config');
      const configFilePath = path.resolve(jdtConfigPath, 'config.ini');
      if (!fs.existsSync(jdtConfigPath)) {
        const codeDataPath = path.dirname(jdtConfigPath);
        if(!fs.existsSync(codeDataPath)) {
          fs.mkdirSync(codeDataPath);
        }
        fs.mkdirSync(jdtConfigPath);
        let configPath = 'config_mac';
        const osPlatform = platform();
        if (osPlatform == 'win32') {
          configPath = 'config_win';
        } else if (osPlatform == 'linux') {
          configPath = 'config_linux'
        }
        if (fs.lstatSync(configFilePath)) {
          fs.unlinkSync(configFilePath);
        }
        fs.symlinkSync(path.resolve(__dirname, 'lib/repository', configPath, 'config.ini'), configFilePath);
      }
      server.expose('install', {
        path: path.join(__dirname, 'lib'),
      });
    }
  });
}
