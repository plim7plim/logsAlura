package br.com.alura.logs.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.*;

import br.com.alura.logs.dto.CursoDto;
import br.com.alura.logs.exceptions.InternalErrorException;
import br.com.alura.logs.model.CursoModel;
import br.com.alura.logs.service.CursoService;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/cursos")
public class CursoController {

    final CursoService cursoService;
    private static final Logger logger = LoggerFactory.getLogger(CursoController.class);

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PostMapping
    public ResponseEntity<Object> saveCurso(@RequestBody @Valid CursoDto cursoDto) {
        try {
            logger.info("Iniciando processo de insercao de novo curso");

            if (cursoService.existsByNumeroMatricula(cursoDto.getNumeroMatricula())) {
                logger.warn("Numero de matricula ja existe");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Numero de matricula ja em uso");
            }

            if (cursoService.existsByNumeroCurso(cursoDto.getNumeroCurso())) {
                logger.warn("Numero de curso ja existe");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Numero de curso ja em uso");
            }

            var cursoModel = new CursoModel();
            BeanUtils.copyProperties(cursoDto, cursoModel);
            cursoModel.setDataInscricao(LocalDateTime.now(ZoneId.of("UTC")));

            logger.info("Curso salvo com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(cursoService.save(cursoModel));

        } catch (DataAccessResourceFailureException e) {
            logger.error("Erro de comunicacao com o database");
            throw new InternalErrorException("Erro momentaneo, tente novamente");
        }
    }

    @GetMapping
    public ResponseEntity<Page<CursoModel>> getAllCursos(
            @PageableDefault(page = 0, size = 10, sort = "dataInscricao", direction = Sort.Direction.ASC)
            Pageable pageable) {

        try {
            logger.info("Buscando cursos");
            return ResponseEntity.ok(cursoService.findAll(pageable));

        } catch (CannotCreateTransactionException e) {
            logger.error("Erro de comunicacao com o database");
            throw new InternalErrorException("Erro momentaneo, tente novamente");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneCursos(@PathVariable UUID id) {
        try {
            logger.info("Buscando curso por id");

            Optional<CursoModel> curso = cursoService.findById(id);

            if (!curso.isPresent()) {
                logger.warn("Curso nao encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Curso nao encontrado");
            }

            return ResponseEntity.ok(curso.get());

        } catch (CannotCreateTransactionException e) {
            logger.error("Erro de comunicacao com o database");
            throw new InternalErrorException("Erro momentaneo, tente novamente");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteCursos(@PathVariable UUID id) {
        try {
            logger.info("Deletando curso");

            Optional<CursoModel> curso = cursoService.findById(id);

            if (!curso.isPresent()) {
                logger.warn("Curso nao encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Curso nao encontrado");
            }

            cursoService.delete(curso.get());

            return ResponseEntity.ok("Curso deletado com sucesso");

        } catch (CannotCreateTransactionException e) {
            logger.error("Erro de comunicacao com o database");
            throw new InternalErrorException("Erro momentaneo, tente novamente");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateCursos(@PathVariable UUID id,
                                               @RequestBody @Valid CursoDto cursoDto) {
        try {
            logger.info("Atualizando curso");

            Optional<CursoModel> curso = cursoService.findById(id);

            if (!curso.isPresent()) {
                logger.warn("Curso nao encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Curso nao encontrado");
            }

            var cursoModel = new CursoModel();
            BeanUtils.copyProperties(cursoDto, cursoModel);
            cursoModel.setId(curso.get().getId());
            cursoModel.setDataInscricao(curso.get().getDataInscricao());

            return ResponseEntity.ok(cursoService.save(cursoModel));

        } catch (CannotCreateTransactionException e) {
            logger.error("Erro de comunicacao com o database");
            throw new InternalErrorException("Erro momentaneo, tente novamente");
        }
    }
}