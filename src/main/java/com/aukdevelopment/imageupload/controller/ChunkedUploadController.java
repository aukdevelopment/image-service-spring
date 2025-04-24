package com.aukdevelopment.imageupload.controller;

import com.aukdevelopment.imageupload.service.ChunkedImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLConnection;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ChunkedUploadController {

    private final ChunkedImageService service;

    public ChunkedUploadController(ChunkedImageService service) {
        this.service = service;
    }

    /**
     * Receives multipart form data with parts:
     * - chunk (FilePart)
     * - uploadId (String)
     * - chunkIndex (Integer)
     * - totalChunks (Integer)
     * - filename (String)
     */

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Map<String, Object>> uploadChunk(
            @RequestPart("chunk") Mono<FilePart> chunkMono,
            @RequestPart("uploadId") Mono<String> uploadIdMono,
            @RequestPart("chunkIndex") Mono<String> chunkIndexMono,
            @RequestPart("totalChunks") Mono<String> totalChunksMono,
            @RequestPart("filename") Mono<String> filenameMono
    ) {
        return Mono.zip(chunkMono, uploadIdMono, chunkIndexMono, totalChunksMono, filenameMono)
                .flatMap(tuple -> {
                    FilePart chunk = tuple.getT1();
                    String uploadId = tuple.getT2();
                    int chunkIndex = Integer.parseInt(tuple.getT3());
                    int totalChunks = Integer.parseInt(tuple.getT4());
                    String filename = tuple.getT5();
                    return service.processChunkReactive(chunk, uploadId, chunkIndex, filename, totalChunks);
                });
    }

    @GetMapping(value = "/{filename:.+}")
    public Mono<ResponseEntity<Resource>> serveImage(
            @PathVariable String filename
    ) {
        return Mono.fromCallable(() -> {
            Resource file = service.loadAsResource(filename);
            String contentType = URLConnection.guessContentTypeFromName(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(file);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
