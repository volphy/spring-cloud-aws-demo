package com.krzysztofwilk.spring.cloud.demo.images;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest(ImagesController.class)
public class ImagesTest {

    private static final String[] SAMPLE_IMAGE_NAMES = {"images/sample1.jpg", "images/sample2.jpg"};
    private static final String[] SAMPLE_IMAGES = { "src/test/resources/images/sample1.jpg",
            "src/test/resources/images/sample2.jpg"};

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ImagesService imagesService;

    @Test
    public void getListOfImages() throws Exception {
        Resource[] resources = { mock(Resource.class), mock(Resource.class) };
        given(resources[0].getFilename()).willReturn(SAMPLE_IMAGE_NAMES[0]);
        given(resources[1].getFilename()).willReturn(SAMPLE_IMAGE_NAMES[1]);

        given(imagesService.getResources()).willReturn(resources);

        mvc.perform(get("/demo/images"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$..name", containsInAnyOrder(SAMPLE_IMAGE_NAMES[0], SAMPLE_IMAGE_NAMES[1])));
    }

    private List<FileEntry> fileEntries(ResponseEntity files) {
        Gson gson = new Gson();
        return gson.fromJson(files.getBody().toString(),
                new TypeToken<List<FileEntry>>(){}.getType());
    }

    @Test
    public void getFirstImageFromTheList() throws Exception {
        Resource resource = mock(Resource.class);
        given(resource.getInputStream()).willReturn(new FileInputStream(SAMPLE_IMAGES[0]));

        given(imagesService.findImageById(anyInt())).willReturn(resource);

        mvc.perform(get("/demo/image/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(StreamUtils.copyToByteArray(new FileInputStream(SAMPLE_IMAGES[0]))));
    }

    @Test
    public void createNewImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
                StreamUtils.copyToByteArray(new FileInputStream(SAMPLE_IMAGES[1])));

        doNothing().when(imagesService).createResource(any());

        mvc.perform(fileUpload("/demo/images")
                .file(file))
                .andExpect(status().isCreated());
    }

    @Test
    public void updateFirstImageFromEmptyList() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
                StreamUtils.copyToByteArray(new FileInputStream(SAMPLE_IMAGES[0])));

        doNothing().when(imagesService).updateResource(anyInt(), any());

        mvc.perform(fileUpload("/demo/image/{id}", 1)
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteFirstImageFromEmptyList() throws Exception {

        doNothing().when(imagesService).deleteResource(anyInt());

        mvc.perform(delete("/demo/image/{id}", 1))
                .andExpect(status().isOk());
    }
}
