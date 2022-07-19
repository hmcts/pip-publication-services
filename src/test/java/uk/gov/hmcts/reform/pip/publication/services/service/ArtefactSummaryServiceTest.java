package uk.gov.hmcts.reform.pip.publication.services.service;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ArtefactSummaryServiceTest {

    @Test
    void civilDailyCauseList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "civilDailyCauseList.json")),
                     writer, Charset.defaultCharset());
        assertThat(writer.toString().contains("sitting"));
    }


}
