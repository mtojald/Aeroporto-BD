-- =====================================================================
-- Aeroporto-BD — Módulo 2: Consultas avançadas
-- Continua a numeração de database/consultas.sql (que vai até a 10).
-- Execute depois de migrations.sql, queries.sql e indices.sql.
-- =====================================================================
USE aeroporto;

-- ---------------------------------------------------------------------
-- Consulta 11: Passageiros que nunca compraram bilhete
-- Pergunta : Quais passageiros estão cadastrados mas nunca viraram
--            clientes (nenhum bilhete em nome deles)?
-- Conceitos: ANTI-JOIN (LEFT JOIN + WHERE ... IS NULL) — usa o índice
--            idx_bilhete_passageiro (database/indices.sql)
-- ---------------------------------------------------------------------
SELECT p.cpf, CONCAT(p.primeiro_nome, ' ', p.sobrenome) AS passageiro, p.email
FROM Passageiro p
LEFT JOIN Bilhete b ON b.fk_Passageiro_cpf = p.cpf
WHERE b.cod_bilhete IS NULL
ORDER BY passageiro;

-- ---------------------------------------------------------------------
-- Consulta 12: Pilotos e os voos que comandam
-- Pergunta : Juntando todo piloto (mesmo sem nenhum voo comandado) com
--            todo voo (que sempre tem piloto, por ser FK obrigatória),
--            quem comanda o quê?
-- Conceitos: FULL OUTER JOIN — MySQL não tem FULL OUTER JOIN nativo,
--            então é simulado com LEFT JOIN UNION RIGHT JOIN. Usa o
--            índice idx_voo_piloto.
--            Observação: como Voo.fk_Piloto_comandante é NOT NULL, não
--            existe "voo órfão" (sem piloto) — o UNION garante mesmo
--            assim que nenhuma linha de nenhum dos dois lados se perca,
--            mesmo quando um dos lados nunca tem órfãos.
-- ---------------------------------------------------------------------
SELECT f.matricula, CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS piloto,
       v.num_voo, v.data_voo
FROM Piloto p
JOIN Funcionario f ON f.matricula = p.fk_Funcionario_matricula
LEFT JOIN Voo v ON v.fk_Piloto_comandante = p.fk_Funcionario_matricula
UNION
SELECT f.matricula, CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS piloto,
       v.num_voo, v.data_voo
FROM Piloto p
JOIN Funcionario f ON f.matricula = p.fk_Funcionario_matricula
RIGHT JOIN Voo v ON v.fk_Piloto_comandante = p.fk_Funcionario_matricula
ORDER BY matricula, num_voo;

-- ---------------------------------------------------------------------
-- Consulta 13: Voos com preço médio de bilhete acima da média geral
-- Pergunta : Em quais voos o preço médio dos bilhetes vendidos supera
--            o preço médio de TODOS os bilhetes da base?
-- Conceitos: subconsulta ESCALAR CORRELACIONADA (a subconsulta no WHERE
--            referencia v.num_voo, da consulta externa) combinada com
--            uma subconsulta não-correlacionada (a média geral)
-- ---------------------------------------------------------------------
SELECT v.num_voo, v.data_voo,
       (SELECT ROUND(AVG(b.preco), 2)
        FROM Bilhete b WHERE b.fk_Voo_num_voo = v.num_voo) AS preco_medio_voo
FROM Voo v
WHERE (SELECT AVG(b.preco) FROM Bilhete b WHERE b.fk_Voo_num_voo = v.num_voo)
      > (SELECT AVG(preco) FROM Bilhete)
ORDER BY preco_medio_voo DESC;

-- ---------------------------------------------------------------------
-- Consulta 14: Bilhetes sem nenhuma bagagem despachada
-- Pergunta : Quais bilhetes vendidos não têm nenhuma bagagem registrada?
-- Conceitos: subconsulta correlacionada com NOT EXISTS — a mesma
--            pergunta de "não existe correspondência" da Consulta 11,
--            mas resolvida por subconsulta em vez de JOIN. Usa o
--            índice idx_bagagem_bilhete.
-- ---------------------------------------------------------------------
SELECT b.cod_bilhete, CONCAT(p.primeiro_nome, ' ', p.sobrenome) AS passageiro,
       v.num_voo, v.data_voo
FROM Bilhete b
JOIN Passageiro p ON p.cpf = b.fk_Passageiro_cpf
JOIN Voo v ON v.num_voo = b.fk_Voo_num_voo
WHERE NOT EXISTS (
    SELECT 1 FROM Bagagem g WHERE g.fk_Bilhete_cod_bilhete = b.cod_bilhete
)
ORDER BY v.data_voo, b.cod_bilhete;
