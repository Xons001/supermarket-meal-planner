package com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence;

import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListResponse;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListWarningSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_warnings")
public class ShoppingListWarningEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingListEntity shoppingList;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_item_id")
    private ShoppingListItemEntity item;
    @Column(name = "warning_code", nullable = false, length = 80)
    private String warningCode;
    @Column(nullable = false, length = 1000)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShoppingListWarningSeverity severity;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ShoppingListWarningEntity() {
    }

    ShoppingListWarningEntity(
            ShoppingListEntity shoppingList,
            ShoppingListItemEntity item,
            ShoppingListResponse.ShoppingWarning warning,
            OffsetDateTime now
    ) {
        this.id = UUID.randomUUID();
        this.shoppingList = shoppingList;
        this.item = item;
        this.warningCode = warning.code();
        this.message = warning.message();
        this.severity = warning.severity();
        this.createdAt = now;
    }

    public String getWarningCode() { return warningCode; }
    public String getMessage() { return message; }
    public ShoppingListWarningSeverity getSeverity() { return severity; }
    public ShoppingListItemEntity getItem() { return item; }
}
