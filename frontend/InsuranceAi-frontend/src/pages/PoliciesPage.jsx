import { PageHeader } from '../components/layout/PageHeader'
import { EmptyState } from '../components/ui/EmptyState'

export function PoliciesPage() {
  return (
    <>
      <PageHeader title="Policies" description="Manage insurance policies." />
      <EmptyState
        title="No policies to show yet"
        message="This page will list policies once the API is connected."
      />
    </>
  )
}
