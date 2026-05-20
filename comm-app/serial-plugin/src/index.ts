import { registerPlugin } from '@capacitor/core';

import type { SerialPluginPlugin } from './definitions';

const SerialPlugin = registerPlugin<SerialPluginPlugin>('SerialPlugin', {
  web: () => import('./web').then((m) => new m.SerialPluginWeb()),
});

export * from './definitions';
export { SerialPlugin };
