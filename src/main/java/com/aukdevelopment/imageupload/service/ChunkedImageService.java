package com.aukdevelopment.imageupload.service;

import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ChunkedImageService {

    @Value("${uploads.temp-dir}")
    private Path tempDir;

    @Value("${uploads.storage-dir}")
    private Path storageDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(tempDir);
        Files.createDirectories(storageDir);
    }

    public Path getTempDir() {
        return tempDir;
    }

    public boolean isUploadComplete(String uploadId, int totalChunks) throws IOException {
        try (Stream<Path> files = Files.list(tempDir.resolve(uploadId))) {
            return files.count() == totalChunks;
        }
    }

    public String assembleAndOptimize(String uploadId, String originalFilename, int totalChunks) throws IOException {
        // Always output JPEG
        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path finalImage = storageDir.resolve(fileName);

        // Merge chunks
        String origExt = StringUtils.getFilenameExtension(originalFilename);
        Path merged = tempDir.resolve(uploadId + "_merged." + (origExt != null ? origExt : "tmp"));
        try (OutputStream os = Files.newOutputStream(merged, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < totalChunks; i++) {
                Files.copy(tempDir.resolve(uploadId).resolve("chunk_" + i), os);
            }
        }

        // Optimize and convert to JPG
        try (InputStream in = Files.newInputStream(merged);
             OutputStream out = Files.newOutputStream(finalImage)) {
            Thumbnails.of(in)
                    .size(1200, 1200)
                    .outputFormat("jpg")
                    .outputQuality(0.7)
                    .toOutputStream(out);
        }

        // Cleanup
        FileSystemUtils.deleteRecursively(tempDir.resolve(uploadId));
        Files.deleteIfExists(merged);

        return fileName;
    }

    public Mono<Map<String, Object>> processChunkReactive(
            FilePart chunkPart,
            String uploadId,
            int chunkIndex,
            String filename,
            int totalChunks) {
        try {
            Files.createDirectories(tempDir.resolve(uploadId));
        } catch (IOException e) {
            return Mono.error(e);
        }
        Path chunkPath = tempDir.resolve(uploadId).resolve("chunk_" + chunkIndex);
        return chunkPart.transferTo(chunkPath)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> result = new HashMap<>();
                    if (isUploadComplete(uploadId, totalChunks)) {
                        String url = assembleAndOptimize(uploadId, filename, totalChunks);
                        result.put("complete", true);
                        result.put("url", url);
                    } else {
                        result.put("complete", false);
                    }
                    return result;
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    public Resource loadAsResource(String filename) throws MalformedURLException, FileNotFoundException {
        Path file = storageDir.resolve(filename).normalize();
        UrlResource resource = new UrlResource(file.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new FileNotFoundException("Could not read file: " + filename);
    }
}
