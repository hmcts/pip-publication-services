package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
@Slf4j
public final class GeneralHelper {

    private GeneralHelper() {
        throw new UnsupportedOperationException();
    }

    public static String stringDelimiter(String text, String delimiter) {
        return text.isEmpty() ? "" : delimiter;
    }

    public static String findAndReturnNodeText(JsonNode node, String nodeName) {
        if (node.has(nodeName)) {
            return node.get(nodeName).asText();
        }
        return "";
    }

    public static String trimAnyCharacterFromStringEnd(String text) {
        return StringUtils.isBlank(text) ? "" : text.trim().replaceAll(",$", "");
    }

    public static void appendToStringBuilder(StringBuilder builder, String text, JsonNode node,
                                             String nodeName) {
        builder.append('\n')
            .append(text)
            .append(GeneralHelper.findAndReturnNodeText(node, nodeName));
    }

    public static void loopAndFormatString(JsonNode nodes, String nodeName,
                                           StringBuilder builder, String delimiter) {
        nodes.get(nodeName).forEach(node -> {
            if (!node.asText().isEmpty()) {
                builder
                    .append(node.asText())
                    .append(delimiter);
            }
        });
    }

    public static String listTypeToCamelCase(ListType listType) {
        StringBuilder outputString = new StringBuilder();
        List<String> splitList = List.of(listType.toString().split("_"));
        outputString.append(splitList.get(0).toLowerCase(Locale.UK))
            .append(splitList.stream().skip(1).map(WordUtils::capitalizeFully).collect(Collectors.joining()));
        return outputString.toString();
    }

    @SuppressWarnings("PMD.AvoidCatchingNPE")
    public static String safeGet(String jsonPath, JsonNode node) {
        return safeGetNode(jsonPath, node).asText();
    }

    @SuppressWarnings("PMD.AvoidCatchingNPE")
    public static JsonNode safeGetNode(String jsonPath, JsonNode node) {
        String[] stringArray = jsonPath.split("\\.");
        JsonNode outputNode = node;
        int index = -1;
        try {
            for (String arg : stringArray) {
                if (NumberUtils.isCreatable(arg)) {
                    outputNode = outputNode.get(Integer.parseInt(arg));
                } else {
                    outputNode = outputNode.get(arg);
                }
                index += 1;
            }
            return outputNode;
        } catch (NullPointerException e) {
            log.error("Parsing failed for path " + jsonPath + ", specifically " + stringArray[index]);
            return node;
        }
    }
}
