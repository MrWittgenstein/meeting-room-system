# 小组成员 Git 协作操作手册

本手册适用于本项目的所有小组成员。默认远程仓库为：

```text
https://github.com/MrWittgenstein/meeting-room-system.git
```

项目采用以下协作方式：

```text
main 主分支
    ↑ Pull Request 审核合并
个人功能分支
    ↑ commit + push
组员本地修改
```

除紧急情况并经组内确认外，**不要直接向 `main` 分支提交代码，也不要强制推送 `main`**。

## 1. 必须遵守的规则

1. 每位成员使用自己的 GitHub 账号和 Git 提交身份。
2. 开始新任务前，先更新本地 `main`。
3. 每项任务创建一个新的个人分支。
4. 修改完成后推送个人分支，通过 Pull Request 合并。
5. 提交前检查密码、密钥、日志、构建产物和个人数据。
6. 不提交真实人脸照片、数据库完整备份或本地配置。
7. 不使用 `git push --force`；确有需要时只在自己的分支使用 `--force-with-lease`。
8. 不运行 `git reset --hard`、`git clean -fd` 等可能丢失代码的命令，除非已经确认修改不再需要。

## 2. 第一次加入项目前的准备

### 2.1 注册并接受邀请

1. 注册自己的 GitHub 账号。
2. 将 GitHub 用户名发给仓库管理员。
3. 等待管理员邀请加入仓库。
4. 在 GitHub 通知或邀请邮件中点击接受邀请。

未接受邀请时，私有仓库可能无法克隆或推送。

### 2.2 检查 Git 和 Git LFS

在 PowerShell 中执行：

```powershell
git --version
git lfs version
```

两个命令都应显示版本号。项目使用 Git LFS 管理 ONNX 人脸识别模型，因此不能跳过 Git LFS。

## 3. 第一次克隆仓库

先进入准备存放项目的父目录，例如：

```powershell
Set-Location "D:\code"
```

克隆仓库：

```powershell
git clone https://github.com/MrWittgenstein/meeting-room-system.git
```

进入项目：

```powershell
Set-Location ".\meeting-room-system"
```

初始化 Git LFS 并下载模型：

```powershell
git lfs install
git lfs pull
```

检查仓库：

```powershell
git rev-parse --show-toplevel
git remote -v
git status
git lfs ls-files
```

`git remote -v` 应显示：

```text
origin  https://github.com/MrWittgenstein/meeting-room-system.git (fetch)
origin  https://github.com/MrWittgenstein/meeting-room-system.git (push)
```

## 4. 配置自己的提交身份

每位成员都要使用自己的姓名和邮箱，不能共用组长身份。

只为当前仓库配置：

```powershell
git config user.name "你的姓名"
git config user.email "你的邮箱"
```

检查：

```powershell
git config user.name
git config user.email
```

如果是自己的私人电脑，并希望所有仓库都使用同一身份，可以改用：

```powershell
git config --global user.name "你的姓名"
git config --global user.email "你的邮箱"
```

在多人共用的电脑上不要使用 `--global`。

## 5. 每次开始新任务的标准流程

### 5.1 确认工作区没有遗留修改

```powershell
git status
```

如果显示：

```text
nothing to commit, working tree clean
```

说明可以继续。

如果存在上一次未完成的修改，应先选择以下一种方式处理：

- 完成并提交修改；
- 暂存修改；
- 确认不需要后再放弃修改。

不要在不清楚修改内容时直接覆盖或删除。

### 5.2 更新本地 `main`

```powershell
git switch main
git pull --ff-only origin main
```

`--ff-only` 可以避免在本地 `main` 上意外生成合并提交。

### 5.3 创建任务分支

```powershell
git switch -c feature/姓名-任务名称
```

示例：

```powershell
git switch -c feature/zhangsan-frontend-status
```

建议的分支类型：

