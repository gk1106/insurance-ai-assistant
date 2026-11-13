import { policyApi } from './policyApi'
import { claimApi } from './claimApi'

async function allPages(load) {
  const first = await load({ page: 0, size: 200 })
  const items = [...(first.content || [])]
  for (let page = 1; page < (first.totalPages || 1); page++) {
    const next = await load({ page, size: 200 })
    items.push(...(next.content || []))
  }
  return items
}
export const dashboardApi = {
  async load(user) {
    const staff = ['ADMIN', 'AGENT'].includes(user?.role)
    if (!staff && !user?.customerId)
      throw new Error(
        'Your account is not linked to a customer record. Contact your administrator to connect your policies.',
      )
    const policies = await allPages((params) =>
      policyApi.list({
        ...params,
        ...(!staff ? { customerId: user.customerId } : {}),
      }),
    )
    const claims = staff
      ? await allPages((params) => claimApi.list(params))
      : (
          await Promise.all(
            policies.map((policy) =>
              allPages((params) => claimApi.listByPolicy(policy.id, params)),
            ),
          )
        ).flat()
    return { policies, claims }
  },
}
