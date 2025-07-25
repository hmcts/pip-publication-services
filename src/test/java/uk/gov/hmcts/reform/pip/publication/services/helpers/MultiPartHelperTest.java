package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiPartHelperTest {
    @Test
    void testCreateSingleMultiPartByteArrayBody() {
        byte[] data = {10, 20, 30};
        List<Triple<String, byte[], String>> input = Collections.singletonList(
            Triple.of("file", data, "filename.pdf")
        );
        MultiValueMap<String, HttpEntity<?>> output = MultiPartHelper.createMultiPartByteArrayBody(input);

        assertThat(output).hasSize(1);
        HttpEntity<?> result = output.get("file").get(0);
        assertThat(result.getHeaders().get("Content-Disposition"))
            .as("Incorrect content disposition")
            .hasSize(1)
            .first()
            .isEqualTo("form-data; name=\"file\"; filename=\"filename.pdf\"");
        assertThat(((ByteArrayResource) result.getBody()).getByteArray())
            .as("Incorrect byte array data")
            .isEqualTo(data);
    }

    @Test
    void testCreateMultipleMultiPartByteArrayBody() {
        byte[] data1 = {10, 20, 30};
        byte[] data2 = {20, 20};
        List<Triple<String, byte[], String>> input = Arrays.asList(
            Triple.of("file1", data1, null),
            Triple.of("file2", data2, "file2.pdf")
        );
        MultiValueMap<String, HttpEntity<?>> output = MultiPartHelper.createMultiPartByteArrayBody(input);

        assertThat(output).hasSize(2);

        HttpEntity<?> file1 = output.get("file1").get(0);
        assertThat(file1.getHeaders()).isEmpty();
        assertThat(((ByteArrayResource) file1.getBody()).getByteArray())
            .as("Incorrect byte array data")
            .isEqualTo(data1);

        HttpEntity<?> file2 = output.get("file2").get(0);
        assertThat(file2.getHeaders().get("Content-Disposition"))
            .as("Incorrect content disposition")
            .hasSize(1)
            .first()
            .isEqualTo("form-data; name=\"file2\"; filename=\"file2.pdf\"");
        assertThat(((ByteArrayResource) file2.getBody()).getByteArray())
            .as("Incorrect byte array data")
            .isEqualTo(data2);
    }


    @Test
    void testGetFileExtension() {
        MultipartFile file = new MockMultipartFile(
            "test.html",
            "test.html",
            "text/html",
            new byte[0]);

        String ext = MultiPartHelper.getFileExtension(file);
        assertEquals("html", ext,
                     "Expected extension to be html");
    }

    @Test
    void testGetFileExtensionWithUppercaseExtension() {
        MultipartFile file = new MockMultipartFile(
            "test.HTML",
            "test.HTML",
            "text/html",
            new byte[0]);

        String ext = MultiPartHelper.getFileExtension(file);
        assertEquals("html", ext,
                     "Expected extension to be in lowercase");
    }

    @Test
    void testGetFileExtensionWithNoExtension() {
        MultipartFile fileWithNoExtension = new MockMultipartFile(
            "test",
            "test",
            "text/plain",
            new byte[0]);

        String ext = MultiPartHelper.getFileExtension(fileWithNoExtension);
        assertEquals("", ext,
                     "Expected extension to be empty when there is no extension in filename");
    }

    @Test
    void testGetFileExtensionWithNoFilename() {
        MultipartFile mockEmptyFile = Mockito.mock(MultipartFile.class);
        Mockito.when(mockEmptyFile.getOriginalFilename()).thenReturn(null);

        String ext = MultiPartHelper.getFileExtension(mockEmptyFile);
        assertEquals("", ext,
                     "Expected extension to be empty when filename is null");
    }

}
