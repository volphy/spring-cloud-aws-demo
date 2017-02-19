package com.krzysztofwilk.spring.cloud.demo.images;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
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

    private final ResourcePatternResolver resourcePatternResolver;

    private final String s3BucketName;


    @Autowired
    public ImagesController(ResourcePatternResolver resourcePatternResolver) {

        this.resourcePatternResolver = resourcePatternResolver;

        if (System.getProperty(S3_BUCKET_NAME) == null) {
            throw new MissingResourceException("Missing S3 Bucket Name", "String", S3_BUCKET_NAME);
        }

        this.s3BucketName = System.getProperty(S3_BUCKET_NAME);
    }

    @RequestMapping(method = RequestMethod.GET, value = "images", produces = "application/json")
    public ResponseEntity getFiles() throws IOException {
        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources("s3://" + s3BucketName + "/**/*.jpg");

        AtomicInteger index = new AtomicInteger(1);
        List<FileEntry> files = Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(f -> new FileEntry(index.getAndIncrement(), f.getFilename()))
                .collect(toList());

        Gson gson = new Gson();

        // TODO: return proper HTTP Statuses (as specified by HTTP Specification) in exceptional situations

        return new ResponseEntity<>(gson.toJson(files), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "image/{id}")
    public ResponseEntity getFile(@PathVariable int id) throws IOException {

        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources("s3://" + s3BucketName + "/**/*.jpg");

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

        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        // TODO: return proper HTTP Statuses (as specified by HTTP Specification) in exceptional situations

        return new ResponseEntity<>(inputStreamResource, HttpStatus.OK);
    }
}
