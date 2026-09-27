-- =====================================================================
-- Projeto BD - Aeroporto Internacional do Recife
-- Script de POPULAÇÃO (DML) - Versão MySQL
-- Execute depois de rodar o script de criação das tabelas.
-- A ordem dos INSERTs respeita as dependências de chave estrangeira.
-- =====================================================================

-- =====================================================================
-- 1. AEROPORTO
-- =====================================================================

USE aeroporto;

INSERT INTO Aeroporto (cod_iata, nome, cidade, pais) VALUES
('REC', 'Aeroporto Internacional do Recife/Guararapes', 'Recife', 'Brasil'),
('GRU', 'Aeroporto Internacional de São Paulo/Guarulhos', 'Guarulhos', 'Brasil'),
('GIG', 'Aeroporto Internacional do Rio de Janeiro/Galeão', 'Rio de Janeiro', 'Brasil'),
('CNF', 'Aeroporto Internacional de Belo Horizonte/Confins', 'Confins', 'Brasil'),
('SSA', 'Aeroporto Internacional de Salvador/Deputado Luís Eduardo Magalhães', 'Salvador', 'Brasil');

-- =====================================================================
-- 2. TERMINAL
-- =====================================================================
INSERT INTO Terminal (num_terminal, piso) VALUES
(1, 3),
(2, 2),
(3, 1);

-- =====================================================================
-- 3. PORTAO
-- =====================================================================
INSERT INTO Portao (num_portao, num_terminal) VALUES
(10, 1),
(11, 1),
(12, 1),
(20, 2),
(21, 2),
(30, 3);

-- =====================================================================
-- 4. COMPANHIA AEREA
-- =====================================================================
INSERT INTO CompanhiaAerea (cod_cia, nome) VALUES
('LA', 'LATAM Airlines Brasil'),
('G3', 'GOL Linhas Aéreas'),
('AD', 'Azul Linhas Aéreas Brasileiras');

INSERT INTO CompanhiaAerea_Telefone (cod_cia, telefone) VALUES
('LA', '08001234567'),
('LA', '1140028922'),
('G3', '08002345678'),
('AD', '08003456789');

-- =====================================================================
-- 5. AERONAVE
-- =====================================================================
INSERT INTO Aeronave (cod_aeronave, modelo, capac_passageiros, ano_fabricacao, fk_CompanhiaAerea_cod_cia) VALUES
('PRABC', 'Boeing 737-800', 189, 2015, 'LA'),
('PRXYZ', 'Airbus A320', 180, 2018, 'G3'),
('PRQWE', 'Airbus A321', 220, 2020, 'AD'),
('PRJKL', 'Embraer E195', 118, 2019, 'AD'),
('PRMNO', 'Boeing 737 MAX 8', 176, 2021, 'LA');

-- =====================================================================
-- 6. FUNCIONARIO
-- =====================================================================
INSERT INTO Funcionario (matricula, primeiro_nome, sobrenome, cpf, data_nasci, email, endereco_cep, endereco_rua, endereco_numero, endereco_bairro) VALUES
(1,  'Carlos',    'Andrade',  '11111111111', '1980-03-12', 'carlos.andrade@aeroporto.com',  '50000-000', 'Rua das Graças',     '120', 'Graças'),
(2,  'Fernanda',  'Lima',     '22222222222', '1985-07-22', 'fernanda.lima@aeroporto.com',   '50010-000', 'Av. Boa Viagem',     '450', 'Boa Viagem'),
(3,  'Roberto',   'Souza',    '33333333333', '1978-11-05', 'roberto.souza@aeroporto.com',   '50020-000', 'Rua da Aurora',      '300', 'Boa Vista'),
(4,  'Juliana',   'Alves',    '44444444444', '1990-02-14', 'juliana.alves@aeroporto.com',   '50030-000', 'Rua do Sol',         '80',  'Espinheiro'),
(5,  'Marcos',    'Pereira',  '55555555555', '1988-09-30', 'marcos.pereira@aeroporto.com',  '50040-000', 'Av. Conde da Boa Vista', '210', 'Boa Vista'),
(6,  'Patrícia',  'Gomes',    '66666666666', '1992-05-18', 'patricia.gomes@aeroporto.com',  '50050-000', 'Rua Sete de Setembro', '55', 'Santo Amaro'),
(7,  'Eduardo',   'Santos',   '77777777777', '1975-12-01', 'eduardo.santos@aeroporto.com',  '50060-000', 'Rua Imperial',       '150', 'Santo Amaro'),
(8,  'Camila',    'Ribeiro',  '88888888888', '1983-04-09', 'camila.ribeiro@aeroporto.com',  '50070-000', 'Rua Real da Torre',  '90',  'Torre'),
(9,  'Rafael',    'Costa',    '99999999999', '1995-01-25', 'rafael.costa@aeroporto.com',    '50080-000', 'Av. Caxangá',        '600', 'Iputinga'),
(10, 'Beatriz',   'Martins',  '10101010101', '1991-08-17', 'beatriz.martins@aeroporto.com', '50090-000', 'Rua João Fernandes', '75',  'Casa Amarela');

