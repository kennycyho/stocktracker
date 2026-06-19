package org.example.notifier.cooldown.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "COOLDOWN")
public class Cooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048, unique = true)
    private String url;

    @CreationTimestamp
    @Column(name = "cooldown_since", nullable = false)
    private LocalDateTime cooldownSince;

    @Column(nullable = false)
    private boolean disabled = false;

    public Cooldown(String url) {
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getCooldownSince() {
        return cooldownSince;
    }

    public void setCooldownSince(LocalDateTime cooldownSince) {
        this.cooldownSince = cooldownSince;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}