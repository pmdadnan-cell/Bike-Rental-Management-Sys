package com.bikevault.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Live figures for the signed-in customer's home screen.
 */
public class CustomerHomeSnapshot {

    private String customerName = "Guest";
    private long availableBikes;
    private Rental currentRental;
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    private long activePenalties;
    private List<Bike> featuredBikes = new ArrayList<>();

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public long getAvailableBikes() {
        return availableBikes;
    }

    public void setAvailableBikes(long availableBikes) {
        this.availableBikes = availableBikes;
    }

    public Rental getCurrentRental() {
        return currentRental;
    }

    public void setCurrentRental(Rental currentRental) {
        this.currentRental = currentRental;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public long getActivePenalties() {
        return activePenalties;
    }

    public void setActivePenalties(long activePenalties) {
        this.activePenalties = activePenalties;
    }

    public List<Bike> getFeaturedBikes() {
        return featuredBikes;
    }

    public void setFeaturedBikes(List<Bike> featuredBikes) {
        this.featuredBikes = featuredBikes;
    }
}
