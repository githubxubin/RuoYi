# 阿里三 AI 多智能体开发包

这是一套面向阿里三业务项目的通用开发规范，不绑定 RuoYi、技术栈或模型。开发人员可以选择自己创建 Agent，也可以授权主控在 Orca 中创建并调度；`ali3-dev` 负责需求拆分、计划、隔离、测试追踪、审查门禁和证据化交付。

## 目录

- `skills/ali3-dev/`：完整五环节统一入口。
- `skills/ali3-plan/`、`ali3-implement/`、`ali3-review/`、`ali3-fix/`：四个短命令专项 Skill。
- `rules/`：团队红线和 Orca 工作树协作规则。
- `templates/`：接入业务仓库时复制的规则及报告模板。
- [五环节编排设计](skills/ali3-dev/references/orca-orchestration.md)：需求计划、并行开发、集成审查、验收修复和人工合并。
- [老项目结构识别](skills/ali3-dev/references/legacy-project-discovery.md)：在第 1 环节按需识别代码结构、运行、接口、数据、鉴权、测试和复用资产。
- [交付文档与证据规则](skills/ali3-dev/references/delivery-evidence.md)：定义需求、实施、编码、测试、审查和交付文档及 AC→DEV→TC 追踪。

## 一次性安装

每位开发者先在 Orca 中配置至少一个编码智能体，再安装 Orca 官方技能包：

```bash
npx skills add https://github.com/stablyai/orca --skill orca-cli --global
npx skills add https://github.com/stablyai/orca --skill orchestration --global
```

推荐随业务项目安装，不使用本机全局副本：

```bash
mkdir -p <业务项目根目录>/.agents/skills
cp -R /Users/xubin/local/atos_project/Ali-3/ai-dev/skills/ali3-* \
  <业务项目根目录>/.agents/skills/
```

将 `.agents/skills/ali3-dev` 提交到业务仓库后，团队成员拉取代码即可获得与项目版本一致的 Skill。

**必须先提交接入文件再创建 Orca 子工作树。** 未提交的 `.agents/`、根目录 `AGENTS.md` 和 `docs/ai/project-rules.md` 不会出现在新工作树中；全局同名 Skill 不能替代项目规则。

## 业务项目接入

1. 将 `templates/AGENTS.md.template` 复制为业务仓库根目录的 `AGENTS.md`。
2. 将 `templates/project-rules.md` 复制为 `docs/ai/project-rules.md`，补全项目命令、模块边界和特有红线。
3. Skill 使用 `assets/` 自动生成任务产物；`templates/` 可作为团队审查和人工填写的副本放入 `docs/ai/templates/`。
4. 确认 `.gitignore` 排除本地凭据和无需提交的智能体临时文件。
5. 在 Orca 中打开业务仓库，而不是打开本规范仓库。

## 日常使用

```text
使用 $ali3-dev 开发功能：新增预测结果人工调整，支持保存、查询和权限控制。
```

需要只执行一个环节时使用短入口，不需要复制流程说明：

```text
$ali3-plan 新增预测结果人工调整
$ali3-implement DEV-01
$ali3-review
$ali3-fix 保存时返回 500
```

`$ali3-implement`、`$ali3-review` 和 `$ali3-fix` 会自动从 `docs/ai/runs/` 定位唯一活动任务。只有存在多个合理候选时才询问一次任务或 DEV 编号。

Skill 会完成：识别新老项目 → 必要时扫描老项目 → 生成项目地图 → 输出需求计划、实施计划、编码计划和测试用例 → 拆分甲乙任务 → 分配文件所有权 → 展示计划并等待确认。开发者不需要复制编排提示词。

确认计划时选择 Agent 启动方式：

```text
计划确认，主控创建并调度。
```

或者：

```text
计划确认，我手动创建 Agent。
```

选择主控模式时可以附加配置，例如“甲用 Codex，乙用 Qoder，模型使用 Orca 默认”；选择手动模式时，主控只输出任务单和工作树建议；也可以选择由主控单独完成老项目识别。这里的 Agent 包括模型内置、后台和只读子 Agent，用户未选择前不得创建。

开发过程中持续维护：

```text
docs/ai/runs/<任务编号>/
├── 00-run-plan.md
├── 01-requirement-plan.md
├── 02-implementation-plan.md
├── 03-coding-plan.md
├── 04-test-cases.md
├── 05-unit-test-report.md
├── 06-automation-test-report.md
├── 07-review-report.md
├── 08-delivery-report.md
├── root-cause-report.md          # 仅失败时
└── evidence/
```

最终交付报告必须能够从每条验收标准追踪到编码任务、实际代码、测试用例、执行结果和证据。

单独审查时可以使用：

```text
使用 $ali3-dev，审查当前代码差异，只报告阻断、严重、一般问题及证据，不修改代码。
```

默认流程固定为五个环节：需求与任务计划 → 甲乙并行开发 → 集成与交叉审查 → 验收与修复 → 人工合并。老项目结构识别、Figma、数据库迁移、E2E 和根因分析都是按需挂靠的条件活动，不增加主流程环节。

## 权限基线

- 智能体权限默认设为人工确认。
- 未经明确授权，不提交、不推送、不合并、不部署，不改生产或共享数据库。
- 工作树是代码隔离机制，不是安全沙箱。
- 同一文件同一时刻只能有一个写入智能体。
- 凭据只放本地密钥或环境变量文件，不进入提示词、报告、代码或版本控制。
- 自动化测试必须逐用例记录结果和证据；无法执行时明确标记阻塞或未执行，不能写成通过。

## 发布前

修改技能包后运行一次集中校验：

```bash
python3 /Users/xubin/.codex/skills/.system/skill-creator/scripts/quick_validate.py \
  /Users/xubin/local/atos_project/Ali-3/ai-dev/skills/ali3-dev
```

先在练手项目验证，再以版本号发布给团队；练手项目的技术栈和目录不得回写为阿里三全局规则。
