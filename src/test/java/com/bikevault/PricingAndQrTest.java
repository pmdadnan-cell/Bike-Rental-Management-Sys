package com.bikevault;

import com.bikevault.model.QrPayload;
import com.bikevault.util.PricingCalculator;
import com.bikevault.util.QRDecoder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingAndQrTest {

    @Test
    void rentalDaysTreatsSameDayAsOneDay() {
        assertEquals(1, PricingCalculator.rentalDays(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)));
        assertEquals(3, PricingCalculator.rentalDays(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 23)));
    }

    @Test
    void rentalAmountIsDaysTimesRate() {
        assertEquals(new BigDecimal("3600.00"),
                PricingCalculator.rentalAmount(new BigDecimal("1200"), 3));
    }

    @Test
    void lateFeeIsZeroWhenReturnedOnTime() {
        assertEquals(0, PricingCalculator.lateDays(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 23)));
        assertEquals(new BigDecimal("700.00"),
                PricingCalculator.lateFee(new BigDecimal("350"), 2));
        assertEquals(new BigDecimal("4300.00"),
                PricingCalculator.finalAmount(new BigDecimal("3600"), new BigDecimal("700")));
    }

    @Test
    void structuredQrPayloadIsParsedWithoutTrustingOnlyTheNumericId() {
        QrPayload payload = QRDecoder.parse("BIKEVAULT|BIKE_ID=25|QR=BIKE-QR-A83F92D1");
        assertEquals(25L, payload.getBikeId());
        assertEquals("BIKE-QR-A83F92D1", payload.getQrCode());
    }

    @Test
    void bareBikeIdentityIsAccepted() {
        QrPayload payload = QRDecoder.parse("BIKE-550E8400-E29B-41D4-A716-446655440000");
        assertNull(payload.getBikeId());
        assertEquals("BIKE-550E8400-E29B-41D4-A716-446655440000", payload.getQrCode());
    }

    @Test
    void unknownQrTextIsRejected() {
        assertThrows(RuntimeException.class, () -> QRDecoder.parse("random-store-coupon"));
    }
}
