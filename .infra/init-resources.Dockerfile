FROM alpine:3.21

# Install required tools
RUN apk add --no-cache \
    curl \
    jq \
    aws-cli \
    bash \
    openjdk17-jre-headless

# Download and install Kafka
RUN curl -L https://archive.apache.org/dist/kafka/3.8.0/kafka_2.13-3.8.0.tgz -o /tmp/kafka.tgz && \
    tar -xzf /tmp/kafka.tgz -C /opt && \
    mv /opt/kafka_2.13-3.8.0 /opt/kafka && \
    rm /tmp/kafka.tgz

# Copy init script
COPY .infra/init-resources.sh /init-resources.sh
RUN chmod +x /init-resources.sh

ENTRYPOINT ["/init-resources.sh"]
