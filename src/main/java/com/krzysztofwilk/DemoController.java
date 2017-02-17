package com.krzysztofwilk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.InputStreamResource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.StringJoiner;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final Logger LOG = LoggerFactory.getLogger(DemoController.class);

    private static final String S3_BUCKET_NAME = "s3BucketName";
    private static final String S3_FILE_NAME = "s3FileName";

    private final ResourceLoader resourceLoader;

    private final String s3ImageFile;

    @Autowired
    public DemoController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;

        if (System.getProperty(S3_BUCKET_NAME) == null) {
            throw new MissingResourceException("Missing S3 Bucket Name", "String", S3_BUCKET_NAME);
        }

        if (System.getProperty(S3_FILE_NAME) == null) {
            throw new MissingResourceException("Missing S3 File Name", "String", S3_FILE_NAME);
        }

        StringJoiner stringJoiner = new StringJoiner("/", "s3://", "");
        this.s3ImageFile = stringJoiner.add(System.getProperty(S3_BUCKET_NAME))
                .add(System.getProperty(S3_FILE_NAME))
                .toString();
    }

    @RequestMapping("image")
    public ResponseEntity getFile() throws IOException {

        Resource resource = this.resourceLoader.getResource(s3ImageFile);

        InputStream inputStream;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException e) {
            LOG.error("Cannot get input stream from S3: {}", e.getMessage(), e);
            throw e;
        }

        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        return new ResponseEntity<>(inputStreamResource, HttpStatus.OK);
    }
}
