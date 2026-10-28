-- =====================================================================
-- Aeroporto-BD — Módulo 2: Views
-- Execute depois de migrations.sql e queries.sql (e, de preferência,
-- depois de indices.sql, já que ambas se beneficiam dos novos índices).
-- =====================================================================
USE aeroporto;

-- ---------------------------------------------------------------------
-- View: vw_voo_completo
-- Junta Voo com companhia, aeronave, aeroportos de origem/destino e o
-- piloto comandante — 5 JOINs (Aeronave, CompanhiaAerea, Aeroporto
-- duas vezes, Funcionario). É a mesma junção que hoje aparece separada
-- em VooDAO.listar() (src/aeroporto/dao/VooDAO.java) e na Consulta 1 de
-- database/consultas.sql. Centralizar numa view evita reescrever esse
-- JOIN em cada relatório novo e garante que todo lugar que "olha para
-- um voo" enxerga exatamente os mesmos campos, com o mesmo significado.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_voo_completo AS
SELECT v.num_voo, v.data_voo, v.hora_partida, v.hora_chegada, v.status,
       a.cod_aeronave, a.modelo AS aeronave_modelo,
       c.cod_cia, c.nome AS companhia,
       ao.cod_iata AS origem_iata, ao.cidade AS origem_cidade,
       ad.cod_iata AS destino_iata, ad.cidade AS destino_cidade,
       f.matricula AS piloto_matricula,
       CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS piloto_nome,
       v.fk_Voo_conexao AS voo_conexao
FROM Voo v
JOIN Aeronave a       ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
JOIN Aeroporto ao     ON ao.cod_iata = v.fk_Aeroporto_origem
JOIN Aeroporto ad     ON ad.cod_iata = v.fk_Aeroporto_destino
JOIN Funcionario f    ON f.matricula = v.fk_Piloto_comandante;

-- Exemplo de uso:
-- SELECT * FROM vw_voo_completo WHERE companhia = 'Gol' ORDER BY data_voo;

-- ---------------------------------------------------------------------
-- View: vw_passageiro_resumo
-- Resumo de consumo por passageiro: quantos bilhetes comprou, quanto
-- gastou no total, a data do voo mais recente e quais companhias já
-- usou. 4 JOINs (Bilhete, Voo, Aeronave, CompanhiaAerea). Hoje
-- PassageiroDAO.listar() já calcula "quantidade de bilhetes" com uma
-- subconsulta correlacionada por linha; esta view resolve o mesmo tipo
-- de pergunta de forma reutilizável e mais completa (soma gasto e
-- histórico de companhias), útil para um futuro perfil de passageiro
-- ou um relatório de clientes mais frequentes/rentáveis.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_passageiro_resumo AS
SELECT p.cpf, CONCAT(p.primeiro_nome, ' ', p.sobrenome) AS passageiro,
       COUNT(b.cod_bilhete) AS qtd_bilhetes,
       COALESCE(SUM(b.preco), 0) AS total_gasto,
       MAX(v.data_voo) AS ultimo_voo,
       GROUP_CONCAT(DISTINCT c.nome ORDER BY c.nome SEPARATOR ', ') AS companhias_utilizadas
FROM Passageiro p
LEFT JOIN Bilhete b        ON b.fk_Passageiro_cpf = p.cpf
LEFT JOIN Voo v            ON v.num_voo = b.fk_Voo_num_voo
LEFT JOIN Aeronave a       ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
LEFT JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
GROUP BY p.cpf, p.primeiro_nome, p.sobrenome;

-- Exemplo de uso:
-- SELECT * FROM vw_passageiro_resumo ORDER BY total_gasto DESC;
