package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

import java.util.List;

public final class MultiPartHelper {
    private MultiPartHelper() {
    }

    /**
     * Create multi part file body from byte array data.
     * @param parts list of the part body containing the part name, the byte array data and the filename
     * @return multi part body
     */
    public static MultiValueMap<String, HttpEntity<?>> createMultiPartByteArrayBody(//NOSONAR
        List<Triple<String, byte[], String>> parts
    ) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        parts.forEach(p -> {
            MultipartBodyBuilder.PartBuilder part = builder.part(p.getLeft(), new ByteArrayResource(p.getMiddle()));
            String filename = p.getRight();
            if (StringUtils.isNotEmpty(filename)) {
                part.filename(filename);
            }
        });
        return builder.build();
    }
}
