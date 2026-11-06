# ⚙️ Sistema de Gestão para Oficina Mecânica e Autopeças

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

Sistema completo de gestão comercial, operacional e financeira desenvolvido como **Trabalho de Conclusão de Curso (TCC)** e projetado sob medida para atender os fluxos de trabalho do setor automotivo (oficinas mecânicas, centros de manutenção e autopeças).

> 💡 **Nota de Portfólio & Demonstração:**
> Este projeto foi desenvolvido e validado em ambiente real de produção para uma oficina mecânica. Para fins de portfólio público, apresentação acadêmica e rigorosa conformidade com a **LGPD** e boas práticas de segurança, **esta versão foi sanitizada**: credenciais de acesso, dados cadastrais de clientes e links diretos para a infraestrutura de produção foram completamente desvinculados do repositório público.

---

## 🛠️ Tecnologias e Arquitetura

### Backend (API RESTful)
- **Linguagem & Framework:** Java 17, Spring Boot 3.2+
- **Segurança:** Spring Security com autenticação stateless JWT, criptografia BCrypt, proteção contra Mass Assignment e validação rigorosa de DTOs aninhados.
- **Proteção contra Abuso:** Filtro de Rate Limiting com proteção contra IP spoofing (`X-Forwarded-For` confiável) para mitigação de força bruta e DoS.
- **Persistência de Dados:** Spring Data JPA, Hibernate, PostgreSQL (com H2 em memória configurado para testes automatizados).
- **Relatórios & Documentação:** OpenPDF / iText para geração dinâmica de Ordens de Serviço, Orçamentos e Relatórios em PDF.
- **Containerização:** Docker (Multi-stage build otimizado com execução em usuário não-root `spring:spring`).
- **Testes Automatizados:** 80+ testes com JUnit 5 e Mockito cobrindo serviços, controladores e regras críticas de segurança.

### Frontend (SPA & Desktop)
- **Framework & Build:** React 18, Vite, JavaScript moderno (ESM).
- **Design & UI:** Tailwind CSS, Lucide Icons, interface responsiva e componentes modulares.
- **Camada HTTP:** Axios com interceptors para injeção de Bearer Token e tratamento global de expiração de sessão (401/403).
- **Desktop Ready:** Configurado para empacotamento desktop via Electron para ambiente Windows.

---

## 🚀 Principais Módulos do Sistema

1. **Ordens de Serviço (OS):**
   - Ciclo de vida completo: abertura, execução, finalização e entrega.
   - Associação de mecânicos responsáveis, itens de serviço e peças aplicadas com baixa automática de estoque.
   - Emissão e impressão profissional de O.S. formatada em PDF.

2. **Orçamentos:**
   - Elaboração e precificação rápida para aprovação do cliente.
   - Conversão com um clique de orçamento aprovado em Ordem de Serviço.
   - Emissão de via impressa em PDF para o cliente.

3. **Gestão Financeira & Comissões:**
   - Contas a pagar e receber (despesas operacionais e receitas de serviços/peças).
   - Baixa individual e em lote com conciliação de valores.
   - Apuração automatizada de comissões por mecânico e período.
   - Dashboard analítico com visão de faturamento mensal, saldo líquido e distribuição financeira.

4. **Estoque e Cadastros:**
   - Controle de peças e materiais com estoque mínimo, preço de custo e margem de venda.
   - Cadastro unificado de clientes, fornecedores e equipe técnica.

5. **Controle de Acesso (RBAC):**
   - Separação estrita de privilégios entre perfis:
     - `ADMIN`: acesso integral ao financeiro, exclusões, relatórios e gestão de usuários.
     - `OPERADOR`: foco nas rotinas de atendimento, orçamentos e execução de ordens de serviço.

---

## ⚙️ Como Executar Localmente

### Pré-requisitos
- **Java JDK 17+**
- **Node.js 18+** e **npm**
- **PostgreSQL 14+** (ou executar direto com o perfil de teste H2)

### 1. Clonando o Repositório
```bash
git clone https://github.com/JoaoPedro456/TCC_.git
cd TCC_
```

### 2. Executando o Backend
```bash
cd backend-TCC

# No Linux/macOS:
./mvnw spring-boot:run

# No Windows (PowerShell):
.\mvnw.cmd spring-boot:run
```
O backend estará ativo em `http://localhost:8080`.

### 3. Executando o Frontend
```bash
cd ../frontend-TCC

# Instalação das dependências
npm install

# Inicialização em modo de desenvolvimento
npm run dev
```
O frontend estará acessível em `http://localhost:5173`.

---

## 🛡️ Configuração de Variáveis de Ambiente

As configurações de produção são mantidas fora do código e lidas exclusivamente através de variáveis de ambiente:

| Variável | Descrição |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | URL de conexão JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco de dados |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco de dados |
| `JWT_SECRET` | Chave de assinatura HMAC-SHA256 (mínimo 256 bits) |
| `JWT_EXPIRATION` | Tempo de validade do token (em ms) |
| `ADMIN_DEFAULT_PASSWORD` | Senha inicial gerada para o primeiro bootstrap |

---

## 📄 Licença e Propriedade Intelectual

Projeto desenvolvido como Trabalho de Conclusão de Curso (TCC). Código disponibilizado publicamente para fins de demonstração técnica e portfólio.
