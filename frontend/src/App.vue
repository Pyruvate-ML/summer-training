<template>
  <main class="app-shell">
    <section v-if="!session" class="login-screen">
      <div class="login-card paper">
        <img class="brand-mark" :src="icons.logoPeople" alt="" />
        <p class="eyebrow">校园心理服务协作平台</p>
        <h1>心桥心理</h1>
        <p class="login-copy">账号登录后由后端自动识别身份，并进入对应工作台。</p>

        <form class="login-form" @submit.prevent="handleLogin">
          <label>
            <span>账号</span>
            <input v-model.trim="loginForm.username" autocomplete="username" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="loginForm.password" type="password" autocomplete="current-password" />
          </label>
          <button class="pixel-btn" type="submit" :disabled="loggingIn">
            {{ loggingIn ? '登录中...' : '进入工作台' }}
          </button>
          <p v-if="loginError" class="error-text">{{ loginError }}</p>
        </form>

        <div class="demo-row">
          <button v-for="account in demoAccounts" :key="account.username" type="button" @click="fillAccount(account)">
            {{ account.label }}
          </button>
        </div>
      </div>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar paper">
        <div class="brand">
          <img :src="icons.logoPeople" alt="" />
          <div>
            <strong>心桥心理</strong>
            <span>{{ session.user.roleName }}</span>
          </div>
        </div>
        <nav>
          <button
            v-for="item in session.navigation"
            :key="item.key"
            :class="{ active: currentPage === item.key }"
            type="button"
            @click="currentPage = item.key"
          >
            <img :src="navIcon(item.key)" alt="" />
            {{ item.label }}
          </button>
        </nav>
        <button class="pixel-btn secondary logout" type="button" @click="logout">退出登录</button>
      </aside>

      <section class="content paper">
        <header class="topbar">
          <div>
            <p class="eyebrow">{{ session.user.displayName }} · {{ session.user.roleName }}</p>
            <h1>{{ session.workspace.title }}</h1>
            <p>{{ session.workspace.subtitle }}</p>
          </div>
          <button class="pixel-btn" type="button">{{ session.workspace.primaryAction }}</button>
        </header>

        <section v-if="currentPage === 'dashboard'" class="dashboard">
          <article v-for="card in session.dashboardCards" :key="card.key" class="stat-card paper">
            <img :src="cardIcon(card.key)" alt="" />
            <div>
              <span>{{ card.label }}</span>
              <strong>{{ card.value }} <small>{{ card.unit }}</small></strong>
            </div>
          </article>
        </section>

        <DataTable
          v-else-if="currentPage === 'appointments' || currentPage === 'bookings'"
          title="预约排期"
          eyebrow="Appointment"
          :columns="appointmentColumns"
          :rows="appointments"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <DataTable
          v-else-if="currentPage === 'patients'"
          title="来访者档案"
          eyebrow="Patient"
          :columns="patientColumns"
          :rows="patients"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <DataTable
          v-else-if="currentPage === 'doctors'"
          title="咨询师管理"
          eyebrow="Doctor"
          :columns="doctorColumns"
          :rows="doctors"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <DataTable
          v-else-if="currentPage === 'visitRecords'"
          title="咨询记录"
          eyebrow="Record"
          :columns="recordColumns"
          :rows="visitRecords"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <DataTable
          v-else-if="currentPage === 'auditLogs'"
          title="审计日志"
          eyebrow="AUDIT"
          :columns="auditLogColumns"
          :rows="auditLogs"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <DataTable
          v-else-if="currentPage === 'users'"
          title="系统用户"
          eyebrow="RBAC"
          :columns="userColumns"
          :rows="users"
          :loading="loading"
          @refresh="loadWorkspaceData"
        />

        <article v-else class="paper empty-page">
          <p class="eyebrow">{{ currentNavLabel }}</p>
          <h2>页面已纳入导航</h2>
          <p>该模块的接口和表单会在后续 CRUD 阶段继续补齐。</p>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import DataTable from './components/DataTable.vue';
import { apiGet, clearSession, loadSession, login, saveSession } from './api/client';
import logoPeople from '../../assets/xinqiao/icons/png/logo_people.png';
import overview from '../../assets/xinqiao/icons/png/overview_grid.png';
import calendar from '../../assets/xinqiao/icons/png/calendar.png';
import plus from '../../assets/xinqiao/icons/png/plus_button.png';
import team from '../../assets/xinqiao/icons/png/team.png';
import archive from '../../assets/xinqiao/icons/png/archive_file.png';
import clipboard from '../../assets/xinqiao/icons/png/clipboard.png';
import sectionTeam from '../../assets/xinqiao/icons/png/section_team.png';
import leaf from '../../assets/xinqiao/icons/png/leaf.png';
import heart from '../../assets/xinqiao/icons/png/heart.png';
import shield from '../../assets/xinqiao/icons/png/shield_check.png';
import star from '../../assets/xinqiao/icons/png/star.png';

