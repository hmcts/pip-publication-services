package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Getter
public enum Environments {
    PRODUCTION("prod", "Production"),
    STAGING("stg", "Staging"),
    TEST("test", "Test"),
    ITHC("ithc", "ITHC"),
    DEMO("demo", "Demo"),
    SANDBOX("sbox", "Sandbox"),
    LOCAL("local", "Local");

    private String originalName;
    private String formattedName;

    public static String convertEnvironmentName(String input) {
        Optional<Environments> matchedEnvironment = Arrays.stream(Environments.values())
            .filter(env -> env.originalName.equals(input))
            .findFirst();

        return matchedEnvironment.isPresent() ? matchedEnvironment.get().getFormattedName() : input;
    }
}
