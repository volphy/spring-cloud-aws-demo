package com.krzysztofwilk.spring.cloud.demo.images;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@RequestMapping("/demo")
public class ImagesController {

    private static final String S3_PROTOCOL_PREFIX = "s3://";
    private static final String ALL_IMAGES_SUFFIX = "/**/*.jpg";

    private final ResourcePatternResolver resourcePatternResolver;

    private final ResourceLoader resourceLoader;

    private final AmazonS3Client s3Client;

    @Value("#{systemProperties['s3BucketName']}")
    private String s3BucketName;

    @GetMapping(value = "images", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getFiles() throws IOException {
        log.info("Downloading list of images from S3 bucket {}", s3BucketName);

        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX
                + s3BucketName
                + ALL_IMAGES_SUFFIX);

        AtomicInteger index = new AtomicInteger(1);
        List<FileEntry> files = Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(f -> new FileEntry(index.getAndIncrement(), f.getFilename()))
                .collect(toList());

        Gson gson = new Gson();

        return new ResponseEntity<>(gson.toJson(files), HttpStatus.OK);
    }

    @GetMapping(value = "image/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity getFile(@PathVariable int id, HttpServletResponse response) throws IOException {
        log.info("Downloading image no. {} from S3 bucket {}", id, s3BucketName);

        Resource image = findImageById(id);

        InputStream inputStream;
        try {
            inputStream = image.getInputStream();
        } catch (IOException e) {
            log.error("Cannot get input stream from S3 bucket {} - {}", s3BucketName, e.getMessage(), e);
            throw e;
        }

        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + image.getFilename());

        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        return new ResponseEntity<>(inputStreamResource, HttpStatus.OK);
    }

    @PutMapping(value = "image/{id}")
    public ResponseEntity updateFile(@PathVariable int id, @RequestParam MultipartFile file) throws IOException {
        log.info("Updating image no. {} from S3 bucket {}", id, s3BucketName);

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

    @DeleteMapping(value = "image/{id}")
    public ResponseEntity deleteFile(@PathVariable int id) throws IOException {
        log.info("Deleting image no. {} from S3 bucket {}", id, s3BucketName);

        Resource image = findImageById(id);

        s3Client.deleteObject(s3BucketName, image.getFilename());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Resource findImageById(@PathVariable int id) throws IOException {
        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX
                + s3BucketName
                + ALL_IMAGES_SUFFIX);

        return Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .skip(id - 1)
                .findFirst()
                .orElseThrow(() -> new MissingResourceException("Missing image id=" + id, "String", "id"));
    }

    @PostMapping(value = "images")
    public ResponseEntity sendFile(@RequestParam MultipartFile file) throws IOException {
        log.info("Uploading new image to S3 bucket {}", s3BucketName);

        InputStream inputStream = new ByteArrayInputStream(file.getBytes());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(s3BucketName, file.getOriginalFilename(), inputStream, metadata);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
