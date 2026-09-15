import { apiClient } from './client'

export const renewalAgentApi = {
  chat({ message }) {
    return apiClient
      .post('/ai/renewal-agent/chat', { message })
      .then((response) => response.data)
  },
}
