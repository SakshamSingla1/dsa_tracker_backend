package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.MetaConfigResponse;
import com.dsatracker.dto.admin.MetaConfigUpsertRequest;
import com.dsatracker.model.MetaConfig;
import com.dsatracker.model.User;
import com.dsatracker.repository.MetaConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class AdminConfigService {

    private final MetaConfigRepository metaConfigRepository;

    public AdminConfigService(MetaConfigRepository metaConfigRepository) {
        this.metaConfigRepository = metaConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<MetaConfigResponse> listConfigs() {
        return metaConfigRepository.findAllByOrderByContextAsc().stream().map(MetaConfigResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MetaConfigResponse getConfig(String context) {
        return MetaConfigResponse.from(findConfig(context));
    }

    /** Upsert: creates the row the first time a context is saved, updates it every time after. */
    @Transactional
    public MetaConfigResponse upsertConfig(String context, MetaConfigUpsertRequest request, User currentAdmin) {
        if (context == null || context.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "context is required.");
        }
        if (request.data() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "data is required.");
        }
        MetaConfig config = metaConfigRepository.findByContext(context).orElseGet(() -> {
            MetaConfig c = new MetaConfig();
            c.setContext(context);
            return c;
        });
        config.setData(request.data());
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(currentAdmin.getId());
        return MetaConfigResponse.from(metaConfigRepository.save(config));
    }

    @Transactional
    public void deleteConfig(String context) {
        metaConfigRepository.delete(findConfig(context));
    }

    private MetaConfig findConfig(String context) {
        return metaConfigRepository.findByContext(context)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No config with context '" + context + "'"));
    }
}
