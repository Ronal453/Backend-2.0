package com.plantopolis.backend.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EstadoTarea — Máquina de estados de dominio")
class EstadoTareaTest {

    @Test
    @DisplayName("COMPLETADA es terminal: no tiene destinos permitidos")
    void completada_noTieneDestinosPermitidos() {
        assertThat(EstadoTarea.COMPLETADA.destinosPermitidos()).isEmpty();
        assertThat(EstadoTarea.COMPLETADA.puedeTransicionarA(EstadoTarea.EN_REVISION)).isFalse();
        assertThat(EstadoTarea.COMPLETADA.puedeTransicionarA(EstadoTarea.POR_HACER)).isFalse();
        assertThat(EstadoTarea.COMPLETADA.puedeTransicionarA(EstadoTarea.BLOQUEADA)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1} debería ser {2}")
    @CsvSource({
            "POR_HACER, EN_PROGRESO, true",
            "POR_HACER, BLOQUEADA, true",
            "POR_HACER, EN_REVISION, false",
            "POR_HACER, COMPLETADA, false",
            "EN_PROGRESO, EN_REVISION, true",
            "EN_PROGRESO, POR_HACER, true",
            "EN_PROGRESO, BLOQUEADA, true",
            "EN_PROGRESO, COMPLETADA, false",
            "EN_REVISION, COMPLETADA, true",
            "EN_REVISION, EN_PROGRESO, true",
            "EN_REVISION, BLOQUEADA, true",
            "EN_REVISION, POR_HACER, false",
            "BLOQUEADA, POR_HACER, true",
            "BLOQUEADA, EN_PROGRESO, true",
            "BLOQUEADA, EN_REVISION, false",
            "BLOQUEADA, COMPLETADA, false"
    })
    void matrizTransiciones(EstadoTarea origen, EstadoTarea destino, boolean esperada) {
        assertThat(origen.puedeTransicionarA(destino)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("desde() normaliza texto válido")
    void desde_textoValido_retornaEnum() {
        assertThat(EstadoTarea.desde("por_hacer")).isEqualTo(EstadoTarea.POR_HACER);
        assertThat(EstadoTarea.desde(" EN_PROGRESO ")).isEqualTo(EstadoTarea.EN_PROGRESO);
    }

    @Test
    @DisplayName("desde() lanza excepción con valor inválido")
    void desde_textoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> EstadoTarea.desde("INVENTADO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado de tarea no válido");
    }
}