| 前缀 | 用途 | 示例 |
| --- | --- | --- |
| `feature/` | 新功能 | `feature/zhangsan-room-status` |
| `fix/` | 修复问题 | `fix/lisi-websocket-reconnect` |
| `docs/` | 文档修改 | `docs/wangwu-backend-guide` |
| `test/` | 测试工作 | `test/zhaoliu-sensor-api` |
| `refactor/` | 代码重构 | `refactor/zhangsan-device-service` |

分支名使用英文、数字和连字符，不要使用空格。

检查当前分支：

```powershell
git branch --show-current
```

确认输出是自己的任务分支，而不是 `main`。

## 6. 开发过程中应注意什么

### 6.1 经常检查状态

```powershell
git status --short
```

常见标记：

| 标记 | 含义 |
| --- | --- |
| `M` | 文件被修改 |
| `A` | 新文件已加入暂存区 |
| `D` | 文件被删除 |
| `??` | 新文件尚未跟踪 |

### 6.2 不要提交以下内容

```text
frontend/node_modules/
frontend/dist/
backend/target/
*.jar
*.class
*.log
.venv/
__pycache__/
raspberry-pi/config.json
raspberry-pi/known_faces/
raspberry-pi/data/face_db.npz
真实数据库备份
密码、Token、邮箱授权码和OSS密钥
```

以下依赖说明文件应当提交：

```text
frontend/package.json
frontend/package-lock.json
backend/pom.xml
raspberry-pi/requirements.txt
raspberry-pi/config.example.json
```

### 6.3 不要随意修改他人的任务文件

如果两个人需要同时修改同一个文件，应先在组内沟通并划分范围，否则容易发生冲突。

## 7. 完成修改后的检查

### 7.1 查看修改内容

```powershell
git status
git diff
```

`git diff` 显示尚未加入暂存区的修改。

### 7.2 运行对应测试

前端常用检查：

```powershell
Set-Location ".\frontend"
npm ci
npm run lint
npm run build
Set-Location ".."
```

后端常用检查：

```powershell
Set-Location ".\backend"
mvn test
Set-Location ".."
```

树莓派模拟运行示例：

```powershell
Set-Location ".\raspberry-pi"
python sensor_server.py --config config.json --demo
```

测试结束后回到仓库根目录：

```powershell
Set-Location (git rev-parse --show-toplevel)
```

只运行与本次修改相关、且当前环境能够支持的测试。无法运行的测试应在 Pull Request 中说明原因。

## 8. 加入暂存区并提交

### 8.1 精确加入文件

推荐明确指定文件：

```powershell
git add ".\frontend\src\需要提交的文件"
git add ".\backend\src\需要提交的文件"
```

如果已经确认所有修改都应该提交，可以使用：

```powershell
git add .
```

### 8.2 检查暂存内容

```powershell
git status
git diff --cached
git diff --cached --stat
```

必须在 `git commit` 前检查：

- 是否有与本任务无关的文件；
- 是否包含真实密码或密钥；
- 是否包含日志、缓存、构建产物或个人数据；
- 是否误删了文件；
- 修改量是否与本次任务相符。

### 8.3 从暂存区移除误加文件

该命令只取消暂存，不删除本地修改：

```powershell
git restore --staged "文件路径"
```

### 8.4 创建提交

```powershell
git commit -m "feat: 增加会议室实时状态展示"
```

推荐提交类型：

| 类型 | 用途 |
| --- | --- |
| `feat` | 新增功能 |
| `fix` | 修复问题 |
| `docs` | 修改文档 |
| `test` | 增加或修改测试 |
| `refactor` | 重构代码，不改变外部功能 |
| `build` | 修改依赖或构建配置 |
| `chore` | 其他维护工作 |

提交示例：

```text
feat: 增加会议室实时环境状态页面
fix: 修复树莓派WebSocket断线后无法重连
docs: 补充后端本地启动说明
test: 增加传感器模拟数据接口测试
refactor: 重构设备命令处理服务
```

一次提交只处理一个清晰问题。不要使用“修改代码”“更新一下”等无法说明内容的提交信息。

