import FocusSessionForm from './FocusSessionForm'

export default function EditFocusModal({ session, ...props }) {
  return <FocusSessionForm key={session.id} session={session} {...props} />
}
