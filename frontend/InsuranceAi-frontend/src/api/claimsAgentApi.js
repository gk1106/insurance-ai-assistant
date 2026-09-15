import { apiClient } from './client'

export const claimsAgentApi = {
  chat({ message }) {
    return apiClient
      .post('/ai/claims-agent/chat', { message })
      .then((response) => response.data)
  },
}
