package edu.jmaycon.cdcapp.state;

import edu.jmaycon.cdcapp.model.SnapshotId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CursorStore {
    private final Path cursorPath;

    public Optional<SnapshotId> load() {
        if (!Files.exists(cursorPath)) {
            return Optional.empty();
        }
        try {
            String raw = Files.readString(cursorPath).trim();
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SnapshotId(Long.parseLong(raw)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read snapshot cursor", ex);
        }
    }

    public void save(SnapshotId snapshotId) {
        try {
            Files.createDirectories(cursorPath.getParent());
            Files.writeString(cursorPath, Long.toString(snapshotId.value()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist snapshot cursor", ex);
        }
    }
}
