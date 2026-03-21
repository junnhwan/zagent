# Legacy SQL Scripts

这些 SQL 文件是历史联调阶段留下的 MCP 场景脚本和修复脚本，主要用于：

- 快速切换某一种 MCP 场景
- 修复当时的路径、斜杠、命令参数问题
- 记录项目演进过程中的手工配置方式

当前推荐方式：

- MySQL 主结构初始化：执行 `docs/sql/zagent_mysql.sql`
- PgVector 结构初始化：执行 `docs/sql/zagent_pgvector.sql`
- MCP 配置切换：修改 `src/main/resources/mcp-tools.json`，然后重启后端

也就是说，这个目录下的脚本现在主要用于“历史参考”和“问题追溯”，
不是日常开发和联调的主流程。
