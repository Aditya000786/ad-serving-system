package com.adserving.core.port;

import com.adserving.core.model.AdEvent;

public interface EventPort {
    void publishEvent(AdEvent event);
}
