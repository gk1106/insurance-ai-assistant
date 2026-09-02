import { apiClient } from './client'

export const claimApi = {
  list({ page = 0, size = 20 } = {}) {
    return apiClient
      .get('/claims', { params: { page, size } })
      .then((response) => response.data)
  },

  listByPolicy(policyId, { page = 0, size = 20 } = {}) {
    return apiClient
      .get('/claims', { params: { policyId, page, size } })
      .then((response) => response.data)
  },

  getById(id) {
    return apiClient.get(`/claims/${id}`).then((response) => response.data)
  },

  create(data) {
    return apiClient.post('/claims', data).then((response) => response.data)
  },

  review(id) {
    return apiClient.post(`/claims/${id}/review`).then((response) => response.data)
  },

  approve(id, data) {
    return apiClient.post(`/claims/${id}/approve`, data).then((response) => response.data)
  },

  reject(id, data) {
    return apiClient.post(`/claims/${id}/reject`, data).then((response) => response.data)
  },

  pay(id) {
    return apiClient.post(`/claims/${id}/pay`).then((response) => response.data)
  },
}
