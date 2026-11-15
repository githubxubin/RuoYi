---
name: ali3-plan
description: "为阿里三软件变更单独完成需求分析和开发规划，生成需求、实施、编码和测试用例文档并停在 G1；适用于用户只想描述需求、暂不编码的场景。"
---

# 阿里三开发规划

用户只需输入：

```text
$ali3-plan <需求描述>
```

不要要求用户补充任务目录、文档清单、Agent 分工或流程规则；这些由本 Skill 推导。只有缺失信息会实质改变需求或验收结果时，才问一个简短问题。

## 执行

1. 将当前仓库作为目标项目。若 `AGENTS.md`、`docs/ai/project-rules.md` 或 `.agents/skills/ali3-dev/SKILL.md` 任一缺失，立即停止并报告接入不完整，不得使用全局 Skill 继续规划。
2. 读取 [统一规划规则](../ali3-dev/references/planning.md) 和 [交付证据规则](../ali3-dev/references/delivery-evidence.md)。老项目地图缺失、过期或未覆盖本次模块时，再读 [老项目结构识别](../ali3-dev/references/legacy-project-discovery.md)。
3. 自动生成稳定任务编号和 `docs/ai/runs/<任务编号>/`，使用 `../ali3-dev/assets/` 的模板生成 `00`～`04` 文档。
4. 建立 AC→DEV→TC 追踪、文件所有权、依赖、验证命令和风险；测试用例预期不得从实现代码反推。
5. Agent 包括 Orca Agent、模型内置子 Agent、后台 Agent和只读 Agent。首次创建任何一种前先询问用户选择“主控创建并调度”“用户手动创建”或“主控独立完成”；将选择写入实施计划。未选择前不得创建 Agent。
6. 本 Skill 只负责规划，即使用户选择主控模式也不立即创建；展示摘要和文档路径后停在 G1，后续由 `$ali3-dev` 完整流程按已记录选择执行。

无论用户是否重复说明，均不得提交、推送、合并、部署或操作生产/共享数据库。
