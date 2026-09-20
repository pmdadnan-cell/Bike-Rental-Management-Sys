package com.bikevault.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rental agreement linking a customer to a bike for a date range.
 */
public class Rental {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long bikeId;
    private String bikeLabel;
    private String bikeQrCode;
    private String registrationNumber;
    private Long userId;
    private LocalDate startDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private Integer durationDays;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private BigDecimal lateFee;
    private BigDecimal finalAmount;
    private String status;
    private BigDecimal securityDeposit;
    private String rentalQrToken;
    private String rentalQrStatus;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String rejectionNotes;
    private boolean returnRequested;
    private LocalDateTime returnRequestedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getBikeId() {
        return bikeId;
    }

    public void setBikeId(Long bikeId) {
        this.bikeId = bikeId;
    }

    public String getBikeLabel() {
        return bikeLabel;
    }

    public void setBikeLabel(String bikeLabel) {
        this.bikeLabel = bikeLabel;
    }

    public String getBikeQrCode() {
        return bikeQrCode;
    }

    public void setBikeQrCode(String bikeQrCode) {
        this.bikeQrCode = bikeQrCode;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDate actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public String getRentalQrToken() {
        return rentalQrToken;
    }

    public void setRentalQrToken(String rentalQrToken) {
        this.rentalQrToken = rentalQrToken;
    }

    public String getRentalQrStatus() {
        return rentalQrStatus;
    }

    public void setRentalQrStatus(String rentalQrStatus) {
        this.rentalQrStatus = rentalQrStatus;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionNotes() {
        return rejectionNotes;
    }

    public void setRejectionNotes(String rejectionNotes) {
        this.rejectionNotes = rejectionNotes;
    }

    public boolean isReturnRequested() {
        return returnRequested;
    }

    public void setReturnRequested(boolean returnRequested) {
        this.returnRequested = returnRequested;
    }

    public LocalDateTime getReturnRequestedAt() {
        return returnRequestedAt;
    }

    public void setReturnRequestedAt(LocalDateTime returnRequestedAt) {
        this.returnRequestedAt = returnRequestedAt;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String displayId() {
        return id == null ? "RV-??????" : String.format("RV-%06d", id);
    }

    public String displayStatus() {
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) {
            return "PENDING ADMIN APPROVAL";
        }
        if ("RETURN_REQUESTED".equalsIgnoreCase(status) || returnRequested) {
            return "RETURN REQUESTED";
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "ACTIVE";
        }
        if (status == null || status.isBlank()) {
            return "N/A";
        }
        return status;
    }

    public boolean occupiesBike() {
        return "PENDING_APPROVAL".equalsIgnoreCase(status)
                || "APPROVED".equalsIgnoreCase(status)
                || "ACTIVE".equalsIgnoreCase(status)
                || "RETURN_REQUESTED".equalsIgnoreCase(status);
    }

    public boolean canBeApproved() {
        return "PENDING_APPROVAL".equalsIgnoreCase(status)
                && ("PENDING_APPROVAL".equalsIgnoreCase(rentalQrStatus)
                || rentalQrStatus == null);
    }

    public boolean qrIsExpired() {
        return "COMPLETED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)
                || "EXPIRED".equalsIgnoreCase(rentalQrStatus)
                || "COMPLETED".equalsIgnoreCase(rentalQrStatus)
                || "REJECTED".equalsIgnoreCase(rentalQrStatus);
    }

    public boolean qrAuthorizesCollection() {
        return canBeApproved();
    }

    @Override
    public String toString() {
        return "Rental #" + (id == null ? "?" : id) + " — "
                + (customerName == null ? "Customer" : customerName)
                + " / " + (bikeLabel == null ? "Bike" : bikeLabel);
    }
}
