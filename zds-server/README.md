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