## 9. 推送前同步远程 `main`

先获取远程更新：

```powershell
git fetch origin
```

把自己的分支变基到最新 `main`：

```powershell
git rebase origin/main
```

如果没有冲突，Git会直接完成同步。

检查提交记录：

```powershell
git log --oneline --graph --decorate -10
```

## 10. 推送个人分支

第一次推送当前分支：

```powershell
git push -u origin HEAD
```

`HEAD` 表示当前所在的个人分支，不需要重复输入完整分支名。

以后继续推送同一分支：

```powershell
git push
```

推送完成后，终端通常会显示创建 Pull Request 的链接。

## 11. 创建 Pull Request

1. 打开 GitHub 仓库页面。
2. 点击 `Compare & pull request`，或者进入 `Pull requests` 后点击 `New pull request`。
3. 确认目标分支为 `main`。
4. 确认来源分支为自己的任务分支。
5. 填写标题和说明。
6. 创建 Pull Request。

标题示例：

```text
feat: 增加会议室实时状态展示
```

说明模板：

```markdown
## 本次修改

- 修改内容1
- 修改内容2

## 测试方法

- 执行的命令：
- 实际结果：

## 影响范围

- 前端 / 后端 / 树莓派 / 数据库

## 检查清单

- [ ] 没有提交密码、密钥和个人数据
- [ ] 没有提交构建产物和日志
- [ ] 已完成能够运行的测试
- [ ] README或配置模板已按需更新
```

创建后通知负责审核的组员。不要自己在未检查的情况下立即合并。

## 12. 根据审核意见继续修改

如果审核人提出修改意见，继续在原来的个人分支修改：

```powershell
git switch 你的分支名
```

完成修改后：

```powershell
git add .
git commit -m "fix: 根据审核意见修正状态刷新逻辑"
git push
```

新的提交会自动出现在原Pull Request中，不需要重新创建Pull Request。

## 13. Pull Request合并后的处理

切换到本地 `main`：

```powershell
git switch main
```

更新本地代码：

```powershell
git pull --ff-only origin main
```

删除已经合并的本地分支：

```powershell
git branch -d 已合并的分支名
```

例如：

```powershell
git branch -d feature/zhangsan-frontend-status
```

如果GitHub没有自动删除远程分支，可以在Pull Request页面点击 `Delete branch`。也可以由分支本人执行：

```powershell
git push origin --delete 已合并的分支名
```

开始下一项任务时，再从最新的 `main` 创建新分支。

## 14. 临时切换任务：使用 stash

如果当前修改尚未完成，但需要临时切换分支：

```powershell
git stash push -u -m "临时保存：任务说明"
```

查看暂存记录：

```powershell
git stash list
```

切换回来后恢复：

```powershell
git stash pop
```

恢复后立即执行：

```powershell
git status
```

检查是否存在冲突。不要长期把重要工作只保存在 stash 中。

## 15. 处理 rebase 冲突

执行：

```powershell
git rebase origin/main
```

如果出现冲突：

### 15.1 查看冲突文件

```powershell
git status
```

### 15.2 手动编辑冲突

冲突文件中可能出现：

```text
<<<<<<< HEAD
远程main中的内容
=======
个人分支中的内容
>>>>>>> 提交编号
```

与相关成员确认应该保留的代码，编辑为最终正确内容，并删除上述冲突标记。

### 15.3 标记已解决

```powershell
git add "发生冲突的文件路径"
```

### 15.4 继续 rebase

```powershell
git rebase --continue
```

如果还有冲突，重复以上步骤。

### 15.5 放弃本次 rebase

如果无法确认应如何解决：

```powershell
git rebase --abort
```

该命令会回到执行rebase之前的状态。然后联系相关成员共同处理。

### 15.6 rebase后推送

如果个人分支以前已经推送过，rebase改变了提交历史，需要执行：

```powershell
git push --force-with-lease
```

