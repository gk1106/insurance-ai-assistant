import { apiClient } from './client'

export const aiAssistantApi = {
  chat({ message }) {
    return apiClient
      .post('/ai/chat', { message })
      .then((response) => response.data)
  },
}
