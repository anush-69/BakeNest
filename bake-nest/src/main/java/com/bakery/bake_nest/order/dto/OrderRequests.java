package com.bakery.bake_nest.order.dto;

import com.bakery.bake_nest.order.model.OrderStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class OrderRequests {
    @Getter
    @Setter
    public static class Item {
        @NotNull @Positive private Long productId;

        @NotNull
        @Min(1)
        @Max(10000)
        private Integer quantity;
    }

    @Getter
    @Setter
    public static class Create {
        @NotEmpty
        @Size(max = 100)
        private List<@NotNull @Valid Item> items;

        @NotBlank
        @Size(max = 200)
        private String customerName;

        @NotBlank
        @Size(max = 20)
        private String customerPhone;

        @NotBlank
        @Size(max = 500)
        private String deliveryAddress;

        @NotBlank
        @Size(max = 100)
        private String deliveryCity;

        @NotBlank
        private String paymentMethod;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal deliveryCost;

        @Size(max = 1000)
        private String customerNote;
    }

    @Getter
    @Setter
    public static class Update {
        @NotBlank
        @Size(max = 200)
        private String customerName;

        @NotBlank
        @Size(max = 20)
        private String customerPhone;

        @NotBlank
        @Size(max = 500)
        private String deliveryAddress;

        @NotBlank
        @Size(max = 100)
        private String deliveryCity;

        @NotBlank
        private String paymentMethod;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal deliveryCost;

        @Size(max = 1000)
        private String customerNote;
    }

    @Getter
    @Setter
    public static class Quantity {
        @NotNull
        @Min(1)
        @Max(10000)
        private Integer quantity;
    }

    @Getter
    @Setter
    public static class Status {
        @NotNull private OrderStatus status;
    }
}
