# Multi-stage build for Angular with Node v22.19.0
ARG NODE_IMAGE=node:22-alpine
FROM ${NODE_IMAGE} AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build Angular application for production
RUN npm run build -- --configuration production

# Development stage - for local development with hot reload
FROM ${NODE_IMAGE} AS development

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install all dependencies (including dev dependencies) deterministically
RUN npm ci

# Copy source code (will be overridden by volume mount in docker-compose)
COPY . .

# Create non-root user
RUN addgroup -S angular && adduser -S angular -G angular && \
    chown -R angular:angular /app

# Change ownership
RUN chown -R angular:angular /app

USER angular

# Development server with hot reload
EXPOSE 4200
CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--poll", "2000"]

# Production stage with Nginx
FROM nginx:alpine AS production

# Generate nginx SPA configuration inline
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    \
    location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ { \
        expires 1y; \
        add_header Cache-Control "public, immutable"; \
    } \
    \
    gzip on; \
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript; \
    gzip_min_length 1000; \
    \
    add_header X-Frame-Options "SAMEORIGIN" always; \
    add_header X-Content-Type-Options "nosniff" always; \
    add_header X-XSS-Protection "1; mode=block" always; \
    add_header Referrer-Policy "strict-origin-when-cross-origin" always; \
}' > /etc/nginx/conf.d/default.conf

# Copy built Angular app
COPY --from=builder /app/dist/airsen-frontend /tmp/dist

# Intelligently copy based on Angular version (v17+ uses /browser subfolder)
RUN if [ -d /tmp/dist/browser ]; then \
        cp -r /tmp/dist/browser/* /usr/share/nginx/html/; \
    else \
        cp -r /tmp/dist/* /usr/share/nginx/html/; \
    fi && \
    rm -rf /tmp/dist

# Verify index.html exists (fail fast if build is broken)
RUN test -f /usr/share/nginx/html/index.html || (echo "ERROR: index.html not found!" && exit 1)


EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]