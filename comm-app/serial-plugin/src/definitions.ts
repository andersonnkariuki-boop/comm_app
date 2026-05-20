export interface SerialPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
