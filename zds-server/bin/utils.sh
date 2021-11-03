#!/usr/bin/env bash

read_prop_file_to_opts() {
  local file=$1
  echo Read properties from $file >&2
  local opts=""
  if [[ -f ${file} ]]; then
    while read prop; do
      if [[ "$prop" == *"="* ]]; then
        opts="$opts -D$prop"
      fi
    done <${file}
  fi
  echo $opts
}

check_kite() {
  if [ -z "${ZDS_KITE_URL}" ]; then
    echo KITE url is unset, skip...
    return
  fi
  echo Test KITE service ${ZDS_KITE_URL}
  local code=""
  for (( ; ; )); do
    code=$(curl --write-out %{http_code} --silent --output /dev/null ${ZDS_KITE_URL}/kite/test)
    if [[ "$code" == 200 ]]; then
      echo KITE service is fine
      return
    fi
    echo Error! KITE service is not working
    sleep 3
  done
}

contains() {
  if [[ $1 =~ (^|[[:space:]])$2($|[[:space:]]) ]]; then
    echo yes
  else
    echo no
  fi
}