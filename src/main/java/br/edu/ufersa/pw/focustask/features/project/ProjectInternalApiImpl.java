package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
class ProjectInternalApiImpl { // Remova 'implements ProjectInternalApi' se não for criar a interface pública do Project por enquanto, ou implemente-a se criar.

    private final ProjectRepository repository;
    private final UserInternalApi userApi; // Injeta a API pública, não o repositório

    ProjectInternalApiImpl(ProjectRepository repository, UserInternalApi userApi) {
        this.repository = repository;
        this.userApi = userApi;
    }

    public ProjectDTO criarProjeto(Long userId, ProjectDTO dto) {
        // Usa a Fachada para validar o usuário isoladamente
        if (!userApi.existePorId(userId)) {
            throw new RuntimeException("Usuário não encontrado");
        }

        Project project = new Project(userId, dto.name(), dto.description());
        Project salvo = repository.save(project);

        return new ProjectDTO(salvo.getId(), salvo.getUserId(), salvo.getName(), salvo.getDescription());
    }

    public List<ProjectDTO> listarPorUsuario(Long userId) {
        return repository.findAllByUserId(userId).stream()
                .map(p -> new ProjectDTO(p.getId(), p.getUserId(), p.getName(), p.getDescription()))
                .collect(Collectors.toList());
    }
}