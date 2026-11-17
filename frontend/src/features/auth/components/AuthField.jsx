export default function AuthField({ name, label, error, ...props }) {
  const message = Array.isArray(error) ? error.join(' ') : error
  return (
    <div className="form-field">
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} aria-invalid={Boolean(message)} aria-describedby={message ? `${name}-error` : undefined} {...props} />
      {message && <p className="field-error" id={`${name}-error`}>{message}</p>}
    </div>
  )
}
