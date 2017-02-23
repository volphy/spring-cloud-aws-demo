package com.krzysztofwilk.spring.cloud.demo.images;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@RequestMapping("/demo")
class ImagesController {

    @Value("#{systemProperties['s3BucketName']}")
    private String s3BucketName;

    private final ImagesService imagesService;

    @GetMapping(value = "images", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity getFiles() throws IOException {
        log.info("Downloading list of images from S3 bucket {}", s3BucketName);

        AtomicInteger index = new AtomicInteger(1);
        List<FileEntry> files = Arrays.stream(imagesService.getResources())
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(f -> new FileEntry(index.getAndIncrement(), f.getFilename()))
                .collect(toList());

        Gson gson = new Gson();

        return new ResponseEntity<>(gson.toJson(files), HttpStatus.OK);
    }

    @GetMapping(value = "image/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<InputStreamResource> getFile(@PathVariable int id, HttpServletResponse response) throws IOException {
        log.info("Downloading image no. {} from S3 bucket {}", id, s3BucketName);

        Resource image = imagesService.findImageById(id);

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
    ResponseEntity updateFile(@PathVariable int id, @RequestParam MultipartFile file) throws IOException {
        log.info("Updating image no. {} from S3 bucket {}", id, s3BucketName);

        imagesService.updateResource(id, file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(value = "image/{id}")
    ResponseEntity deleteFile(@PathVariable int id) throws IOException {
        log.info("Deleting image no. {} from S3 bucket {}", id, s3BucketName);

        imagesService.deleteResource(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "images")
    ResponseEntity sendFile(@RequestParam MultipartFile file) throws IOException {
        log.info("Uploading new image to S3 bucket {}", s3BucketName);

        imagesService.createResource(file);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
