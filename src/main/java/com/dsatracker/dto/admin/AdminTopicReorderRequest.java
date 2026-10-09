package com.dsatracker.dto.admin;

import java.util.List;

/** Every topic id under one sheet, in the desired display order -- the service assigns
 *  orderIndex 0..n-1 to match, and rejects a request that omits or duplicates an id. */
public record AdminTopicReorderRequest(Long sheetId, List<Long> orderedTopicIds) {
}
