import { PageHeader } from '../components/layout/PageHeader'
import { EmptyState } from '../components/ui/EmptyState'

export function RenewalsPage() {
  return (
    <>
      <PageHeader
        title="Renewals"
        description="Track and process policy renewals."
      />
      <EmptyState
        title="No renewals to show yet"
        message="This page will list renewals once the API is connected."
      />
    </>
  )
}
