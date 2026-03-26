<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'

const router = useRouter()
const { register } = useAuth()

const form = ref({
  username: '',
  password: '',
  name: '',
  phone: '',
  email: '',
  address: '',
  vehicleNumber: '',
  deviceSerial: '',
})

const passwordFieldType = ref('password')

function togglePasswordVisibility() {
  passwordFieldType.value = passwordFieldType.value === 'password' ? 'text' : 'password'
}

const submitted = ref(false)
const touched = ref({})

const errors = computed(() => {
  const e = {}

  if (!form.value.username) {
    e.username = '아이디를 입력하세요.'
  } else if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.value.username)) {
    e.username = '4~20자 영문, 숫자, 밑줄만 가능합니다.'
  }

  if (!form.value.password) {
    e.password = '비밀번호를 입력하세요.'
  } else if (form.value.password.length < 6) {
    e.password = '6자 이상이어야 합니다.'
  }

  if (!form.value.name) {
    e.name = '이름을 입력하세요.'
  } else if (form.value.name.length < 2) {
    e.name = '2자 이상이어야 합니다.'
  }

  if (!form.value.phone) {
    e.phone = '전화번호를 입력하세요.'
  } else if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(form.value.phone.replace(/-/g, ''))) {
    e.phone = '올바른 번호를 입력하세요.'
  }

  if (!form.value.email) {
    e.email = '이메일을 입력하세요.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    e.email = '올바른 이메일 형식이 아닙니다.'
  }

  if (!form.value.address) {
    e.address = '주소를 입력하세요.'
  }

  if (!form.value.vehicleNumber) {
    e.vehicleNumber = '차량 번호를 입력하세요.'
  }

  if (!form.value.deviceSerial) {
    e.deviceSerial = '단말기 번호를 입력하세요.'
  }

  return e
})

const isValid = computed(() =>
  Object.keys(errors.value).length === 0 &&
  form.value.username && form.value.password &&
  form.value.name && form.value.phone &&
  form.value.email && form.value.address &&
  form.value.vehicleNumber && form.value.deviceSerial
)

function touch(field) {
  touched.value[field] = true
}

function showError(field) {
  return !!touched.value[field] && !!errors.value[field]
}

async function handleRegister() {
  Object.keys(form.value).forEach((k) => (touched.value[k] = true))
  if (!isValid.value) return

  const result = await register(
    form.value.username,
    form.value.name,
    form.value.password,
    form.value.phone,
    form.value.email,
    form.value.address,
    form.value.vehicleNumber,
    `GT-${form.value.deviceSerial}`,
  )

  if (result.success) {
    submitted.value = true
  } else {
    touched.value['username'] = true
    alert(result.message)
  }
}
</script>

<template>
  <div class="register-wrapper">
    <div class="register-container">
      <h1 class="register-title">회원가입</h1>
      <p class="register-subtitle">GoldenTime 서비스 이용을 위해 정보를 입력해주세요.</p>

      <!-- 성공 화면 -->
      <div v-if="submitted" class="success-box">
        <div class="success-icon">✓</div>
        <h3>회원가입이 완료되었습니다!</h3>
        <p>이제 로그인하여 서비스를 이용하세요.</p>
        <button class="btn-submit" @click="router.push('/login')">로그인하러 가기</button>
      </div>

      <!-- 회원가입 폼 -->
      <form v-else @submit.prevent="handleRegister">
        <p class="section-label">기본 정보</p>

        <div class="form-grid">
          <div class="form-group">
            <label>아이디</label>
            <input
              v-model="form.username"
              type="text"
              :class="{ invalid: showError('username') }"
              @blur="touch('username')"
              @input="touch('username')"
            />
            <span v-if="showError('username')" class="error-msg">{{ errors.username }}</span>
          </div>

          <div class="form-group">
            <label>비밀번호</label>
            <div class="password-input-wrapper">
              <input
                v-model="form.password"
                :type="passwordFieldType"
                :class="{ invalid: showError('password') }"
                @blur="touch('password')"
                @input="touch('password')"
                autocomplete="new-password"
              />
              <span class="toggle-password" @click="togglePasswordVisibility">
                <svg v-if="passwordFieldType === 'password'" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-eye"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-eye-off"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.06M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              </span>
            </div>
            <span v-if="showError('password')" class="error-msg">{{ errors.password }}</span>
          </div>

          <div class="form-group">
            <label>이름</label>
            <input
              v-model="form.name"
              type="text"
              :class="{ invalid: showError('name') }"
              @blur="touch('name')"
              @input="touch('name')"
            />
            <span v-if="showError('name')" class="error-msg">{{ errors.name }}</span>
          </div>

          <div class="form-group">
            <label>전화번호</label>
            <input
              v-model="form.phone"
              type="tel"
              placeholder="010-0000-0000"
              :class="{ invalid: showError('phone') }"
              @blur="touch('phone')"
              @input="touch('phone')"
            />
            <span v-if="showError('phone')" class="error-msg">{{ errors.phone }}</span>
          </div>
        </div>

        <div class="form-group full">
          <label>이메일</label>
          <input
            v-model="form.email"
            type="email"
            :class="{ invalid: showError('email') }"
            @blur="touch('email')"
            @input="touch('email')"
          />
          <span v-if="showError('email')" class="error-msg">{{ errors.email }}</span>
        </div>

        <div class="form-group full">
          <label>주소</label>
          <input
            v-model="form.address"
            type="text"
            :class="{ invalid: showError('address') }"
            @blur="touch('address')"
            @input="touch('address')"
          />
          <span v-if="showError('address')" class="error-msg">{{ errors.address }}</span>
        </div>

        <p class="section-label">차량 및 단말기 정보</p>

        <div class="form-grid">
          <div class="form-group">
            <label>차량 번호</label>
            <input
              v-model="form.vehicleNumber"
              type="text"
              placeholder="예: 12가 3456"
              :class="{ invalid: showError('vehicleNumber') }"
              @blur="touch('vehicleNumber')"
              @input="touch('vehicleNumber')"
            />
            <span v-if="showError('vehicleNumber')" class="error-msg">{{ errors.vehicleNumber }}</span>
          </div>

          <div class="form-group">
            <label>단말기 시리얼</label>
            <div class="serial-input-wrapper">
              <span class="serial-prefix">GT-</span>
              <input
                v-model="form.deviceSerial"
                type="text"
                placeholder="001"
                :class="{ invalid: showError('deviceSerial') }"
                @blur="touch('deviceSerial')"
                @input="touch('deviceSerial')"
              />
            </div>
            <span v-if="showError('deviceSerial')" class="error-msg">{{ errors.deviceSerial }}</span>
          </div>
        </div>

        <button type="submit" class="btn-submit">회원가입 완료</button>
      </form>

      <div v-if="!submitted" class="register-footer">
        <span>이미 계정이 있으신가요?</span>
        <button class="btn-link" @click="router.push('/login')">로그인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f6f8;
  padding: 40px 16px;
}

