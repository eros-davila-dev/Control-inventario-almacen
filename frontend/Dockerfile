# Compila la aplicacion Angular y la sirve con Nginx sin privilegios.
FROM node:22-alpine AS construccion
WORKDIR /origen
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=construccion /origen/dist/frontend/browser /usr/share/nginx/html
EXPOSE 8080
