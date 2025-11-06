import { CommuneDataService } from "@/core/services/commune-data.service";

/**
 * APP_INITIALIZER factory that triggers loading of commune data on application startup.
 *
 * Non-blocking behavior:
 * - Returns a function that resolves a Promise immediately, so Angular bootstrap is not delayed.
 * - The CommuneDataService performs the actual data fetching in the background and updates state
 *   when data arrives. This keeps startup fast and resilient to network latency or failures.
 */
export function initializeCommunes(
  communeDataService: CommuneDataService
): () => Promise<void> {
  return () => communeDataService.loadCommunesOnStartup();
}

