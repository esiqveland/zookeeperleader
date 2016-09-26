#!/usr/bin/env bash

curl -s -H "Accept: application/json" http://localhost:9000/dealer/next | jq
