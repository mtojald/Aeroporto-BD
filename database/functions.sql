
DELIMITER //

CREATE FUNCTION calcular_duracao_voo(
    p_num_voo INT
)
RETURNS INT
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_partida TIME;
    DECLARE v_chegada TIME;
    DECLARE v_duracao INT;

    SELECT hora_partida, hora_chegada
    INTO v_partida, v_chegada
    FROM Voo
    WHERE num_voo = p_num_voo;

    IF v_chegada < v_partida THEN
        SET v_duracao = TIMESTAMPDIFF(
            MINUTE,
            v_partida,
            v_chegada
        ) + 1440;
    ELSE
        SET v_duracao = TIMESTAMPDIFF(
            MINUTE,
            v_partida,
            v_chegada
        );
    END IF;

    RETURN v_duracao;
END //

DELIMITER ;


DELIMITER //

CREATE FUNCTION calcular_taxa_ocupacao_voo(
    p_num_voo INT
)
RETURNS VARCHAR(20)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_capacidade INT;
    DECLARE v_bilhetes INT;
    DECLARE v_taxa DECIMAL(5,2);
    DECLARE v_status VARCHAR(20);

    SELECT
        a.capac_passageiros,
        v.status
    INTO
        v_capacidade,
        v_status
    FROM Voo v
    JOIN Aeronave a
        ON v.fk_Aeronave_cod_aeronave = a.cod_aeronave
    WHERE v.num_voo = p_num_voo;

    IF v_status = 'Cancelado' THEN
        RETURN 'Cancelado';
    END IF;

    SELECT COUNT(*)
    INTO v_bilhetes
    FROM Bilhete
    WHERE fk_Voo_num_voo = p_num_voo;

    SET v_taxa = (v_bilhetes / v_capacidade) * 100;

    RETURN CONCAT(v_taxa, '%');
END //

DELIMITER ;
