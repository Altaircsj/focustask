package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.task.dto.TaskResponseDTO;

/** Application result; only task is exposed as the response body. */
record TaskCreationResult(Long userId, TaskResponseDTO task) {}
