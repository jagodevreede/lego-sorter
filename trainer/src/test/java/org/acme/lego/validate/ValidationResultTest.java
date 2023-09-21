package org.acme.lego.validate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void testSort() {
        var listOfResults = new java.util.ArrayList<>(List.of(
                new ValidationResult(1, 0.20),
                new ValidationResult(2, 0.30),
                new ValidationResult(3, 0.50),
                new ValidationResult(4, 0.10),
                new ValidationResult(5, 0.0)
        ));

        listOfResults.sort(ValidationResult::compareTo);

        // "Best epoch was 3"
        assertEquals(3, listOfResults.get(0).epoch());
    }

}