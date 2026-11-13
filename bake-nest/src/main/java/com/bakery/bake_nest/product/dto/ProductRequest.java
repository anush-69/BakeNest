package com.bakery.bake_nest.product.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @NotNull @Positive private Long categoryId;
    @NotNull private Boolean available = true;

    @Size(max = 1000)
    private String imageUrl;
}