.register-container {
  background: #fff;
  border-radius: 0;
  padding: 40px 42px;
  width: 100%;
  max-width: 500px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.register-title {
  text-align: center;
  font-size: 1.45rem;
  font-weight: 700;
  margin-bottom: 6px;
  color: #1a202c;
}

.register-subtitle {
  text-align: center;
  color: #718096;
  font-size: 0.84rem;
  margin-bottom: 26px;
}

.section-label {
  font-size: 0.78rem;
  color: #aaa;
  margin: 18px 0 10px;
  font-weight: 500;
  padding-bottom: 6px;
  border-bottom: 1px solid #eeeeee;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group.full {
  margin-top: 10px;
}

.password-input-wrapper {
  position: relative;
  width: 100%;
}

.password-input-wrapper input {
  padding-right: 45px !important; /* 아이콘 공간 확보 */
}

.serial-input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid #d1dbe8;
  background: #fff;
  transition: border-color 0.2s;
}

.serial-input-wrapper:focus-within {
  border-color: #1976d2;
}

.serial-prefix {
  padding: 0 10px;
  background: #f8f9fa;
  color: #4a5568;
  font-weight: 600;
  font-size: 0.88rem;
  border-right: 1px solid #d1dbe8;
  height: 100%;
  display: flex;
  align-items: center;
  min-height: 38px;
}

.serial-input-wrapper input {
  border: none !important;
  flex: 1;
  padding: 9px 12px;
}

.toggle-password {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  user-select: none;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #718096;
  border-radius: 4px;
  transition: all 0.2s;
}

.toggle-password:hover {
  background: #edf2f7;
  color: #2d3748;
}

.form-group label {
  font-size: 0.82rem;
  font-weight: 500;
  color: #4a5568;
}

.form-group input {
  padding: 9px 12px;
  border: 1px solid #d1dbe8;
  border-radius: 0;
  font-size: 0.88rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  color: #2d3748;
  font-family: inherit;
}

.form-group input:focus {
  border-color: #1976d2;
  outline: none;
}

.form-group input.invalid {
  border-color: #dc2626;
}

.error-msg {
  font-size: 0.76rem;
  color: #dc2626;
}

.btn-submit {
  width: 100%;
  padding: 11px;
  background: #1565c0;
  color: #fff;
  border: none;
  border-radius: 0;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 22px;
  transition: background 0.2s, box-shadow 0.2s;
  font-family: inherit;
  letter-spacing: 0.02em;
}

.btn-submit:hover {
  background: #0d47a1;
  box-shadow: 0 4px 14px rgba(13, 71, 161, 0.35);
}

.register-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 18px;
  font-size: 0.86rem;
  color: #718096;
}

.btn-link {
  background: none;
  border: none;
  color: #1565c0;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  font-family: inherit;
}

/* 성공 화면 */
.success-box {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #dcfce7;
  color: #16a34a;
  font-size: 1.6rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.success-box h3 {
  font-size: 1.1rem;
  margin-bottom: 8px;
  color: #1a202c;
}

.success-box p {
  color: #718096;
  margin-bottom: 24px;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .register-wrapper {
    padding: 22px 14px;
    align-items: flex-start;
  }

  .register-container {
    padding: 22px 18px;
    max-width: 520px;
  }

  .register-title {
    font-size: 1.25rem;
  }

  .register-subtitle {
    font-size: 0.8rem;
    margin-bottom: 18px;
  }

  .section-label {
    margin: 16px 0 10px;
  }

  /* 모바일: 2열 -> 1열 */
  .form-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .form-group.full {
    margin-top: 8px;
  }

  .form-group label {
    font-size: 0.8rem;
  }

  .form-group input {
    font-size: 0.86rem;
    padding: 10px 12px;
  }

  .serial-prefix {
    font-size: 0.84rem;
    min-height: 40px;
  }

  .toggle-password {
    width: 34px;
    height: 34px;
  }

  .btn-submit {
    margin-top: 18px;
    padding: 12px;
    font-size: 0.92rem;
  }
}

@media (max-width: 480px) {
  .register-wrapper {
    padding: 18px 12px;
  }

  .register-container {
    padding: 18px 14px;
  }

  .register-title {
    font-size: 1.18rem;
  }

  .register-footer {
    font-size: 0.82rem;
  }

  .btn-link {
    font-size: 0.82rem;
  }
}
</style>
