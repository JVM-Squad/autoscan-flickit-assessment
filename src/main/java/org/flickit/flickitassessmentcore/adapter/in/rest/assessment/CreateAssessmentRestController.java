package org.flickit.flickitassessmentcore.adapter.in.rest.assessment;

import lombok.RequiredArgsConstructor;
import org.flickit.flickitassessmentcore.application.port.in.assessment.CreateAssessmentCommand;
import org.flickit.flickitassessmentcore.application.port.in.assessment.CreateAssessmentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("{spaceId}/assessments")
public class CreateAssessmentRestController {
    private final CreateAssessmentUseCase useCase;

    @PostMapping
    public ResponseEntity<CreateAssessmentResponseDto> createAssessment(@RequestBody CreateAssessmentRequestDto requestDto,
                                                                        @PathVariable("spaceId") Long spaceId) {

        CreateAssessmentCommand command = mapRequestDtoToCommand(requestDto, spaceId);

        CreateAssessmentResponseDto responseDto =
            mapToResponseDto(
                useCase.createAssessment(command)
            );

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    private CreateAssessmentCommand mapRequestDtoToCommand(CreateAssessmentRequestDto webModel, Long spaceId) {
        return new CreateAssessmentCommand(
            webModel.title(),
            webModel.description(),
            spaceId,
            webModel.assessmentKitId(),
            webModel.colorId()
        );
    }

    private CreateAssessmentResponseDto mapToResponseDto(UUID id) {
        return new CreateAssessmentResponseDto(id);
    }
}
