// Generic API Response Models

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
  timestamp: Date;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  error: string;
  message: string;
  status: number;
  timestamp: Date;
  path: string;
  details?: { [key: string]: any };
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}

export interface ApiValidationError extends ApiError {
  validationErrors: ValidationError[];
}

// Pagination parameters for requests
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

// Filter parameters for search requests
export interface FilterParams {
  [key: string]: string | number | boolean | Date | null;
}

// Generic search request
export interface SearchRequest {
  query?: string;
  filters?: FilterParams;
  pagination: PaginationParams;
}

// Health check response
export interface HealthResponse {
  status: 'UP' | 'DOWN';
  components: {
    [key: string]: {
      status: 'UP' | 'DOWN';
      details?: { [key: string]: any };
    };
  };
}
