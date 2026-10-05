package br.com.devtasker.api.task.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalTaskAttachmentStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldStoreLoadAndDeleteFile() throws Exception {
        LocalTaskAttachmentStorage storage = new LocalTaskAttachmentStorage(
                temporaryDirectory.toString()
        );
        byte[] content = "arquivo de teste".getBytes();

        storage.store("safe-key", new ByteArrayInputStream(content));

        assertTrue(storage.load("safe-key").exists());
        try (InputStream storedContent = storage.load("safe-key").getInputStream()) {
            assertArrayEquals(content, storedContent.readAllBytes());
        }

        storage.delete("safe-key");

        assertFalse(Files.exists(temporaryDirectory.resolve("safe-key")));
    }

    @Test
    void shouldRejectPathTraversal() {
        LocalTaskAttachmentStorage storage = new LocalTaskAttachmentStorage(
                temporaryDirectory.toString()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.load("../outside")
        );
    }
}
