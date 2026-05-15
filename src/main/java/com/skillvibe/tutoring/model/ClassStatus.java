package com.skillvibe.tutoring.model;

import com.skillvibe.tutoring.exception.BusinessLogicException;

import java.util.Set;

/**
 * Enum que actúa como Máquina de Estados (State Pattern) para las tutorías.
 *
 * Cada constante define cuáles son sus transiciones válidas, encapsulando
 * las reglas del dominio (ej. no se puede finalizar una clase que nunca inició).
 * Esta lógica estaba antes dispersa en TutoringClassService como comparaciones de String.
 */
public enum ClassStatus {

    PROGRAMMED {
        @Override
        public Set<ClassStatus> transicionesValidas() {
            return Set.of(IN_PROGRESS, CANCELLED);
        }
    },
    IN_PROGRESS {
        @Override
        public Set<ClassStatus> transicionesValidas() {
            return Set.of(COMPLETED, CANCELLED);
        }
    },
    COMPLETED {
        @Override
        public Set<ClassStatus> transicionesValidas() {
            return Set.of(); // Estado terminal
        }
    },
    CANCELLED {
        @Override
        public Set<ClassStatus> transicionesValidas() {
            return Set.of(); // Estado terminal
        }
    };

    /**
     * Define qué estados son destinos válidos desde el estado actual.
     */
    public abstract Set<ClassStatus> transicionesValidas();

    /**
     * Transiciona al nuevo estado si la transición es válida.
     * Lanza BusinessLogicException si la transición no está permitida.
     *
     * @param nuevoEstado El estado al que se quiere transicionar.
     */
    public void validarTransicion(ClassStatus nuevoEstado) {
        if (!this.transicionesValidas().contains(nuevoEstado)) {
            throw new BusinessLogicException(
                "Transición de estado inválida: no se puede pasar de [" + this.name() + "] a [" + nuevoEstado.name() + "]."
            );
        }
    }
}
