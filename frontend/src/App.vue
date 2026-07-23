<template>
  <main class="app-shell">
    <section v-if="!session" class="login-screen">
      <div class="login-card paper">
        <img class="brand-mark" :src="icons.heart" alt="" />
        <p class="eyebrow">校园心理咨询协作管理平台</p>
        <h1>心桥心理</h1>
        <p class="login-copy">{{ authMode === 'login' ? '登录后按账号角色进入对应工作台。' : '当前仅开放来访者自助注册。' }}</p>

        <div class="auth-tabs">
          <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'">登录</button>
          <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'">来访者注册</button>
        </div>

        <form v-if="authMode === 'login'" class="login-form" @submit.prevent="handleLogin">
          <label><span>账号</span><input v-model.trim="loginForm.username" autocomplete="username" /></label>
          <label><span>密码</span><input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
          <button class="pixel-btn" type="submit" :disabled="loggingIn">{{ loggingIn ? '登录中...' : '进入心桥' }}</button>
          <p v-if="loginError" class="error-text">{{ loginError }}</p>
        </form>

        <form v-else class="register-form" @submit.prevent="handleRegister">
          <label><span>账号</span><input v-model.trim="registerForm.username" autocomplete="username" required /></label>
          <label><span>显示名称</span><input v-model.trim="registerForm.displayName" required /></label>
          <label><span>密码</span><input v-model="registerForm.password" type="password" autocomplete="new-password" required /></label>
          <label><span>确认密码</span><input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" required /></label>
          <label><span>真实姓名</span><input v-model.trim="registerForm.name" required /></label>
          <label><span>学号</span><input v-model.trim="registerForm.studentNo" /></label>
          <label><span>学院</span><input v-model.trim="registerForm.college" /></label>
          <label><span>年级</span><input v-model.trim="registerForm.grade" /></label>
          <label class="wide"><span>主要诉求</span><input v-model.trim="registerForm.primaryTopic" placeholder="如：学业压力 / 人际关系 / 情绪困扰" required /></label>
          <button class="pixel-btn wide" type="submit" :disabled="registering">{{ registering ? '创建中...' : '创建来访者账号' }}</button>
          <p v-if="registerError" class="error-text wide">{{ registerError }}</p>
        </form>

        <div v-if="authMode === 'login'" class="demo-row">
          <button v-for="account in demoAccounts" :key="account.username" type="button" @click="fillAccount(account)">
            {{ account.label }}
          </button>
        </div>
      </div>
    </section>

    <section v-else class="workspace">
      <header class="app-header wood">
        <div class="app-title">
          <img :src="icons.heart" alt="" />
          <strong>心桥心理</strong>
          <span>{{ headerTitle }}</span>
        </div>
        <div class="app-user">
          <img :src="avatarForRole(session.user.role)" alt="" />
          <div>
            <strong>{{ session.user.displayName }}</strong>
            <span>{{ session.user.roleName }}</span>
          </div>
          <button class="pixel-btn secondary" type="button" @click="logout">退出登录</button>
        </div>
      </header>

      <aside class="sidebar wood">
        <nav>
          <button v-for="item in session.navigation" :key="item.key" :class="{ active: currentPage === item.key }" type="button" @click="setCurrentPage(item.key)">
            <img :src="navIcon(item.key)" alt="" />
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <section class="content">
        <header class="topbar">
          <div class="hero-copy">
            <div class="hero-title">
              <img :src="heroIcon" alt="" />
              <h1>{{ session.workspace.title }}</h1>
              <span v-if="session.user.role === 'PATIENT'" class="badge warn">仅本人可见</span>
            </div>
            <p>{{ session.workspace.subtitle }}</p>
          </div>
          <img class="hero-house" :src="icons.house" alt="" />
          <button class="pixel-btn hero-action" type="button" @click="handlePrimaryAction">{{ session.workspace.primaryAction }}</button>
        </header>

        <section v-if="currentPage === 'dashboard'" class="dashboard-page">
          <section class="dashboard">
            <article v-for="card in session.dashboardCards" :key="card.key" class="stat-card paper">
              <img :src="cardIcon(card.key)" alt="" />
              <div><span>{{ card.label }}</span><strong>{{ compactCardValue(card.value) }} <small>{{ card.unit }}</small></strong></div>
            </article>
          </section>

          <section v-if="session.user.role === 'ADMIN'" class="overview-grid">
            <DataTable title="全平台预约列表" eyebrow="Today" :columns="appointmentColumns" :rows="appointments" :loading="loading" @refresh="loadWorkspaceData" />
            <DataTable title="重点关注来访者列表" eyebrow="Focus" :columns="patientColumns" :rows="focusPatients" :loading="loading" @refresh="loadWorkspaceData" />
            <DataTable title="值班与排班概览" eyebrow="Roster" :columns="doctorColumns" :rows="doctors" :loading="loading" @refresh="loadWorkspaceData" />
            <DataTable title="系统用户概况" eyebrow="Users" :columns="userColumns" :rows="users" :loading="loading" @refresh="loadWorkspaceData" />
          </section>

          <section v-else-if="session.user.role === 'PATIENT'" class="overview-grid patient-home">
            <article class="paper info-panel">
              <h3><img :src="icons.calendar" alt="" />我的下一次预约详情</h3>
              <p>预约时间：{{ nextAppointment?.appointment_time || '暂无预约' }}</p>
              <p>咨询师：{{ nextAppointment?.doctor_name || '-' }}</p>
              <p>咨询主题：{{ nextAppointment?.topic || '-' }}</p>
              <p>预约状态：<span class="badge ok">{{ nextAppointment?.status || '暂无' }}</span></p>
            </article>
            <article class="paper info-panel">
              <h3><img :src="icons.team" alt="" />我的咨询师信息</h3>
              <p>{{ nextAppointment?.doctor_name || doctors[0]?.name || '暂无关联咨询师' }}</p>
              <p>{{ doctors[0]?.title || '预约后展示咨询师信息' }}</p>
              <p>{{ doctors[0]?.specialties || '-' }}</p>
            </article>
            <DataTable title="最近咨询记录摘要" eyebrow="Records" :columns="recordColumns" :rows="visitRecords" :loading="loading" @refresh="loadWorkspaceData" />
            <article class="paper info-panel">
              <h3><img :src="icons.leaf" alt="" />跟进建议 / 今日提醒</h3>
              <p>{{ patients[0]?.follow_plan || '暂无跟进建议' }}</p>
              <p>建议持续记录情绪和睡眠状态，必要时再次预约咨询。</p>
            </article>
          </section>
        </section>

        <article v-else-if="currentPage === 'bookings'" class="paper form-panel">
          <div class="table-title"><div><p class="eyebrow">Booking</p><h3>提交预约申请</h3></div></div>
          <form class="booking-form" @submit.prevent="submitBooking">
            <label v-if="session.user.role === 'ADMIN'"><span>来访者</span><select v-model.number="bookingForm.patientId" required><option v-for="patient in patients" :key="patient.id" :value="patient.id">{{ patient.name }} · {{ patient.primary_topic }}</option></select></label>
            <label><span>咨询师</span><select v-model.number="bookingForm.doctorId" required><option disabled :value="null">请选择咨询师</option><option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id">{{ doctor.name }} · {{ doctor.title }}</option></select></label>
            <label><span>预约时间</span><input v-model="bookingForm.appointmentTime" type="datetime-local" required /></label>
            <label><span>咨询主题</span><input v-model.trim="bookingForm.topic" placeholder="如：考试焦虑 / 人际关系" required /></label>
            <label><span>地点/方式</span><input v-model.trim="bookingForm.location" placeholder="咨询室 / 在线咨询" /></label>
            <label class="wide"><span>预约原因</span><textarea v-model.trim="bookingForm.reason" placeholder="简要说明希望咨询的问题，便于咨询师确认" required></textarea></label>
            <div class="form-actions wide"><button class="pixel-btn" type="submit" :disabled="bookingSubmitting">{{ bookingSubmitting ? '提交中...' : '提交预约申请' }}</button><p v-if="bookingMessage" :class="bookingSuccess ? 'success-text' : 'error-text'">{{ bookingMessage }}</p></div>
          </form>
        </article>

        <DataTable v-else-if="currentPage === 'appointments'" title="预约排期" eyebrow="Appointment" :columns="appointmentColumns" :rows="appointments" :loading="loading" @refresh="loadWorkspaceData" />
        <DataTable v-else-if="currentPage === 'patients'" title="来访者档案" eyebrow="Patient" :columns="patientColumns" :rows="patients" :loading="loading" @refresh="loadWorkspaceData" />
        <DataTable v-else-if="currentPage === 'doctors'" title="咨询师管理" eyebrow="Doctor" :columns="doctorColumns" :rows="doctors" :loading="loading" @refresh="loadWorkspaceData" />
        <DataTable v-else-if="currentPage === 'visitRecords'" title="咨询记录" eyebrow="Record" :columns="recordColumns" :rows="visitRecords" :loading="loading" @refresh="loadWorkspaceData" />
        <DataTable v-else-if="currentPage === 'auditLogs'" title="审计日志" eyebrow="Audit" :columns="auditLogColumns" :rows="auditLogs" :loading="loading" @refresh="loadWorkspaceData" />

        <section v-else-if="currentPage === 'users'" class="page-stack">
          <article class="paper form-panel">
            <div class="table-title"><div><p class="eyebrow">Users</p><h3>系统用户管理</h3></div><button class="pixel-btn secondary" type="button" @click="loadWorkspaceData">刷新</button></div>
            <div class="admin-split">
              <div class="user-list">
                <button v-for="user in users" :key="user.id" type="button" :class="{ active: selectedUser?.id === user.id }" @click="selectUser(user)">
                  <strong>{{ user.display_name }}</strong><span>{{ user.username }} · {{ user.role }} · {{ user.enabled ? '启用' : '停用' }}</span>
                </button>
              </div>
              <form v-if="selectedUser" class="profile-form admin-form" @submit.prevent="saveSelectedUser">
                <label><span>显示名</span><input v-model.trim="adminUserForm.displayName" required /></label>
                <label><span>角色</span><select v-model="adminUserForm.role"><option value="ADMIN">管理员</option><option value="DOCTOR">咨询师</option><option value="COUNSELOR">辅导员</option><option value="PATIENT">来访者</option></select></label>
                <label><span>账号状态</span><select v-model="adminUserForm.enabled"><option :value="true">启用</option><option :value="false">停用</option></select></label>
                <label><span>电话</span><input v-model.trim="adminUserForm.phone" /></label>
                <label><span>邮箱</span><input v-model.trim="adminUserForm.email" /></label>
                <label><span>部门/学院</span><input v-model.trim="adminUserForm.department" /></label>
                <label><span>办公室/咨询地点</span><input v-model.trim="adminUserForm.officeLocation" /></label>
                <label><span>紧急联系人</span><input v-model.trim="adminUserForm.emergencyContact" /></label>
                <label><span>偏好说明</span><input v-model.trim="adminUserForm.preferenceNote" /></label>
                <label class="wide"><span>个人简介</span><textarea v-model.trim="adminUserForm.bio"></textarea></label>
                <label class="wide"><span>修改原因</span><input v-model.trim="adminUserForm.reason" required placeholder="写入审计日志" /></label>
                <div class="form-actions wide"><button class="pixel-btn" type="submit">保存用户信息</button><button class="pixel-btn secondary" type="button" @click="sendAdminNotice">发送修改通知</button><p v-if="adminUserMessage" :class="adminUserSuccess ? 'success-text' : 'error-text'">{{ adminUserMessage }}</p></div>
              </form>
            </div>
          </article>
        </section>

        <section v-else-if="currentPage === 'rbac'" class="page-stack">
          <article class="paper form-panel">
            <div class="table-title"><div><p class="eyebrow">RBAC</p><h3>角色权限管理</h3></div><button class="pixel-btn secondary" type="button" @click="fetchRbac">刷新</button></div>
            <div class="rbac-reason">
              <label><span>本次权限调整原因</span><input v-model.trim="rbacReason" placeholder="例如：答辩演示权限调整 / 角色职责变更" /></label>
              <p>勾选或取消勾选会立即写入角色权限表，并记录到审计日志。</p>
            </div>
            <div class="rbac-table">
              <table>
                <thead><tr><th>权限</th><th v-for="role in rbac.roles" :key="role.code">{{ role.name }}</th></tr></thead>
                <tbody>
                  <tr v-for="permission in rbac.permissions" :key="permission.code">
                    <td><strong>{{ permission.name }}</strong><small>{{ permission.code }}</small></td>
                    <td v-for="role in rbac.roles" :key="role.code">
                      <input type="checkbox" :checked="hasPermission(role.code, permission.code)" @change="togglePermission(role.code, permission.code, $event.target.checked)" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="rbacMessage" :class="rbacSuccess ? 'success-text rbac-message' : 'error-text rbac-message'">{{ rbacMessage }}</p>
          </article>
        </section>

        <section v-else-if="currentPage === 'messages'" class="page-stack">
          <article class="paper form-panel">
            <div class="table-title"><div><p class="eyebrow">Mailbox</p><h3>站内信箱</h3></div><div class="toolbar"><button class="pixel-btn secondary" type="button" :class="{ active: messageTab === 'inbox' }" @click="setMessageTab('inbox')">收件箱</button><button class="pixel-btn secondary" type="button" :class="{ active: messageTab === 'sent' }" @click="setMessageTab('sent')">已发送</button><button class="pixel-btn" type="button" @click="showCompose = !showCompose">写信</button></div></div>
            <form v-if="showCompose" class="profile-form" @submit.prevent="submitMessage"><label><span>收件人账号</span><input v-model.trim="composeForm.receiverUsername" required /></label><label><span>主题</span><input v-model.trim="composeForm.title" required /></label><label class="wide"><span>内容</span><textarea v-model.trim="composeForm.content" required></textarea></label><div class="form-actions wide"><button class="pixel-btn" type="submit" :disabled="messageSending">{{ messageSending ? '发送中...' : '发送' }}</button><p v-if="messageText" :class="messageSuccess ? 'success-text' : 'error-text'">{{ messageText }}</p></div></form>
          </article>
          <DataTable :title="messageTab === 'inbox' ? '收件箱' : '已发送'" eyebrow="Message" :columns="messageTab === 'inbox' ? inboxMessageColumns : sentMessageColumns" :rows="messageTab === 'inbox' ? messages : sentMessages" :loading="loading" @refresh="fetchMessages" @row-click="openMessage" />
          <article v-if="selectedMessage" class="paper message-detail"><div class="table-title"><div><p class="eyebrow">Message Detail</p><h3>{{ selectedMessage.title }}</h3></div><button class="pixel-btn secondary" type="button" @click="selectedMessage = null">关闭</button></div><div class="message-meta"><span>时间：{{ selectedMessage.created_at }}</span><span>{{ messageTab === 'inbox' ? '发件人：' + selectedMessage.sender_name : '收件人：' + selectedMessage.receiver_name }}</span><span class="badge" :class="selectedMessage.readLabel.includes('已读') ? 'ok' : 'warn'">{{ selectedMessage.readLabel }}</span></div><div class="message-body">{{ selectedMessage.content }}</div></article>
        </section>

        <section v-else-if="currentPage === 'profile'" class="profile-page">
          <article class="paper profile-summary"><img class="profile-avatar" :src="avatarForRole(session.user.role)" alt="" /><h3>{{ session.user.displayName }}</h3><p>{{ session.user.username }} · {{ session.user.roleName }}</p><div class="profile-note">{{ profileSummary }}</div></article>
          <article class="paper form-panel profile-main">
            <div class="table-title"><div><p class="eyebrow">Profile</p><h3>个人资料与账号安全</h3></div><button class="pixel-btn" type="button" :disabled="profileSaving" @click="saveProfile">{{ profileSaving ? '保存中...' : '保存资料' }}</button></div>
            <form class="profile-form" @submit.prevent="saveProfile"><label><span>联系电话</span><input v-model.trim="profileForm.phone" /></label><label><span>邮箱</span><input v-model.trim="profileForm.email" type="email" /></label><label><span>部门 / 学院</span><input v-model.trim="profileForm.department" /></label><label><span>办公室 / 咨询地点</span><input v-model.trim="profileForm.officeLocation" /></label><label><span>紧急联系人</span><input v-model.trim="profileForm.emergencyContact" /></label><label><span>偏好说明</span><input v-model.trim="profileForm.preferenceNote" /></label><label class="wide"><span>个人简介</span><textarea v-model.trim="profileForm.bio"></textarea></label><p v-if="profileMessage" :class="profileSuccess ? 'success-text wide' : 'error-text wide'">{{ profileMessage }}</p></form>
            <form class="profile-form password-form" @submit.prevent="changePassword"><label><span>原密码</span><input v-model="passwordForm.oldPassword" type="password" /></label><label><span>新密码</span><input v-model="passwordForm.newPassword" type="password" /></label><div class="form-actions wide"><button class="pixel-btn secondary" type="submit">修改密码</button><p v-if="passwordMessage" :class="passwordSuccess ? 'success-text' : 'error-text'">{{ passwordMessage }}</p></div></form>
          </article>
        </section>

        <section v-else-if="currentPage === 'statistics'" class="statistics-page">
          <div class="stats-hero paper"><div><p class="eyebrow">Statistics</p><h3>运营统计</h3><p>{{ statistics?.periodLabel || '按日 / 周 / 月查看预约、咨询、访问与风险结构。' }}</p></div><div class="range-tabs"><button v-for="item in statisticRanges" :key="item.key" type="button" :class="{ active: statisticRange === item.key }" @click="setStatisticRange(item.key)">{{ item.label }}</button></div></div>
          <section class="stats-grid"><article v-for="card in statisticOverviewCards" :key="card.key" class="stat-card paper insight-card"><img :src="cardIcon(card.key)" alt="" /><div><span>{{ card.label }}</span><strong>{{ card.value }} <small>{{ card.unit }}</small></strong></div></article></section>
          <DataTable title="预约状态统计" eyebrow="Appointment Status" :columns="appointmentStatusColumns" :rows="statistics?.appointmentStatusDistribution || []" :loading="loading" @refresh="loadStatistics" />
          <DataTable title="咨询师工作量" eyebrow="Doctor Workload" :columns="doctorWorkloadColumns" :rows="statistics?.doctorWorkload || []" :loading="loading" @refresh="loadStatistics" />
          <DataTable title="关注等级分布" eyebrow="Assessment Level" :columns="assessmentLevelColumns" :rows="statistics?.assessmentLevelDistribution || []" :loading="loading" @refresh="loadStatistics" />
          <DataTable title="来访者活跃度" eyebrow="Patient Activity" :columns="patientActivityColumns" :rows="statistics?.patientActivity || []" :loading="loading" @refresh="loadStatistics" />
          <DataTable title="咨询师-来访者交互次数" eyebrow="Doctor Patient Pair" :columns="doctorPatientPairColumns" :rows="statistics?.doctorPatientPairs || []" :loading="loading" @refresh="loadStatistics" />
          <section class="stats-grid">
            <article v-for="card in operationSignalCards" :key="card.key" class="stat-card paper insight-card">
              <img :src="card.icon" alt="" />
              <div><span>{{ card.label }}</span><strong>{{ card.value }} <small>{{ card.unit }}</small></strong></div>
            </article>
          </section>
        </section>

        <article v-else class="paper empty-page"><p class="eyebrow">{{ currentNavLabel }}</p><h2>页面已纳入导航</h2><p>该模块后续继续补齐完整表单。</p></article>
      </section>

      <footer class="app-footer wood"><span>{{ footerTip }}</span><span>心桥心理咨询协作管理平台</span></footer>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import DataTable from './components/DataTable.vue';
