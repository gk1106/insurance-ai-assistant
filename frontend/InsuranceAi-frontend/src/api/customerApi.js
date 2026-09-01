import { apiClient } from './client'

export const customerApi = {
  list({ page = 0, size = 20 } = {}) {
    return apiClient
      .get('/customers', { params: { page, size } })
      .then((response) => response.data)
  },
}
