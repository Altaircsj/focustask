import { DEFAULT_COLOR } from '../appearance'

export default function ProjectBadge({ project }) {
  return <span className="project-tag" style={{ '--project-color': project?.color ?? DEFAULT_COLOR }}>{project?.name ?? 'Geral'}</span>
}
