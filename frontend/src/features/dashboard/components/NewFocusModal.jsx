import FocusSessionForm from './FocusSessionForm'

export default function NewFocusModal({ onStart, ...props }) {
  return <FocusSessionForm {...props} onSave={onStart} />
}
