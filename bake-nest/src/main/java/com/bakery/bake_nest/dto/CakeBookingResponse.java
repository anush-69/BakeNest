package com.bakery.bake_nest.dto;

import java.time.LocalDate;
import java.math.BigDecimal;


public class CakeBookingResponse {

    private Long id;
    private String cakeType;
    private LocalDate requiredDate;
    private String status;

    private String cakeSize;
    private String flavour;
    private String designDescription;
    private String cakeMessage;
    private String referenceImageUrl;
    private BigDecimal estimatedPrice;
    private BigDecimal quotedPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCakeType() {
        return cakeType;
    }

    public void setCakeType(String cakeType) {
        this.cakeType = cakeType;
    }

    public LocalDate getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(LocalDate requiredDate) {
        this.requiredDate = requiredDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCakeSize() {
        return cakeSize;
    }

    public void setCakeSize(String cakeSize) {
        this.cakeSize = cakeSize;
    }

    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    public String getDesignDescription() {
        return designDescription;
    }

    public void setDesignDescription(String designDescription) {
        this.designDescription = designDescription;
    }

    public String getCakeMessage() {
        return cakeMessage;
    }

    public void setCakeMessage(String cakeMessage) {
        this.cakeMessage = cakeMessage;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getQuotedPrice() {
        return quotedPrice;
    }

    public void setQuotedPrice(BigDecimal quotedPrice) {
        this.quotedPrice = quotedPrice;
    }
}