import { apiDelete, apiGet, apiPost, apiPut, clearSession, loadSession, login, register, saveSession } from './api/client';

const asset = path => `/assets/${path}`;
const icons = {
  logoPeople: asset('xinqiao/icons/png/logo_people.png'),
  overview: asset('xinqiao/icons/png/overview_grid.png'),
  calendar: asset('xinqiao/icons/png/calendar.png'),
  plus: asset('xinqiao/icons/png/plus_button.png'),
  team: asset('xinqiao/icons/png/team.png'),
  archive: asset('xinqiao/icons/png/archive_file.png'),
  clipboard: asset('xinqiao/icons/png/clipboard.png'),
  sectionTeam: asset('xinqiao/icons/png/section_team.png'),
  leaf: asset('xinqiao/icons/png/leaf.png'),
  heart: asset('xinqiao/icons/png/heart.png'),
  shield: asset('xinqiao/icons/png/shield_check.png'),
  star: asset('xinqiao/icons/png/star.png'),
  house: asset('xinqiao/env/png/house.png'),
  patient: asset('xinqiao/avatars/png/visitor_01.png'),
  doctor: asset('xinqiao/avatars/png/counselor_01.png'),
  admin: asset('xinqiao/avatars/png/round_avatar_orange.png')
};

const session = ref(loadSession());
const currentPage = ref(session.value?.navigation?.[0]?.key || 'dashboard');
const authMode = ref('login');
const loginForm = ref({ username: 'admin', password: 'admin123' });
const loggingIn = ref(false);
const loginError = ref('');
const registering = ref(false);
const registerError = ref('');
const registerForm = ref({ username: '', password: '', confirmPassword: '', displayName: '', name: '', studentNo: '', college: '', grade: '', primaryTopic: '' });
const loading = ref(false);

