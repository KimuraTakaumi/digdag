package io.digdag.client.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableNameLongId.class)
@JsonDeserialize(as = ImmutableNameLongId.class)
public interface NameLongId
{
    String getName();

    long getId();

    static NameLongId of(String name, long id)
    {
        return ImmutableNameLongId.builder()
            .name(name)
            .id(id)
            .build();
    }
}
