import { PageHeader } from '../components/layout/PageHeader'
import { EmptyState } from '../components/ui/EmptyState'

export function ClaimsPage() {
  return (
    <>
      <PageHeader
        title="Claims"
        description="Review and manage insurance claims."
      />
      <EmptyState
        title="No claims to show yet"
        message="This page will list claims once the API is connected."
      />
    </>
  )
}