const appointments = ref([]);
const patients = ref([]);
const doctors = ref([]);
const visitRecords = ref([]);
const users = ref([]);
const auditLogs = ref([]);
const statistics = ref(null);
const rbac = ref({ roles: [], permissions: [], rolePermissions: [] });
const selectedUser = ref(null);
const adminUserMessage = ref('');
const adminUserSuccess = ref(false);
const adminUserForm = ref({});
const rbacMessage = ref('');
const rbacSuccess = ref(false);
const rbacReason = ref('角色职责调整');
const statisticRange = ref('week');
const statisticRanges = [{ key: 'day', label: '今日' }, { key: 'week', label: '近 7 日' }, { key: 'month', label: '近 30 日' }];

const profileData = ref(null);
const profileSaving = ref(false);
const profileMessage = ref('');
const profileSuccess = ref(false);
const profileForm = ref({ phone: '', email: '', department: '', officeLocation: '', emergencyContact: '', preferenceNote: '', bio: '' });
const passwordForm = ref({ oldPassword: '', newPassword: '' });
const passwordMessage = ref('');
const passwordSuccess = ref(false);
const messageTab = ref('inbox');
const messages = ref([]);
const sentMessages = ref([]);
const showCompose = ref(false);
const messageSending = ref(false);
const messageText = ref('');
const messageSuccess = ref(false);
const selectedMessage = ref(null);
const composeForm = ref({ receiverUsername: '', title: '', content: '' });
const bookingSubmitting = ref(false);
const bookingMessage = ref('');
const bookingSuccess = ref(false);
const bookingForm = ref({ patientId: null, doctorId: null, topic: '考试焦虑', appointmentTime: '2026-07-23T14:00', location: '心桥心理咨询室', reason: '希望预约一次个体咨询' });

