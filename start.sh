#!/usr/bin/env bash

export DW_PORT=9000
export DW_ADMIN_PORT=9100

java -jar target/zookeeper-leader-0.1.0-*.jar server config.yml