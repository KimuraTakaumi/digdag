package io.digdag.client.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import io.digdag.client.config.Config;

@Value.Immutable
@JsonSerialize(as = ImmutableRestSchedule.class)
@JsonDeserialize(as = ImmutableRestSchedule.class)
public interface RestSchedule
{
    int getId();

    IdName getProject();

    NameLongId getWorkflow();

    Instant getNextRunTime();

    OffsetDateTime getNextScheduleTime();

    static ImmutableRestSchedule.Builder builder()
    {
        return ImmutableRestSchedule.builder();
    }
}
