package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL-backed implementation of {@link Repository} for {@link Menu}.
 *
 * <p>Replaces the old {@code FileRepository<Menu>} ("menu.dat").
 * All public method signatures are identical to the old implementation.</p>
 *
 * <h3>Tree persistence strategy</h3>
 * <ol>
 *   <li>Walk the in-memory Composite tree depth-first.</li>
 *   <li>Upsert every {@link MenuCategory} and {@link MenuItem} encountered.</li>
 *   <li>After the walk, delete any rows in the DB that are no longer present
 *       in the tree (handles remove-category / remove-item operations).</li>
 * </ol>
 *
 * <p>Everything runs inside a single transaction per {@code save()} call.</p>
 */
public class DBMenuRepository implements Repository<Menu> {

    // ── SQL constants ────────────────────────────────────────────────────────

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

    // Delete items and categories that are no longer in the live tree
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

    // ── Repository<Menu> ─────────────────────────────────────────────────────

    /**
     * Persists the entire menu tree atomically.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Collect all live category and item ids by walking the tree.</li>
     *   <li>Delete db rows whose ids are NOT in the live sets.</li>
     *   <li>Upsert every node still in the tree.</li>
     * </ol>
     */
    @Override
    public void save(Menu menu) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Upsert the menu row itself
                upsertMenu(menu, conn);

                // 2. Walk tree, collecting live ids
                Set<String> liveCategoryIds = new LinkedHashSet<>();
                Set<String> liveItemIds    = new LinkedHashSet<>();
                collectLiveIds(menu.getRootCategory(), liveCategoryIds, liveItemIds);

                // 3. Remove DB rows that are no longer in the tree
                pruneOrphanedItems(menu.getId(), liveItemIds, conn);
                pruneOrphanedCategories(menu.getId(), liveCategoryIds, conn);

                // 4. Upsert every category and item in the live tree
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

    // ── private save helpers ─────────────────────────────────────────────────

    private void upsertMenu(Menu menu, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_MENU)) {
            ps.setString(1, menu.getId());
            ps.setString(2, menu.getRootCategory().getName()); // restaurant name stored here
            ps.executeUpdate();
        }
    }

    /**
     * Depth-first tree walk — collects ids of every live category and item.
     * Used to identify orphaned rows that must be deleted from the DB.
     */
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

    /**
     * Deletes menu_items rows that belong to this menu but whose ids are no
     * longer in the live tree.  Runs <em>before</em> upserting to avoid FK
     * violations when a category is removed.
     */
    private void pruneOrphanedItems(String menuId,
                                    Set<String> liveItemIds,
                                    Connection conn) throws SQLException {
        // Build the NOT IN list dynamically with PreparedStatement placeholders
        if (liveItemIds.isEmpty()) {
            // No live items → delete everything under this menu
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

    /**
     * Deletes menu_categories rows that belong to this menu but whose ids are
     * no longer in the live tree.
     */
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

    /**
     * Recursively upserts a category and all its children (sub-categories + items).
     *
     * @param menuId           id of the owning Menu row
     * @param category         the category node to upsert
     * @param parentCategoryId null for the root (restaurant) category
     */
    private void upsertCategoryTree(String menuId,
                                    MenuCategory category,
                                    String parentCategoryId,
                                    Connection conn) throws SQLException {
        // Upsert this category row
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

        // Recurse into children
        for (MenuComponent comp : category.getComponents()) {
            if (comp instanceof MenuCategory sub) {
                upsertCategoryTree(menuId, sub, category.getId(), conn);
            } else if (comp instanceof MenuItem item) {
                upsertMenuItem(item, category.getId(), conn);
            }
        }
    }

    /** Upserts a single menu item row. */
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

    // ── private load helpers ─────────────────────────────────────────────────

    /** Fully reconstructs a {@link Menu} from its DB rows. */
    private Menu loadMenu(ResultSet rs, Connection conn) throws SQLException {
        String menuId          = rs.getString("id");
        String restaurantName  = rs.getString("restaurant_name");

        // Load all categories for this menu indexed by id
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

        // Load all items, grouped by category_id
        Map<String, List<MenuItem>> itemsByCategory = new HashMap<>();
        for (String catId : catById.keySet()) {
            itemsByCategory.put(catId, loadItemsForCategory(catId, conn));
        }

        // Attach items to their categories
        for (Map.Entry<String, List<MenuItem>> entry : itemsByCategory.entrySet()) {
            MenuCategory cat = catById.get(entry.getKey());
            if (cat != null) {
                for (MenuItem item : entry.getValue()) {
                    cat.add(item);
                }
            }
        }

        // Attach child categories to their parents, find root category
        MenuCategory rootCategory = null;
        for (Map.Entry<String, String> entry : parentOf.entrySet()) {
            String childId  = entry.getKey();
            String parentId = entry.getValue();

            if (parentId == null) {
                // This IS the root category
                rootCategory = catById.get(childId);
            } else {
                MenuCategory parent = catById.get(parentId);
                MenuCategory child  = catById.get(childId);
                if (parent != null && child != null) {
                    parent.add(child);
                }
            }
        }

        // Safety fallback: if nothing persisted yet, create a fresh root
        if (rootCategory == null) {
            rootCategory = new MenuCategory(restaurantName);
        }

        return new Menu(menuId, rootCategory);
    }

    /** Loads all {@link MenuItem}s belonging to a given category. */
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

    // ── utility ──────────────────────────────────────────────────────────────

    /** Builds a {@code (?,?,?,...)} placeholder string for {@code count} params. */
    private String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }
}
