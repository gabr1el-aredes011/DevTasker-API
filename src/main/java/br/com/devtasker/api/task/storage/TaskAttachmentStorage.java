package br.com.devtasker.api.task.storage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface TaskAttachmentStorage {

    void store(String storageKey, InputStream content) throws IOException;

    Resource load(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