INSERT INTO Funcionario_Telefone (matricula, telefone) VALUES
(1,  '81988880001'),
(2,  '81988880002'),
(3,  '81988880003'),
(3,  '8121234567'),
(4,  '81988880004'),
(5,  '81988880005'),
(6,  '81988880006'),
(7,  '81988880007'),
(8,  '81988880008'),
(9,  '81988880009'),
(10, '81988880010');

-- =====================================================================
-- 7. PILOTO (especialização de Funcionario)
-- =====================================================================
INSERT INTO Piloto (fk_Funcionario_matricula, num_licenca, horas_voo_acumuladas) VALUES
(1, 'ANAC-001', 8500.50),
(2, 'ANAC-002', 6200.00),
(3, 'ANAC-003', 4300.75);

-- =====================================================================
-- 8. COMISSARIO (especialização de Funcionario)
-- =====================================================================
INSERT INTO Comissario (fk_Funcionario_matricula) VALUES
(4),
(5),
(6);

INSERT INTO Comissario_Idioma (fk_Comissario_matricula, idioma) VALUES
(4, 'Português'),
(4, 'Inglês'),
(5, 'Português'),
(5, 'Espanhol'),
(6, 'Português'),
(6, 'Inglês'),
(6, 'Francês');

-- =====================================================================
-- 9. MECANICO (especialização de Funcionario)
-- =====================================================================
INSERT INTO Mecanico (fk_Funcionario_matricula, especialidade) VALUES
(7, 'Motores'),
(8, 'Aviônicos');

-- =====================================================================
-- 10. PASSAGEIRO
-- =====================================================================
INSERT INTO Passageiro (cpf, primeiro_nome, sobrenome, data_nasci, email) VALUES
('12312312312', 'João Pedro',  'Silva',       '1990-06-15', 'joaopedro.silva@email.com'),
('32132132132', 'Ana Beatriz', 'Costa',       '1993-02-20', 'anabeatriz.costa@email.com'),
('45645645645', 'Lucas Gabriel', 'Oliveira',  '1988-10-11', 'lucas.oliveira@email.com'),
('65465465465', 'Mariana',     'Fernandes',   '1995-12-03', 'mariana.fernandes@email.com'),
('78978978978', 'Pedro Henrique', 'Souza',    '1982-04-27', 'pedro.souza@email.com'),
('89689689689', 'Isabela',     'Martins',     '1997-08-09', 'isabela.martins@email.com'),
('14714714714', 'Gustavo',     'Rocha',       '1991-01-30', 'gustavo.rocha@email.com'),
('25825825825', 'Larissa',     'Barbosa',     '1994-07-19', 'larissa.barbosa@email.com');

INSERT INTO Passageiro_Telefone (cpf, telefone) VALUES
('12312312312', '81977770001'),
('32132132132', '81977770002'),
('45645645645', '81977770003'),
('65465465465', '81977770004'),
('78978978978', '81977770005'),
('89689689689', '81977770006'),
('14714714714', '81977770007'),
('25825825825', '81977770008');

-- =====================================================================
-- 11. VOO
-- (inserido sem fk_Voo_conexao primeiro, pois o voo de conexão
--  ainda pode não existir; a ligação é feita depois com UPDATE)
-- =====================================================================
INSERT INTO Voo (num_voo, data_voo, hora_partida, hora_chegada, status,
                  fk_Aeronave_cod_aeronave, fk_Aeroporto_origem, fk_Aeroporto_destino,
                  fk_Portao_num_portao, fk_Portao_num_terminal, fk_Voo_conexao, fk_Piloto_comandante) VALUES
(101, '2026-09-25', '08:00:00', '09:30:00', 'Confirmado', 'PRABC', 'REC', 'GRU', 10, 1, NULL, 1),
(102, '2026-09-25', '10:00:00', '11:20:00', 'Confirmado', 'PRXYZ', 'GRU', 'REC', 20, 2, NULL, 2),
(201, '2026-09-25', '14:00:00', '15:15:00', 'Confirmado', 'PRQWE', 'REC', 'SSA', 11, 1, NULL, 3),
(202, '2026-09-25', '17:00:00', '19:30:00', 'Confirmado', 'PRQWE', 'SSA', 'GIG', 30, 3, NULL, 3),
(301, '2026-09-26', '06:30:00', '08:00:00', 'Confirmado', 'PRJKL', 'REC', 'CNF', 12, 1, NULL, 1),
(302, '2026-09-26', '09:00:00', '10:30:00', 'Cancelado',  'PRMNO', 'CNF', 'REC', 21, 2, NULL, 2),
(401, '2026-09-27', '07:00:00', '08:45:00', 'Confirmado', 'PRABC', 'GRU', 'SSA', 10, 1, NULL, 1),
(402, '2026-09-27', '11:00:00', '12:10:00', 'Confirmado', 'PRXYZ', 'GIG', 'REC', 20, 2, NULL, 3);

