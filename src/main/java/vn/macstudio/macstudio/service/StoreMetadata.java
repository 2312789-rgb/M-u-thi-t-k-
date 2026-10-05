package vn.macstudio.macstudio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StoreMetadata {
    private final String name;
    public StoreMetadata(@Value("${store.name:Mặc Studio}") String name) { this.name = name; }
    public String getName() { return name; }
}