-- 菜单表
CREATE TABLE IF NOT EXISTS `ledger_menus` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单ID',
    `name` VARCHAR(50) NOT NULL COMMENT '菜单名称（标识）',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '显示标题',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    `component` VARCHAR(100) DEFAULT NULL COMMENT '组件路径',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色',
    `type` VARCHAR(10) NOT NULL DEFAULT 'MENU' COMMENT '类型：DIRECTORY-目录, MENU-菜单, BUTTON-按钮',
    `permission` VARCHAR(200) DEFAULT NULL COMMENT '权限标识',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `hidden` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否隐藏：0-显示, 1-隐藏',
    `keep_alive` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否缓存：0-不缓存, 1-缓存',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-正常, 1-已删除',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_type` (`type`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 插入默认菜单数据
INSERT INTO `ledger_menus` (`id`, `parent_id`, `name`, `title`, `path`, `component`, `icon`, `type`, `permission`, `sort_order`, `hidden`, `keep_alive`, `deleted`, `created_at`, `updated_at`) VALUES
-- 系统管理目录
(1, NULL, 'System', '系统管理', '/system', NULL, 'Setting', 'DIRECTORY', NULL, 100, 0, 1, 0, NOW(), NOW()),
-- 系统管理子菜单
(2, 1, 'UserManage', '用户管理', '/system/user', 'system/user/index', 'User', 'MENU', 'system:user:list', 1, 0, 1, 0, NOW(), NOW()),
(3, 1, 'MenuManage', '菜单管理', '/system/menu', 'system/menu/index', 'Menu', 'MENU', 'system:menu:list', 2, 0, 1, 0, NOW(), NOW()),
-- 记账管理目录
(10, NULL, 'Ledger', '记账管理', '/ledger', NULL, 'Wallet', 'DIRECTORY', NULL, 200, 0, 1, 0, NOW(), NOW()),
-- 记账管理子菜单
(11, 10, 'Transaction', '交易流水', '/ledger/transaction', 'ledger/transaction/index', 'List', 'MENU', 'ledger:transaction:list', 1, 0, 1, 0, NOW(), NOW()),
(12, 10, 'Account', '账户管理', '/ledger/account', 'ledger/account/index', 'CreditCard', 'MENU', 'ledger:account:list', 2, 0, 1, 0, NOW(), NOW()),
(13, 10, 'Category', '分类管理', '/ledger/category', 'ledger/category/index', 'Folder', 'MENU', 'ledger:category:list', 3, 0, 1, 0, NOW(), NOW()),
(14, 10, 'Book', '账本管理', '/ledger/book', 'ledger/book/index', 'Book', 'MENU', 'ledger:book:list', 4, 0, 1, 0, NOW(), NOW()),
-- 统计报表目录
(20, NULL, 'Statistics', '统计报表', '/statistics', NULL, 'TrendCharts', 'DIRECTORY', NULL, 300, 0, 1, 0, NOW(), NOW()),
-- 统计报表子菜单
(21, 20, 'Overview', '数据概览', '/statistics/overview', 'statistics/overview/index', 'Dashboard', 'MENU', 'statistics:overview', 1, 0, 1, 0, NOW(), NOW()),
(22, 20, 'Trend', '收支趋势', '/statistics/trend', 'statistics/trend/index', 'LineChart', 'MENU', 'statistics:trend', 2, 0, 1, 0, NOW(), NOW()),
(23, 20, 'CategoryStats', '分类统计', '/statistics/category', 'statistics/category/index', 'PieChart', 'MENU', 'statistics:category', 3, 0, 1, 0, NOW(), NOW()),
-- 用户菜单（无需权限，所有用户可见）
(30, NULL, 'UserCenter', '个人中心', '/user', NULL, 'UserFilled', 'DIRECTORY', NULL, 400, 0, 1, 0, NOW(), NOW()),
(31, 30, 'Profile', '个人资料', '/user/profile', 'user/profile/index', 'User', 'MENU', NULL, 1, 0, 1, 0, NOW(), NOW()),
(32, 30, 'Settings', '系统设置', '/user/settings', 'user/settings/index', 'Setting', 'MENU', NULL, 2, 0, 1, 0, NOW(), NOW());

-- 按钮权限（以用户管理为例）
INSERT INTO `ledger_menus` (`parent_id`, `name`, `title`, `type`, `permission`, `sort_order`, `hidden`, `keep_alive`, `deleted`, `created_at`, `updated_at`) VALUES
(2, 'UserAdd', '新增用户', 'BUTTON', 'system:user:add', 1, 0, 1, 0, NOW(), NOW()),
(2, 'UserEdit', '编辑用户', 'BUTTON', 'system:user:edit', 2, 0, 1, 0, NOW(), NOW()),
(2, 'UserDelete', '删除用户', 'BUTTON', 'system:user:delete', 3, 0, 1, 0, NOW(), NOW()),
(3, 'MenuAdd', '新增菜单', 'BUTTON', 'system:menu:add', 1, 0, 1, 0, NOW(), NOW()),
(3, 'MenuEdit', '编辑菜单', 'BUTTON', 'system:menu:edit', 2, 0, 1, 0, NOW(), NOW()),
(3, 'MenuDelete', '删除菜单', 'BUTTON', 'system:menu:delete', 3, 0, 1, 0, NOW(), NOW());