const demoAccounts = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '咨询师', username: 'doctor_zhang', password: 'doctor123' },
  { label: '辅导员', username: 'counselor_wang', password: 'doctor123' },
  { label: '来访者', username: 'patient_chen', password: 'patient123' }
];

const appointmentColumns = [{ key: 'appointment_time', label: '时间' }, { key: 'doctor_name', label: '咨询师' }, { key: 'patient_name', label: '来访者' }, { key: 'topic', label: '类型/主题' }, { key: 'status', label: '状态', badge: true }, { key: 'location', label: '地点/方式' }];
const patientColumns = [{ key: 'name', label: '来访者' }, { key: 'assessment_level', label: '关注等级', badge: true }, { key: 'primary_topic', label: '重点标签' }, { key: 'follow_plan', label: '跟进计划' }];
const doctorColumns = [{ key: 'name', label: '咨询师' }, { key: 'title', label: '职称' }, { key: 'specialties', label: '擅长方向' }, { key: 'schedule_note', label: '本周排班' }];
const recordColumns = [{ key: 'visit_time', label: '咨询时间' }, { key: 'doctor_name', label: '咨询师' }, { key: 'patient_name', label: '来访者' }, { key: 'diagnosis_summary', label: '内容摘要' }, { key: 'next_plan', label: '下一步' }];
const userColumns = [{ key: 'created_at', label: '日期' }, { key: 'display_name', label: '用户' }, { key: 'role', label: '身份', badge: true }, { key: 'enabledLabel', label: '状态', badge: true }];
const auditLogColumns = [{ key: 'created_at', label: '操作时间' }, { key: 'operator_name', label: '操作人' }, { key: 'operation_type', label: '操作类型', badge: true }, { key: 'target_description', label: '操作对象' }, { key: 'reason', label: '操作原因' }, { key: 'ip_address', label: 'IP' }];
const inboxMessageColumns = [{ key: 'created_at', label: '时间' }, { key: 'sender_name', label: '发件人' }, { key: 'title', label: '主题' }, { key: 'readLabel', label: '状态', badge: true }];
const sentMessageColumns = [{ key: 'created_at', label: '时间' }, { key: 'receiver_name', label: '收件人' }, { key: 'title', label: '主题' }, { key: 'readLabel', label: '状态', badge: true }];
const appointmentStatusColumns = [{ key: 'status', label: '预约状态', badge: true }, { key: 'total', label: '数量' }];
const doctorWorkloadColumns = [{ key: 'name', label: '咨询师' }, { key: 'appointment_count', label: '预约数' }, { key: 'visit_count', label: '咨询记录' }, { key: 'patient_count', label: '关联来访者' }];
const assessmentLevelColumns = [{ key: 'assessment_level', label: '关注等级', badge: true }, { key: 'total', label: '来访者数量' }];
const patientActivityColumns = [{ key: 'name', label: '来访者' }, { key: 'assessment_level', label: '关注等级', badge: true }, { key: 'appointment_count', label: '预约数' }, { key: 'visit_count', label: '咨询记录' }, { key: 'doctor_count', label: '接触咨询师' }];
const doctorPatientPairColumns = [{ key: 'doctor_name', label: '咨询师' }, { key: 'patient_name', label: '来访者' }, { key: 'appointment_count', label: '预约次数' }, { key: 'visit_count', label: '诊断/咨询次数' }];

