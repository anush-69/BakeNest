package com.bakery.bake_nest.review.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

public class ReviewRequests {
    @Getter
    @Setter
    public static class Edit {
        @NotNull
        @Min(1)
        @Max(5)
        private Integer rating;

        @NotBlank
        @Size(max = 2000)
        private String comment;
    }

    @Getter
    @Setter
    public static class Create extends Edit {
        @NotNull @Positive private Long productId;
    }

    @Getter
    @Setter
    public static class Moderation {
        @NotBlank
        @Pattern(regexp = "APPROVED|REJECTED")
        private String status;

        @Size(max = 1000)
        private String note;
    }
}
