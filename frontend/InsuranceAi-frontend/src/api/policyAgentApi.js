import { apiClient } from './client'

export const policyAgentApi = {
  chat({ message }) {
    return apiClient
      .post('/ai/policy-agent/chat', { message })
      .then((response) => response.data)
  },
}