const headerTitle = computed(() => session.value?.workspace.title || '');
const currentNavLabel = computed(() => session.value?.navigation.find(item => item.key === currentPage.value)?.label || '功能页');
const heroIcon = computed(() => session.value?.user.role === 'PATIENT' ? icons.heart : icons.overview);
const focusPatients = computed(() => patients.value.filter(row => ['关注', '重点'].includes(row.assessment_level)));
const nextAppointment = computed(() => appointments.value[0] || null);
const profileSummary = computed(() => session.value?.user.role === 'PATIENT' ? '这里保存你的预约信息、咨询偏好和联系资料，仅本人可见。' : '资料更新会进入审计链路。');
const footerTip = computed(() => {
  if (!session.value) return '';
  if (session.value.user.role === 'ADMIN') return '提示：您可以查看和管理所有预约、来访者、咨询师、辅导员、系统用户与权限矩阵。';
  if (session.value.user.role === 'COUNSELOR') return '提示：您可以查看绑定学生的关注等级、预约状态，并接收预约联动通知。';
  if (session.value.user.role === 'DOCTOR') return '提示：您只能访问与自己存在咨询关系的来访者数据。';
  return '你的所有信息均受严格保护，仅本人可见。若需帮助，请联系咨询师或平台客服。';
});
const statisticOverviewCards = computed(() => {
  const overview = statistics.value?.overview || {};
  return [
    { key: 'appointments', label: '周期预约', value: overview.periodAppointments ?? 0, unit: '次' },
    { key: 'visitRecords', label: '周期咨询记录', value: overview.periodVisitRecords ?? 0, unit: '条' },
    { key: 'patients', label: '活跃来访者', value: overview.activePatients ?? 0, unit: '人' },
    { key: 'doctors', label: '活跃咨询师', value: overview.activeDoctors ?? 0, unit: '人' }
  ];
});
const operationSignalCards = computed(() => {
  const signals = statistics.value?.operationSignals || {};
  return [
    { key: 'sensitiveViews', label: '敏感详情查看', value: signals.sensitiveViews ?? 0, unit: '次', icon: icons.shield },
    { key: 'profileUpdates', label: '资料更新', value: signals.profileUpdates ?? 0, unit: '次', icon: icons.clipboard },
    { key: 'appointmentCreates', label: '预约创建', value: signals.appointmentCreates ?? 0, unit: '次', icon: icons.calendar }
  ];
});

