package com.bikevault.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One customer trip: rental, payment, charges, incidents, and a derived timeline.
 */
public class TripDetail {

    private Rental rental;
    private Bike bike;
    private Payment latestPayment;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal penaltyTotal = BigDecimal.ZERO;
    private List<Penalty> penalties = new ArrayList<>();
    private List<Challan> challans = new ArrayList<>();
    private List<Incident> incidents = new ArrayList<>();
    private List<String> timeline = new ArrayList<>();

    public Rental getRental() {
        return rental;
    }

    public void setRental(Rental rental) {
        this.rental = rental;
    }

    public Bike getBike() {
        return bike;
    }

    public void setBike(Bike bike) {
        this.bike = bike;
    }

    public Payment getLatestPayment() {
        return latestPayment;
    }

    public void setLatestPayment(Payment latestPayment) {
        this.latestPayment = latestPayment;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getPenaltyTotal() {
        return penaltyTotal;
    }

    public void setPenaltyTotal(BigDecimal penaltyTotal) {
        this.penaltyTotal = penaltyTotal;
    }

    public List<Penalty> getPenalties() {
        return penalties;
    }

    public void setPenalties(List<Penalty> penalties) {
        this.penalties = penalties;
    }

    public List<Challan> getChallans() {
        return challans;
    }

    public void setChallans(List<Challan> challans) {
        this.challans = challans;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
    }

    public List<String> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<String> timeline) {
        this.timeline = timeline;
    }
}
