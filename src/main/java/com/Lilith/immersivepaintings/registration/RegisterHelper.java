package com.Lilith.immersivepaintings.registration;

/** Generic name to value registration callback, kept from upstream for structure parity. */
public interface RegisterHelper<T> {

    void register(String name, T value);
}
