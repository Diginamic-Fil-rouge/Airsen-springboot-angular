#!/bin/sh

# Replace environment variables in built Angular files
find /usr/share/nginx/html -name "*.js" -exec sed -i "s|API_BASE_URL_PLACEHOLDER|$API_BASE_URL|g" {} \;

echo "Environment variables configured successfully"