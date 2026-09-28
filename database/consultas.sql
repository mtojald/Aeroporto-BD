USE aeroporto;
-- ---------------------------------------------------------------------
-- Consulta 1: Voos com todos os detalhes
-- Pergunta : Quais são os voos, com companhia, aeronave, cidades de origem/destino e comandante?
-- Conceitos: JOIN de 6 tabelas · 2 JOINs na mesma tabela (Aeroporto origem e destino) · CONCAT
-- ---------------------------------------------------------------------
SELECT v.num_voo, v.data_voo, v.hora_partida, v.status,
       c.nome AS companhia, a.modelo AS aeronave,
       ao.cidade AS origem, ad.cidade AS destino,
       CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS comandante
FROM Voo v
JOIN Aeronave a       ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
JOIN Aeroporto ao     ON ao.cod_iata = v.fk_Aeroporto_origem
JOIN Aeroporto ad     ON ad.cod_iata = v.fk_Aeroporto_destino
JOIN Funcionario f    ON f.matricula = v.fk_Piloto_comandante
ORDER BY v.data_voo, v.hora_partida;

-- ---------------------------------------------------------------------
-- Consulta 2: Receita por companhia aérea
-- Pergunta : Quanto cada companhia arrecadou com bilhetes, e qual o preço médio?
-- Conceitos: LEFT JOIN encadeado (mantém companhia sem vendas) · GROUP BY · SUM / AVG / COUNT · COALESCE
-- ---------------------------------------------------------------------
SELECT c.nome AS companhia,
       COUNT(b.cod_bilhete)              AS bilhetes_vendidos,
       COALESCE(SUM(b.preco), 0)         AS receita_total,
       COALESCE(ROUND(AVG(b.preco), 2), 0) AS preco_medio
FROM CompanhiaAerea c
LEFT JOIN Aeronave a ON a.fk_CompanhiaAerea_cod_cia = c.cod_cia
LEFT JOIN Voo v      ON v.fk_Aeronave_cod_aeronave = a.cod_aeronave
LEFT JOIN Bilhete b  ON b.fk_Voo_num_voo = v.num_voo
GROUP BY c.cod_cia, c.nome
ORDER BY receita_total DESC;

-- ---------------------------------------------------------------------
-- Consulta 3: Passageiros que gastaram acima da média
-- Pergunta : Quais passageiros gastaram mais do que o gasto médio por passageiro?
-- Conceitos: GROUP BY + HAVING · subconsulta escalar com tabela derivada (subconsulta dentro de subconsulta)
-- ---------------------------------------------------------------------
SELECT p.cpf, CONCAT(p.primeiro_nome, ' ', p.sobrenome) AS passageiro,
       COUNT(b.cod_bilhete) AS bilhetes, SUM(b.preco) AS total_gasto
FROM Passageiro p
JOIN Bilhete b ON b.fk_Passageiro_cpf = p.cpf
GROUP BY p.cpf, p.primeiro_nome, p.sobrenome
HAVING SUM(b.preco) > (
    SELECT AVG(gasto)
    FROM (SELECT SUM(preco) AS gasto
          FROM Bilhete
          GROUP BY fk_Passageiro_cpf) AS gasto_por_passageiro
)
ORDER BY total_gasto DESC;

-- ---------------------------------------------------------------------
-- Consulta 4: Voos ativos com aeronave em manutenção aberta
-- Pergunta : Algum voo não cancelado está escalado para uma aeronave que ainda está em manutenção?
-- Conceitos: Subconsulta correlacionada com EXISTS · IS NULL (manutenção sem data_fim) · JOIN
-- ---------------------------------------------------------------------
SELECT v.num_voo, v.data_voo, v.status,
       v.fk_Aeronave_cod_aeronave AS aeronave, a.modelo
FROM Voo v
JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
WHERE v.status <> 'Cancelado'
  AND EXISTS (SELECT 1
              FROM Manutencao m
              WHERE m.fk_Aeronave_cod_aeronave = v.fk_Aeronave_cod_aeronave
                AND m.data_fim IS NULL)
ORDER BY v.data_voo, v.num_voo;

-- ---------------------------------------------------------------------
-- Consulta 5: Taxa de ocupação por voo
-- Pergunta : Que percentual da capacidade da aeronave foi vendido em cada voo?
-- Conceitos: LEFT JOIN · GROUP BY · expressão aritmética com ROUND (bilhetes ÷ capacidade)
-- ---------------------------------------------------------------------
SELECT v.num_voo, v.data_voo, a.modelo, a.capac_passageiros AS capacidade,
       COUNT(b.cod_bilhete) AS bilhetes_vendidos,
       ROUND(100 * COUNT(b.cod_bilhete) / a.capac_passageiros, 2) AS ocupacao_pct
FROM Voo v
JOIN Aeronave a    ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
LEFT JOIN Bilhete b ON b.fk_Voo_num_voo = v.num_voo
GROUP BY v.num_voo, v.data_voo, a.modelo, a.capac_passageiros
ORDER BY ocupacao_pct DESC, v.num_voo;

