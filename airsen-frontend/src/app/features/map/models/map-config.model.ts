/**
 * Map configuration and constants used by the rebuilt map feature.
 */
export interface TileLayerConfig {
  id: 'streets' | 'satellite' | 'dark' | 'terrain';
  name: string;
  url: string;
  attribution?: string;
}

