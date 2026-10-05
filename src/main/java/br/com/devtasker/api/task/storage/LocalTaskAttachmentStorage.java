package br.com.devtasker.api.task.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

@Component
public class LocalTaskAttachmentStorage implements TaskAttachmentStorage {

    private final Path storageRoot;

    public LocalTaskAttachmentStorage(
            @Value("${app.task-attachments.storage-root}") String storageRoot
    ) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream content) throws IOException {
        Files.createDirectories(storageRoot);

        Path target = resolveSafely(storageKey);
        Files.copy(content, target);
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path source = resolveSafely(storageKey);

        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new IOException("Arquivo armazenado não encontrado.");
        }

        return new UrlResource(source.toUri());
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafely(storageKey));
    }

    private Path resolveSafely(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();

        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida.");
        }

        return resolved;
    }
}
