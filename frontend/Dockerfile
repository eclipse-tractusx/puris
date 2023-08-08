FROM node:lts-alpine

ARG npm_build_mode=dockerbuild
ENV JQ_VERSION=1.6

#RUN apk --no-cache add curl
#RUN curl https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/jq-linux64 -o /tmp/jq-linux64
RUN wget --no-check-certificate https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/jq-linux64 -O /tmp/jq-linux64
RUN cp /tmp/jq-linux64 /usr/bin/jq
RUN chmod +x /usr/bin/jq

WORKDIR /app
COPY . .

RUN jq 'to_entries | map_values({ (.key) : ("$" + .key) }) | reduce .[] as $item ({}; . + $item)' ./src/config.json > ./src/config.tmp.json && mv ./src/config.tmp.json ./src/config.json

RUN npm install && npm run build npm_build_mode


FROM nginxinc/nginx-unprivileged:stable-alpine

ENV SFOLDER=/usr/share/nginx/html/js/*.js

COPY ./start-nginx.sh /usr/bin/start-nginx.sh
#RUN sudo chmod +x /usr/bin/start-nginx.sh

WORKDIR /usr/share/nginx/html
# VueJS
COPY --from=0 /app/dist .
ENTRYPOINT [ "start-nginx.sh" ]
