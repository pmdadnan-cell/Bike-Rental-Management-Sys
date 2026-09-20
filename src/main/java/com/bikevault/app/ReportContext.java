package com.bikevault.app;

import com.bikevault.util.NavigationUtil;

/**
 * Lets the MenuBar open the Reports workspace on a specific tab.
 */
public final class ReportContext {

    public enum Focus {
        RENTALS,
        AVAILABILITY,
        CUSTOMERS
    }

    private static Focus focus = Focus.RENTALS;

    private ReportContext() {
    }

    public static void open(Focus requested) {
        focus = requested == null ? Focus.RENTALS : requested;
        NavigationUtil.showView(AppView.REPORTS);
    }

    public static Focus take() {
        Focus current = focus;
        focus = Focus.RENTALS;
        return current;
    }
}
