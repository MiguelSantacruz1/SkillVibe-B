package com.skillvibe.tutoring.service.payment;

import com.skillvibe.tutoring.model.User;

/**
 * Interfaz del Patrón Strategy para procesadores de pago.
 *
 * Define el contrato que cualquier proveedor de pago debe cumplir.
 * PaymentService depende de esta abstracción (no de una implementación concreta),
 * respetando el Principio de Inversión de Dependencias (SOLID - DIP).
 *
 * Implementación activa: {@link WompiPaymentProcessor} (Wompi Colombia - COP).
 */
public interface PaymentProcessor {

    /**
     * Crea un enlace de pago (checkout URL) para el usuario.
     *
     * @param user   El usuario que realiza la recarga de saldo.
     * @param amount El monto en COP (pesos colombianos) a recargar.
     *               Ejemplo: 50000.0 = $50.000 COP
     * @return URL del checkout hacia la que redirigir al usuario.
     * @throws RuntimeException si el proveedor de pago falla.
     */
    String createCheckoutSession(User user, Double amount);
}

