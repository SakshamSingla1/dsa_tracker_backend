package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminSheetRequest;
import com.dsatracker.model.Sheet;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSheetServiceTest {

    @Mock SheetRepository sheetRepository;
    @Mock TopicRepository topicRepository;

    private AdminSheetService adminSheetService;

    @BeforeEach
    void setUp() {
        adminSheetService = new AdminSheetService(sheetRepository, topicRepository);
    }

    @Test
    void createSheet_rejectsDuplicateSlug() {
        Sheet existing = new Sheet();
        existing.setId(1L);
        when(sheetRepository.findBySlug("a2z")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminSheetService.createSheet(new AdminSheetRequest("a2z", "A2Z", "desc", 0)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteSheet_rejectsWhenTopicsExist() {
        Sheet sheet = new Sheet();
        sheet.setId(1L);
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet));
        when(topicRepository.existsBySheetId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminSheetService.deleteSheet(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Remove every topic");
    }
}
