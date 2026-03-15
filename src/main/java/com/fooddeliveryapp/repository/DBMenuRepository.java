package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;

import java.sql.*;
import java.util.*;

public class DBMenuRepository implements Repository<Menu> {

    private static final String UPSERT_MENU = """
            INSERT INTO menus (id, restaurant_name)
            VALUES (?,?)
            ON CONFLICT (id) DO UPDATE SET restaurant_name = EXCLUDED.restaurant_name
            """;

    private static final String UPSERT_CATEGORY = """
            INSERT INTO menu_categories (id, name, menu_id, parent_category_id)
            VALUES (?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                name               = EXCLUDED.name,
                parent_category_id = EXCLUDED.parent_category_id
            """;

    private static final String UPSERT_ITEM = """
            INSERT INTO menu_items (id, name, price, category_id)
            VALUES (?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                name        = EXCLUDED.name,
                price       = EXCLUDED.price,
                category_id = EXCLUDED.category_id
            """;

    private static final String DELETE_ORPHANED_ITEMS = """
            DELETE FROM menu_items
            WHERE category_id IN (
                SELECT id FROM menu_categories WHERE menu_id = ?
            )
            """;

    private static final String DELETE_ORPHANED_CATEGORIES =
            "DELETE FROM menu_categories WHERE menu_id = ?";

    private static final String SELECT_MENU_BY_ID =
            "SELECT * FROM menus WHERE id = ?";

    private static final String SELECT_ALL_MENUS =
            "SELECT * FROM menus";

    private static final String SELECT_CATEGORIES_FOR_MENU =
            "SELECT * FROM menu_categories WHERE menu_id = ?";

    private static final String SELECT_ITEMS_FOR_CATEGORY =
            "SELECT * FROM menu_items WHERE category_id = ?";

    private static final String DELETE_MENU =
            "DELETE FROM menus WHERE id = ?";

    @Override
    public void save(Menu menu) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertMenu(menu, conn);

                Set<String> liveCategoryIds = new LinkedHashSet<>();
                Set<String> liveItemIds    = new LinkedHashSet<>();
                collectLiveIds(menu.getRootCategory(), liveCategoryIds, liveItemIds);

                pruneOrphanedItems(menu.getId(), liveItemIds, conn);
                pruneOrphanedCategories(menu.getId(), liveCategoryIds, conn);

