package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.PaymentService;
import com.skillvibe.tutoring.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de pagos (Simulación en COP).
 *
 * Endpoints:
 *   POST /api/payments/simulate       → Simula un pago exitoso y acredita el saldo.
 *   GET  /api/payments/historial      → Historial de transacciones del usuario.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos", description = "Endpoints para recarga de saldo e historial (COP) - Modo Simulación")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/payments/simulate
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simula un pago y acredita el saldo de inmediato al usuario autenticado.
     *
     * @param payload JSON con campo "amount" en COP. Ej: { "amount": 50000.0 }
     */
    @PostMapping("/simulate")
    @Operation(summary = "Simular recarga de saldo (COP)",
               description = "Acredita el saldo inmediatamente sin pasarelas externas.")
    public ResponseEntity<ApiResponse<String>> simulatePayment(
            @RequestBody Map<String, Double> payload,
            Authentication auth) {

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userService.findById(principal.getId());

        Double amount = payload.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El monto debe ser mayor a 0 COP"));
        }
        if (amount > 10000000) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El monto máximo por recarga es de 10,000,000 COP"));
        }

        paymentService.simulatePayment(user.getId(), amount);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Pago simulado exitosamente por $" + amount.longValue() + " COP", "OK"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/payments/historial
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/historial")
    @Operation(summary = "Obtener historial de transacciones del usuario autenticado")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistorial(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success("Historial recuperado",
                        paymentService.getHistory(principal.getId()))
        );
    }
}