const session = ref(loadSession());
const currentPage = ref(session.value?.navigation?.[0]?.key || 'dashboard');
const loginForm = ref({ username: 'admin', password: 'admin123' });
const loggingIn = ref(false);
const loginError = ref('');
const loading = ref(false);

const appointments = ref([]);
const patients = ref([]);
const doctors = ref([]);
const visitRecords = ref([]);
const users = ref([]);
const auditLogs = ref([]);
const icons = {
  logoPeople,
  overview,
  calendar,
  plus,
  team,
  archive,
  clipboard,
  sectionTeam,
  leaf,
  heart,
  shield,
  star
};

const demoAccounts = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '医生', username: 'doctor_zhang', password: 'doctor123' },
  { label: '就诊人员', username: 'patient_chen', password: 'patient123' }
];

const appointmentColumns = [
  { key: 'appointment_time', label: '时间' },
  { key: 'doctor_name', label: '咨询师' },
  { key: 'patient_name', label: '来访者' },
  { key: 'topic', label: '核心诉求' },
  { key: 'status', label: '状态', badge: true }
];

const patientColumns = [
  { key: 'name', label: '来访者' },
  { key: 'student_no', label: '学号' },
  { key: 'primary_topic', label: '主题' },
  { key: 'assessment_level', label: '评估等级', badge: true },
  { key: 'follow_plan', label: '跟进计划' }
];

const doctorColumns = [
  { key: 'name', label: '咨询师' },
  { key: 'title', label: '职称' },
  { key: 'specialties', label: '擅长方向' }
];

const recordColumns = [
  { key: 'visit_time', label: '时间' },
  { key: 'doctor_name', label: '咨询师' },
  { key: 'patient_name', label: '来访者' },
  { key: 'diagnosis_summary', label: '摘要' },
  { key: 'next_plan', label: '下一步计划' }
];

const auditLogColumns = [
  { key: 'created_at', label: '操作时间' },
  { key: 'operator_name', label: '操作人' },
  { key: 'operation_type', label: '操作类型', badge: true },
  { key: 'target_description', label: '操作对象' },
  { key: 'reason', label: '操作原因' }
];

const userColumns = [
  { key: 'username', label: '账号' },
  { key: 'display_name', label: '显示名' },
  { key: 'role', label: '角色', badge: true },
  { key: 'enabled', label: '启用' },
  { key: 'created_at', label: '创建时间' }
];

const currentNavLabel = computed(() => session.value?.navigation.find(item => item.key === currentPage.value)?.label || '功能页');

function fillAccount(account) {
  loginForm.value = { username: account.username, password: account.password };
}

async function handleLogin() {
  loggingIn.value = true;
  loginError.value = '';
  try {
    session.value = await login(loginForm.value);
    saveSession(session.value);
    currentPage.value = session.value.navigation[0].key;
    await loadWorkspaceData();
  } catch (error) {
    loginError.value = error.message || '登录失败';
  } finally {
    loggingIn.value = false;
  }
}

async function loadWorkspaceData() {
  if (!session.value) return;
  loading.value = true;
  try {
    const tasks = [
      apiGet('/api/appointments', session.value).then(data => { appointments.value = data; }),
      apiGet('/api/patients', session.value).then(data => { patients.value = data; }),
      apiGet('/api/doctors', session.value).then(data => { doctors.value = data; }),
      apiGet('/api/visit-records', session.value).then(data => { visitRecords.value = data; })
    ];
    if (session.value.user.role === 'ADMIN') {
      tasks.push(apiGet('/api/admin/users', session.value).then(data => { users.value = data; }));
      tasks.push(apiGet('/api/admin/audit-logs', session.value).then(data => { auditLogs.value = data; }));
    }
    await Promise.all(tasks);
  } finally {
    loading.value = false;
  }
}

function logout() {
  session.value = null;
  currentPage.value = 'dashboard';
  clearSession();
}

function navIcon(key) {
  const map = {
    dashboard: icons.overview,
    appointments: icons.calendar,
    bookings: icons.plus,
    doctors: icons.team,
    patients: icons.archive,
    visitRecords: icons.clipboard,
    users: icons.sectionTeam,
    statistics: icons.leaf,
    auditLogs: icons.shield,
    followPlans: icons.heart,
    profile: icons.logoPeople
  };
  return map[key] || icons.clipboard;
}

function cardIcon(key) {
  if (key.toLowerCase().includes('appointment')) return icons.calendar;
  if (key.toLowerCase().includes('doctor')) return icons.team;
  if (key.toLowerCase().includes('patient') || key.toLowerCase().includes('focus')) return icons.heart;
  if (key.toLowerCase().includes('health')) return icons.shield;
  if (key.toLowerCase().includes('record')) return icons.clipboard;
  return icons.star;
}

watch(session, value => {
  if (value) saveSession(value);
});

onMounted(loadWorkspaceData);
</script>
