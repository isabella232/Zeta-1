FROM ecr.vip.ebayc3.com/zeta/node as builder
WORKDIR /frontend
COPY . /frontend
RUN npm install
RUN node --max_old_space_size=4096 /frontend/node_modules/@vue/cli-service/bin/vue-cli-service.js build

FROM ecr.vip.ebayc3.com/zeta/nginx
# netcat is used in k8s prob to test sentinel delay
RUN apt-get update && apt-get install -y netcat

COPY --from=builder /frontend/dist /usr/share/nginx/html
