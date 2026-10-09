package com.dsatracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 280)
    private String bio;

    /** Cumulative XP earned from first-time ACCEPTED submissions. Level is derived from this, never stored.
     *  columnDefinition carries an explicit DEFAULT so Hibernate's ddl-auto=update ALTER TABLE can backfill
     *  this NOT NULL column on an existing table that already has rows (plain `nullable=false` has no
     *  default, and Postgres rejects adding a NOT NULL column with no way to fill existing rows). */
    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private long xp = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Null for every ordinary student (the default/existing state of every row today) --
     *  only set for admin-portal users. EAGER: {@link com.dsatracker.security.JwtAuthFilter}
     *  needs the role's permissions on every request, so this widens the user lookup it
     *  already does rather than adding a second query. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    /** columnDefinition carries an explicit DEFAULT for the same reason as {@link #xp}'s --
     *  Hibernate's ddl-auto=update ALTER TABLE needs a value to backfill existing rows with
     *  when adding this NOT NULL column. Disabling an account (not deleting the role/FK) is
     *  what actually revokes access immediately, since JwtAuthFilter re-checks this on every request. */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean enabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
