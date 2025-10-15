ARG NODE_IMAGE=node:22.12-bullseye-slim
ARG NGINX_IMAGE=nginx:1.27-alpine

# DEVELOPMENT STAGE
FROM ${NODE_IMAGE} AS development
WORKDIR /usr/src/app
EXPOSE 4200
CMD ["bash","-lc","npm ci --no-audit --no-fund && npx ng serve --host 0.0.0.0 --port 4200 --poll 2000"]

# BUILD STAGE
FROM ${NODE_IMAGE} AS build
WORKDIR /usr/src/app

# OOM protection for large Angular apps
ENV NODE_OPTIONS=--max-old-space-size=4096

# Install dependencies
COPY package*.json ./
RUN npm ci --no-audit --no-fund

# Copy source and build
COPY . .
RUN npm run build:docker

# Verify build output
RUN test -d /usr/src/app/dist

# PRODUCTION STAGE
FROM ${NGINX_IMAGE} AS production

# Copy built Angular app
COPY --from=build /usr/src/app/dist/ /usr/share/nginx/html/

# Create non-root user for nginx
RUN addgroup -g 101 -S nginx-runtime && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx-runtime -g nginx-runtime nginx-runtime

# Set proper permissions
RUN chown -R nginx-runtime:nginx-runtime /usr/share/nginx/html && \
    chown -R nginx-runtime:nginx-runtime /var/cache/nginx && \
    chown -R nginx-runtime:nginx-runtime /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx-runtime:nginx-runtime /var/run/nginx.pid

USER nginx-runtime

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]