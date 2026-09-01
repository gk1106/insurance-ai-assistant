import { apiClient } from './client'

export const policyApi = {
  list({ page = 0, size = 20 } = {}) {
    return apiClient
      .get('/policies', { params: { page, size } })
      .then((response) => response.data)
  },

  getExpiring(days = 30) {
    return apiClient
      .get('/policies/expiring', { params: { days } })
      .then((response) => response.data)
  },
}
