package com.dsatracker.service.admin;

import com.dsatracker.dto.SheetResponse;
import com.dsatracker.dto.admin.AdminSheetRequest;
import com.dsatracker.model.Sheet;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminSheetService {

    private final SheetRepository sheetRepository;
    private final TopicRepository topicRepository;

    public AdminSheetService(SheetRepository sheetRepository, TopicRepository topicRepository) {
        this.sheetRepository = sheetRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<SheetResponse> listSheets() {
        return sheetRepository.findAllByOrderByOrderIndexAsc().stream().map(SheetResponse::from).toList();
    }

    @Transactional
    public SheetResponse createSheet(AdminSheetRequest request) {
        validate(request);
        if (sheetRepository.findBySlug(request.slug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A sheet with slug '" + request.slug() + "' already exists.");
        }
        Sheet sheet = new Sheet();
        applyRequest(sheet, request);
        return SheetResponse.from(sheetRepository.save(sheet));
    }

    @Transactional
    public SheetResponse updateSheet(Long id, AdminSheetRequest request) {
        validate(request);
        Sheet sheet = sheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No sheet with id " + id));
        sheetRepository.findBySlug(request.slug())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A sheet with slug '" + request.slug() + "' already exists.");
                });
        applyRequest(sheet, request);
        return SheetResponse.from(sheetRepository.save(sheet));
    }

    @Transactional
    public void deleteSheet(Long id) {
        Sheet sheet = sheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No sheet with id " + id));
        if (topicRepository.existsBySheetId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove every topic from this sheet before deleting it.");
        }
        sheetRepository.delete(sheet);
    }

    private void validate(AdminSheetRequest request) {
        if (request.slug() == null || request.slug().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug is required.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
    }

    private void applyRequest(Sheet sheet, AdminSheetRequest request) {
        sheet.setSlug(request.slug().trim());
        sheet.setName(request.name().trim());
        sheet.setDescription(request.description());
        sheet.setOrderIndex(request.orderIndex());
    }
}
