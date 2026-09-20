package com.bikevault.exception;

/**
 * Raised when QR identity generation, encoding, decoding, or verification fails.
 * Implemented here so later QR phases have a stable type; unused in Phase 2.
 */
public class QRException extends RuntimeException {

    public QRException(String message) {
        super(message);
    }

    public QRException(String message, Throwable cause) {
        super(message, cause);
    }
}
