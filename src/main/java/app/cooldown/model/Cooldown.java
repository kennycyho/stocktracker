package app.cooldown.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a cooldown record for a product URL.
 * <p>
 * Tracks when a product was last seen and whether notifications are disabled for it.
 * Used to prevent duplicate notifications within a configured time interval.
 */
@Entity
@Table(name = "COOLDOWN")
public class Cooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048, unique = true)
    private String url;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(nullable = false)
    private boolean disabled = false;

    /**
     * Gets the unique identifier of this cooldown record.
     *
     * @return the ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this cooldown record.
     *
     * @param id the ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the product URL associated with this cooldown.
     *
     * @return the product URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the product URL for this cooldown.
     *
     * @param url the product URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the timestamp when this product was last seen.
     *
     * @return the last seen timestamp
     */
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets the timestamp when this product was last seen.
     *
     * @param lastSeen the last seen timestamp to set
     */
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Checks if notifications are disabled for this product.
     *
     * @return true if disabled, false otherwise
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets whether notifications are disabled for this product.
     *
     * @param disabled true to disable notifications, false otherwise
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}