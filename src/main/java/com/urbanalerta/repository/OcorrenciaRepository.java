package com.urbanalerta.repository;

import com.urbanalerta.model.Ocorrencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Camada de acesso a dados (Spring Data JPA): o Spring gera a implementação.
 * A consulta sai do nome do método e o EntityGraph traz autor e responsável no mesmo SELECT,
 * evitando uma consulta extra por ocorrência.
 */
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {

    @EntityGraph(attributePaths = {"autor", "responsavel"})
    List<Ocorrencia> findAllByOrderByDataHoraRegistroDesc();
}
