#!/usr/bin/env bash

export DW_PORT=9001
export DW_ADMIN_PORT=9101

java -jar target/zookeeper-leader-0.1.0-*.jar server config.yml