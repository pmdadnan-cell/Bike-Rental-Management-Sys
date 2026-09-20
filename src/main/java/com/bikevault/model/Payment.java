package com.bikevault.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment collected against a rental.
 */
public class Payment {

    private Long id;
    private Long rentalId;
    private Long userId;
    private BigDecimal amount;
    private String method;
    private String status;
    private String referenceNumber;
    private LocalDateTime paidAt;
    private String notes;
    private String customerName;
    private String bikeLabel;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getBikeLabel() {
        return bikeLabel;
    }

    public void setBikeLabel(String bikeLabel) {
        this.bikeLabel = bikeLabel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String displayStatus() {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "PAID";
        }
        return status;
    }

    public boolean isPenaltyPayment() {
        return notes != null && notes.toUpperCase().contains("PENALTY");
    }

    public String displayType() {
        if (isPenaltyPayment()) {
            return "PENALTY PAYMENT";
        }
        return "RENTAL PAYMENT";
    }

    public String penaltyLabel() {
        if (notes == null || !isPenaltyPayment()) {
            return null;
        }
        int sep = notes.indexOf('·');
        return sep >= 0 ? notes.substring(sep + 1).trim() : "Penalty";
    }
}
