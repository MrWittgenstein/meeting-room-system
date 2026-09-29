<template>
  <main class="analysis-page">
    <header class="hero">
      <div>
        <p class="eyebrow">WEEK 4 · DATA ANALYSIS</p>
        <h1>会议室运行分析</h1>
        <p class="subtitle">从清洗后的传感器记录定位无人运行设备，并验证 30 分钟占用预测链路。</p>
      </div>
      <span v-if="summary" class="mode-badge">{{ modeLabel }}</span>
    </header>

    <el-alert
      v-if="summary"
      title="本页使用可复现的教学模拟数据，不代表真实会议室运行结论。"
      type="warning"
      :closable="false"
      show-icon
      class="notice"
    />

    <section v-if="loading" class="state-card">正在读取分析结果…</section>
    <section v-else-if="error" class="state-card error-state">
      <h2>分析结果暂不可用</h2>
      <p>{{ error }}</p>
      <p>请在仓库根目录运行 <code>python scripts/run_week4.py</code> 后刷新页面。</p>
    </section>

    <template v-else-if="summary">
      <section class="scope-grid" aria-label="数据范围">
        <article class="scope-card">
          <span>分析记录</span>
          <strong>{{ number(summary.data.records) }}</strong>
          <small>{{ summary.data.rooms }} 个会议室</small>
        </article>
        <article class="scope-card">
          <span>覆盖时间</span>
          <strong>{{ dateOnly(summary.data.start) }} — {{ dateOnly(summary.data.end) }}</strong>
          <small>每 {{ summary.data.sampling_minutes }} 分钟采样</small>
        </article>
        <article class="scope-card">
          <span>基线测试集</span>
          <strong>{{ number(summary.baseline?.test_period.samples || 0) }}</strong>
          <small>严格按时间后置切分</small>
        </article>
        <article class="scope-card accent-card">
          <span>30 分钟预测准确率</span>
          <strong>{{ percent(summary.baseline?.metrics.accuracy) }}</strong>
          <small>F1 {{ percent(summary.baseline?.metrics.f1) }}</small>
        </article>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p>KEY FINDINGS</p>
          <h2>三个可核验发现</h2>
        </div>
        <div class="finding-grid">
          <article v-for="item in summary.highlights" :key="item.label" class="finding-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p>EVIDENCE</p>
          <h2>图表与统计口径</h2>
        </div>
        <div class="figure-grid">
          <article v-for="(figure, index) in summary.figures" :key="figure.file" class="figure-card">
            <div class="figure-number">0{{ index + 1 }}</div>
            <img :src="figureUrl(figure.file)" :alt="figure.title" loading="lazy" />
            <div class="figure-copy">
              <h3>{{ figure.title }}</h3>
              <p>{{ figure.conclusion }}</p>
              <details>
                <summary>查看计算口径</summary>
                <p>{{ figure.definition }}</p>
              </details>
            </div>
          </article>
        </div>
      </section>

      <section v-if="summary.baseline" class="section-block model-section">
        <div class="section-heading">
          <p>BASELINE</p>
          <h2>占用预测结果</h2>
        </div>
        <div class="metric-row">
          <div><span>准确率</span><strong>{{ percent(summary.baseline.metrics.accuracy) }}</strong></div>
          <div><span>精确率</span><strong>{{ percent(summary.baseline.metrics.precision) }}</strong></div>
          <div><span>召回率</span><strong>{{ percent(summary.baseline.metrics.recall) }}</strong></div>
          <div><span>F1</span><strong>{{ percent(summary.baseline.metrics.f1) }}</strong></div>
          <div><span>平衡准确率</span><strong>{{ percent(summary.baseline.metrics.balanced_accuracy) }}</strong></div>
        </div>
        <p class="model-note">
          {{ summary.baseline.model }}。训练集 {{ summary.baseline.train_period.start }} 至
          {{ summary.baseline.train_period.end }}（n={{ number(summary.baseline.train_period.samples) }}），测试集
          {{ summary.baseline.test_period.start }} 至 {{ summary.baseline.test_period.end }}（n={{ number(summary.baseline.test_period.samples) }}）。
        </p>
      </section>

      <footer class="limitations">
        <strong>结果边界</strong>
        <ul>
          <li v-for="item in summary.limitations" :key="item">{{ item }}</li>
        </ul>
      </footer>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const summary = ref(null)
const loading = ref(true)
const error = ref('')

const modeLabel = computed(() => {
  const modes = summary.value?.data?.mode || []
  return modes.includes('simulated') ? 'SIMULATED · 模拟数据' : modes.join(' · ').toUpperCase()
})

const number = (value) => new Intl.NumberFormat('zh-CN').format(value ?? 0)
const percent = (value) => value == null ? '—' : `${(value * 100).toFixed(1)}%`
const dateOnly = (value) => value ? value.slice(0, 10) : '—'
const figureUrl = (file) => `${import.meta.env.BASE_URL}analysis/figures/${file}`

