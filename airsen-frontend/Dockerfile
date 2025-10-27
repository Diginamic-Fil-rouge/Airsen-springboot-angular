
# DEVELOPMENT STAGE - Live Reload with Angular CLI
FROM node:22-alpine AS development

WORKDIR /app

# Install basic utilities
RUN apk add --no-cache curl bash

# Copy ONLY package files for dependency caching (source will be bind-mounted)
COPY package*.json ./

# Install dependencies (cached layer - only invalidates if package.json changes)
RUN npm ci --prefer-offline --no-audit

# DON'T COPY src - will be bind-mounted in docker-compose
# This allows Angular CLI to detect file changes and live reload

EXPOSE 4200

# Set Node memory options
ENV NODE_OPTIONS="--max-old-space-size=2048"

# Angular CLI with live reload (polling for Docker compatibility)
CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--poll", "2000"]



# BUILDER STAGE - Compile Angular Application
FROM node:22-alpine AS builder

WORKDIR /build

# Copy package files first for dependency caching
COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production --prefer-offline --no-audit

# Copy Angular configuration files
COPY angular.json tsconfig*.json ./

# Copy source code (invalidates only if src changes)
COPY src ./src

# Build production-optimized Angular app
RUN npm run build -- --configuration production --output-path=dist/airsen-frontend



# PRODUCTION STAGE - Nginx Static Server
FROM nginx:alpine AS production

# Install curl for health checks
RUN apk add --no-cache curl

# Copy ONLY built Angular app from builder stage
COPY --from=builder /build/dist/airsen-frontend /usr/share/nginx/html

# Create health check endpoint
RUN echo "OK" > /usr/share/nginx/html/health

# Health check for production readiness
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1


EXPOSE 80

# Run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
