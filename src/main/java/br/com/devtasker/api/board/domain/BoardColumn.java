package br.com.devtasker.api.board.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_columns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardColumnCategory category;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private BoardColumn(
            Board board,
            String name,
            BoardColumnCategory category,
            Integer position
    ) {
        if (position == null || position < 0) {
            throw new IllegalArgumentException(
                    "A posição da coluna não pode ser negativa."
            );
        }

        this.board = board;
        this.name = name;
        this.category = category;
        this.position = position;
    }

    public static BoardColumn create(
            Board board,
            String name,
            BoardColumnCategory category,
            Integer position
    ) {
        return new BoardColumn(
                board,
                name,
                category,
                position
        );
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        this.updatedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }
}