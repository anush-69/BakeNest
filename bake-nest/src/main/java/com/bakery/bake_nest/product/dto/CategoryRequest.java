package com.bakery.bake_nest.product.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;
}
