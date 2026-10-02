package com.ecommerce.paymentservice.service.gateway;

/**
 * Immutable outcome of a payment-gateway charge attempt.
 *
 * <p>Carries only the OUTCOME facts a gateway can determine: whether the charge
 * succeeded, the gateway transaction id, the resulting status, and an optional
 * failure reason. It does not model validation, idempotency, or persistence —
 * those remain the caller's responsibility.</p>
 *
 * @param success       whether the charge succeeded
 * @param transactionId the gateway transaction id (may be {@code null} on failure)
 * @param status        the resulting payment status (e.g. {@code "SUCCESS"} / {@code "FAILED"})
 * @param failureReason an optional human-readable failure reason ({@code null} on success)
 */
public record GatewayResult(boolean success, String transactionId, String status, String failureReason) {

    /**
     * Builds a successful result with no failure reason.
     *
     * @param transactionId the gateway transaction id
     * @param status        the resulting payment status
     * @return a successful {@link GatewayResult}
     */
    public static GatewayResult success(String transactionId, String status) {
        return new GatewayResult(true, transactionId, status, null);
    }

    /**
     * Builds a failed result with status {@code "FAILED"} and no transaction id.
     *
     * @param failureReason the human-readable failure reason
     * @return a failed {@link GatewayResult}
     */
    public static GatewayResult failure(String failureReason) {
        return new GatewayResult(false, null, "FAILED", failureReason);
    }
}