onMounted(async () => {
  try {
    const response = await fetch(`${import.meta.env.BASE_URL}analysis/week4_summary.json`, { cache: 'no-store' })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    summary.value = await response.json()
  } catch (reason) {
    error.value = `无法读取 week4_summary.json：${reason instanceof Error ? reason.message : reason}`
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.analysis-page {
  --ink: #17333b;
  --muted: #62777d;
  --mint: #c8eee7;
  --aqua: #3e918b;
  min-height: 100%;
  padding: 32px clamp(20px, 4vw, 56px) 56px;
  color: var(--ink);
  background:
    radial-gradient(circle at 92% 2%, rgba(116, 203, 194, 0.22), transparent 28%),
    linear-gradient(180deg, #f9fcfb 0%, #eef6f4 100%);
}

.hero { display: flex; justify-content: space-between; gap: 24px; align-items: flex-start; }
.eyebrow, .section-heading p { margin: 0 0 8px; color: var(--aqua); font-size: 12px; font-weight: 800; letter-spacing: .18em; }
h1 { margin: 0; font-size: clamp(30px, 4vw, 48px); letter-spacing: -.04em; }
.subtitle { max-width: 720px; margin: 14px 0 0; color: var(--muted); line-height: 1.8; }
.mode-badge { flex: none; padding: 10px 14px; border: 1px solid #8bc9c2; border-radius: 999px; background: #e9faf7; color: #24716c; font-size: 12px; font-weight: 800; }
.notice { margin: 24px 0; }
.state-card { margin-top: 24px; padding: 30px; border-radius: 18px; background: white; box-shadow: 0 16px 44px rgba(35, 79, 76, .08); }
.error-state { color: #9f3c36; }

.scope-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin: 24px 0 42px; }
.scope-card { min-height: 112px; padding: 20px; border: 1px solid rgba(88, 137, 132, .16); border-radius: 18px; background: rgba(255, 255, 255, .84); box-shadow: 0 12px 30px rgba(35, 79, 76, .06); }
.scope-card span, .metric-row span { display: block; color: var(--muted); font-size: 12px; }
.scope-card strong { display: block; margin: 13px 0 8px; font-size: clamp(18px, 2vw, 26px); }
.scope-card small { color: var(--muted); }
.accent-card { border-color: transparent; color: #fff; background: linear-gradient(135deg, #347e79, #54aaa2); }
.accent-card span, .accent-card small { color: rgba(255, 255, 255, .78); }

.section-block { margin-top: 42px; }
.section-heading { margin-bottom: 18px; }
.section-heading h2 { margin: 0; font-size: 25px; }
.finding-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.finding-card { padding: 24px; border-radius: 18px; background: #173f46; color: white; }
.finding-card span { color: #9adbd3; font-size: 12px; font-weight: 700; }
.finding-card strong { display: block; margin: 12px 0; font-size: 20px; }
.finding-card p { margin: 0; color: rgba(255, 255, 255, .72); line-height: 1.65; font-size: 14px; }

.figure-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.figure-card { position: relative; overflow: hidden; border: 1px solid rgba(88, 137, 132, .16); border-radius: 20px; background: white; box-shadow: 0 16px 44px rgba(35, 79, 76, .07); }
.figure-card img { display: block; width: 100%; aspect-ratio: 16 / 10; object-fit: contain; background: #fff; }
.figure-number { position: absolute; z-index: 1; top: 14px; left: 14px; padding: 6px 9px; border-radius: 8px; background: rgba(23, 51, 59, .88); color: white; font-size: 11px; letter-spacing: .12em; }
.figure-copy { padding: 20px 22px 22px; }
.figure-copy h3 { margin: 0 0 10px; }
.figure-copy p { margin: 0; color: var(--muted); line-height: 1.65; }
details { margin-top: 14px; padding-top: 12px; border-top: 1px solid #e8efed; }
summary { cursor: pointer; color: var(--aqua); font-weight: 700; }
details p { margin-top: 10px !important; font-size: 13px; }

.model-section { padding: 28px; border-radius: 22px; background: #fff; box-shadow: 0 16px 44px rgba(35, 79, 76, .07); }
.metric-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.metric-row div { padding: 16px; border-radius: 14px; background: #f1f8f6; }
.metric-row strong { display: block; margin-top: 8px; font-size: 22px; }
.model-note { margin: 20px 0 0; color: var(--muted); line-height: 1.7; }
.limitations { margin-top: 42px; padding: 22px 24px; border-left: 4px solid #d49a51; background: #fff8ed; }
.limitations ul { margin: 10px 0 0; padding-left: 20px; color: #735b3e; line-height: 1.8; }
code { padding: 2px 6px; border-radius: 5px; background: #edf3f2; }

@media (max-width: 1050px) {
  .scope-grid { grid-template-columns: repeat(2, 1fr); }
  .metric-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 720px) {
  .analysis-page { padding: 24px 16px 40px; }
  .hero { flex-direction: column; }
  .scope-grid, .finding-grid, .figure-grid, .metric-row { grid-template-columns: 1fr; }
}
</style>
