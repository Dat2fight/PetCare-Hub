package com.petcarehub.service;

import com.petcarehub.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderExpiryJob {
    private final OrderRepository orders;
    private final OrderService service;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.order-expiry.interval-ms:60000}", initialDelayString = "${app.order-expiry.interval-ms:60000}")
    public void releaseExpiredOrders() {
        long cursor = 0;
        int released = 0;
        var now = LocalDateTime.now(clock);
        while (true) {
            var ids = orders.dueIds(cursor, now, PageRequest.of(0, 100));
            if (ids.isEmpty()) break;
            for (long id : ids) {
                try {
                    if (service.expireIfDue(id)) released++;
                } catch (RuntimeException ex) {
                    log.warn("Order expiry failed for id {} ({})", id, ex.getClass().getSimpleName());
                }
            }
            cursor = ids.get(ids.size() - 1);
        }
        if (released > 0) log.info("Expired {} orders", released);
    }
}