function fillAccount(account) { loginForm.value = { username: account.username, password: account.password }; }
async function handleLogin() { loggingIn.value = true; loginError.value = ''; try { session.value = await login(loginForm.value); saveSession(session.value); currentPage.value = session.value.navigation[0].key; await loadWorkspaceData(); } catch (error) { loginError.value = error.message || '登录失败'; } finally { loggingIn.value = false; } }
async function handleRegister() { registerError.value = ''; if (registerForm.value.password !== registerForm.value.confirmPassword) { registerError.value = '两次输入的密码不一致'; return; } registering.value = true; try { const payload = { ...registerForm.value }; delete payload.confirmPassword; session.value = await register(payload); saveSession(session.value); currentPage.value = session.value.navigation[0].key; await loadWorkspaceData(); } catch (error) { registerError.value = error.message || '注册失败'; } finally { registering.value = false; } }

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
      tasks.push(apiGet('/api/admin/users', session.value).then(data => { users.value = data.map(normalizeUserRow); if (!selectedUser.value && users.value.length) selectUser(users.value[0]); }));
      tasks.push(apiGet('/api/admin/audit-logs', session.value).then(data => { auditLogs.value = data; }));
      tasks.push(loadStatistics());
      tasks.push(fetchRbac());
    }
    if (currentPage.value === 'profile') tasks.push(fetchProfile());
    if (currentPage.value === 'messages') tasks.push(fetchMessages());
    await Promise.all(tasks);
    ensureBookingDefaults();
  } finally { loading.value = false; }
}