只允许对自己的个人分支使用该命令。不要使用 `git push --force`，也不要对 `main` 使用任何强制推送。

## 16. 常见误操作恢复

### 16.1 文件误加入暂存区

保留本地修改，仅取消暂存：

```powershell
git restore --staged "文件路径"
```

### 16.2 放弃某个文件尚未提交的修改

警告：该操作会丢失这个文件的本地修改。

```powershell
git restore "文件路径"
```

执行前先用 `git diff -- "文件路径"` 检查内容。

### 16.3 最近一次提交信息写错且尚未推送

```powershell
git commit --amend -m "正确的提交信息"
```

### 16.4 已推送的提交需要撤销

不要直接重写 `main` 历史。创建一个反向提交：

```powershell
git log --oneline
git revert 提交编号
git push
```

### 16.5 不确定修改是否还能恢复

先停止执行删除、重置或清理命令，并保留：

```powershell
git status
git log --oneline --all --decorate -20
git reflog -20
```

将输出发给仓库管理员后再处理。

## 17. 误提交密码或敏感数据

如果密码、Token、邮箱授权码、OSS密钥、人脸照片或真实数据库已经提交：

1. 立即停止继续推送和合并。
2. 通知仓库管理员和相关成员。
3. 立即更换已经泄露的密码或密钥。
4. 不要以为“再提交一次删除文件”就能消除泄露，因为旧提交仍然保存内容。
5. 由仓库管理员统一清理Git历史并通知所有成员重新同步。

不要在群聊中再次粘贴真实密钥。

## 18. 常见报错

### 18.1 `not a git repository`

说明当前目录不在仓库内：

```powershell
Set-Location "你的路径\meeting-room-system"
git rev-parse --show-toplevel
```

### 18.2 `Permission denied` 或没有推送权限

检查：

- 是否登录了被邀请的GitHub账号；
- 是否已经接受仓库邀请；
- `git remote -v` 是否指向正确仓库；
- 当前GitHub凭据是否属于自己的账号。

### 18.3 `non-fast-forward`

远程分支出现了新提交：

```powershell
git fetch origin
git rebase origin/main
```

解决冲突后再推送。

### 18.4 无法直接推送 `main`

如果 `main` 受到保护，这是正常现象。创建个人分支并通过Pull Request提交：

```powershell
git switch -c feature/姓名-任务
git push -u origin HEAD
```

### 18.5 ONNX模型只是很小的文本指针

说明Git LFS对象尚未下载：

```powershell
git lfs install
git lfs pull
git lfs ls-files
```

### 18.6 前端出现大量 `node_modules` 修改

确认自己位于正确仓库并检查忽略规则：

```powershell
git check-ignore -v ".\frontend\node_modules"
```

不要把 `node_modules` 加入仓库。

## 19. 每日操作速查

### 开始任务

```powershell
git status
git switch main
git pull --ff-only origin main
git switch -c feature/姓名-任务
```

### 完成修改

```powershell
git status
git diff
git add .
git diff --cached
git commit -m "feat: 修改说明"
git fetch origin
git rebase origin/main
git push -u origin HEAD
```

然后在GitHub创建Pull Request。

### 合并后更新

```powershell
git switch main
git pull --ff-only origin main
git branch -d 已合并的分支名
```

## 20. 提交前最终检查清单

- [ ] 当前分支不是 `main`
- [ ] 本次修改只包含一个明确任务
- [ ] 已检查 `git diff`
- [ ] 已运行当前环境能够运行的测试
- [ ] 未提交数据库密码、邮箱授权码、Token或OSS密钥
- [ ] 未提交真实人脸照片和特征数据库
- [ ] 未提交 `node_modules`、`dist`、`target`、虚拟环境和日志
- [ ] 提交信息能够准确说明修改内容
- [ ] 推送的是个人分支
- [ ] Pull Request说明了修改内容和测试结果

遵守以上流程，可以确保每位成员的提交记录清晰、主分支稳定，并减少多人协作时的覆盖和冲突。
