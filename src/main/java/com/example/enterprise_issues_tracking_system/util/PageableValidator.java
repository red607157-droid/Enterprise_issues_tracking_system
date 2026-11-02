package com.example.enterprise_issues_tracking_system.util;

import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.stream.Collectors;

public final class PageableValidator {

    private PageableValidator() {
    }
    public static void requireSortableBy(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                String allowed = allowedProperties.stream().sorted().collect(Collectors.joining(", "));
                throw new InvalidRequestException("cannot sort by '" + order.getProperty() + "'. Allowed fields: " + allowed);
            }
        }
    }
}
