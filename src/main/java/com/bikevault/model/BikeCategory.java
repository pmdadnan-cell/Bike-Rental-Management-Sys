package com.bikevault.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bike classification used for rates, late fees, and reporting.
 */
public class BikeCategory {

    private Long id;
    private String name;
    private String description;
    private BigDecimal lateFeePerDay;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getLateFeePerDay() {
        return lateFeePerDay;
    }

    public void setLateFeePerDay(BigDecimal lateFeePerDay) {
        this.lateFeePerDay = lateFeePerDay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
