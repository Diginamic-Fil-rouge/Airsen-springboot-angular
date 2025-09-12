# Multi-stage build for Angular
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Build Angular application
RUN npm run build --prod

# Production stage with Nginx
FROM nginx:alpine

# Copy custom nginx configuration
COPY ../docker/nginx/default.conf /etc/nginx/conf.d/default.conf

# Copy built Angular app
COPY --from=builder /app/dist/airsen-frontend /usr/share/nginx/html

# Copy environment configuration script
COPY ../docker/nginx/env-config.sh /docker-entrypoint.d/40-env-config.sh
RUN chmod +x /docker-entrypoint.d/40-env-config.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]