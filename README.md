# InsuranceHub AI

An AI-powered insurance management application with a modern React dashboard and an AI Policy Assistant.

## 🚀 Features

* Policy management
* Claims management
* Renewal management
* Insurance dashboard
* AI Policy Assistant
* AI tool calling with GPT
* JWT-based authentication
* PostgreSQL database
* Docker support
* Responsive modern UI

## 🤖 AI Policy Agent

The AI assistant can interact with the existing Policy APIs using tools such as:

* `search_policies`
* `get_policy_details`
* `create_policy`
* `update_policy`
* `check_policy_status`

### Flow

```text
User → React Chatbot → Policy Agent → GPT
                         ↓
                    Policy Tools
                         ↓
                  Policy Service
                         ↓
                   PostgreSQL
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

<img width="1919" height="955" alt="image" src="https://github.com/user-attachments/assets/b8d3dd60-386b-448f-808d-f32abcb93813" />

<img width="1919" height="943" alt="image" src="https://github.com/user-attachments/assets/439c2325-56da-40cc-9b0a-cfb6d9fef00d" />




