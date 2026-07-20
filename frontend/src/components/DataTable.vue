<template>
  <article class="paper table-panel">
    <div class="table-title">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h3>{{ title }}</h3>
      </div>
      <button class="pixel-btn secondary" type="button" @click="$emit('refresh')">刷新</button>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td :colspan="columns.length" class="empty">正在加载...</td>
          </tr>
          <tr v-else-if="rows.length === 0">
            <td :colspan="columns.length" class="empty">暂无数据</td>
          </tr>
          <tr v-for="row in rows" v-else :key="row.id || JSON.stringify(row)">
            <td v-for="column in columns" :key="column.key">
              <span v-if="column.badge" class="badge" :class="badgeClass(row[column.key])">{{ row[column.key] }}</span>
              <span v-else>{{ row[column.key] || '-' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </article>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  eyebrow: { type: String, default: '数据列表' },
  columns: { type: Array, required: true },
  rows: { type: Array, required: true },
  loading: { type: Boolean, default: false }
});

defineEmits(['refresh']);

function badgeClass(value) {
  if (['静候确认', '关注'].includes(value)) return 'warn';
  if (['重点'].includes(value)) return 'danger';
  if (['已排定', '普通'].includes(value)) return 'ok';
  return 'neutral';
}
</script>
