import { apiClient } from './client'

export const authApi = {
  login({ username, password }) {
    return apiClient
      .post('/auth/login', { username, password })
      .then((response) => response.data)
  },
}
