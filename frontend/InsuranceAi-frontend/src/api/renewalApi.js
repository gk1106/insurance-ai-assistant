import { apiClient } from './client'

export const renewalApi = {
  listByPolicy(policyId, { page = 0, size = 20 } = {}) {
    return apiClient
      .get('/renewals', { params: { policyId, page, size } })
      .then((response) => response.data)
  },

  getById(id) {
    return apiClient.get(`/renewals/${id}`).then((response) => response.data)
  },

  create(data) {
    return apiClient.post('/renewals', data).then((response) => response.data)
  },

  confirm(id) {
    return apiClient.post(`/renewals/${id}/confirm`).then((response) => response.data)
  },

  reject(id) {
    return apiClient.post(`/renewals/${id}/reject`).then((response) => response.data)
  },
}
