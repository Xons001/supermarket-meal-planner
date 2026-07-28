package com.sean.supermarketmealplanner.shoppinglist.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListCsvExporter {

    public byte[] export(ShoppingListResponse response) {
        var rows = new ArrayList<List<String>>();
        rows.add(List.of(
                "Categoría", "Producto", "Marca", "Cantidad requerida", "Unidad",
                "Formato del paquete", "Paquetes", "Cantidad comprada", "Sobrante",
                "Coste consumido", "Coste de compra", "Coste sobrante",
                "Disponibilidad", "Advertencias"
        ));
        response.groups().forEach(group -> group.items().forEach(item -> rows.add(List.of(
                value(group.categoryName()),
                value(item.productName()),
                value(item.brand()),
                decimal(item.requiredQuantity()),
                value(item.requiredUnit()),
                item.packageQuantity() == null
                        ? "No calculable"
                        : decimal(item.packageQuantity()) + " " + value(item.packageUnit()),
                item.packagesRequired() == null ? "No calculable" : item.packagesRequired().toString(),
                decimal(item.purchasedQuantity()),
                decimal(item.leftoverQuantity()),
                decimal(item.consumedCost()),
                decimal(item.purchaseCost()),
                decimal(item.wasteCost()),
                availability(item.available()),
                String.join(" | ", item.warnings())
        ))));
        var csv = new StringBuilder("\uFEFF");
        rows.forEach(row -> csv.append(row.stream().map(this::escape)
                .collect(java.util.stream.Collectors.joining(","))).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String availability(Boolean available) {
        if (available == null) {
            return "Desconocida";
        }
        return available ? "Disponible" : "No disponible";
    }
}
