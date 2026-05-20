import { WebPlugin } from '@capacitor/core';

import type { SerialPluginPlugin } from './definitions';

export class SerialPluginWeb extends WebPlugin implements SerialPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