-- ---------------------------------------------------------------------
-- Consulta 6: Tripulação de cada voo
-- Pergunta : Quem está escalado em cada voo e em qual função?
-- Conceitos: JOIN da tabela associativa (N:N) Escala · GROUP_CONCAT · GROUP BY
-- ---------------------------------------------------------------------
SELECT e.fk_Voo_num_voo AS num_voo,
       COUNT(*) AS tripulantes,
       GROUP_CONCAT(CONCAT(f.primeiro_nome, ' (', e.funcao_no_voo, ')')
                    ORDER BY e.funcao_no_voo SEPARATOR ', ') AS equipe
FROM Escala e
JOIN Funcionario f ON f.matricula = e.fk_Funcionario_matricula
GROUP BY e.fk_Voo_num_voo
ORDER BY e.fk_Voo_num_voo;

-- ---------------------------------------------------------------------
-- Consulta 7: Manutenções por mecânico
-- Pergunta : Quantas manutenções cada mecânico fez e há quantos dias (em média) elas duram/estão abertas?
-- Conceitos: JOIN de especialização (Mecanico → Funcionario) · DATEDIFF · CASE · GROUP BY
-- ---------------------------------------------------------------------
SELECT CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS mecanico,
       m.especialidade,
       COUNT(*) AS manutencoes,
       SUM(CASE WHEN mt.data_fim IS NULL THEN 1 ELSE 0 END) AS em_aberto,
       ROUND(AVG(DATEDIFF(COALESCE(mt.data_fim, CURDATE()), mt.data_inicio)), 1) AS dias_medios
FROM Manutencao mt
JOIN Mecanico m    ON m.fk_Funcionario_matricula = mt.fk_Mecanico_matricula
JOIN Funcionario f ON f.matricula = m.fk_Funcionario_matricula
GROUP BY f.matricula, f.primeiro_nome, f.sobrenome, m.especialidade
ORDER BY manutencoes DESC;

-- ---------------------------------------------------------------------
-- Consulta 8: Ranking de pilotos por horas de voo
-- Pergunta : Qual a posição de cada piloto por horas acumuladas e quantos voos ele comanda?
-- Conceitos: Função de janela RANK() OVER · LEFT JOIN · GROUP BY
-- ---------------------------------------------------------------------
SELECT RANK() OVER (ORDER BY p.horas_voo_acumuladas DESC) AS posicao,
       CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS piloto,
       p.num_licenca, p.horas_voo_acumuladas,
       COUNT(v.num_voo) AS voos_comandados
FROM Piloto p
JOIN Funcionario f ON f.matricula = p.fk_Funcionario_matricula
LEFT JOIN Voo v    ON v.fk_Piloto_comandante = p.fk_Funcionario_matricula
GROUP BY p.fk_Funcionario_matricula, f.primeiro_nome, f.sobrenome,
         p.num_licenca, p.horas_voo_acumuladas
ORDER BY posicao;

-- ---------------------------------------------------------------------
-- Consulta 9: Voos com bagagem acima da média
-- Pergunta : Quais voos despacharam mais peso de bagagem do que a média dos voos?
-- Conceitos: CTE (WITH) · JOIN · GROUP BY · subconsulta escalar sobre a própria CTE
-- ---------------------------------------------------------------------
WITH bagagem_por_voo AS (
    SELECT b.fk_Voo_num_voo AS num_voo,
           COUNT(*)         AS qtd_bagagens,
           SUM(g.peso_kg)   AS peso_total
    FROM Bagagem g
    JOIN Bilhete b ON b.cod_bilhete = g.fk_Bilhete_cod_bilhete
    GROUP BY b.fk_Voo_num_voo
)
SELECT num_voo, qtd_bagagens, peso_total
FROM bagagem_por_voo
WHERE peso_total > (SELECT AVG(peso_total) FROM bagagem_por_voo)
ORDER BY peso_total DESC;

-- ---------------------------------------------------------------------
-- Consulta 10: Voos com conexão e tempo de espera
-- Pergunta : Quais voos têm conexão e quanto tempo o passageiro espera entre a chegada e a próxima partida?
-- Conceitos: Auto-relacionamento (JOIN da tabela Voo com ela mesma) · TIMEDIFF
-- ---------------------------------------------------------------------
SELECT v1.num_voo AS voo, v1.fk_Aeroporto_origem AS origem,
       v1.fk_Aeroporto_destino AS escala,
       v2.num_voo AS voo_conexao, v2.fk_Aeroporto_destino AS destino_final,
       v1.hora_chegada, v2.hora_partida,
       TIMEDIFF(v2.hora_partida, v1.hora_chegada) AS tempo_de_espera
FROM Voo v1
JOIN Voo v2 ON v2.num_voo = v1.fk_Voo_conexao
ORDER BY v1.num_voo;