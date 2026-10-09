package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.MetaConfigUpsertRequest;
import com.dsatracker.model.MetaConfig;
import com.dsatracker.model.User;
import com.dsatracker.repository.MetaConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConfigServiceTest {

    @Mock MetaConfigRepository metaConfigRepository;

    private AdminConfigService adminConfigService;

    @BeforeEach
    void setUp() {
        adminConfigService = new AdminConfigService(metaConfigRepository);
    }

    private User adminWithId(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void upsertConfig_createsNewRowWhenContextUnseen() {
        when(metaConfigRepository.findByContext("ANNOUNCEMENT_BANNER")).thenReturn(Optional.empty());
        when(metaConfigRepository.save(any(MetaConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = adminConfigService.upsertConfig("ANNOUNCEMENT_BANNER", new MetaConfigUpsertRequest("{\"text\":\"hi\"}"), adminWithId(1L));

        assertThat(response.context()).isEqualTo("ANNOUNCEMENT_BANNER");
        assertThat(response.updatedBy()).isEqualTo(1L);
    }

    @Test
    void upsertConfig_updatesExistingRowInPlace() {
        MetaConfig existing = new MetaConfig();
        existing.setId(7L);
        existing.setContext("ANNOUNCEMENT_BANNER");
        existing.setData("old");
        when(metaConfigRepository.findByContext("ANNOUNCEMENT_BANNER")).thenReturn(Optional.of(existing));
        when(metaConfigRepository.save(any(MetaConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = adminConfigService.upsertConfig("ANNOUNCEMENT_BANNER", new MetaConfigUpsertRequest("new"), adminWithId(2L));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.data()).isEqualTo("new");
        assertThat(response.updatedBy()).isEqualTo(2L);
    }

    @Test
    void getConfig_throwsNotFoundForUnknownContext() {
        when(metaConfigRepository.findByContext("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminConfigService.getConfig("MISSING"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No config with context");
    }
}
