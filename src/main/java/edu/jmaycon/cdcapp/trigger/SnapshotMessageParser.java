package edu.jmaycon.cdcapp.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmaycon.cdcapp.model.SnapshotId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SnapshotMessageParser {

    private final ObjectMapper objectMapper;

    public SnapshotEvent parse(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            long to = node.get("to").asLong();
            JsonNode fromNode = node.get("from");
            Long from = (fromNode == null || fromNode.isNull()) ? null : fromNode.asLong();
            return new SnapshotEvent(from == null ? null : new SnapshotId(from), new SnapshotId(to));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse snapshot message", e);
        }
    }
}