async function loadStatistics() { if (session.value?.user.role === 'ADMIN') statistics.value = await apiGet(`/api/admin/statistics?range=${statisticRange.value}`, session.value); }
async function fetchRbac() { if (session.value?.user.role === 'ADMIN') rbac.value = await apiGet('/api/admin/rbac', session.value); }
function hasPermission(roleCode, permissionCode) { return rbac.value.rolePermissions.some(item => item.role_code === roleCode && item.permission_code === permissionCode); }
async function togglePermission(roleCode, permissionCode, checked) {
  const reason = rbacReason.value.trim();
  if (!reason) {
    rbacSuccess.value = false;
    rbacMessage.value = '请先填写权限调整原因';
    await fetchRbac();
    return;
  }
  try {
    if (checked) await apiPost(`/api/admin/rbac/${roleCode}/${permissionCode}`, session.value, { reason });
    else await apiDelete(`/api/admin/rbac/${roleCode}/${permissionCode}`, session.value, { reason });
    rbacSuccess.value = true;
    rbacMessage.value = '权限已更新，并已写入审计日志';
    await fetchRbac();
  } catch (error) {
    rbacSuccess.value = false;
    rbacMessage.value = error.message;
    await fetchRbac();
  }
}
async function setStatisticRange(range) { statisticRange.value = range; await loadStatistics(); }

function ensureBookingDefaults() { if (!bookingForm.value.doctorId && doctors.value.length > 0) bookingForm.value.doctorId = doctors.value[0].id; if (!bookingForm.value.patientId && patients.value.length > 0) bookingForm.value.patientId = patients.value[0].id; }
async function setCurrentPage(key) { currentPage.value = key; bookingMessage.value = ''; messageText.value = ''; profileMessage.value = ''; if (session.value) await loadWorkspaceData(); }
async function handlePrimaryAction() { if (session.value?.user.role === 'ADMIN' || session.value?.user.role === 'PATIENT') await setCurrentPage('bookings'); }
async function submitBooking() { bookingMessage.value = ''; bookingSuccess.value = false; bookingSubmitting.value = true; try { const payload = { ...bookingForm.value }; if (session.value.user.role !== 'ADMIN') delete payload.patientId; const result = await apiPost('/api/appointments', session.value, payload); bookingSuccess.value = true; bookingMessage.value = result.message || '预约申请已提交'; await loadWorkspaceData(); currentPage.value = 'appointments'; } catch (error) { bookingMessage.value = error.message || '预约提交失败'; } finally { bookingSubmitting.value = false; } }

function normalizeUserRow(user) {
  return { ...user, enabledLabel: user.enabled ? '启用' : '停用' };
}

