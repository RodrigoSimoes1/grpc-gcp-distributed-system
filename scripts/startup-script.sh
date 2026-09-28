#!/bin/bash

# ==========================================
# Dependencies Installation
# ==========================================

apt-get update
apt-get install -y openjdk-25-jre-headless

# ==========================================
# Directory folders and files
# ==========================================

mkdir -p /var/grpcserver
chmod 755 /var/grpcserver

# ==========================================
# Downloads
# ==========================================

gsutil cp gs://final-work-bucket-isel2026-g01/server/CN2026Labels-Server-1.0.jar /var/grpcserver/server.jar

gsutil cp gs://final-work-bucket-isel2026-g01/credentials/cn2526-t1-g01-6081d5c94bf8.json /var/grpcserver/key.json

#gsutil cp gs://final-work-bucket-isel2026-g01/labels-worker/CN2026Labels-LabelsApp-1.0.jar /var/grpcserver/server.jar

# ==========================================
# App execution
# ==========================================

# Exports the credentials
cd /var/grpcserver || exit 1
export GOOGLE_APPLICATION_CREDENTIALS=key.json

# Executes the JAR in background
#nohup java -jar /var/grpcserver/server.jar [args] > /var/grpcserver/server.log 2>&1 &

nohup java -jar /var/grpcserver/server.jar > /var/grpcserver/server.log 2>&1 &