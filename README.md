# 🚀 InsuranceHub AI

An AI-powered insurance management platform built with **React, Spring Boot, PostgreSQL, and OpenAI**, featuring multi-agent AI, RAG, MCP, tool calling, and a modern enterprise UI.

## ✨ Features

- 📊 Insurance Dashboard
- 📋 Policy Management
- 🧾 Claims Management
- 🔄 Renewal Management
- 🤖 AI Insurance Assistant
- 🧠 Multi-Agent Architecture
- 🔧 AI Tool Calling
- 📚 RAG (Retrieval-Augmented Generation)
- 🔌 MCP Integration
- 🛡️ JWT Authentication & Authorization
- 🛡️ AI Guardrails
- 📈 AI Observability & Tracing
- 🐳 Docker & Docker Compose
- ⚙️ GitHub Actions CI/CD
- 📱 Responsive Premium UI

---

## 🤖 AI Architecture

The application uses specialized agents for different insurance domains.

```text
                         ┌───────────────┐
                         │     User      │
                         └───────┬───────┘
                                 │
                                 ▼
                     ┌─────────────────────┐
                     │   AI Orchestrator   │
                     └──────────┬──────────┘
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
      ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
      │ Policy Agent│    │ Claims Agent│    │Renewal Agent│
      └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
             │                  │                  │
             ▼                  ▼                  ▼
        Policy Tools       Claims Tools       Renewal Tools
             │                  │                  │
             └──────────────────┼──────────────────┘
                                ▼
                        Spring Boot Services
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
              PostgreSQL                RAG System
                                            │
                                            ▼
                                      Vector Store
                                            │
                                            ▼
                                     Insurance Docs
```

The AI does not directly access the database; it uses the application's existing service layer.

## 🛠️ Tech Stack

**Frontend**

* React
* Tailwind CSS
* Modern responsive UI

**Backend**

* Java
* Spring Boot
* Spring AI
* JWT Authentication

**Database**

* PostgreSQL

**AI**

* OpenAI GPT
* Tool Calling

**DevOps**

* Docker
* Docker Compose

## ▶️ Run Locally

```bash
git clone <repository-url>
cd InsuranceHub
cd backend
docker compose up --build
```

Configure your OpenAI API key:

```env
OPENAI_API_KEY=your-api-key
OPENAI_MODEL=gpt-5-mini
```

## 📌 Project Status

Current implementation includes the InsuranceHub dashboard and a working AI Policy Agent with tool calling.

Next planned features:

* Claims Agent
* Renewal Agent
* Multi-Agent Orchestration
* RAG
* MCP
* Evaluation & Guardrails


<img width="1919" height="952" alt="image" src="https://github.com/user-attachments/assets/dcee6119-e137-4da2-9fae-55cc1d62c825" />

<img width="1919" height="951" alt="image" src="https://github.com/user-attachments/assets/6c25eb47-4da7-4749-ae78-aafe719e4c18" />

<img width="1919" height="950" alt="image" src="https://github.com/user-attachments/assets/2816a73c-9293-4c2e-bc93-b31b0ba07a37" />

<img width="1919" height="952" alt="image" src="https://github.com/user-attachments/assets/3e5d693b-c169-4c3a-a195-91b5b181c0ea" />

<img width="1919" height="948" alt="image" src="https://github.com/user-attachments/assets/07ee8970-b72c-4194-90b7-2bc340ef3563" />