function selectUser(user) {
  selectedUser.value = user;
  adminUserForm.value = {
    displayName: user.display_name,
    role: user.role,
    enabled: Boolean(user.enabled),
    phone: user.phone || '',
    email: user.email || '',
    department: user.department || '',
    officeLocation: user.office_location || '',
    emergencyContact: user.emergency_contact || '',
    preferenceNote: user.preference_note || '',
    bio: user.bio || '',
    reason: ''
  };
  adminUserMessage.value = '';
}
async function saveSelectedUser() { try { await apiPut(`/api/admin/users/${selectedUser.value.id}`, session.value, adminUserForm.value); adminUserSuccess.value = true; adminUserMessage.value = '用户信息已保存并写入审计日志'; await loadWorkspaceData(); } catch (error) { adminUserSuccess.value = false; adminUserMessage.value = error.message; } }
async function sendAdminNotice() { const title = window.prompt('通知标题', '资料修改通知'); const content = window.prompt('通知内容', '管理员已更新你的账号资料，请登录后查看。'); const reason = window.prompt('发送原因'); if (!title || !content || !reason) return; try { await apiPost(`/api/admin/users/${selectedUser.value.id}/notify`, session.value, { title, content, reason }); adminUserSuccess.value = true; adminUserMessage.value = '通知已发送并写入审计日志'; } catch (error) { adminUserSuccess.value = false; adminUserMessage.value = error.message; } }

async function fetchProfile() { const data = await apiGet('/api/profile', session.value); profileData.value = data; profileForm.value = { phone: data.profile?.phone || '', email: data.profile?.email || '', department: data.profile?.department || '', officeLocation: data.profile?.officeLocation || '', emergencyContact: data.profile?.emergencyContact || '', preferenceNote: data.profile?.preferenceNote || '', bio: data.profile?.bio || '' }; }
async function saveProfile() { profileSaving.value = true; try { await apiPut('/api/profile', session.value, profileForm.value); profileSuccess.value = true; profileMessage.value = '资料已保存'; } catch (error) { profileSuccess.value = false; profileMessage.value = error.message; } finally { profileSaving.value = false; } }
async function changePassword() { try { await apiPost('/api/account/password', session.value, passwordForm.value); passwordSuccess.value = true; passwordMessage.value = '密码已修改，请下次使用新密码登录'; passwordForm.value = { oldPassword: '', newPassword: '' }; } catch (error) { passwordSuccess.value = false; passwordMessage.value = error.message; } }

async function fetchMessages() { const [inbox, sent] = await Promise.all([apiGet('/api/messages', session.value), apiGet('/api/messages/sent', session.value)]); messages.value = inbox.map(item => ({ ...item, readLabel: item.is_read ? '已读' : '未读' })); sentMessages.value = sent.map(item => ({ ...item, readLabel: item.is_read ? '对方已读' : '对方未读' })); }
async function setMessageTab(tab) { messageTab.value = tab; selectedMessage.value = null; await fetchMessages(); }
async function openMessage(row) { selectedMessage.value = row; if (messageTab.value === 'inbox' && row.readLabel === '未读') { await apiPut(`/api/messages/${row.id}/read`, session.value, {}); await fetchMessages(); } }
async function submitMessage() { messageSending.value = true; try { await apiPost('/api/messages', session.value, composeForm.value); composeForm.value = { receiverUsername: '', title: '', content: '' }; showCompose.value = false; messageSuccess.value = true; messageText.value = '信件已发送'; await fetchMessages(); } catch (error) { messageSuccess.value = false; messageText.value = error.message; } finally { messageSending.value = false; } }

function logout() { session.value = null; currentPage.value = 'dashboard'; clearSession(); }
function avatarForRole(role) { if (role === 'ADMIN') return icons.admin; if (role === 'DOCTOR' || role === 'COUNSELOR') return icons.doctor; return icons.patient; }
function navIcon(key) { return { dashboard: icons.overview, appointments: icons.calendar, bookings: icons.plus, doctors: icons.team, patients: icons.archive, visitRecords: icons.clipboard, users: icons.sectionTeam, auditLogs: icons.shield, statistics: icons.leaf, rbac: icons.shield, followPlans: icons.leaf, profile: icons.team, messages: icons.clipboard }[key] || icons.clipboard; }
function cardIcon(key) { const lower = String(key).toLowerCase(); if (lower.includes('appointment')) return icons.calendar; if (lower.includes('doctor')) return icons.team; if (lower.includes('patient') || lower.includes('focus')) return icons.heart; if (lower.includes('health')) return icons.shield; if (lower.includes('record')) return icons.clipboard; return icons.star; }
function compactCardValue(value) {
  const text = String(value ?? '');
  return text.length > 8 ? `${text.slice(0, 8)}...` : text;
}

watch(session, value => { if (value) saveSession(value); });
onMounted(loadWorkspaceData);
</script>
