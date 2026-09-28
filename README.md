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

Interface web com 4 abas:

- **Passageiros:** cadastrar, alterar, excluir e listar
- **Voos:** cadastrar, alterar, excluir e listar
- **Dashboard:** indicadores, gráficos e estatística descritiva (média, mediana, desvio padrão e histograma)
- **Consultas:** 10 consultas SQL com o resultado ao vivo


## Como executar

**Requisitos:** JDK 17+ e MySQL 8+.

1. Rode no MySQL, nesta ordem:
   - `database/migrations.sql` (cria o banco e as tabelas)
   - `database/queries.sql` (insere os dados)
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
database/   migrations.sql, queries.sql e consultas.sql (as 10 consultas)
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
