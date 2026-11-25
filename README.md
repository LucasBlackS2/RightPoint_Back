# CheckPoint — Back-end (Java / Spring Boot)

API REST de **gestão de estágios com ponto eletrônico por geolocalização**.
O aluno registra entrada e saída pelo app, a API confere se ele está perto do
local do estágio e soma as horas até completar a carga horária do contrato.

> Esta versão substitui o back-end antigo em Python/FastAPI (que continua no
> histórico do Git). As tabelas e colunas foram mantidas, então um banco que já
> existia continua funcionando.

## Tecnologias

- Java 17 ou mais novo (recomendado: JDK 21) + Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- PostgreSQL
- JWT (jjwt) + senhas com BCrypt
- Swagger UI (springdoc) em `/swagger-ui.html`
- Docker e Docker Compose

## Como executar

### 1. Configurar o `.env`

Copie o `.env.example` para `.env` e troque pelo menos `POSTGRES_PASSWORD`,
`JWT_SECRET` (32+ caracteres) e `ADMIN_SENHA`.

### 2a. Com Docker (sobe banco + API)

```bash
docker compose up --build
```

### 2b. Sem Docker (IntelliJ / VS Code / terminal)

Tenha o PostgreSQL rodando e um banco com o nome de `POSTGRES_DB` criado.
A API lê o `.env` da pasta do projeto automaticamente.

```bash
mvn spring-boot:run
```

No IntelliJ: abra a pasta, espere o Maven baixar as dependências e rode a
configuração **"CheckPoint API"** (já vem pronta na pasta `.run`).

> **Avisos `A restricted method in java.lang.System has been called`?** Não são
> erros: no Java 22+ a JVM avisa quando o Tomcat verifica uma biblioteca nativa
> opcional. A configuração "CheckPoint API", o `mvn spring-boot:run`, o `.jar` e
> o Docker já usam `--enable-native-access=ALL-UNNAMED`, que remove os avisos. Se
> rodar de outro jeito, coloque essa opção em *VM options*.

### 3. Testar

- Saúde: `GET http://localhost:8080/` → `{"status":"ok"}`
- Documentação interativa: http://localhost:8080/swagger-ui.html
- Testes: `mvn test`

Na primeira execução é criado um **admin** com `ADMIN_MATRICULA` / `ADMIN_SENHA`.

## Fluxo de uso

1. **Admin/coordenação** faz login e cria professores e alunos.
2. **Professor/coordenação** cadastra o endereço da empresa (`POST /enderecos`) e
   cria o contrato com a carga horária (`POST /contratos`).
3. **Aluno** faz login no app, registra a entrada (`POST /ponto/entrada`) e a
   saída (`PATCH /ponto/saida`) enviando a localização do celular.
4. **Aluno** acompanha o progresso em `GET /ponto/resumo`; o **professor** em
   `GET /contratos/{id}/resumo` e `GET /contratos/{id}/pontos`.

## Rotas

Todas (menos `/` e `/login`) exigem o cabeçalho `Authorization: Bearer <token>`.
O JSON usa **snake_case** (`id_aluno`, `hora_entrada`...). Erros vêm como
`{"detail": "mensagem"}`.

| Método | Rota | Quem | Descrição |
|---|---|---|---|
| POST | `/login` | todos | Login. Formulário (`username`, `password`) **ou** JSON (`matricula`, `senha`) |
| GET | `/me` | logado | Dados do usuário logado |
| PATCH | `/me/senha` | logado | `{"senha_atual", "nova_senha"}` |
| POST | `/utilizadores` | professor+ | Cria usuário (professor só cria aluno; só admin cria admin) |
| GET | `/utilizadores?tipo=aluno` | professor+ | Lista usuários |
| POST / PUT | `/enderecos`, `/enderecos/{id}` | professor+ | Endereço do estágio. `latitude`/`longitude` opcionais (busca no Google se tiver chave) |
| GET | `/enderecos` | professor+ | Lista endereços |
| POST | `/contratos` | professor+ | `{"id_aluno", "id_professor", "id_endereco", "data_inicio", "data_final", "carga_horaria_total"}` |
| GET | `/contratos` | logado | Aluno vê os seus, professor os que orienta, coordenação todos |
| GET | `/contratos/{id}` | com acesso | Detalhe |
| PATCH | `/contratos/{id}/status` | professor+ | `{"status": false}` encerra |
| GET | `/contratos/{id}/pontos` | com acesso | Histórico de pontos |
| GET | `/contratos/{id}/resumo` | com acesso | Horas cumpridas x carga |
| POST | `/ponto/entrada` | aluno | **multipart**: `latitude`, `longitude`, `qr_code`, `wifi_ssid`, `wifi_bssid` e o arquivo `foto` (selfie). Também aceita JSON sem foto |
| POST | `/ponto/saida` | aluno | Mesmos campos da entrada (multipart). `PATCH /ponto/saida` aceita JSON sem foto |
| GET | `/ponto/{id}/foto/entrada` ou `/saida` | com acesso | Selfie do registro (imagem) |
| GET | `/enderecos/{id}` | professor+ | Detalhe do local |
| PATCH | `/enderecos/{id}/local-valido` | professor+ | `{"latitude", "longitude", "raio_metros", "aceita_qr_impresso"}` (todos opcionais) — define onde o QR vale |
| POST | `/enderecos/{id}/qrcode-ao-vivo` | professor+ | `{"latitude", "longitude"}` do professor → QR ao vivo (só dentro do local válido) |
| GET | `/enderecos/{id}/qrcode` | professor+ | PNG do QR Code do local, para imprimir |
| POST | `/enderecos/{id}/qrcode/novo` | professor+ | Gera um QR Code novo (o antigo para de valer) |
| GET | `/rede/meu-ip` | professor+ | IP da rede atual (para preencher o "IP público" do local) |
| POST | `/demo/entrar` | todos | `{"perfil": "aluno" \| "professor"}` — entra numa conta de demonstração |
| GET | `/demo/qrcode` | conta demo | Conteúdo do QR Code de demonstração (simula a leitura) |
| GET | `/ponto/aberto` | aluno | Ponto aberto (404 se não houver) |
| GET | `/ponto/historico` | aluno | Todos os pontos do aluno |
| GET | `/ponto/resumo` | aluno | Progresso do contrato ativo |
| POST | `/ponto/verificar-localizacao` | aluno | Diz se está dentro do raio e a distância |
| PATCH | `/ponto/{id}/encerrar` | professor+ | Fecha ponto esquecido: `{"tempo_trabalhado_minutos": 240}` |

