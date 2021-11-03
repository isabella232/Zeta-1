# Server part of zeta dev suite

Prod:[![Build Status](https://ci.altus.vip.ebay.com/zeta-tess-ci-5162/buildStatus/icon?job=zds-server/tess)](https://ci.altus.vip.ebay.com/zeta-tess-ci-5162/job/zds-server/tess)
Dev: [![Build Status](https://ci.altus.vip.ebay.com/zeta-tess-ci-5162/buildStatus/icon?job=zds-server/tess)](https://ci.altus.vip.ebay.com/zeta-tess-ci-5162/job/zds-server/tess-dev)


## Build

```
# build jar in local
mvn package -DskipTests

# build image in local
./buildscripts/install_lib_2_local.sh
./buildscripts/image_build_local.sh

# build image with cache
cd zds-server
./buildscripts/install_lib_2_local.sh
./buildscripts/image_build.sh
```

## Tess CI & Deploy

Zeta on Tess enables two branches for CI, `tess` and `tess-dev`. The two branches have different build trigger. `tess` will start build
on every push action. `tess-dev` only start build on push whose latest commit message contains keyword
`[ci zds-server]`

Following is the release flow:
1. commits to `tess-dev` with keyword `[ci zds-server]`
1. jenkins job start, success with image whose tag is `dev-${version}`
1. deploy staging:
    ```shell script
    # If no Tess deployment & secret & configuration change
   tess kubectl rollout restart statefulset zds-server-staging
   tess kubectl rollout restart statefulset zds-server-scheduler-staging
   
   # If any Tess  deployment & secret & configuration change
   cd ../tess
   # Modify related templates
   ./gen_deploy_yaml.sh zds-server staging
   ./gen_deploy_yaml.sh zds-server staging-scheduler
   tess apply -f zds-server-staging.yaml
   tess apply -f zds-server-staging-scheduler.yaml
    ```
 1. verify dev on `dss-api-gateway-proxy-dev.zeta-prod-ns.svc.57.tess.io/zeta/#/`
 1. pull request from `tess-dev` to `tess`
 1. jenkins job start, success with image whose tag is `${version}`
 1. deploy dev:
    ```shell script
    # If no Tess deployment & secret & configuration change
    tess kubectl rollout restart statefulset zds-server
    tess kubectl rollout restart statefulset zds-server-scheduler
   
    # If any Tess  deployment & secret & configuration change
    cd ../tess
    # Modify related templates
    ./gen_deploy_yaml.sh zds-server prod
    ./gen_deploy_yaml.sh zds-server prod-scheduler
    tess apply -f zds-server-prod.yaml
    tess apply -f zds-server-prod-scheduler.yaml
    ```
        
Something you need to know:
 1. `${version}` is same value to the project version in pom.xml
 2. to execute `./gen_deploy_yaml.sh` please install helm first: `brew install helm`
 
## How to debug restful API locally

Start server locally on a port (8090 by default), the server must be started with "dev" profile by adding "-Dspring.profiles.active=dev" to java cmd line.

### Access below URL to make sure the server is started correctly: 
```
curl http://localhost:8090/actuator/health
```

The URL should return meaningful json response like below:
```
{
  status: "UP"
}
```

### Call login URL to get a zeta token, run below command:
```
curl -X POST \
  http://localhost:8090/login \
  -H 'Authorization: Basic a2V6aHU6ZGV2LWZha2UtdG9rZW4=' \
  -H 'Cache-Control: no-cache' \
  -H 'Postman-Token: ef6d0320-eeca-4727-8833-6a86657c74d0'
```

Please note the "Authorization" header, it is base64 of string "kezhu:dev-fake-token". You should replace "kezhu" with your own NT id and base64 encode it (you may do it online at https://www.base64encode.org/). 

It will return something like below:
```
{
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJrZXpodVdlZCBTZXAgMTkgMTM6NDA6MzAgQ1NUIDIwMTgiLCJzdWIiOiJrZXpodSIsImlzcyI6InpldGEuY29ycC5lYmF5LmNvbSIsImlhdCI6MTUzNzMzNTYzMCwiZXhwIjoxNTM3NTA4NDMwfQ.lWlZAM4Y3TVOCW-jPqdRHAjS4NcqeqW0D5PL8PG-emE95QmFhd9gs5-JH4w-Bdl6_TMA0mmudOCGuDIplobO7A"
}
```

### Make other rest API calls using the generated token in previous step:
```
curl -X GET \
  http://localhost:8090/statements/1 \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJrZXpodVdlZCBTZXAgMTkgMTM6Mjg6MDggQ1NUIDIwMTgiLCJzdWIiOiJrZXpodSIsImlzcyI6InpldGEuY29ycC5lYmF5LmNvbSIsImlhdCI6MTUzNzMzNDg4OCwiZXhwIjoxNTM3NTA3Njg4fQ.74WOIOppgvZ0EKPRrbD-5HfGngPvZKBnLxeDCzejVmJ_Gm0kCnEr5HV-XvgyKExDc_J47N9YKAQi520EyBluJw' \
  -H 'Cache-Control: no-cache' \
  -H 'Postman-Token: 4167362b-5821-408a-aeed-aa88ca01964d'
```


### Error handling

Usually, an exception in zds-server will return such a error message whatever it is a http request or websocket request:
```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "errorDetail": {
    "rule": {
      "id": 1,
      "order": 1,
      "filter": "...",
      "message": "...",
      "errorCode": "",
      "createDt": "...",
      "updateDt": "...",
      "regex": "..."
    },
    "action": null,
    "context": {
      "notebook_id": "..."
    },
    "message": "There are something wrong"
  } 
}
```
The code list can be check in `com.ebay.dss.zds.exception.ErrorCode`

If rule matching is applied, the message will contain a `rule` field the id may useful to ranking a rule's efficiency.

The `action` is a field to tell user which action can be take if exception occurs.

The target contains some extra information to help locate problem. For example, if connect request crash, it will lead an error with target contains `notebook_id` and `job_id`.

For error trace convenient, currently each request including websocket upgrading request we will give a `zds-server-request-id` header. Provide its value will make debugging more easy.
