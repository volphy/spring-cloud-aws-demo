package com.krzysztofwilk.spring.cloud.demo.images;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.MissingResourceException;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ImagesService {

    private static final String S3_PROTOCOL_PREFIX = "s3://";
    private static final String ALL_IMAGES_SUFFIX = "/**/*.jpg";

    private final ResourcePatternResolver resourcePatternResolver;

    private final AmazonS3Client s3Client;

    @Value("#{systemProperties['s3BucketName']}")
    private String s3BucketName;

    Resource[] getResources() throws IOException {
        return this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX
                + s3BucketName
                + ALL_IMAGES_SUFFIX);

    }

    Resource findImageById(int id) throws IOException {
        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX
                + s3BucketName
                + ALL_IMAGES_SUFFIX);

        return Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .skip(id - 1)
                .findFirst()
                .orElseThrow(() -> new MissingResourceException("Missing image id=" + id, "String", "id"));
    }

    void updateResource(int id, MultipartFile file) throws IOException {
        Resource resource = findImageByIdAndName(id, file.getOriginalFilename());

        WritableResource writableResource = (WritableResource) resource;
        try (OutputStream outputStream = writableResource.getOutputStream()) {
            outputStream.write(file.getBytes());
        }
    }

    private Resource findImageByIdAndName(int id, String name) throws IOException {
        Resource[] allImagesInBucket =  this.resourcePatternResolver.getResources(S3_PROTOCOL_PREFIX
                + s3BucketName
                + ALL_IMAGES_SUFFIX);

        return Arrays.stream(allImagesInBucket)
                .sorted(Comparator.comparing(Resource::getFilename))
                .skip(id - 1)
                .filter(r -> r.getFilename().contentEquals(name))
                .findFirst()
                .orElseThrow(() ->
                        new MissingResourceException("Missing image id=" + id + " name=" + name, "String", "id"));
    }

    void deleteResource(int id) throws IOException {
        Resource image = findImageById(id);

        s3Client.deleteObject(s3BucketName, image.getFilename());
    }

    void createResource(MultipartFile file) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(file.getBytes());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(s3BucketName, file.getOriginalFilename(), inputStream, metadata);
    }
}
