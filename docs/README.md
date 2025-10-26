# 📚 文档目录结构

本目录包含了芝麻粒-TK项目的所有文档，按类型分类整理。

## 📂 目录结构

```
docs/
├── README.md                    # 本文件
├── migration/                   # 代码迁移相关文档
│   ├── Kotlin迁移进度和记录
│   ├── 各阶段迁移总结
│   └── 实体类迁移文档
├── daily-reports/              # 每日工作报告
│   ├── 按天/周的工作总结
│   └── 阶段性完成报告
├── bug-fixes/                  # Bug修复记录
│   ├── 错误分析报告
│   ├── 修复总结
│   └── 日志异常处理
├── testing/                    # 测试相关文档
│   ├── 测试计划
│   ├── 测试状态
│   └── 构建验证清单
├── archive/                    # 历史归档
│   ├── 已完成的里程碑
│   ├── 优化总结
│   └── 旧版本发布说明
│
├── BUILD_APK_GUIDE.md          # APK构建指南
├── KOTLIN_REFACTOR_GUIDE.md    # Kotlin重构指南
├── test-setup-guide.md         # 测试环境配置
├── 可利用组件分析.md           # 组件分析报告
└── 支付宝验证码完整文档.md     # 验证码功能文档
```

## 📖 重要文档快速索引

### 构建相关
- **APK构建指南**: `BUILD_APK_GUIDE.md`
- **快速构建步骤**: `QUICK_BUILD_STEPS.txt`
- **构建状态**: `BUILD_STATUS.md`

### 开发指南
- **Kotlin重构指南**: `KOTLIN_REFACTOR_GUIDE.md`
- **重构快速开始**: `REFACTOR_QUICKSTART.md`
- **测试环境配置**: `test-setup-guide.md`

### 迁移记录
- **迁移总体进度**: `migration/MIGRATION_PROGRESS.md`
- **Phase 1-3完成报告**: `migration/PHASE*.md`
- **实体类迁移**: `migration/ENTITY_MIGRATION_PHASE2_COMPLETE.md`

### 技术分析
- **可利用组件分析**: `可利用组件分析.md`
- **支付宝验证码文档**: `支付宝验证码完整文档.md`
- **版本更新分析**: `VERSION_UPDATE_ANALYSIS.md`

## 🗂️ 归档说明

`archive/` 目录存放已完成的阶段性文档和历史记录：
- 早期迁移报告
- 已完成的优化总结
- 旧版本的发布说明
- 过时的操作指南

## 📝 文档规范

### 命名规范
- 英文文档使用大写字母和下划线：`BUILD_GUIDE.md`
- 中文文档使用中文命名：`构建指南.md`
- 日期相关文档包含时间标识：`DAY1_COMPLETION.md`

### 分类原则
- **migration**: 代码结构变更、语言迁移
- **daily-reports**: 每日/每周工作总结
- **bug-fixes**: Bug分析和修复记录
- **testing**: 测试计划、用例、结果
- **archive**: 完成的里程碑文档

## 🔍 查找文档

使用以下命令快速查找文档：

```bash
# 查找所有迁移相关文档
Get-ChildItem -Path docs\migration -Recurse -Filter *.md

# 查找特定关键词
Select-String -Path docs\**\*.md -Pattern "关键词"

# 列出最近修改的文档
Get-ChildItem -Path docs -Recurse -Filter *.md | Sort-Object LastWriteTime -Descending | Select-Object -First 10
```

## 📌 根目录保留文档

根目录只保留核心文档：
- `README.md` - 项目主文档
- `QUICK_START.md` - 快速开始指南

---

*最后更新: 2025-10-27*
*维护者: 项目开发团队*
