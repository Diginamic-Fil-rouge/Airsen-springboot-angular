/**
 * Map state model interfaces for the rebuilt map feature.
 */
export interface MapUiState {
  isPanelOpen: boolean;
  showHeatmap: boolean;
  mapStyle: 'streets' | 'satellite' | 'dark' | 'terrain';
}

