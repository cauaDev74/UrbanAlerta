package com.urbanalerta.controller;

import com.urbanalerta.dto.InfoSistema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informações públicas do sistema, lidas pela tela de login. */
@RestController
public class SistemaController {

    private final String armazenamento;

    public SistemaController(@Value("${urbanalerta.armazenamento.rotulo:Banco de dados}") String armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/sistema")
    public InfoSistema sistema() {
        return new InfoSistema(armazenamento);
    }
}
