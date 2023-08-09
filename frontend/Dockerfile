FROM node:lts-alpine

ARG NPM_BUILD_MODE=dockerbuild
ENV JQ_VERSION=1.6

# TODO curl results in problem with node during ```npm run ${NPM_BUILD_MODE}```
#RUN apk --no-cache add curl
#RUN curl https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/jq-linux64 -o /tmp/jq-linux64
RUN wget --no-check-certificate https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/jq-linux64 -O /tmp/jq-linux64
RUN cp /tmp/jq-linux64 /usr/bin/jq
RUN chmod +x /usr/bin/jq

WORKDIR /app
COPY . .

RUN jq 'to_entries | map_values({ (.key) : ("$" + .key) }) | reduce .[] as $item ({}; . + $item)' ./src/config.json > ./src/config.tmp.json && mv ./src/config.tmp.json ./src/config.json

RUN npm install && npm run ${NPM_BUILD_MODE}

RUN chmod -R 777 /app/dist


# TODO use nginx-unpriviledged:stable-alpine BUT problem installing moreutils
#FROM nginxinc/nginx-unprivileged:stable-alpine
FROM nginx:stable-alpine

# commly it would be .../html/js/.*js, but due to the project structure the JS files are in .../html/assets
ENV JSFOLDER=/usr/share/nginx/html/assets/*.js

COPY ./start-nginx.sh /usr/bin/start-nginx.sh
RUN apk --no-cache add moreutils

WORKDIR /usr/share/nginx/html
# VueJS
COPY --from=0 /app/dist .
ENTRYPOINT [ "start-nginx.sh" ]
# nginx exposes at port 80
# nginx-unpriviledged exposes at port 8080
