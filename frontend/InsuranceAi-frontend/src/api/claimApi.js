import { apiClient } from './client'

export const claimApi = {
  list({ page = 0, size = 20 } = {}) {
    return apiClient
      .get('/claims', { params: { page, size } })
      .then((response) => response.data)
  },
}
