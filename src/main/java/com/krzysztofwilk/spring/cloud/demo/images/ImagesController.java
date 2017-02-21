package com.krzysztofwilk.spring.cloud.demo.images;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/demo")
public class ImagesController {

    private static final Logger LOG = LoggerFactory.getLogger(ImagesController.class);

    private static final String S3_BUCKET_NAME = "s3BucketName";

    private static final String S3_PROTOCOL_PREFIX = "s3://";

    private final ResourcePatternResolver resourcePatternResolver;

    private final ResourceLoader resourceLoader;

    private final String s3BucketName;

    @Autowired
    public ImagesController(ResourcePatternResolver resourcePatternResolver, ResourceLoader resourceLoader) {

        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;

        if (System.getProperty(S3_BUCKET_NAME) == null) {
            throw new MissingResourceException("Missing S3 Bucket Name", "String", S3_BUCKET_NAME);
        }

        this.s3BucketName = System.getProperty(S3_BUCKET_NAME);
    }

    @GetMapping(value = "images", produces = "application/json")
    public ResponseEntity getFiles() throws IOException {
        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX + s3BucketName + "/**/*.jpg");

        AtomicInteger index = new AtomicInteger(1);
        List<FileEntry> files = Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(f -> new FileEntry(index.getAndIncrement(), f.getFilename()))
                .collect(toList());

        Gson gson = new Gson();

        return new ResponseEntity<>(gson.toJson(files), HttpStatus.OK);
    }

    @GetMapping(value = "image/{id}", produces = "application/octet-stream")
    public ResponseEntity getFile(@PathVariable int id, HttpServletResponse response) throws IOException {

        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX + s3BucketName + "/**/*.jpg");

        Resource image = Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .skip(id - 1)
                .findFirst()
                .orElseThrow(() -> new MissingResourceException("Missing image id=" + id, "String", "id"));

        InputStream inputStream;
        try {
            inputStream = image.getInputStream();
        } catch (IOException e) {
            LOG.error("Cannot get input stream from S3: {}", e.getMessage(), e);
            throw e;
        }

        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + image.getFilename());

        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        return new ResponseEntity<>(inputStreamResource, HttpStatus.OK);
    }

    @PutMapping(value = "image/{id}")
    public ResponseEntity updateFile(@PathVariable int id, @RequestParam MultipartFile file) throws IOException {

        Resource resource = this.resourceLoader.getResource(S3_PROTOCOL_PREFIX
                + s3BucketName
                + "/"
                + file.getOriginalFilename());

        WritableResource writableResource = (WritableResource) resource;
        try (OutputStream outputStream = writableResource.getOutputStream()) {
            outputStream.write(file.getBytes());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
