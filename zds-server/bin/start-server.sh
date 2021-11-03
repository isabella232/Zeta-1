#!/usr/bin/env bash

source utils.sh
check_kite

echo Using profile: ${PROFILE}

ADDITIONAL_OPTS=$(read_prop_file_to_opts ${ZDS_CONF})
OPTS="$ADDITIONAL_OPTS -noverify -Dspring.profiles.active=${PROFILE} -Dserver.servlet.contextPath=/notebook"
java ${JAVA_OPTS} -jar ${OPTS} zds-server.jar