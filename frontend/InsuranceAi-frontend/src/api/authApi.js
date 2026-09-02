import { apiClient } from './client'

export const authApi = {
  login({ username, password }) {
    return apiClient
      .post('/auth/login', { username, password })
      .then((response) => response.data)
  },

  register({ username, email, password }) {
    return apiClient
      .post('/auth/register', { username, email, password })
      .then((response) => response.data)
  },
}
