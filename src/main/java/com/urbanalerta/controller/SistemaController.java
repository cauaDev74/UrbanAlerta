package com.urbanalerta.controller;

import com.urbanalerta.dto.InfoSistema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informações públicas do sistema, lidas pela tela de login. */
@RestController
public class SistemaController {

    private final String armazenamento;
    private final boolean demonstracao;

    public SistemaController(@Value("${urbanalerta.armazenamento.rotulo:Banco de dados}") String armazenamento,
                             @Value("${urbanalerta.dados-demonstracao:false}") boolean demonstracao) {
        this.armazenamento = armazenamento;
        this.demonstracao = demonstracao;
    }

    @GetMapping("/sistema")
    public InfoSistema sistema() {
        return new InfoSistema(armazenamento, demonstracao);
    }
}
