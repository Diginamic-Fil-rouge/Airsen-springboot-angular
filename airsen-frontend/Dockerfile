# Multi-stage build for Angular with Node v22.19.0
FROM node:22.19.0-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build Angular application for production
RUN npm run build

# Development stage - for local development with hot reload
FROM node:22.19.0-alpine AS development

WORKDIR /app

# Install Angular CLI globally
RUN npm install -g @angular/cli

# Copy package files
COPY package*.json ./

# Install all dependencies (including dev dependencies)
RUN npm ci

# Copy source code (will be overridden by volume mount in docker-compose)
COPY . .

# Create non-root user
RUN addgroup -S angular && adduser -S angular -G angular

# Change ownership
RUN chown -R angular:angular /app

USER angular

# Development server with hot reload
EXPOSE 4200
CMD ["ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--poll", "2000"]

# Production stage with Nginx
FROM nginx:alpine AS production

# Copy custom nginx configuration if it exists
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf 2>/dev/null || echo "No custom nginx config found"

# Copy built Angular app
COPY --from=builder /app/dist/airsen-frontend /usr/share/nginx/html

# Copy environment configuration script if it exists
COPY docker/nginx/env-config.sh /docker-entrypoint.d/40-env-config.sh 2>/dev/null || echo "No env config script found"
RUN chmod +x /docker-entrypoint.d/40-env-config.sh 2>/dev/null || true

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]