                upsertCategoryTree(menu.getId(), menu.getRootCategory(), null, conn);

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save menu: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Menu> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_MENU_BY_ID)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(loadMenu(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find menu [" + id + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Menu> findAll() {
        List<Menu> menus = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_MENUS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                menus.add(loadMenu(rs, conn));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load menus: " + e.getMessage(), e);
        }
        return menus;
    }

    @Override
    public void delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_MENU)) {

            ps.setString(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete menu [" + id + "]: " + e.getMessage(), e);
        }
    }

    private void upsertMenu(Menu menu, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_MENU)) {
            ps.setString(1, menu.getId());
            ps.setString(2, menu.getRootCategory().getName()); // restaurant name stored here
            ps.executeUpdate();
        }
    }

    private void collectLiveIds(MenuCategory category,
                                Set<String> catIds,
                                Set<String> itemIds) {
        catIds.add(category.getId());
        for (MenuComponent comp : category.getComponents()) {
            if (comp instanceof MenuCategory sub) {
                collectLiveIds(sub, catIds, itemIds);
            } else if (comp instanceof MenuItem item) {
                itemIds.add(item.getId());
            }
        }
    }

    private void pruneOrphanedItems(String menuId,
                                    Set<String> liveItemIds,
                                    Connection conn) throws SQLException {
        if (liveItemIds.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(DELETE_ORPHANED_ITEMS)) {
                ps.setString(1, menuId);
                ps.executeUpdate();
            }
            return;
        }

        String placeholders = buildPlaceholders(liveItemIds.size());
        String sql = """
                DELETE FROM menu_items
                WHERE category_id IN (
                    SELECT id FROM menu_categories WHERE menu_id = ?
                )
                AND id NOT IN (""" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, menuId);
            int idx = 2;
            for (String id : liveItemIds) {
                ps.setString(idx++, id);
            }
            ps.executeUpdate();
        }
    }

    private void pruneOrphanedCategories(String menuId,
                                         Set<String> liveCatIds,
                                         Connection conn) throws SQLException {
        if (liveCatIds.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(DELETE_ORPHANED_CATEGORIES)) {
                ps.setString(1, menuId);
                ps.executeUpdate();
            }
            return;
        }

        String placeholders = buildPlaceholders(liveCatIds.size());
        String sql = "DELETE FROM menu_categories WHERE menu_id = ? AND id NOT IN ("
                + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, menuId);
            int idx = 2;
            for (String id : liveCatIds) {
                ps.setString(idx++, id);
            }
            ps.executeUpdate();
        }
    }

    private void upsertCategoryTree(String menuId,
                                    MenuCategory category,
                                    String parentCategoryId,
                                    Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CATEGORY)) {
            ps.setString(1, category.getId());
            ps.setString(2, category.getName());
            ps.setString(3, menuId);
            if (parentCategoryId != null) {
                ps.setString(4, parentCategoryId);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            ps.executeUpdate();
        }

        for (MenuComponent comp : category.getComponents()) {
            if (comp instanceof MenuCategory sub) {
                upsertCategoryTree(menuId, sub, category.getId(), conn);
            } else if (comp instanceof MenuItem item) {
                upsertMenuItem(item, category.getId(), conn);
            }
        }
    }

    private void upsertMenuItem(MenuItem item,
                                String categoryId,
                                Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_ITEM)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, categoryId);
            ps.executeUpdate();
        }
    }

    private Menu loadMenu(ResultSet rs, Connection conn) throws SQLException {
        String menuId          = rs.getString("id");
        String restaurantName  = rs.getString("restaurant_name");

        Map<String, MenuCategory> catById = new LinkedHashMap<>();
        Map<String, String>       parentOf = new LinkedHashMap<>();  // childId → parentId

        try (PreparedStatement ps = conn.prepareStatement(SELECT_CATEGORIES_FOR_MENU)) {
            ps.setString(1, menuId);
            try (ResultSet crs = ps.executeQuery()) {
                while (crs.next()) {
                    String catId        = crs.getString("id");
                    String catName      = crs.getString("name");
                    String parentCatId  = crs.getString("parent_category_id");

                    catById.put(catId, new MenuCategory(catId, catName));
                    parentOf.put(catId, parentCatId); // null → root
                }
            }
        }

        Map<String, List<MenuItem>> itemsByCategory = new HashMap<>();
        for (String catId : catById.keySet()) {
            itemsByCategory.put(catId, loadItemsForCategory(catId, conn));
        }

        for (Map.Entry<String, List<MenuItem>> entry : itemsByCategory.entrySet()) {
            MenuCategory cat = catById.get(entry.getKey());
            if (cat != null) {
                for (MenuItem item : entry.getValue()) {
                    cat.add(item);
                }
            }
        }

        MenuCategory rootCategory = null;
        for (Map.Entry<String, String> entry : parentOf.entrySet()) {
            String childId  = entry.getKey();
            String parentId = entry.getValue();

            if (parentId == null) {
                rootCategory = catById.get(childId);
            } else {
                MenuCategory parent = catById.get(parentId);
                MenuCategory child  = catById.get(childId);
                if (parent != null && child != null) {
                    parent.add(child);
                }
            }
        }

        if (rootCategory == null) {
            rootCategory = new MenuCategory(restaurantName);
        }

        return new Menu(menuId, rootCategory);
    }

    private List<MenuItem> loadItemsForCategory(String categoryId,
                                                Connection conn) throws SQLException {
        List<MenuItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ITEMS_FOR_CATEGORY)) {
            ps.setString(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemId    = rs.getString("id");
                    String itemName  = rs.getString("name");
                    double itemPrice = rs.getDouble("price");
                    items.add(new MenuItem(itemId, itemName, itemPrice));
                }
            }
        }
        return items;
    }

    private String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }
}
