
DELIMITER //

CREATE PROCEDURE atualizar_status_voo(
    IN p_num_voo INT,
    IN p_novo_status VARCHAR(20)
)
BEGIN
    IF p_novo_status NOT IN ('Confirmado', 'Cancelado') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Status invalido!';
    ELSE
        UPDATE Voo
        SET status = p_novo_status
        WHERE num_voo = p_num_voo;
    END IF;
END //

DELIMITER ;


DELIMITER //

CREATE PROCEDURE verificar_conexoes_passageiros()
BEGIN
    DECLARE v_fim INT DEFAULT 0;
    DECLARE v_cpf CHAR(11);
    DECLARE v_nome VARCHAR(100);
    DECLARE v_primeiro_voo INT;
    DECLARE v_segundo_voo INT;
    DECLARE v_intervalo INT;

    DECLARE cursor_conexoes CURSOR FOR
        SELECT
            p.cpf,
            CONCAT(p.primeiro_nome, ' ', p.sobrenome),
            v1.num_voo,
            v2.num_voo,
            TIMESTAMPDIFF(
                MINUTE,
                TIMESTAMP(v1.data_voo, v1.hora_chegada),
                TIMESTAMP(v2.data_voo, v2.hora_partida)
            )
        FROM Voo v1
        JOIN Voo v2
            ON v2.num_voo = v1.fk_Voo_conexao
        JOIN Bilhete b1
            ON b1.fk_Voo_num_voo = v1.num_voo
        JOIN Bilhete b2
            ON b2.fk_Voo_num_voo = v2.num_voo
            AND b2.fk_Passageiro_cpf = b1.fk_Passageiro_cpf
        JOIN Passageiro p
            ON p.cpf = b1.fk_Passageiro_cpf
        WHERE v1.status = 'Confirmado'
          AND v2.status = 'Confirmado';

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_fim = 1;

    DROP TEMPORARY TABLE IF EXISTS relatorio_conexoes;

    CREATE TEMPORARY TABLE relatorio_conexoes (
        cpf CHAR(11),
        passageiro VARCHAR(100),
        primeiro_voo INT,
        segundo_voo INT,
        intervalo_minutos INT,
        classificacao VARCHAR(20)
    );

    OPEN cursor_conexoes;

    loop_conexoes: LOOP
        FETCH cursor_conexoes INTO
            v_cpf,
            v_nome,
            v_primeiro_voo,
            v_segundo_voo,
            v_intervalo;

        IF v_fim = 1 THEN
            LEAVE loop_conexoes;
        END IF;

        INSERT INTO relatorio_conexoes
        VALUES (
            v_cpf,
            v_nome,
            v_primeiro_voo,
            v_segundo_voo,
            v_intervalo,
            CASE
                WHEN v_intervalo < 45 THEN 'Inviavel'
                WHEN v_intervalo <= 100 THEN 'Apertada'
                ELSE 'Viavel'
            END
        );
    END LOOP;

    CLOSE cursor_conexoes;

    SELECT *
    FROM relatorio_conexoes;

    DROP TEMPORARY TABLE relatorio_conexoes;
END //

DELIMITER ;
