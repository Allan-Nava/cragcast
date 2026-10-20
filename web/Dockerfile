# CG-110: the page for the Raspberry Pi, run from the repository root:
#   docker build -f web/Dockerfile .
# The same Vite build as GitHub Pages; nginx serves it and hands the data paths to the Spring
# service (nginx.conf). Both base images are multi-arch: image.yml builds linux/arm64.
FROM node:22-alpine AS build
WORKDIR /src/web
COPY web/package.json web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY assets /src/assets
COPY web ./
# The release this image is (vYYYY.MM.DD, from release.yml); the footer names it. Unset: none.
ARG VERSION=
ENV VITE_CRAGCAST_VERSION=$VERSION
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/site /usr/share/nginx/html
# CG-45: the link previews' image, as tools/build_site.sh copies it for Pages.
COPY assets/social-preview.png /usr/share/nginx/html/og.png
EXPOSE 8080