-- Voo 201 tem conexão com o voo 202 (mesmo passageiro pode seguir viagem)
UPDATE Voo SET fk_Voo_conexao = 202 WHERE num_voo = 201;

-- =====================================================================
-- 12. ESCALA
-- =====================================================================
INSERT INTO Escala (fk_Funcionario_matricula, fk_Voo_num_voo, funcao_no_voo) VALUES
(1, 101, 'Comandante'),
(4, 101, 'Comissário Chefe'),
(5, 101, 'Comissário'),
(2, 102, 'Comandante'),
(4, 102, 'Comissário Chefe'),
(6, 102, 'Comissário'),
(3, 201, 'Comandante'),
(5, 201, 'Comissário Chefe'),
(6, 201, 'Comissário'),
(3, 202, 'Comandante'),
(4, 202, 'Comissário Chefe'),
(1, 301, 'Comandante'),
(6, 301, 'Comissário Chefe'),
(2, 302, 'Comandante'),
(5, 302, 'Comissário Chefe'),
(1, 401, 'Comandante'),
(4, 401, 'Comissário Chefe'),
(3, 402, 'Comandante'),
(6, 402, 'Comissário Chefe');

-- =====================================================================
-- 13. MANUTENCAO
-- (data_fim NULL = manutenção ainda em andamento)
-- =====================================================================
INSERT INTO Manutencao (cod_manutencao, fk_Aeronave_cod_aeronave, data_inicio, data_fim, descricao, fk_Mecanico_matricula) VALUES
(1, 'PRABC', '2026-08-01', '2026-08-05', 'Revisão programada de 100 horas', 7),
(2, 'PRXYZ', '2026-09-15', NULL,        'Troca de pneus e verificação de freios', 8),
(1, 'PRJKL', '2026-07-15', '2026-07-20', 'Inspeção de motores', 7),
(2, 'PRMNO', '2026-09-18', NULL,        'Manutenção não programada - sistema hidráulico', 8);

-- =====================================================================
-- 14. ASSENTO
-- =====================================================================
INSERT INTO Assento (num_assento, fk_Aeronave_cod_aeronave) VALUES
('1A', 'PRABC'), ('1B', 'PRABC'), ('2A', 'PRABC'), ('2B', 'PRABC'),
('1A', 'PRXYZ'), ('1B', 'PRXYZ'), ('2A', 'PRXYZ'),
('1A', 'PRQWE'), ('1B', 'PRQWE'),
('1A', 'PRJKL'), ('1B', 'PRJKL'),
('1A', 'PRMNO'), ('1B', 'PRMNO');

-- =====================================================================
-- 15. BILHETE
-- =====================================================================
INSERT INTO Bilhete (cod_bilhete, classe, preco, data_compra, fk_Passageiro_cpf, fk_Voo_num_voo, fk_Assento_num_assento, fk_Assento_cod_aeronave) VALUES
(1, 'Econômica', 450.00, '2026-09-01', '12312312312', 101, '1A', 'PRABC'),
(2, 'Econômica', 450.00, '2026-09-02', '32132132132', 101, '1B', 'PRABC'),
(3, 'Executiva', 980.00, '2026-09-03', '45645645645', 102, '1A', 'PRXYZ'),
(4, 'Econômica', 620.00, '2026-09-05', '65465465465', 201, '1A', 'PRQWE'),
(5, 'Econômica', 590.00, '2026-09-05', '65465465465', 202, '1B', 'PRQWE'),
(6, 'Econômica', 380.00, '2026-09-10', '78978978978', 301, '1A', 'PRJKL'),
(7, 'Econômica', 700.00, '2026-09-12', '89689689689', 401, '2A', 'PRABC'),
(8, 'Econômica', 410.00, '2026-09-14', '14714714714', 402, '2A', 'PRXYZ');

-- =====================================================================
-- 16. BAGAGEM
-- =====================================================================
INSERT INTO Bagagem (num_etiqueta, fk_Bilhete_cod_bilhete, peso_kg) VALUES
(1001, 1, 23.5),
(1002, 1, 5.0),
(1003, 2, 18.0),
(1004, 3, 20.0),
(1005, 4, 15.5),
(1006, 6, 22.0),
(1007, 7, 19.0),
(1008, 8, 12.0);




