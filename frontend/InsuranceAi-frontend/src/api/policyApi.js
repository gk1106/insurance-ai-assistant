import { apiClient } from './client'

export const policyApi = {
  list({ customerId, page = 0, size = 20 } = {}) {
    return apiClient
      .get('/policies', { params: { customerId, page, size } })
      .then((response) => response.data)
  },

  getById(id) {
    return apiClient.get(`/policies/${id}`).then((response) => response.data)
  },

  create(data) {
    return apiClient.post('/policies', data).then((response) => response.data)
  },

  cancel(id) {
    return apiClient.post(`/policies/${id}/cancel`).then((response) => response.data)
  },

  renew(id, data) {
    return apiClient.post(`/policies/${id}/renew`, data).then((response) => response.data)
  },

  getExpiring(days = 30) {
    return apiClient
      .get('/policies/expiring', { params: { days } })
      .then((response) => response.data)
  },
}
