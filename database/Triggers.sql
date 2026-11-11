

DELIMITER //

CREATE TRIGGER registrar_alteracao_status_voo
AFTER UPDATE ON Voo
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO Log_Voo (
            num_voo,
            status_anterior,
            status_novo
        )
        VALUES (
            OLD.num_voo,
            OLD.status,
            NEW.status
        );
    END IF;
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER verificar_manutencao_voo
BEFORE UPDATE ON Voo
FOR EACH ROW
BEGIN
    IF NEW.status = 'Confirmado' THEN
        IF EXISTS (
            SELECT 1
            FROM Manutencao
            WHERE fk_Aeronave_cod_aeronave = NEW.fk_Aeronave_cod_aeronave
              AND NEW.data_voo >= data_inicio
              AND (data_fim IS NULL OR NEW.data_voo <= data_fim)
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Aeronave em manutencao na data do voo!';
        END IF;
    END IF;
END //

DELIMITER ;