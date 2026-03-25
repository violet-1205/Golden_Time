<script setup>
/** 공식 문의 접수 (Google 설문) — 새 창용 */
const INQUIRY_GOOGLE_FORM_URL =
  'https://docs.google.com/forms/d/e/1FAIpQLSce09hOyxBSOwn0y61RORUAnA83Ek8v517SpKnJ1gIdVHD0lQ/viewform'

/** 같은 설문을 페이지 안에 넣기 위한 임베드 URL */
const INQUIRY_GOOGLE_FORM_EMBED_URL = `${INQUIRY_GOOGLE_FORM_URL}?embedded=true`
</script>

<template>
  <div class="inquiry">
    <div class="panel">
      <div class="page-header">
        <div class="header-text">
          <h2 class="page-title">문의게시판</h2>
          <p class="page-subtitle">
            아래 양식에 작성해 주시면 문의가 접수됩니다.
          </p>
        </div>
        <a
          class="btn-open-new-tab"
          :href="INQUIRY_GOOGLE_FORM_URL"
          target="_blank"
          rel="noopener noreferrer"
        >
          새 창에서 열기
        </a>
      </div>

      <div class="form-embed-wrap">
        <iframe
          class="google-form-iframe"
          :src="INQUIRY_GOOGLE_FORM_EMBED_URL"
          title="문의 접수 양식 (Google Forms)"
          frameborder="0"
          marginwidth="0"
          marginheight="0"
          loading="lazy"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.inquiry {
  padding: 24px;
  background: #f8fafc;
  min-height: 100%;
}

.panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.page-header {
  padding: 24px 28px;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.header-text {
  flex: 1;
  min-width: 200px;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 8px;
}

.page-subtitle {
  margin: 0;
  font-size: 0.9rem;
  color: #64748b;
  line-height: 1.55;
  max-width: 52rem;
}

.btn-open-new-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  background: #fff;
  color: #1a73e8 !important;
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 600;
  border-radius: 6px;
  white-space: nowrap;
  flex-shrink: 0;
  border: 1px solid #c6dafc;
  transition: background 0.2s, border-color 0.2s;
}

.btn-open-new-tab:hover {
  background: #e8f0fe;
  color: #1557b0 !important;
}

.form-embed-wrap {
  margin: 0 28px 28px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  box-shadow: inset 0 1px 0 rgba(0, 0, 0, 0.03);
}

.google-form-iframe {
  display: block;
  width: 100%;
  min-height: 1000px;
  height: 1200px;
  border: 0;
  vertical-align: top;
}

@media (max-width: 640px) {
  .google-form-iframe {
    min-height: 800px;
    height: 1000px;
  }
}
</style>
