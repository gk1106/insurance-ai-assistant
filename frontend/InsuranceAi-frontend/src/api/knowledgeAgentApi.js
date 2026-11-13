import { apiClient } from "./client";
export const knowledgeAgentApi = {
  chat: (data) =>
    apiClient
      .post("/ai/knowledge-agent/chat", data)
      .then((response) => response.data),
};
