package com.skillvibe.tutoring.service.payment;

import com.skillvibe.tutoring.model.User;

/**
 * Interfaz del Patrón Strategy para procesadores de pago.
 *
 * Define el contrato que cualquier proveedor de pago (Stripe, PayPal, etc.)
 * debe cumplir. PaymentService depende de esta abstracción, no de una
 * implementación concreta, respetando el principio de Inversión de Dependencias.
 */
public interface PaymentProcessor {

    /**
     * Crea una sesión de checkout (URL de pago) para el usuario.
     *
     * @param user   El usuario que realiza la recarga.
     * @param amount El monto en USD a recargar.
     * @return URL de la sesión de pago hacia la que redirigir al usuario.
     * @throws Exception si el proveedor de pago falla.
     */
    String createCheckoutSession(User user, Double amount) throws Exception;
}
