FROM alpine:3.21

# Install required tools
RUN apk add --no-cache \
    curl \
    jq \
    aws-cli \
    openjdk17-jre-headless

# Copy Kafka binaries from official Kafka image
COPY --from=apache/kafka:3.8.0 /opt/kafka /opt/kafka

# Copy init script
COPY init-resources.sh /init-resources.sh
RUN chmod +x /init-resources.sh

ENTRYPOINT ["/init-resources.sh"]
