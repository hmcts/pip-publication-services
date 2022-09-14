package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
