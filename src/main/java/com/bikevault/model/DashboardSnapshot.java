package com.bikevault.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Live dashboard figures derived from MySQL records, not hardcoded UI values.
 */
public class DashboardSnapshot {

    private long totalBikes;
    private long availableBikes;
    private long rentedBikes;
    private long maintenanceBikes;
    private long totalCustomers;
    private long activeRentals;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long pendingPayments;
    private long pendingReturns;
    private long penalties;
    private long incidents;
    private long pendingApprovals;
    private String revenueLabel = "TODAY'S REVENUE";
    private List<AuditLog> recentActivity = new ArrayList<>();

    public long getTotalBikes() {
        return totalBikes;
    }

    public void setTotalBikes(long totalBikes) {
        this.totalBikes = totalBikes;
    }

    public long getAvailableBikes() {
        return availableBikes;
    }

    public void setAvailableBikes(long availableBikes) {
        this.availableBikes = availableBikes;
    }

    public long getRentedBikes() {
        return rentedBikes;
    }

    public void setRentedBikes(long rentedBikes) {
        this.rentedBikes = rentedBikes;
    }

    public long getMaintenanceBikes() {
        return maintenanceBikes;
    }

    public void setMaintenanceBikes(long maintenanceBikes) {
        this.maintenanceBikes = maintenanceBikes;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getActiveRentals() {
        return activeRentals;
    }

    public void setActiveRentals(long activeRentals) {
        this.activeRentals = activeRentals;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public String getRevenueLabel() {
        return revenueLabel;
    }

    public void setRevenueLabel(String revenueLabel) {
        this.revenueLabel = revenueLabel;
    }

    public List<AuditLog> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<AuditLog> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public long getPendingReturns() {
        return pendingReturns;
    }

    public void setPendingReturns(long pendingReturns) {
        this.pendingReturns = pendingReturns;
    }

    public long getPenalties() {
        return penalties;
    }

    public void setPenalties(long penalties) {
        this.penalties = penalties;
    }

    public long getIncidents() {
        return incidents;
    }

    public void setIncidents(long incidents) {
        this.incidents = incidents;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }
}