"professor+" = professor, coordenador ou admin.

### Exemplo de login no app (React Native)

```ts
const { data } = await api.post("/login", { matricula, senha });
// data.access_token, data.tipo_acesso
```

## Regras do ponto

Para registrar entrada **e** saída o aluno precisa provar que está no local:

1. **QR Code do local**, de um destes dois tipos:
   - **ao vivo** — aparece no celular do professor, é assinado pelo servidor e
     muda a cada `QR_AO_VIVO_SEGUNDOS` (padrão 30 s). A API **só gera** o QR se o
     GPS do professor estiver dentro do **local válido** que ele definiu
     (`PATCH /enderecos/{id}/local-valido`). Foto ou print expiram em segundos;
   - **impresso** — código fixo para colar na empresa. O professor pode
     desligá-lo (`aceita_qr_impresso: false`) para valer só o ao vivo, ou gerar
     um novo se ele vazar.
2. **GPS** — até o raio do local (`raio_metros`, definido pelo professor) ou, se
   não houver, `GEOFENCE_RAIO_METROS` (padrão 200 m).
3. **Wi-Fi do local** (opcional, por endereço) — se `wifi_ssid` estiver preenchido,
   o celular precisa estar conectado nessa rede; `wifi_bssid` exige o mesmo
   roteador; `ip_publico` exige que a requisição venha da internet do local
   (funciona também no iPhone, que não informa o nome da rede).
4. **Selfie** — foto tirada na hora, guardada no banco (tabela `fotos_ponto`),
   que o professor vê no histórico.

`EXIGIR_QRCODE` e `EXIGIR_SELFIE` (no `.env`) ligam/desligam as provas 1 e 4.

- Só um ponto aberto por aluno; entrada com ponto aberto devolve **409**.
- Entrada e saída só são aceitas a até `GEOFENCE_RAIO_METROS` (padrão 200 m)
  do endereço do contrato; fora disso devolve **400** com a distância.
- O contrato precisa estar ativo e dentro de `data_inicio`/`data_final`.
- Cada registro conta no máximo `MAX_HORAS_POR_REGISTRO` horas (padrão 8).
- Se o aluno esquecer o ponto aberto, o professor fecha em `/ponto/{id}/encerrar`.

## Modo demonstração

Com `DEMO_HABILITADO=true` (padrão), a API cria um **aluno** e um **professor** de
exemplo, um local, um contrato de 300 h e 12 registros com selfie. O app entra
nessas contas pelo botão **"Conhecer o app"**, sem senha, e mostra um tour guiado.

- As contas de demonstração **só enxergam dados de demonstração** (e os usuários
  reais não veem os dados de demonstração).
- No local de demonstração o GPS e o Wi-Fi são simulados e a selfie é opcional.
- Desligue com `DEMO_HABILITADO=false` quando for usar de verdade.

## O que mudou em relação à versão em Python

- **Login funcionando**: senhas agora são salvas com BCrypt. Senhas antigas em
  texto puro são convertidas automaticamente no primeiro login.
- **Geofencing de verdade**: a distância (Haversine) é calculada na entrada e na saída.
- **Geocodificação** do endereço pelo Google (opcional), com timeout.
- **Permissões por perfil**: aluno não lista usuários nem vê contratos de outros;
  professor não cria admin.
- **Primeiro admin** criado automaticamente a partir do `.env`.
- **JWT_SECRET obrigatório** (sem chave padrão no código) e CORS restrito.
- Erros sem vazar detalhes internos; horários em UTC e dia no fuso local.
- Novos recursos: carga horária, resumo de progresso, histórico, troca de senha
  e encerramento manual de ponto.

## Estrutura

```
src/main/java/br/com/checkpoint
├── config/       Segurança, CORS, criação do admin
├── controller/   Rotas HTTP
├── domain/       Entidades JPA (Usuario, Endereco, Contrato, Ponto)
├── dto/          Objetos de entrada/saída da API
├── exception/    Tratamento de erros
├── repository/   Acesso ao banco (Spring Data)
├── security/     JWT
└── service/      Regras de negócio
```
