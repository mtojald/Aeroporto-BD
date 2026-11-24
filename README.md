# Aeroporto-BD — Sistema de Gestão do Aeroporto Internacional do Recife

 Um sistema web para gerenciar as operações de um aeroporto: voos, aeronaves, companhias aéreas, tripulação, passageiros, bilhetes, bagagens e manutenções. Tudo conversa com um banco **MySQL** por **JDBC**, com **SQL escrito de forma explícita**.

## Integrantes

- Mariana Maliu
- Arthur Coelho
- Miguel Tojal
- Raul Maia
- Arthur Queiroz
- Vitor Gadelha

## Funcionalidades

- CRUD de passageiros, voos, companhias aéreas e aeronaves.
- Dashboard com seis gráficos, indicadores e estatística dos preços com filtros por data do voo, companhia e status.
- 14 consultas das etapas 03 e 04 com resultados filtráveis.
- Views com parâmetros, execução de funções e procedures e histórico de status produzido por trigger.

## Como executar

**Requisitos:** JDK 17+ e MySQL 8+.

1. Rode no MySQL, nesta ordem:
   - `database/migrations.sql` (cria o banco e as tabelas)
   - `database/queries.sql` (insere os dados)
   - `database/indices.sql` (cria os índices adicionais)
   - `database/views.sql` (cria as duas views)
   - Selecione o banco `aeroporto` e execute `database/functions.sql`, `database/Procedures.sql` e `database/Triggers.sql`, nessa ordem.

   Em um banco existente, aplique somente os scripts dos objetos ainda ausentes. Os scripts de criação de tabelas e carga inicial são para a instalação inicial.
2. Copie `config.properties.example` para `config.properties` e coloque o usuário e a senha do seu MySQL.
3. Compile e execute:

   **Linux/macOS/Git Bash:**
```
   ./run.sh
```

   **Windows (PowerShell):**
```
   mkdir out -Force
   javac --release 17 -encoding UTF-8 -cp "lib/*" -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
   java -cp "out;lib/*" aeroporto.Main
```

4. Abra http://localhost:8080

## Estrutura

```
database/   migrations.sql, queries.sql, consultas.sql (as 10 consultas iniciais)
            indices.sql, consultas_avancadas.sql, views.sql
            functions.sql, Procedures.sql, Triggers.sql
src/        código Java (db, dao, web)
web/        interface (index.html, app.js, CSS e Chart.js)
```

## Uso de IA

Usamos IA como apoio para revisar o modelo, rascunhar as consultas e o dashboard e explicar conceitos. Validamos executando as consultas no MySQL, conferindo os resultados e testando inserção, alteração e exclusão pela interface.

## Entregas

| Etapa | Onde está |
|---|---|
| 1. Modelagem | `Projeto BD - Aeroporto.pdf` |
| 2. Tabelas e dados | `database/migrations.sql` e `database/queries.sql` |
| 3. Interface e dashboard | `src/`, `web/` e `database/consultas.sql` |
| 4. Índices, consultas e views | `database/indices.sql`, `consultas_avancadas.sql`, `views.sql` |
| 5. Rotinas e triggers | `database/functions.sql`, `Procedures.sql`, `Triggers.sql` |
