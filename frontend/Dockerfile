FROM node:20.20.2-alpine AS dependencies

WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

FROM dependencies AS source

COPY frontend/ ./

FROM source AS test

CMD ["sh", "-c", "npm run format:check && npm run lint && npm run test && npm run build"]

FROM source AS build

RUN npm run build

FROM nginx:1.29-alpine AS runtime

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
